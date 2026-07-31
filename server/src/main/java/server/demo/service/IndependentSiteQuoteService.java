package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.IndependentSiteDtos;
import server.demo.entity.Channel;
import server.demo.entity.IndependentSite;
import server.demo.entity.IndependentSitePage;
import server.demo.entity.IndependentSitePublication;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomBlockout;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.enums.IndependentSitePageFormat;
import server.demo.enums.IndependentSitePageType;
import server.demo.enums.IndependentSitePaymentProvider;
import server.demo.enums.IndependentSitePublicationType;
import server.demo.enums.PriceAdjustmentType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.IndependentSitePageRepository;
import server.demo.repository.IndependentSitePublicationRepository;
import server.demo.repository.IndependentSiteRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.constants.SaasFeatureCodes;
import server.demo.service.helper.util.ReservationOccupancyProjection;
import server.demo.service.saas.EntitlementService;
import server.demo.util.IndependentSitePricePolicy;
import server.demo.util.LocalBasePriceResolver;
import server.demo.util.StoreTimeZoneUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class IndependentSiteQuoteService {

    private static final int MAX_STAY_NIGHTS = 365;
    private static final int QUOTE_TTL_MINUTES = 5;
    private static final Set<ReservationStatus> BLOCKING_STATUSES = EnumSet.of(
            ReservationStatus.REQUESTED,
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN
    );
    private static final Set<ReservationStatus> QUOTA_OCCUPANCY_STATUSES = EnumSet.of(
            ReservationStatus.REQUESTED,
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT
    );

    private final IndependentSiteRepository siteRepository;
    private final IndependentSitePublicationRepository publicationRepository;
    private final IndependentSitePageRepository pageRepository;
    private final StoreRepository storeRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomTypePricePlanRepository roomTypePricePlanRepository;
    private final RoomPriceRepository roomPriceRepository;
    private final ReservationRepository reservationRepository;
    private final RoomBlockoutRepository roomBlockoutRepository;
    private final IndependentSitePageSchemaValidator pageSchemaValidator;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final IndependentSiteCanvasValidator canvasValidator;
    private final EntitlementService entitlementService;

    public IndependentSiteQuoteService(
            IndependentSiteRepository siteRepository,
            IndependentSitePublicationRepository publicationRepository,
            IndependentSitePageRepository pageRepository,
            StoreRepository storeRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository,
            RoomTypePricePlanRepository roomTypePricePlanRepository,
            RoomPriceRepository roomPriceRepository,
            ReservationRepository reservationRepository,
            RoomBlockoutRepository roomBlockoutRepository,
            IndependentSitePageSchemaValidator pageSchemaValidator,
            ObjectMapper objectMapper,
            Clock clock,
            IndependentSiteCanvasValidator canvasValidator,
            EntitlementService entitlementService
    ) {
        this.siteRepository = siteRepository;
        this.publicationRepository = publicationRepository;
        this.pageRepository = pageRepository;
        this.storeRepository = storeRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.roomTypePricePlanRepository = roomTypePricePlanRepository;
        this.roomPriceRepository = roomPriceRepository;
        this.reservationRepository = reservationRepository;
        this.roomBlockoutRepository = roomBlockoutRepository;
        this.pageSchemaValidator = pageSchemaValidator;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.canvasValidator = canvasValidator;
        this.entitlementService = entitlementService;
    }

    @Transactional(readOnly = true)
    public IndependentSiteDtos.PublicSiteResponse getPublicSite(String slug) {
        IndependentSite site = resolveEnabledSite(slug);
        var store = storeRepository.findById(site.getStoreId())
                .orElseThrow(this::siteUnavailable);
        PublicationScope scope = loadPublicationScope(site);

        List<RoomType> roomTypes = scope.roomTypeIds().isEmpty()
                ? List.of()
                : roomTypeRepository.findByStoreIdAndIdIn(
                        site.getStoreId(),
                        new ArrayList<>(scope.roomTypeIds())
                );
        roomTypes = roomTypes.stream()
                .sorted(Comparator.comparing(RoomType::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        List<Room> rooms = scope.roomIds().isEmpty()
                ? List.of()
                : roomRepository.findByStoreIdAndIdIn(site.getStoreId(), scope.roomIds());
        rooms = rooms.stream()
                .sorted(Comparator.comparing(Room::getRoomNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        List<IndependentSiteDtos.PublicRoomType> publicRoomTypes = roomTypes.stream()
                .map(roomType -> new IndependentSiteDtos.PublicRoomType(
                        roomType.getId(),
                        roomType.getName(),
                        roomType.getCode(),
                        roomType.getDescription(),
                        roomType.getMaxGuests(),
                        roomType.getMaxChildOccupancy(),
                        roomType.getSizeMeasurement(),
                        roomType.getSizeMeasurementUnit(),
                        roomType.getDesktopPhotoUrls(),
                        roomType.getMobilePhotoUrls(),
                        roomType.getFacilities()
                ))
                .toList();
        List<IndependentSiteDtos.PublicRoom> publicRooms = rooms.stream()
                .map(room -> new IndependentSiteDtos.PublicRoom(
                        room.getId(),
                        room.getRoomType() != null ? room.getRoomType().getId() : null,
                        room.getRoomNumber()
                ))
                .toList();

        IndependentSitePage homePage = publishedHomePage(site);
        return new IndependentSiteDtos.PublicSiteResponse(
                site.getSlug(),
                store.getName(),
                store.getDescription(),
                store.getLogo(),
                store.getAddress(),
                store.getCity(),
                store.getState(),
                store.getCountry(),
                currency(store.getCurrency()),
                readAndValidateSchema(homePage),
                publicRoomTypes,
                publicRooms,
                site.getPaymentProvider().name(),
                Boolean.TRUE.equals(site.getSimulatedPaymentEnabled()),
                paymentNotice(site),
                site.getThemeKey(),
                publicPageNavItems(site),
                homePage.getFormat().name(),
                siteClosed(site)
        );
    }

    @Transactional(readOnly = true)
    public IndependentSiteDtos.PublicPageResponse getPublicPage(String rawSlug, String rawPath) {
        IndependentSite site = resolveEnabledSite(rawSlug);
        String path = normalizePublicPagePath(rawPath);
        IndependentSitePage page = pageRepository
                .findByStoreIdAndSiteIdAndPath(site.getStoreId(), site.getId(), path)
                .filter(value -> Boolean.TRUE.equals(value.getEnabled()))
                .filter(value -> value.getPublishedAt() != null)
                .orElseThrow(this::siteUnavailable);
        return new IndependentSiteDtos.PublicPageResponse(
                page.getPath(),
                page.getTitle(),
                page.getSeoDescription(),
                page.getType() != null ? page.getType().name() : null,
                page.getRoomTypeId(),
                readAndValidateSchema(page),
                page.getFormat().name(),
                siteClosed(site)
        );
    }

    /**
     * 门店独立站权益（independent_website）缺失时 closed=true——前端据此展示维护页，
     * 与公开交易端点 403「该店铺暂停接单」的判定口径一致（fail-closed）。
     */
    private boolean siteClosed(IndependentSite site) {
        return !entitlementService.storeHasFeature(site.getStoreId(), SaasFeatureCodes.INDEPENDENT_WEBSITE);
    }

    /**
     * 公开交易端点的权益守卫用：按站点 slug 解析归属门店 id。
     * 站点不可用（slug 非法/未发布/渠道校验失败）时与正文端点一致地抛 404。
     */
    public Long resolveEnabledStoreId(String slug) {
        return resolveEnabledSite(slug).getStoreId();
    }

    /** 公开站点的支付提示文案：STRIPE 站点明示真实收卡；SIMULATED 维持既有模拟语义。 */
    private static String paymentNotice(IndependentSite site) {
        if (site.getPaymentProvider() == IndependentSitePaymentProvider.STRIPE) {
            return "本站使用 Stripe 安全收款，支付成功后订单自动确认";
        }
        return Boolean.TRUE.equals(site.getSimulatedPaymentEnabled())
                ? "当前为模拟支付，不会产生真实扣款"
                : "当前站点未开放在线支付";
    }

    private IndependentSitePage publishedHomePage(IndependentSite site) {
        return pageRepository
                .findBySiteIdAndTypeAndPublishedAtIsNotNullAndEnabledTrue(
                        site.getId(),
                        IndependentSitePageType.HOME
                )
                .orElseThrow(this::siteUnavailable);
    }

    private List<IndependentSiteDtos.PublicPageNavItem> publicPageNavItems(IndependentSite site) {
        return pageRepository
                .findBySiteIdAndPublishedAtIsNotNullAndEnabledTrueOrderBySortOrderAscIdAsc(site.getId())
                .stream()
                .map(page -> new IndependentSiteDtos.PublicPageNavItem(
                        page.getPath(),
                        page.getTitle(),
                        page.getType() != null ? page.getType().name() : null,
                        page.getRoomTypeId()
                ))
                .toList();
    }

    private String normalizePublicPagePath(String rawPath) {
        String normalized = rawPath == null ? "" : rawPath.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.matches("^/[a-z0-9][a-z0-9/-]{0,119}$")) {
            throw siteUnavailable();
        }
        return normalized;
    }

    @Transactional(readOnly = true)
    public IndependentSiteDtos.QuoteResponse quote(
            String slug,
            IndependentSiteDtos.QuoteRequest request
    ) {
        return calculate(resolveEnabledSite(slug), request).response();
    }

    QuoteComputation calculate(IndependentSite site, IndependentSiteDtos.QuoteRequest request) {
        validateSiteChannel(site);
        validateRequest(site, request);
        Long storeId = site.getStoreId();
        Channel channel = site.getChannel();
        PricePlan pricePlan = channel.getDefaultPricePlan();

        PublicationScope scope = loadPublicationScope(site);
        if (!scope.roomTypeIds().contains(request.roomTypeId())) {
            throw unprocessable("ROOM_TYPE_NOT_PUBLISHED", "所选房型当前不可预订");
        }
        RoomType roomType = roomTypeRepository.findByStoreIdAndId(storeId, request.roomTypeId())
                .orElseThrow(() -> unprocessable("ROOM_TYPE_NOT_PUBLISHED", "所选房型当前不可预订"));
        RoomTypePricePlan mapping = roomTypePricePlanRepository
                .findByStoreIdAndRoomTypeIdAndPricePlanId(storeId, roomType.getId(), pricePlan.getId())
                .orElseThrow(() -> unprocessable("PRICE_PLAN_NOT_AVAILABLE", "所选房型暂无有效价格计划"));

        int maxGuestsPerRoom = positiveMinimum(mapping.getMaxGuests(), roomType.getMaxGuests(), 1);
        int maxChildrenPerRoom = Math.max(
                roomType.getMaxChildOccupancy() == null ? 0 : roomType.getMaxChildOccupancy(),
                0
        );
        List<GuestAllocation> guestAllocations = allocateGuests(
                request.rooms(),
                request.adults(),
                request.children(),
                maxGuestsPerRoom,
                maxChildrenPerRoom
        );

        List<RoomPrice> roomPrices = roomPriceRepository
                .findByStoreIdAndRoomTypeIdAndPricePlanIdAndPriceDateBetween(
                        storeId,
                        roomType.getId(),
                        pricePlan.getId(),
                        request.checkInDate(),
                        request.checkOutDate()
                );
        Map<LocalDate, RoomPrice> priceByDate = new HashMap<>();
        for (RoomPrice roomPrice : roomPrices) {
            priceByDate.put(roomPrice.getPriceDate(), roomPrice);
        }

        int nights = (int) ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        validateRestrictions(request, pricePlan, priceByDate, nights);

        List<Room> eligibleRooms = eligibleRooms(site, scope, roomType);
        List<Long> candidateRoomIds = availableRoomIds(
                storeId,
                eligibleRooms,
                request.checkInDate(),
                request.checkOutDate()
        );
        InventoryQuota inventoryQuota = calculateInventoryQuota(
                storeId,
                roomType,
                priceByDate,
                request.checkInDate(),
                request.checkOutDate()
        );
        int effectiveAvailable = Math.min(
                candidateRoomIds.size(),
                inventoryQuota.minimumAvailableRooms()
        );
        if (effectiveAvailable < request.rooms()) {
            throw conflict("NO_AVAILABILITY", "所选日期房量不足，请调整日期或房间数");
        }

        List<IndependentSiteDtos.NightlyRate> nightlyRates = new ArrayList<>();
        List<List<DailyAmount>> dailyAmountsByReservation = new ArrayList<>();
        for (int i = 0; i < request.rooms(); i++) {
            dailyAmountsByReservation.add(new ArrayList<>());
        }
        List<BigDecimal> reservationTotals = new ArrayList<>();
        for (int i = 0; i < request.rooms(); i++) {
            reservationTotals.add(BigDecimal.ZERO.setScale(2));
        }

        BigDecimal total = BigDecimal.ZERO.setScale(2);
        LocalDate date = request.checkInDate();
        while (date.isBefore(request.checkOutDate())) {
            RoomPrice roomPrice = priceByDate.get(date);
            LocalBasePriceResolver.Result resolved =
                    LocalBasePriceResolver.resolve(roomPrice, mapping, roomType, date);
            if (resolved.basePrice() == null || resolved.basePrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw unprocessable("MISSING_PRICE", "所选日期存在未配置价格");
            }
            BigDecimal baseRoomPrice = resolved.basePrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal adjustedRoomPrice =
                    IndependentSitePricePolicy.calculateNightPrice(storeId, channel, baseRoomPrice);
            BigDecimal extraGuestsForNight = BigDecimal.ZERO.setScale(2);
            BigDecimal nightTotal = BigDecimal.ZERO.setScale(2);

            for (int roomIndex = 0; roomIndex < guestAllocations.size(); roomIndex++) {
                GuestAllocation allocation = guestAllocations.get(roomIndex);
                BigDecimal extra = calculateExtraGuestAmount(mapping, allocation);
                BigDecimal roomNightTotal = adjustedRoomPrice.add(extra).setScale(2, RoundingMode.HALF_UP);
                extraGuestsForNight = extraGuestsForNight.add(extra);
                nightTotal = nightTotal.add(roomNightTotal);
                reservationTotals.set(
                        roomIndex,
                        reservationTotals.get(roomIndex).add(roomNightTotal).setScale(2, RoundingMode.HALF_UP)
                );
                dailyAmountsByReservation.get(roomIndex).add(new DailyAmount(date, roomNightTotal));
            }
            nightTotal = nightTotal.setScale(2, RoundingMode.HALF_UP);
            total = total.add(nightTotal).setScale(2, RoundingMode.HALF_UP);
            nightlyRates.add(new IndependentSiteDtos.NightlyRate(
                    date,
                    baseRoomPrice,
                    adjustedRoomPrice,
                    extraGuestsForNight.setScale(2, RoundingMode.HALF_UP),
                    nightTotal
            ));
            date = date.plusDays(1);
        }

        OffsetDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).atOffset(ZoneOffset.UTC);
        String currency = storeRepository.findById(storeId)
                .map(store -> currency(store.getCurrency()))
                .orElse("CNY");
        BigDecimal adjustment = channel.getPriceAdjustmentValue() == null
                ? BigDecimal.ZERO.setScale(2)
                : channel.getPriceAdjustmentValue().setScale(2, RoundingMode.HALF_UP);
        IndependentSiteDtos.QuoteResponse response = new IndependentSiteDtos.QuoteResponse(
                site.getSlug(),
                roomType.getId(),
                roomType.getName(),
                request.checkInDate(),
                request.checkOutDate(),
                request.rooms(),
                request.adults(),
                request.children(),
                effectiveAvailable,
                currency,
                adjustment,
                List.copyOf(nightlyRates),
                total,
                now,
                now.plusMinutes(QUOTE_TTL_MINUTES)
        );
        return new QuoteComputation(
                site,
                roomType,
                pricePlan,
                mapping,
                List.copyOf(candidateRoomIds),
                List.copyOf(guestAllocations),
                copyDailyAmounts(dailyAmountsByReservation),
                List.copyOf(reservationTotals),
                response
        );
    }

    IndependentSite resolveEnabledSite(String rawSlug) {
        String slug = rawSlug == null ? "" : rawSlug.trim().toLowerCase(java.util.Locale.ROOT);
        if (!slug.matches("[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?")) {
            throw siteUnavailable();
        }
        IndependentSite site = siteRepository.findEnabledBySlugWithChannel(slug)
                .orElseThrow(this::siteUnavailable);
        validateSiteChannel(site);
        return site;
    }

    void validateSiteChannel(IndependentSite site) {
        Channel channel = site != null ? site.getChannel() : null;
        if (site == null
                || !Boolean.TRUE.equals(site.getEnabled())
                || site.getPublishedAt() == null
                || channel == null
                || !Objects.equals(site.getStoreId(), channel.getStoreId())
                || !IndependentSiteManagementService.BOOKING_ENGINE_CHANNEL_CODE.equals(channel.getCode())
                || !Boolean.TRUE.equals(channel.getEnabled())
                || channel.getPriceAdjustmentType() != PriceAdjustmentType.PERCENTAGE
                || channel.getDefaultPricePlan() == null
                || !Objects.equals(site.getStoreId(), channel.getDefaultPricePlan().getStoreId())) {
            throw siteUnavailable();
        }
        // 站点可预订前提：存在已发布且启用的 HOME 页
        pageRepository
                .findBySiteIdAndTypeAndPublishedAtIsNotNullAndEnabledTrue(
                        site.getId(),
                        IndependentSitePageType.HOME
                )
                .orElseThrow(this::siteUnavailable);
    }

    void assertHoldInventoryAvailable(
            QuoteComputation quote,
            IndependentSiteDtos.QuoteRequest request
    ) {
        List<RoomPrice> lockedPrices = roomPriceRepository
                .findByStoreIdAndRoomTypeIdAndPricePlanIdAndPriceDateBetweenForUpdate(
                        quote.site().getStoreId(),
                        quote.roomType().getId(),
                        quote.pricePlan().getId(),
                        request.checkInDate(),
                        request.checkOutDate()
                );
        Map<LocalDate, RoomPrice> priceByDate = indexRoomPrices(lockedPrices);
        int nights = (int) ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        validateRestrictions(request, quote.pricePlan(), priceByDate, nights);
        InventoryQuota inventoryQuota = calculateInventoryQuota(
                quote.site().getStoreId(),
                quote.roomType(),
                priceByDate,
                request.checkInDate(),
                request.checkOutDate()
        );
        if (inventoryQuota.minimumAvailableRooms() < request.rooms()) {
            throw conflict("NO_AVAILABILITY", "每日库存配额刚刚发生变化，请重新报价");
        }
    }

    private void validateRequest(IndependentSite site, IndependentSiteDtos.QuoteRequest request) {
        if (request == null || request.roomTypeId() == null
                || request.checkInDate() == null || request.checkOutDate() == null) {
            throw badRequest("INVALID_STAY", "请完整填写房型、入住和退房日期");
        }
        if (request.rooms() < 1 || request.rooms() > 10
                || request.adults() < 1 || request.adults() > 100
                || request.children() < 0 || request.children() > 100) {
            throw badRequest("INVALID_OCCUPANCY", "房间数或入住人数不正确");
        }
        if (request.adults() < request.rooms()) {
            throw badRequest("INVALID_OCCUPANCY", "每间房至少需要一位成人");
        }
        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw badRequest("INVALID_DATES", "退房日期必须晚于入住日期");
        }
        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        if (nights > MAX_STAY_NIGHTS) {
            throw badRequest("INVALID_DATES", "单次入住不可超过 365 晚");
        }
        var store = storeRepository.findById(site.getStoreId()).orElseThrow(this::siteUnavailable);
        ZoneId storeZoneId = StoreTimeZoneUtil.resolveZoneId(store);
        LocalDate today = LocalDate.now(clock.withZone(storeZoneId));
        if (request.checkInDate().isBefore(today)) {
            throw badRequest("INVALID_DATES", "入住日期不可早于今天");
        }
    }

    private static void validateRestrictions(
            IndependentSiteDtos.QuoteRequest request,
            PricePlan pricePlan,
            Map<LocalDate, RoomPrice> priceByDate,
            int nights
    ) {
        RoomPrice arrival = priceByDate.get(request.checkInDate());
        if (arrival != null && Boolean.TRUE.equals(arrival.getCta())) {
            throw unprocessable("CLOSED_TO_ARRIVAL", "所选入住日期不可办理入住");
        }
        RoomPrice departure = priceByDate.get(request.checkOutDate());
        if (departure != null && Boolean.TRUE.equals(departure.getCtd())) {
            throw unprocessable("CLOSED_TO_DEPARTURE", "所选退房日期不可办理退房");
        }

        int effectiveMin = positive(pricePlan.getMinNights(), 1);
        int effectiveMax = positive(pricePlan.getMaxNights(), MAX_STAY_NIGHTS);
        LocalDate date = request.checkInDate();
        while (date.isBefore(request.checkOutDate())) {
            RoomPrice roomPrice = priceByDate.get(date);
            if (roomPrice != null) {
                if (Boolean.TRUE.equals(roomPrice.getCloseRoom())) {
                    throw unprocessable("STOP_SELL", "所选日期包含停售房晚");
                }
                if (roomPrice.getMinStay() != null && roomPrice.getMinStay() > 0) {
                    effectiveMin = Math.max(effectiveMin, roomPrice.getMinStay());
                }
                if (roomPrice.getMaxStay() != null && roomPrice.getMaxStay() > 0) {
                    effectiveMax = Math.min(effectiveMax, roomPrice.getMaxStay());
                }
            }
            date = date.plusDays(1);
        }
        if (nights < effectiveMin) {
            throw unprocessable("MIN_STAY_NOT_MET", "所选价格计划至少入住 " + effectiveMin + " 晚");
        }
        if (nights > effectiveMax) {
            throw unprocessable("MAX_STAY_EXCEEDED", "所选价格计划最多入住 " + effectiveMax + " 晚");
        }
    }

    private List<Room> eligibleRooms(
            IndependentSite site,
            PublicationScope scope,
            RoomType roomType
    ) {
        List<Room> rooms = roomRepository.findByStoreIdAndRoomTypeIdAndStatus(
                site.getStoreId(),
                roomType.getId(),
                RoomStatus.AVAILABLE
        );
        Set<Long> explicitlyPublishedForType = new HashSet<>();
        if (!scope.roomIds().isEmpty()) {
            List<Room> publishedRooms = roomRepository.findByStoreIdAndIdIn(
                    site.getStoreId(),
                    scope.roomIds()
            );
            for (Room room : publishedRooms) {
                if (room.getRoomType() != null
                        && Objects.equals(room.getRoomType().getId(), roomType.getId())
                        && room.getId() != null) {
                    explicitlyPublishedForType.add(room.getId());
                }
            }
        }
        if (!explicitlyPublishedForType.isEmpty()) {
            rooms = rooms.stream()
                    .filter(room -> explicitlyPublishedForType.contains(room.getId()))
                    .toList();
        }
        return rooms.stream()
                .filter(room -> Objects.equals(site.getStoreId(), room.getStoreId()))
                .filter(room -> room.getRoomType() != null
                        && Objects.equals(roomType.getId(), room.getRoomType().getId()))
                .sorted(Comparator.comparing(Room::getId))
                .toList();
    }

    private List<Long> availableRoomIds(
            Long storeId,
            List<Room> eligibleRooms,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        List<Long> roomIds = eligibleRooms.stream().map(Room::getId).filter(Objects::nonNull).toList();
        if (roomIds.isEmpty()) {
            return List.of();
        }
        LocalDate lastNight = checkOut.minusDays(1);
        List<Reservation> reservations =
                reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                        storeId,
                        roomIds,
                        checkIn,
                        lastNight,
                        BLOCKING_STATUSES
                );
        Set<Long> occupiedRoomIds = new HashSet<>();
        for (Reservation reservation : reservations) {
            if (reservation.getRoom() != null && reservation.getRoom().getId() != null) {
                occupiedRoomIds.add(reservation.getRoom().getId());
            }
        }
        List<RoomBlockout> blockouts = roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                storeId,
                roomIds,
                checkIn,
                lastNight
        );
        Set<Long> blockedRoomIds = new HashSet<>();
        for (RoomBlockout blockout : blockouts) {
            if (blockout.getRoom() != null && blockout.getRoom().getId() != null) {
                blockedRoomIds.add(blockout.getRoom().getId());
            }
        }
        return roomIds.stream()
                .filter(roomId -> !occupiedRoomIds.contains(roomId))
                .filter(roomId -> !blockedRoomIds.contains(roomId))
                .sorted()
                .toList();
    }

    private InventoryQuota calculateInventoryQuota(
            Long storeId,
            RoomType roomType,
            Map<LocalDate, RoomPrice> priceByDate,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        List<ReservationRepository.ReservationOccupancyRow> occupancyRows =
                reservationRepository.findOccupancyRowsByStoreIdAndDateRangeAndStatuses(
                        storeId,
                        checkIn,
                        checkOut,
                        QUOTA_OCCUPANCY_STATUSES
                );
        List<RoomBlockout> blockouts = roomBlockoutRepository.findByStoreIdAndBlockDateBetween(
                storeId,
                checkIn,
                checkOut.minusDays(1)
        );
        ZoneId storeZoneId = StoreTimeZoneUtil.resolveZoneId(
                storeRepository.findById(storeId).orElse(null)
        );
        Map<LocalDate, Integer> availableByDate = new LinkedHashMap<>();
        int minimum = Integer.MAX_VALUE;
        LocalDate date = checkIn;
        while (date.isBefore(checkOut)) {
            RoomPrice roomPrice = priceByDate.get(date);
            int baseAvailable = roomPrice != null && roomPrice.getAvailableRooms() != null
                    ? Math.max(roomPrice.getAvailableRooms(), 0)
                    : Math.max(roomType.getTotalRooms() == null ? 0 : roomType.getTotalRooms(), 0);
            int occupied = 0;
            for (ReservationRepository.ReservationOccupancyRow row : occupancyRows) {
                if (row == null) {
                    continue;
                }
                Long effectiveRoomTypeId = row.getAssignedRoomTypeId() != null
                        ? row.getAssignedRoomTypeId()
                        : row.getOtaRoomTypeId();
                if (!Objects.equals(effectiveRoomTypeId, roomType.getId())) {
                    continue;
                }
                if (ReservationOccupancyProjection.occupiesDate(
                        row.getCheckInDate(),
                        row.getCheckOutDate(),
                        row.getStatus(),
                        row.getActualCheckOut(),
                        date,
                        QUOTA_OCCUPANCY_STATUSES,
                        storeZoneId
                )) {
                    occupied++;
                }
            }
            int blocked = 0;
            for (RoomBlockout blockout : blockouts) {
                if (blockout != null
                        && Objects.equals(blockout.getBlockDate(), date)
                        && blockout.getRoom() != null
                        && blockout.getRoom().getRoomType() != null
                        && Objects.equals(
                                blockout.getRoom().getRoomType().getId(),
                                roomType.getId()
                        )) {
                    blocked++;
                }
            }
            int remaining = Math.max(baseAvailable - occupied - blocked, 0);
            availableByDate.put(date, remaining);
            minimum = Math.min(minimum, remaining);
            date = date.plusDays(1);
        }
        return new InventoryQuota(
                minimum == Integer.MAX_VALUE ? 0 : minimum,
                Map.copyOf(availableByDate)
        );
    }

    private static Map<LocalDate, RoomPrice> indexRoomPrices(List<RoomPrice> roomPrices) {
        Map<LocalDate, RoomPrice> priceByDate = new HashMap<>();
        if (roomPrices == null) {
            return priceByDate;
        }
        for (RoomPrice roomPrice : roomPrices) {
            if (roomPrice != null && roomPrice.getPriceDate() != null) {
                priceByDate.put(roomPrice.getPriceDate(), roomPrice);
            }
        }
        return priceByDate;
    }

    static List<GuestAllocation> allocateGuests(
            int rooms,
            int adults,
            int children,
            int maxGuestsPerRoom,
            int maxChildrenPerRoom
    ) {
        if (rooms < 1 || adults < rooms) {
            throw badRequest("INVALID_OCCUPANCY", "每间房至少需要一位成人");
        }
        if (adults + children > rooms * maxGuestsPerRoom
                || children > rooms * maxChildrenPerRoom) {
            throw unprocessable("OCCUPANCY_EXCEEDED", "入住人数超过所选房型容量");
        }
        int[] adultsByRoom = new int[rooms];
        int[] childrenByRoom = new int[rooms];
        java.util.Arrays.fill(adultsByRoom, 1);
        int adultsRemaining = adults - rooms;
        for (int roomIndex = 0; adultsRemaining > 0; roomIndex = (roomIndex + 1) % rooms) {
            if (adultsByRoom[roomIndex] + childrenByRoom[roomIndex] >= maxGuestsPerRoom) {
                continue;
            }
            adultsByRoom[roomIndex]++;
            adultsRemaining--;
        }
        int childrenRemaining = children;
        for (int roomIndex = 0; childrenRemaining > 0; roomIndex = (roomIndex + 1) % rooms) {
            if (childrenByRoom[roomIndex] >= maxChildrenPerRoom
                    || adultsByRoom[roomIndex] + childrenByRoom[roomIndex] >= maxGuestsPerRoom) {
                continue;
            }
            childrenByRoom[roomIndex]++;
            childrenRemaining--;
        }
        List<GuestAllocation> allocations = new ArrayList<>();
        for (int i = 0; i < rooms; i++) {
            allocations.add(new GuestAllocation(adultsByRoom[i], childrenByRoom[i]));
        }
        return allocations;
    }

    private static BigDecimal calculateExtraGuestAmount(
            RoomTypePricePlan mapping,
            GuestAllocation allocation
    ) {
        int included = mapping.getIncludedGuests() != null && mapping.getIncludedGuests() > 0
                ? mapping.getIncludedGuests()
                : positive(mapping.getMaxGuests(), 1);
        int includedAdults = Math.min(allocation.adults(), included);
        int includedRemaining = Math.max(included - includedAdults, 0);
        int extraAdults = Math.max(allocation.adults() - includedAdults, 0);
        int extraChildren = Math.max(allocation.children() - includedRemaining, 0);
        BigDecimal adultRate = nonNegative(mapping.getExtraAdultRate());
        BigDecimal childRate = nonNegative(mapping.getExtraChildRate());
        return adultRate.multiply(BigDecimal.valueOf(extraAdults))
                .add(childRate.multiply(BigDecimal.valueOf(extraChildren)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PublicationScope loadPublicationScope(IndependentSite site) {
        List<IndependentSitePublication> rows =
                publicationRepository.findByStoreIdAndSiteIdAndEnabledTrueOrderByDisplayOrderAscIdAsc(
                        site.getStoreId(),
                        site.getId()
                );
        Set<Long> roomTypeIds = new LinkedHashSet<>();
        Set<Long> roomIds = new LinkedHashSet<>();
        for (IndependentSitePublication row : rows) {
            if (row.getTargetType() == IndependentSitePublicationType.ROOM_TYPE) {
                roomTypeIds.add(row.getTargetId());
            } else if (row.getTargetType() == IndependentSitePublicationType.ROOM) {
                roomIds.add(row.getTargetId());
            }
        }
        if (roomTypeIds.isEmpty()) {
            throw siteUnavailable();
        }
        return new PublicationScope(Set.copyOf(roomTypeIds), Set.copyOf(roomIds));
    }

    /** 公开端读取已发布 schema：按页面 format 分派校验器，任何失败都 fail-closed 为站点不可用。 */
    private JsonNode readAndValidateSchema(IndependentSitePage page) {
        try {
            JsonNode schema = objectMapper.readTree(page.getPublishedSchemaJson());
            return page.getFormat() == IndependentSitePageFormat.CANVAS
                    ? canvasValidator.validate(schema)
                    : pageSchemaValidator.validate(schema);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw siteUnavailable();
        }
    }

    private static List<List<DailyAmount>> copyDailyAmounts(List<List<DailyAmount>> source) {
        List<List<DailyAmount>> result = new ArrayList<>();
        for (List<DailyAmount> values : source) {
            result.add(List.copyOf(values));
        }
        return List.copyOf(result);
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static int positive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private static int positiveMinimum(Integer first, Integer second, int fallback) {
        int a = positive(first, fallback);
        int b = positive(second, fallback);
        return Math.min(a, b);
    }

    private static String currency(String value) {
        return value == null || value.isBlank()
                ? "CNY"
                : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private IndependentSiteServiceException siteUnavailable() {
        return new IndependentSiteServiceException(
                HttpStatus.NOT_FOUND,
                "SITE_UNAVAILABLE",
                "独立站不存在或当前不可用"
        );
    }

    private static IndependentSiteServiceException badRequest(String code, String message) {
        return new IndependentSiteServiceException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static IndependentSiteServiceException unprocessable(String code, String message) {
        return new IndependentSiteServiceException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    private static IndependentSiteServiceException conflict(String code, String message) {
        return new IndependentSiteServiceException(HttpStatus.CONFLICT, code, message);
    }

    record PublicationScope(Set<Long> roomTypeIds, Set<Long> roomIds) {
    }

    record InventoryQuota(int minimumAvailableRooms, Map<LocalDate, Integer> availableByDate) {
    }

    public record GuestAllocation(int adults, int children) {
    }

    public record DailyAmount(LocalDate date, BigDecimal amount) {
    }

    public record QuoteComputation(
            IndependentSite site,
            RoomType roomType,
            PricePlan pricePlan,
            RoomTypePricePlan mapping,
            List<Long> candidateRoomIds,
            List<GuestAllocation> guestAllocations,
            List<List<DailyAmount>> dailyAmountsByReservation,
            List<BigDecimal> reservationTotals,
            IndependentSiteDtos.QuoteResponse response
    ) {
    }
}
