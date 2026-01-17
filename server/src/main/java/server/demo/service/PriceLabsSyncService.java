package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.dto.PriceLabsWebhookRequest;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.PriceLabsSyncLog;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.enums.SyncDirection;
import server.demo.enums.SyncType;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.PriceLabsSyncLogRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.PriceLabsCountryUtil;
import server.demo.util.PriceLabsIdUtil;
import server.demo.constants.PriceLabsSyncDefaults;
import server.demo.constants.PriceLabsSyncStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PriceLabsSyncService {
    private static final Logger logger = LoggerFactory.getLogger(PriceLabsSyncService.class);

    @Autowired private PriceLabsApiClient apiClient;
    @Autowired @Lazy private PriceLabsService priceLabsService;
    @Autowired private RoomTypeRepository roomTypeRepo;
    @Autowired private PricePlanRepository pricePlanRepo;
    @Autowired private RoomTypePricePlanRepository rtppRepo;
    @Autowired private RoomPriceRepository roomPriceRepo;
    @Autowired private RoomRepository roomRepo;
    @Autowired private StoreRepository storeRepo;
    @Autowired private PriceLabsIntegrationRepository integrationRepo;
    @Autowired private PriceLabsSyncLogRepository syncLogRepo;
    @Autowired private PriceLabsConnectionRepository connectionRepo;
    @Autowired private ReservationRepository reservationRepository;

    @Transactional
    public void syncAll() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        logger.info("Full sync start for store {}", storeId);

        try {
            migrateConnectionListingIds(storeId);

            Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
            validateStoreForPriceLabs(store);

            logger.info("Step 1: Register integration with PriceLabs");
            apiClient.registerIntegration();

            logger.info("Step 2: Sync listings");
            syncListings(storeId);

            logger.info("Step 3: Sync rate plans");
            syncRatePlans(storeId);

            LocalDate start = LocalDate.now();
            // PriceLabs 要求首次日历推送至少覆盖 1 年数据，否则会返回 "less than a year of data is not allowed"
            LocalDate end = start.plusYears(1).minusDays(1);
            logger.info("Step 4: Sync calendar {} to {}", start, end);
            syncCalendar(storeId, start, end);

            LocalDateTime syncTime = LocalDateTime.now();
            integrationRepo.findByStoreId(storeId).ifPresent(integration -> {
                integration.setLastListingSyncAt(syncTime);
                integration.setLastPriceSyncAt(syncTime);
                integrationRepo.save(integration);
            });

            LocalDateTime now = LocalDateTime.now();
            connectionRepo.findByStoreId(storeId).forEach(conn -> {
                if (conn.getIsEnabled()) {
                    conn.setSyncStatus(PriceLabsSyncStatus.CONNECTED);
                    conn.setLastSyncAt(now);
                    conn.setErrorMessage(null);
                    connectionRepo.save(conn);
                }
            });

            PriceLabsSyncLog log = new PriceLabsSyncLog();
            log.setStoreId(storeId);
            log.setSyncType(SyncType.LISTING);
            log.setDirection(SyncDirection.OUTBOUND);
            log.setStatus(server.demo.enums.SyncStatus.SUCCESS);
            log.setAffectedCount(0);
            syncLogRepo.save(log);

            logger.info("Full sync complete");
        } catch (Exception e) {
            markEnabledConnectionsAsError(storeId, e.getMessage());

            PriceLabsSyncLog log = new PriceLabsSyncLog();
            log.setStoreId(storeId);
            log.setSyncType(SyncType.LISTING);
            log.setDirection(SyncDirection.OUTBOUND);
            log.setStatus(server.demo.enums.SyncStatus.FAILURE);
            log.setErrorMessage(e.getMessage());
            syncLogRepo.save(log);

            throw e;
        }
    }

    /**
     * PMS 主动从 PriceLabs 拉取推荐价（get_prices），并按 webhook 处理逻辑落库（room_prices/channel_prices/改价记录）。
     * 用于“立即同步”场景的兜底拉取。
     */
    @Transactional
    public void pullPricesForNextDays(Integer days) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        int pullDays = clampSyncDays(days);

        List<PriceLabsConnection> enabledConnections = connectionRepo.findByStoreId(storeId).stream()
                .filter(conn -> Boolean.TRUE.equals(conn.getIsEnabled()))
                .collect(Collectors.toList());

        if (enabledConnections.isEmpty()) {
            logger.warn("[PriceLabsPull] skip get_prices because no enabled PriceLabs connections. storeId={}", storeId);
            return;
        }

        List<String> listingIds = enabledConnections.stream()
                .map(conn -> {
                    if (conn.getPriceLabsListingId() != null && !conn.getPriceLabsListingId().isBlank()) {
                        return conn.getPriceLabsListingId().trim();
                    }
                    if (conn.getRoomType() != null && conn.getRoomType().getId() != null) {
                        return PriceLabsIdUtil.formatListingId(storeId, conn.getRoomType().getId());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (listingIds.isEmpty()) {
            logger.warn("[PriceLabsPull] skip get_prices because listingIds empty. storeId={}", storeId);
            return;
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(pullDays - 1L);

        PriceLabsApiClient.GetPricesResponse res = apiClient.getPrices(listingIds, startDate, endDate, false);
        if (res == null || !res.isSuccess() || res.getData() == null || res.getData().isEmpty()) {
            String msg = res != null ? res.getMessage() : "no response";
            throw new RuntimeException("get_prices 拉取失败: " + (msg != null ? msg : "unknown"));
        }

        PriceLabsWebhookRequest webhookRequest = new PriceLabsWebhookRequest();
        webhookRequest.setType("price_update");

        List<PriceLabsWebhookRequest.ListingData> data = new ArrayList<>();
        for (PriceLabsApiClient.GetPricesCalendarData row : res.getData()) {
            if (row == null || row.getListingId() == null || row.getListingId().isBlank()) {
                continue;
            }
            PriceLabsWebhookRequest.ListingData ld = new PriceLabsWebhookRequest.ListingData();
            ld.setListingId(row.getListingId());
            if (row.getRatePlanId() != null && !row.getRatePlanId().isBlank()) {
                ld.setRatePlanId(row.getRatePlanId());
            }

            List<PriceLabsWebhookRequest.CalendarData> calendar = new ArrayList<>();
            if (row.getCalendar() != null) {
                for (PriceLabsApiClient.GetPricesCalendarEntry ce : row.getCalendar()) {
                    if (ce == null || ce.getDate() == null || ce.getPrice() == null) {
                        continue;
                    }
                    PriceLabsWebhookRequest.CalendarData cd = new PriceLabsWebhookRequest.CalendarData();
                    cd.setDate(ce.getDate());
                    cd.setPrice(ce.getPrice());
                    cd.setMinStay(ce.getMinStay());
                    cd.setMaxStay(ce.getMaxStay());
                    calendar.add(cd);
                }
            }
            ld.setCalendar(calendar);
            data.add(ld);
        }

        if (data.isEmpty()) {
            logger.warn("[PriceLabsPull] get_prices returned empty calendar data. storeId={}", storeId);
            return;
        }

        webhookRequest.setData(data);
        priceLabsService.handleWebhookPriceUpdate(webhookRequest);
        logger.info("[PriceLabsPull] get_prices applied. storeId={}, listingCount={}", storeId, data.size());
    }

    /**
     * PriceLabs SwaggerHub pull_sync: POST /get_prices (body.sync)
     * 稳定优先：delta_only 固定 false，按“启用连接”逐个 listing_id 拉取，再复用 webhook 落库逻辑。
     */
    @Transactional
    public void pullPricesForNextDaysPullSync(Integer days) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        int pullDays = clampSyncDays(days);

        List<PriceLabsConnection> enabledConnections = connectionRepo.findByStoreId(storeId).stream()
                .filter(conn -> Boolean.TRUE.equals(conn.getIsEnabled()))
                .collect(Collectors.toList());

        if (enabledConnections.isEmpty()) {
            logger.warn("[PriceLabsPull] skip get_prices(pull_sync) because no enabled PriceLabs connections. storeId={}", storeId);
            return;
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(pullDays - 1L);

        List<String> failures = new ArrayList<>();
        List<PriceLabsWebhookRequest.ListingData> data = new ArrayList<>();

        for (PriceLabsConnection conn : enabledConnections) {
            if (conn == null) continue;

            String listingId = null;
            if (conn.getPriceLabsListingId() != null && !conn.getPriceLabsListingId().isBlank()) {
                listingId = conn.getPriceLabsListingId().trim();
            } else if (conn.getRoomType() != null && conn.getRoomType().getId() != null) {
                listingId = PriceLabsIdUtil.formatListingId(storeId, conn.getRoomType().getId());
            }
            if (listingId == null || listingId.isBlank()) {
                continue;
            }

            String ratePlanId = null;
            if (conn.getPricePlan() != null && conn.getPricePlan().getId() != null) {
                ratePlanId = PriceLabsIdUtil.formatRatePlanId(conn.getPricePlan().getId());
            }

            try {
                PriceLabsApiClient.PullSyncGetPricesResponse res = apiClient.pullSyncGetPrices(listingId, ratePlanId, false);
                if (res == null || res.getData() == null || res.getData().isEmpty()) {
                    continue;
                }

                List<PriceLabsWebhookRequest.CalendarData> calendar = new ArrayList<>();
                for (PriceLabsApiClient.PullSyncPriceEntry ce : res.getData()) {
                    if (ce == null || ce.getDate() == null || ce.getPrice() == null) {
                        continue;
                    }
                    LocalDate d;
                    try {
                        d = LocalDate.parse(ce.getDate());
                    } catch (Exception ignored) {
                        continue;
                    }
                    if (d.isBefore(startDate) || d.isAfter(endDate)) {
                        continue;
                    }

                    PriceLabsWebhookRequest.CalendarData cd = new PriceLabsWebhookRequest.CalendarData();
                    cd.setDate(ce.getDate());
                    cd.setPrice(ce.getPrice());
                    cd.setMinStay(ce.getMinStay());
                    if (ce.getCheckIn() != null) {
                        cd.setClosedToArrival(!Boolean.TRUE.equals(ce.getCheckIn()));
                    }
                    if (ce.getCheckOut() != null) {
                        cd.setClosedToDeparture(!Boolean.TRUE.equals(ce.getCheckOut()));
                    }
                    calendar.add(cd);
                }

                if (calendar.isEmpty()) {
                    continue;
                }

                PriceLabsWebhookRequest.ListingData ld = new PriceLabsWebhookRequest.ListingData();
                ld.setListingId(listingId);
                if (ratePlanId != null && !ratePlanId.isBlank()) {
                    ld.setRatePlanId(ratePlanId);
                }
                ld.setCalendar(calendar);
                data.add(ld);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                failures.add("listing_id=" + listingId + (ratePlanId != null ? (" rate_plan_id=" + ratePlanId) : "") + " error=" + msg);
            }
        }

        if (data.isEmpty()) {
            if (!failures.isEmpty()) {
                throw new RuntimeException("get_prices(pull_sync) failed: " + String.join("; ", failures));
            }
            logger.warn("[PriceLabsPull] get_prices(pull_sync) returned empty data. storeId={}", storeId);
            return;
        }

        PriceLabsWebhookRequest webhookRequest = new PriceLabsWebhookRequest();
        webhookRequest.setType("price_update");
        webhookRequest.setData(data);
        priceLabsService.handleWebhookPriceUpdate(webhookRequest);
        logger.info("[PriceLabsPull] get_prices(pull_sync) applied. storeId={}, listingCount={}", storeId, data.size());
        if (!failures.isEmpty()) {
            logger.warn("[PriceLabsPull] get_prices(pull_sync) partial failures. storeId={}, details={}", storeId, String.join("; ", failures));
        }
    }

    @Transactional
    public void pullPricesForRoomType(Long roomTypeId, Integer days) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        if (roomTypeId == null) {
            throw new IllegalArgumentException("roomTypeId is required");
        }

        PriceLabsConnection connection = connectionRepo.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(storeId, roomTypeId)
                .orElseThrow(() -> new RuntimeException("No enabled PriceLabs connection found"));

        String listingId = connection.getPriceLabsListingId();
        if (listingId == null || listingId.isBlank()) {
            listingId = PriceLabsIdUtil.formatListingId(storeId, roomTypeId);
        }

        pullPricesForListingIds(List.of(listingId), days);
    }

    /**
     * PriceLabs sync_url listing_ids-only payload: pull prices by listing_id and persist.
     */
    @Transactional
    public void pullPricesForListingIds(List<String> listingIds, Integer days) {
        if (listingIds == null || listingIds.isEmpty()) {
            logger.warn("[PriceLabsPull] listing_ids empty, skip pull_sync");
            return;
        }

        Map<Long, List<PriceLabsConnection>> connectionsByStore = resolveConnectionsForListingIds(listingIds);
        if (connectionsByStore.isEmpty()) {
            logger.warn("[PriceLabsPull] no enabled connections for listing_ids, skip pull_sync. listingCount={}", listingIds.size());
            return;
        }

        int pullDays = clampSyncDays(days);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(pullDays - 1L);

        boolean anyApplied = false;
        List<String> failures = new ArrayList<>();

        for (Map.Entry<Long, List<PriceLabsConnection>> entry : connectionsByStore.entrySet()) {
            List<PriceLabsWebhookRequest.ListingData> data =
                    buildPullSyncData(entry.getKey(), entry.getValue(), startDate, endDate, failures);

            if (data.isEmpty()) {
                continue;
            }

            PriceLabsWebhookRequest webhookRequest = new PriceLabsWebhookRequest();
            webhookRequest.setType("price_update");
            webhookRequest.setData(data);
            priceLabsService.handleWebhookPriceUpdate(webhookRequest);
            anyApplied = true;
            logger.info("[PriceLabsPull] listing_ids pull_sync applied. storeId={}, listingCount={}",
                    entry.getKey(), data.size());
        }

        if (!anyApplied && !failures.isEmpty()) {
            throw new RuntimeException("get_prices(pull_sync) failed: " + String.join("; ", failures));
        }
        if (!failures.isEmpty()) {
            logger.warn("[PriceLabsPull] listing_ids pull_sync partial failures: {}", String.join("; ", failures));
        }
    }

    /**
     * PriceLabs sync_url listing_ids-only payload: push calendar data back.
     */
    @Transactional(readOnly = true)
    public void syncCalendarForListingIds(List<String> listingIds, Integer days) {
        if (listingIds == null || listingIds.isEmpty()) {
            logger.warn("[PriceLabsCalendar] listing_ids empty, skip calendar push");
            return;
        }

        Map<Long, List<PriceLabsConnection>> connectionsByStore = resolveConnectionsForListingIds(listingIds);
        if (connectionsByStore.isEmpty()) {
            logger.warn("[PriceLabsCalendar] no enabled connections for listing_ids, skip calendar push. listingCount={}",
                    listingIds.size());
            return;
        }

        int syncDays = clampSyncDays(days);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(syncDays - 1L);

        for (Map.Entry<Long, List<PriceLabsConnection>> entry : connectionsByStore.entrySet()) {
            syncCalendarForConnections(entry.getKey(), entry.getValue(), startDate, endDate);
        }
    }

    @Transactional(readOnly = true)
    public void syncListings(Long storeId) {
        logger.info("Sync listings for store {}", storeId);
        Store store = storeRepo.findById(storeId).orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
        validateStoreForPriceLabs(store);

        PriceLabsIntegration integration = integrationRepo.findByStoreId(storeId)
            .orElseThrow(() -> new RuntimeException("PriceLabs integration not found for store: " + storeId));
        String userToken = integration.getPriceLabsEmail();
        if (userToken == null || userToken.trim().isEmpty()) {
            throw new RuntimeException("PriceLabs email (user_token) not configured for store: " + storeId);
        }

        List<RoomType> rts = roomTypeRepo.findByStoreId(storeId);
        if (rts.isEmpty()) {
            logger.warn("No room types for store {}", storeId);
            return;
        }

        List<PriceLabsApiClient.ListingData> listings = rts.stream()
            .map(rt -> toListing(rt, store, userToken))
            .collect(Collectors.toList());

        PriceLabsApiClient.PriceLabsResponse res = apiClient.pushListings(listings);
        logger.info("Listings sync done. Success: {}, Fail: {}",
            res.getSuccess() != null ? res.getSuccess().size() : 0,
            res.getFailure() != null ? res.getFailure().size() : 0);

        if (res.getFailure() != null && !res.getFailure().isEmpty()) {
            String summary = summarizeFailures(res.getFailure());
            PriceLabsSyncLog log = PriceLabsSyncLog.failure(storeId, SyncType.LISTING, SyncDirection.OUTBOUND, "房源推送失败");
            log.setResponseData(summary);
            syncLogRepo.save(log);
            throw new RuntimeException("房源推送失败：" + summary);
        }
    }

    @Transactional(readOnly = true)
    public void syncRatePlans(Long storeId) {
        logger.info("Sync rate plans for store {}", storeId);
        List<RoomType> rts = roomTypeRepo.findByStoreId(storeId);
        if (rts.isEmpty()) {
            logger.warn("No room types for store {}", storeId);
            return;
        }

        List<PriceLabsApiClient.RatePlanData> all = new ArrayList<>();
        for (RoomType rt : rts) {
            List<RoomTypePricePlan> plans = rtppRepo.findByRoomTypeId(rt.getId());
            if (plans.isEmpty()) {
                logger.warn("No price plans for room type {}", rt.getName());
                continue;
            }
            all.addAll(plans.stream().map(p -> toRatePlan(rt, p)).collect(Collectors.toList()));
        }

        if (all.isEmpty()) {
            logger.warn("No rate plans to sync");
            return;
        }

        PriceLabsApiClient.PriceLabsResponse res = apiClient.pushRatePlans(all);
        logger.info("Rate plans sync done. Success: {}, Fail: {}",
            res.getSuccess() != null ? res.getSuccess().size() : 0,
            res.getFailure() != null ? res.getFailure().size() : 0);

        if (res.getFailure() != null && !res.getFailure().isEmpty()) {
            String summary = summarizeFailures(res.getFailure());
            PriceLabsSyncLog log = PriceLabsSyncLog.failure(storeId, SyncType.RATE_PLAN, SyncDirection.OUTBOUND, "价格计划推送失败");
            log.setResponseData(summary);
            syncLogRepo.save(log);
            throw new RuntimeException("价格计划推送失败：" + summary);
        }
    }

    @Transactional(readOnly = true)
    public void syncCalendar(Long storeId, LocalDate start, LocalDate end) {
        logger.info("Sync calendar for store {} from {} to {}", storeId, start, end);
        Store store = storeRepo.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
        String currency = (store.getCurrency() != null && !store.getCurrency().trim().isEmpty()) ? store.getCurrency().trim() : "CNY";
        List<RoomType> rts = roomTypeRepo.findByStoreId(storeId);
        if (rts.isEmpty()) {
            logger.warn("No room types for store {}", storeId);
            return;
        }

        Map<Long, Map<LocalDate, Integer>> bookedUnitsByRoomTypeAndDate = buildBookedUnitsByRoomTypeAndDate(storeId, start, end);

        List<PriceLabsApiClient.CalendarData> all = new ArrayList<>();
        for (RoomType rt : rts) {
            List<RoomTypePricePlan> plans = rtppRepo.findByRoomTypeId(rt.getId());
            if (plans.isEmpty()) {
                logger.warn("No price plans for room type {}", rt.getName());
                continue;
            }
            for (RoomTypePricePlan p : plans) {
                PricePlan plan = p.getPricePlan();
                List<RoomPrice> prices = roomPriceRepo.findByStoreIdAndRoomTypeIdAndPriceDateBetween(storeId, rt.getId(), start, end);
                PriceLabsApiClient.CalendarData cal = toCalendar(rt, plan, prices, start, end, currency, bookedUnitsByRoomTypeAndDate);
                if (cal.getCalendar() != null && !cal.getCalendar().isEmpty()) all.add(cal);
            }
        }

        if (all.isEmpty()) {
            logger.warn("No calendar data to sync");
            return;
        }

        PriceLabsApiClient.PriceLabsResponse res = apiClient.pushCalendar(all);
        logger.info("Calendar sync done. Success: {}, Fail: {}",
            res.getSuccess() != null ? res.getSuccess().size() : 0,
            res.getFailure() != null ? res.getFailure().size() : 0);

        if (res.getFailure() != null && !res.getFailure().isEmpty()) {
            String summary = summarizeFailures(res.getFailure());
            PriceLabsSyncLog log = PriceLabsSyncLog.failure(storeId, SyncType.CALENDAR, SyncDirection.OUTBOUND, "日历推送失败");
            log.setRequestData("{\"start\":\"" + start + "\",\"end\":\"" + end + "\"}");
            log.setResponseData(summary);
            syncLogRepo.save(log);
            throw new RuntimeException("日历推送失败：" + summary);
        }
    }

    @Transactional(readOnly = true)
    public void syncRoomType(Long rtId) {
        RoomType rt = roomTypeRepo.findById(rtId).orElseThrow(() -> new RuntimeException("Room type not found: " + rtId));
        Long sid = rt.getStoreId();
        Store store = storeRepo.findById(sid).orElseThrow(() -> new RuntimeException("Store not found: " + sid));
        validateStoreForPriceLabs(store);

        PriceLabsIntegration integration = integrationRepo.findByStoreId(sid)
            .orElseThrow(() -> new RuntimeException("PriceLabs integration not found for store: " + sid));
        String userToken = integration.getPriceLabsEmail();
        if (userToken == null || userToken.trim().isEmpty()) {
            throw new RuntimeException("PriceLabs email (user_token) not configured for store: " + sid);
        }

        PriceLabsApiClient.PriceLabsResponse listingRes = apiClient.pushListings(List.of(toListing(rt, store, userToken)));
        if (listingRes.getFailure() != null && !listingRes.getFailure().isEmpty()) {
            throw new RuntimeException("房源推送失败：" + summarizeFailures(listingRes.getFailure()));
        }

        List<RoomTypePricePlan> plans = rtppRepo.findByRoomTypeId(rtId);
        if (!plans.isEmpty()) {
            PriceLabsApiClient.PriceLabsResponse ratePlanRes = apiClient.pushRatePlans(plans.stream().map(p -> toRatePlan(rt, p)).collect(Collectors.toList()));
            if (ratePlanRes.getFailure() != null && !ratePlanRes.getFailure().isEmpty()) {
                throw new RuntimeException("价格计划推送失败：" + summarizeFailures(ratePlanRes.getFailure()));
            }
        }

        LocalDate start = LocalDate.now();
        // PriceLabs 要求首次日历推送至少覆盖 1 年数据，否则会返回 "less than a year of data is not allowed"
        LocalDate end = start.plusYears(1).minusDays(1);
        syncCalendar(sid, start, end);
        logger.info("Room type {} sync complete", rt.getName());
    }

    /**
     * Push listing + selected rate plan + calendar for a single room type.
     * Failure will throw and should rollback the caller transaction.
     */
    @Transactional
    public void syncListingRatePlanAndCalendar(Long storeId, RoomType roomType, PricePlan pricePlan, Integer days) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (roomType == null || roomType.getId() == null) {
            throw new IllegalArgumentException("roomType is required");
        }
        if (pricePlan == null || pricePlan.getId() == null) {
            throw new IllegalArgumentException("pricePlan is required");
        }
        if (roomType.getStoreId() != null && !roomType.getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("roomType does not belong to store");
        }
        if (pricePlan.getStoreId() != null && !pricePlan.getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("pricePlan does not belong to store");
        }

        Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
        validateStoreForPriceLabs(store);

        PriceLabsIntegration integration = integrationRepo.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("PriceLabs integration not found for store: " + storeId));
        if (!Boolean.TRUE.equals(integration.getIsEnabled())) {
            throw new RuntimeException("PriceLabs integration is disabled");
        }

        String userToken = integration.getPriceLabsEmail();
        if (userToken == null || userToken.trim().isEmpty()) {
            throw new RuntimeException("PriceLabs email (user_token) not configured for store: " + storeId);
        }

        PriceLabsApiClient.ListingData listing = toListing(roomType, store, userToken);
        PriceLabsApiClient.PriceLabsResponse listingRes = apiClient.pushListings(List.of(listing));
        if (listingRes.getFailure() != null && !listingRes.getFailure().isEmpty()) {
            String summary = summarizeFailures(listingRes.getFailure());
            throw new RuntimeException("Listing push failed: " + summary);
        }

        PriceLabsApiClient.RatePlanData ratePlanData = buildRatePlanData(roomType, pricePlan);
        PriceLabsApiClient.PriceLabsResponse ratePlanRes = apiClient.pushRatePlans(List.of(ratePlanData));
        if (ratePlanRes.getFailure() != null && !ratePlanRes.getFailure().isEmpty()) {
            String summary = summarizeFailures(ratePlanRes.getFailure());
            throw new RuntimeException("Rate plan push failed: " + summary);
        }

        int syncDays = clampSyncDays(days);
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(syncDays - 1L);

        PriceLabsConnection temp = new PriceLabsConnection(roomType, pricePlan);
        temp.setStoreId(storeId);
        temp.setIsEnabled(true);
        syncCalendarForConnections(storeId, List.of(temp), start, end);

        LocalDateTime now = LocalDateTime.now();
        integration.setLastListingSyncAt(now);
        integration.setLastPriceSyncAt(now);
        integrationRepo.save(integration);
    }

    private PriceLabsApiClient.ListingData toListing(RoomType rt, Store s, String userToken) {
        PriceLabsApiClient.ListingData d = new PriceLabsApiClient.ListingData();
        d.setListingId(PriceLabsIdUtil.formatListingId(s.getId(), rt.getId()));
        d.setUserToken(userToken);
        if (rt.getName() == null || rt.getName().trim().isEmpty()) {
            throw new RuntimeException("房型名称不能为空（room_type_id=" + rt.getId() + "）");
        }
        d.setName(rt.getName());
        d.setStatus("available");

        d.setAddress(s.getAddress());
        d.setState(s.getState());
        d.setTimezone(s.getTimezone());

        String city = s.getCity();
        d.setCity(city);

        String countryAlpha3 = PriceLabsCountryUtil.normalizeToAlpha3(s.getCountry());
        if (countryAlpha3 == null) {
            throw new RuntimeException("请先补全门店地址信息：国家必须为 ISO 三字码（例如 CHN），当前值=" + s.getCountry());
        }
        d.setCountry(countryAlpha3);

        double[] coords = getCityCoordinates(city);
        d.setLatitude(coords[0]);
        d.setLongitude(coords[1]);
        PriceLabsApiClient.Location location = new PriceLabsApiClient.Location();
        location.setCity(city);
        location.setCountry(countryAlpha3);
        location.setLatitude(coords[0]);
        location.setLongitude(coords[1]);
        d.setLocation(location);

        d.setBedrooms(1);
        d.setBathrooms(1.0);
        d.setAccommodates(2);
        d.setPropertyType("hotel_room");
        d.setCurrency(s.getCurrency() != null && !s.getCurrency().trim().isEmpty() ? s.getCurrency() : "CNY");

        BigDecimal base = rt.getDefaultPrice() != null ? rt.getDefaultPrice() : BigDecimal.valueOf(500);
        d.setBasePrice(base);
        d.setMinPrice(base.multiply(BigDecimal.valueOf(0.6)).setScale(0, RoundingMode.HALF_UP));
        d.setMaxPrice(base.multiply(BigDecimal.valueOf(2.0)).setScale(0, RoundingMode.HALF_UP));
        d.setActive(true);

        Integer cnt = rt.getTotalRooms() != null ? rt.getTotalRooms() : 1;
        d.setMultiUnit(cnt > 1);
        d.setMultiUnitCount(cnt);

        return d;
    }

    private double[] getCityCoordinates(String city) {
        if (city == null) return new double[]{39.9042, 116.4074};

        String cityLower = city.toLowerCase();
        if (cityLower.contains("佛山") || cityLower.contains("foshan")) return new double[]{23.0220, 113.1216};
        if (cityLower.contains("广州") || cityLower.contains("guangzhou")) return new double[]{23.1291, 113.2644};
        if (cityLower.contains("深圳") || cityLower.contains("shenzhen")) return new double[]{22.5431, 114.0579};
        if (cityLower.contains("上海") || cityLower.contains("shanghai")) return new double[]{31.2304, 121.4737};
        if (cityLower.contains("北京") || cityLower.contains("beijing")) return new double[]{39.9042, 116.4074};
        if (cityLower.contains("杭州") || cityLower.contains("hangzhou")) return new double[]{30.2741, 120.1551};
        if (cityLower.contains("成都") || cityLower.contains("chengdu")) return new double[]{30.5728, 104.0668};
        if (cityLower.contains("重庆") || cityLower.contains("chongqing")) return new double[]{29.5630, 106.5516};
        if (cityLower.contains("西安") || cityLower.contains("xian")) return new double[]{34.3416, 108.9398};
        if (cityLower.contains("武汉") || cityLower.contains("wuhan")) return new double[]{30.5928, 114.3055};
        if (cityLower.contains("广东")) return new double[]{23.1291, 113.2644};

        return new double[]{39.9042, 116.4074};
    }

    private PriceLabsApiClient.RatePlanData toRatePlan(RoomType rt, RoomTypePricePlan p) {
        PricePlan plan = p.getPricePlan();
        PriceLabsApiClient.RatePlanData d = new PriceLabsApiClient.RatePlanData();
        d.setListingId(PriceLabsIdUtil.formatListingId(rt.getStoreId(), rt.getId()));
        d.setRatePlanId(PriceLabsIdUtil.formatRatePlanId(plan.getId()));
        d.setName(plan.getName());
        List<RoomTypePricePlan> all = rtppRepo.findByRoomTypeId(rt.getId());
        d.setIsDefault(!all.isEmpty() && p.getId().equals(all.get(0).getId()));
        d.setOccupancyBased(false);
        return d;
    }

    private PriceLabsApiClient.RatePlanData buildRatePlanData(RoomType rt, PricePlan plan) {
        PriceLabsApiClient.RatePlanData d = new PriceLabsApiClient.RatePlanData();
        d.setListingId(PriceLabsIdUtil.formatListingId(rt.getStoreId(), rt.getId()));
        d.setRatePlanId(PriceLabsIdUtil.formatRatePlanId(plan.getId()));
        d.setName(plan.getName());

        boolean isDefault = false;
        List<RoomTypePricePlan> all = rtppRepo.findByRoomTypeId(rt.getId());
        if (!all.isEmpty()) {
            RoomTypePricePlan defaultPlan = all.get(0);
            Long defaultId = defaultPlan.getId();
            if (defaultId != null) {
                for (RoomTypePricePlan p : all) {
                    if (p.getPricePlan() != null && plan.getId().equals(p.getPricePlan().getId())) {
                        isDefault = defaultId.equals(p.getId());
                        break;
                    }
                }
            }
        }

        d.setIsDefault(isDefault);
        d.setOccupancyBased(false);
        return d;
    }

    private PriceLabsApiClient.CalendarData toCalendar(
            RoomType rt,
            PricePlan plan,
            List<RoomPrice> prices,
            LocalDate start,
            LocalDate end,
            String currency,
            Map<Long, Map<LocalDate, Integer>> bookedUnitsByRoomTypeAndDate
    ) {
        PriceLabsApiClient.CalendarData d = new PriceLabsApiClient.CalendarData();
        d.setListingId(PriceLabsIdUtil.formatListingId(rt.getStoreId(), rt.getId()));
        d.setRatePlanId(PriceLabsIdUtil.formatRatePlanId(plan.getId()));
        d.setCurrency(currency);

        Map<LocalDate, RoomPrice> map = new HashMap<>();
        for (RoomPrice p : prices) {
            if (p.getPricePlan() != null && p.getPricePlan().getId().equals(plan.getId())) map.put(p.getPriceDate(), p);
        }

        List<PriceLabsApiClient.CalendarEntry> entries = new ArrayList<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            RoomPrice rp = map.get(cur);
            PriceLabsApiClient.CalendarEntry e = new PriceLabsApiClient.CalendarEntry();
            String date = cur.toString();
            e.setDate(date);
            e.setEndDate(date);
            int totalUnits = rt.getTotalRooms() != null ? rt.getTotalRooms() : 1;
            int bookedUnits = bookedUnitsByRoomTypeAndDate
                    .getOrDefault(rt.getId(), Map.of())
                    .getOrDefault(cur, 0);

            if (rp != null) {
                e.setPrice(rp.getPrice());
            } else {
                e.setPrice(rt.getDefaultPrice() != null ? rt.getDefaultPrice() : BigDecimal.valueOf(500));
            }

            Integer desiredAvailable = rp != null ? rp.getAvailableRooms() : null;
            int maxAvailable = Math.max(totalUnits - bookedUnits, 0);
            int availableUnits = desiredAvailable != null ? Math.max(Math.min(desiredAvailable, maxAvailable), 0) : maxAvailable;
            int blockedUnits = Math.max(totalUnits - bookedUnits - availableUnits, 0);

            // PriceLabs /calendar：回传占用量（booked_units）+ 实际可售（available_units）
            e.setAvailableUnits(availableUnits);
            e.setBookedUnits(bookedUnits);
            e.setBlockedUnits(blockedUnits);
            e.setAvailable(availableUnits > 0);
            PriceLabsApiClient.CalendarSettings settings = new PriceLabsApiClient.CalendarSettings();
            settings.setMinStay(plan.getMinNights());
            settings.setCheckIn(true);
            settings.setCheckOut(true);
            e.setSettings(settings);
            entries.add(e);
            cur = cur.plusDays(1);
        }
        d.setCalendar(entries);
        return d;
    }

    private Map<Long, Map<LocalDate, Integer>> buildBookedUnitsByRoomTypeAndDate(Long storeId, LocalDate start, LocalDate end) {
        if (storeId == null || start == null || end == null) {
            return Map.of();
        }

        LocalDate endExclusive = end.plusDays(1);
        Set<ReservationStatus> statuses = EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.REQUESTED, ReservationStatus.CHECKED_IN);

        List<ReservationRepository.ReservationOccupancyRow> rows = reservationRepository
                .findOccupancyRowsByStoreIdAndDateRangeAndStatuses(storeId, start, endExclusive, statuses);

        Map<Long, Map<LocalDate, Integer>> booked = new HashMap<>();

        for (ReservationRepository.ReservationOccupancyRow row : rows) {
            if (row == null) {
                continue;
            }

            Long roomTypeId = row.getAssignedRoomTypeId() != null ? row.getAssignedRoomTypeId() : row.getOtaRoomTypeId();
            if (roomTypeId == null) {
                continue;
            }

            LocalDate checkIn = row.getCheckInDate();
            LocalDate checkOut = row.getCheckOutDate();
            if (checkIn == null || checkOut == null) {
                continue;
            }

            LocalDate from = checkIn.isAfter(start) ? checkIn : start;
            LocalDate toExclusive = checkOut.isBefore(endExclusive) ? checkOut : endExclusive;
            if (!from.isBefore(toExclusive)) {
                continue;
            }

            Map<LocalDate, Integer> byDate = booked.computeIfAbsent(roomTypeId, k -> new HashMap<>());
            LocalDate d = from;
            while (d.isBefore(toExclusive)) {
                byDate.merge(d, 1, Integer::sum);
                d = d.plusDays(1);
            }
        }

        return booked;
    }

    private int clampSyncDays(Integer days) {
        int result = (days != null && days > 0) ? days : PriceLabsSyncDefaults.DEFAULT_SYNC_DAYS;
        return Math.min(result, PriceLabsSyncDefaults.MAX_SYNC_DAYS);
    }

    private Map<Long, List<PriceLabsConnection>> resolveConnectionsForListingIds(List<String> listingIds) {
        Map<Long, List<PriceLabsConnection>> byStore = new HashMap<>();
        if (listingIds == null) {
            return byStore;
        }

        for (String rawId : listingIds) {
            if (rawId == null || rawId.isBlank()) {
                continue;
            }
            String listingId = rawId.trim();
            Long storeIdFromListing = PriceLabsIdUtil.parseStoreId(listingId).orElse(null);

            List<PriceLabsConnection> candidates = connectionRepo.findByPriceLabsListingId(listingId);
            if (candidates == null || candidates.isEmpty()) {
                Long roomTypeId = PriceLabsIdUtil.parseRoomTypeId(listingId).orElse(null);
                if (roomTypeId != null) {
                    candidates = connectionRepo.findByRoomTypeId(roomTypeId);
                }
            }
            if (candidates == null || candidates.isEmpty()) {
                logger.warn("[PriceLabsPull] listing_id not matched to any connection: {}", listingId);
                continue;
            }

            List<PriceLabsConnection> enabled = candidates.stream()
                    .filter(conn -> Boolean.TRUE.equals(conn.getIsEnabled()))
                    .filter(conn -> storeIdFromListing == null || storeIdFromListing.equals(conn.getStoreId()))
                    .collect(Collectors.toList());
            if (enabled.isEmpty()) {
                logger.warn("[PriceLabsPull] listing_id matched but no enabled connection: {}", listingId);
                continue;
            }

            if (enabled.size() > 1) {
                logger.warn("[PriceLabsPull] listing_id has multiple enabled connections, using default plan. listing_id={}, count={}",
                        listingId, enabled.size());
            }

            PriceLabsConnection selected = selectDefaultConnection(enabled);
            if (selected == null) {
                continue;
            }
            byStore.computeIfAbsent(selected.getStoreId(), k -> new ArrayList<>()).add(selected);
        }

        byStore.replaceAll((storeId, list) -> dedupeConnections(list));
        return byStore;
    }

    private List<PriceLabsWebhookRequest.ListingData> buildPullSyncData(
            Long storeId,
            List<PriceLabsConnection> connections,
            LocalDate startDate,
            LocalDate endDate,
            List<String> failures
    ) {
        List<PriceLabsWebhookRequest.ListingData> data = new ArrayList<>();
        if (connections == null || connections.isEmpty()) {
            return data;
        }

        for (PriceLabsConnection conn : connections) {
            if (conn == null) {
                continue;
            }

            String listingId = normalizeListingId(conn, storeId);
            if (listingId == null || listingId.isBlank()) {
                continue;
            }

            String ratePlanId = null;
            if (conn.getPricePlan() != null && conn.getPricePlan().getId() != null) {
                ratePlanId = PriceLabsIdUtil.formatRatePlanId(conn.getPricePlan().getId());
            }

            try {
                PriceLabsApiClient.PullSyncGetPricesResponse res = apiClient.pullSyncGetPrices(listingId, ratePlanId, false);
                if (res == null || res.getData() == null || res.getData().isEmpty()) {
                    continue;
                }

                List<PriceLabsWebhookRequest.CalendarData> calendar = new ArrayList<>();
                for (PriceLabsApiClient.PullSyncPriceEntry ce : res.getData()) {
                    if (ce == null || ce.getDate() == null || ce.getPrice() == null) {
                        continue;
                    }
                    LocalDate d;
                    try {
                        d = LocalDate.parse(ce.getDate());
                    } catch (Exception ignored) {
                        continue;
                    }
                    if (d.isBefore(startDate) || d.isAfter(endDate)) {
                        continue;
                    }

                    PriceLabsWebhookRequest.CalendarData cd = new PriceLabsWebhookRequest.CalendarData();
                    cd.setDate(ce.getDate());
                    cd.setPrice(ce.getPrice());
                    cd.setMinStay(ce.getMinStay());
                    if (ce.getCheckIn() != null) {
                        cd.setClosedToArrival(!Boolean.TRUE.equals(ce.getCheckIn()));
                    }
                    if (ce.getCheckOut() != null) {
                        cd.setClosedToDeparture(!Boolean.TRUE.equals(ce.getCheckOut()));
                    }
                    calendar.add(cd);
                }

                if (calendar.isEmpty()) {
                    continue;
                }

                PriceLabsWebhookRequest.ListingData ld = new PriceLabsWebhookRequest.ListingData();
                ld.setListingId(listingId);
                if (ratePlanId != null && !ratePlanId.isBlank()) {
                    ld.setRatePlanId(ratePlanId);
                }
                ld.setCalendar(calendar);
                data.add(ld);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                failures.add("listing_id=" + listingId + (ratePlanId != null ? (" rate_plan_id=" + ratePlanId) : "") + " error=" + msg);
            }
        }

        if (data.isEmpty() && storeId != null) {
            logger.warn("[PriceLabsPull] listing_ids pull_sync returned empty data. storeId={}", storeId);
        }
        return data;
    }

    private void syncCalendarForConnections(Long storeId, List<PriceLabsConnection> connections, LocalDate start, LocalDate end) {
        if (storeId == null || connections == null || connections.isEmpty()) {
            return;
        }

        Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
        String currency = (store.getCurrency() != null && !store.getCurrency().trim().isEmpty())
                ? store.getCurrency().trim()
                : "CNY";

        Map<Long, Map<LocalDate, Integer>> bookedUnitsByRoomTypeAndDate =
                buildBookedUnitsByRoomTypeAndDate(storeId, start, end);

        List<PriceLabsApiClient.CalendarData> all = new ArrayList<>();
        for (PriceLabsConnection conn : connections) {
            if (conn == null || conn.getRoomType() == null || conn.getRoomType().getId() == null
                    || conn.getPricePlan() == null || conn.getPricePlan().getId() == null) {
                continue;
            }

            RoomType rt = conn.getRoomType();
            PricePlan plan = conn.getPricePlan();

            List<RoomPrice> prices = roomPriceRepo.findByStoreIdAndRoomTypeIdAndPriceDateBetween(
                    storeId, rt.getId(), start, end);
            PriceLabsApiClient.CalendarData cal = toCalendar(rt, plan, prices, start, end, currency, bookedUnitsByRoomTypeAndDate);
            if (cal.getCalendar() != null && !cal.getCalendar().isEmpty()) {
                all.add(cal);
            }
        }

        if (all.isEmpty()) {
            logger.warn("[PriceLabsCalendar] no calendar data to push. storeId={}, listingCount={}", storeId, connections.size());
            return;
        }

        PriceLabsApiClient.PriceLabsResponse res = apiClient.pushCalendar(all);
        logger.info("[PriceLabsCalendar] calendar push done. storeId={}, success={}, fail={}",
                storeId,
                res.getSuccess() != null ? res.getSuccess().size() : 0,
                res.getFailure() != null ? res.getFailure().size() : 0);

        if (res.getFailure() != null && !res.getFailure().isEmpty()) {
            String summary = summarizeFailures(res.getFailure());
            PriceLabsSyncLog log = PriceLabsSyncLog.failure(storeId, SyncType.CALENDAR, SyncDirection.OUTBOUND, "calendar push failed");
            log.setRequestData("{\"start\":\"" + start + "\",\"end\":\"" + end + "\"}");
            log.setResponseData(summary);
            syncLogRepo.save(log);
            throw new RuntimeException("calendar push failed: " + summary);
        }
    }

    private String normalizeListingId(PriceLabsConnection conn, Long storeId) {
        if (conn == null) {
            return null;
        }
        if (conn.getPriceLabsListingId() != null && !conn.getPriceLabsListingId().isBlank()) {
            return conn.getPriceLabsListingId().trim();
        }
        if (conn.getRoomType() != null && conn.getRoomType().getId() != null && storeId != null) {
            return PriceLabsIdUtil.formatListingId(storeId, conn.getRoomType().getId());
        }
        return null;
    }

    private List<PriceLabsConnection> dedupeConnections(List<PriceLabsConnection> connections) {
        if (connections == null || connections.isEmpty()) {
            return List.of();
        }
        List<PriceLabsConnection> out = new ArrayList<>();
        Set<Long> seen = new java.util.HashSet<>();
        for (PriceLabsConnection conn : connections) {
            if (conn == null) {
                continue;
            }
            Long id = conn.getId();
            if (id != null && !seen.add(id)) {
                continue;
            }
            out.add(conn);
        }
        return out;
    }

    private PriceLabsConnection selectDefaultConnection(List<PriceLabsConnection> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        Long roomTypeId = candidates.get(0).getRoomType() != null ? candidates.get(0).getRoomType().getId() : null;
        if (roomTypeId != null) {
            Long defaultPlanId = rtppRepo.findByRoomTypeId(roomTypeId).stream()
                    .filter(rtp -> rtp.getPricePlan() != null && rtp.getPricePlan().getId() != null)
                    .min((a, b) -> {
                        if (a.getId() == null && b.getId() == null) return 0;
                        if (a.getId() == null) return 1;
                        if (b.getId() == null) return -1;
                        return a.getId().compareTo(b.getId());
                    })
                    .map(rtp -> rtp.getPricePlan().getId())
                    .orElse(null);

            if (defaultPlanId != null) {
                for (PriceLabsConnection conn : candidates) {
                    if (conn.getPricePlan() != null && defaultPlanId.equals(conn.getPricePlan().getId())) {
                        return conn;
                    }
                }
            }
        }

        return candidates.get(0);
    }

    private String summarizeFailures(List<Object> failures) {
        if (failures == null || failures.isEmpty()) return "";

        int limit = Math.min(5, failures.size());
        List<String> parts = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            Object item = failures.get(i);
            if (item instanceof Map<?, ?> map) {
                Object error = map.get("error");
                Object listingId = map.get("listing_id");
                Object ratePlanId = map.get("rate_plan_id");

                Object data = map.get("data");
                if (data instanceof Map<?, ?> dataMap) {
                    if (listingId == null) listingId = dataMap.get("listing_id");
                    if (ratePlanId == null) ratePlanId = dataMap.get("rate_plan_id");
                }

                StringBuilder sb = new StringBuilder();
                if (listingId != null) sb.append("listing_id=").append(listingId).append(' ');
                if (ratePlanId != null) sb.append("rate_plan_id=").append(ratePlanId).append(' ');
                if (error != null) sb.append("error=").append(error);
                if (sb.length() == 0) sb.append(map);
                parts.add(sb.toString().trim());
            } else {
                parts.add(String.valueOf(item));
            }
        }

        String summary = String.join("; ", parts);
        if (failures.size() > limit) summary += " ...(共" + failures.size() + "条)";
        return summary;
    }

    private void validateStoreForPriceLabs(Store store) {
        if (store.getAddress() == null || store.getAddress().trim().isEmpty()
            || store.getCity() == null || store.getCity().trim().isEmpty()
            || store.getCountry() == null || store.getCountry().trim().isEmpty()) {
            throw new RuntimeException("请先补全门店地址信息（地址/城市/国家为必填）");
        }

        String normalizedCountryAlpha3 = PriceLabsCountryUtil.normalizeToAlpha3(store.getCountry());
        if (normalizedCountryAlpha3 == null) {
            throw new RuntimeException("请先补全门店地址信息：国家必须可转换为 ISO 三字码（例如 China/CHN）");
        }
    }

    private void migrateConnectionListingIds(Long storeId) {
        connectionRepo.findByStoreId(storeId).forEach(conn -> {
            if (conn.getRoomType() == null) return;
            String expected = PriceLabsIdUtil.formatListingId(storeId, conn.getRoomType().getId());
            if (!expected.equals(conn.getPriceLabsListingId())) {
                conn.setPriceLabsListingId(expected);
                connectionRepo.save(conn);
            }
        });
    }

    private void markEnabledConnectionsAsError(Long storeId, String message) {
        LocalDateTime now = LocalDateTime.now();
        connectionRepo.findByStoreId(storeId).forEach(conn -> {
            if (conn.getIsEnabled()) {
                conn.setSyncStatus(PriceLabsSyncStatus.ERROR);
                conn.setLastSyncAt(now);
                conn.setErrorMessage(message);
                connectionRepo.save(conn);
            }
        });
    }

    public void initSettings() {
        logger.info("Init PriceLabs settings");
        apiClient.registerIntegration();
    }
}
