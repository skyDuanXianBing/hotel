package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.Room;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.util.LocalBasePriceResolver;
import server.demo.util.SuRoomIdUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * PMS -> Su: push room rates (room_number + rateplan) via invratecontrol.ratecontrol.
 * <p>
 * Pricing source priority:
 * room_prices (roomType + pricePlan + date) >
 * room_type_price_plans (weekday price) >
 * room_type (weekday/weekend/default).
 */
@Service
public class SuRateSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SuRateSyncService.class);
    private static final Logger pmsPushLogger = LoggerFactory.getLogger("SU_PMS_PUSH");

    private static final int DEFAULT_DAYS = 365;
    private static final int MAX_DAYS = 500;
    private static final int BATCH_ITEMS = 100;

    // Su OTACode: Booking=19, Airbnb=244
    private static final List<Integer> DEFAULT_SU_OTA_CODES = List.of(19, 244);

    private final RoomRepository roomRepository;
    private final RoomTypePricePlanRepository roomTypePricePlanRepository;
    private final RoomPriceRepository roomPriceRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;

    public SuRateSyncService(
            RoomRepository roomRepository,
            RoomTypePricePlanRepository roomTypePricePlanRepository,
            RoomPriceRepository roomPriceRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService
    ) {
        this.roomRepository = roomRepository;
        this.roomTypePricePlanRepository = roomTypePricePlanRepository;
        this.roomPriceRepository = roomPriceRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
    }

    public record MissingRate(
            String roomId,
            String rateId,
            int missingDays
    ) {}

    public record SuRateSyncSummary(
            int roomCount,
            int ratePlanCount,
            int combinationsConsidered,
            int combinationsPushed,
            int combinationsSkippedNoPrice,
            int requestsPosted,
            int days,
            LocalDate startDate,
            LocalDate endDate,
            boolean rateSynced,
            String error,
            List<MissingRate> missingRates
    ) {}

    public SuRateSyncSummary syncRoomRatesForNextDays(Long storeId, String hotelId, Integer days) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId is required");
        }

        int effectiveDays = normalizeDays(days);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(effectiveDays - 1L);

        logger.info(
                "[SuRateSync] start. storeId={}, hotelId={}, days={}, startDate={}, endDate={}",
                storeId,
                hotelId,
                effectiveDays,
                startDate,
                endDate
        );
        pmsPushLogger.info(
            "[SuRateSync] start. storeId={}, hotelId={}, days={}, startDate={}, endDate={}",
            storeId,
            hotelId,
            effectiveDays,
            startDate,
            endDate
        );

        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        if (rooms == null || rooms.isEmpty()) {
            logger.info("Skip Su rate sync: no rooms. storeId={}, hotelId={}", storeId, hotelId);
            return new SuRateSyncSummary(0, 0, 0, 0, 0, 0, effectiveDays, startDate, endDate, true, null, List.of());
        }
        List<Room> eligibleRooms = rooms.stream()
                .filter(r -> r != null && r.getRoomNumber() != null && !r.getRoomNumber().isBlank())
                .toList();
        SuRoomIdUtil.assertRoomIdsWithinLimit(eligibleRooms);

        List<RoomTypePricePlan> rtpps = roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(storeId);
        Map<String, RoomTypePricePlan> rtppByKey = new HashMap<>();
        Map<Long, Set<Long>> planIdsByRoomTypeId = new HashMap<>();

        for (RoomTypePricePlan rtpp : rtpps) {
            if (rtpp == null || rtpp.getRoomType() == null || rtpp.getPricePlan() == null) {
                continue;
            }
            Long roomTypeId = rtpp.getRoomType().getId();
            Long planId = rtpp.getPricePlan().getId();
            if (roomTypeId == null || planId == null) {
                continue;
            }
            rtppByKey.put(roomPlanKey(roomTypeId, planId), rtpp);
            planIdsByRoomTypeId.computeIfAbsent(roomTypeId, ignored -> new TreeSet<>()).add(planId);
        }

        List<RoomPrice> roomPrices = roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                storeId,
                startDate,
                endDate
        );
        Map<String, RoomPrice> roomPriceByKey = new HashMap<>();
        for (RoomPrice rp : roomPrices) {
            if (rp == null || rp.getRoomType() == null || rp.getRoomType().getId() == null) {
                continue;
            }
            if (rp.getPricePlan() == null || rp.getPricePlan().getId() == null) {
                continue;
            }
            if (rp.getPriceDate() == null) {
                continue;
            }
            Long roomTypeId = rp.getRoomType().getId();
            Long planId = rp.getPricePlan().getId();
            roomPriceByKey.put(roomPlanDateKey(roomTypeId, planId, rp.getPriceDate()), rp);
            planIdsByRoomTypeId.computeIfAbsent(roomTypeId, ignored -> new TreeSet<>()).add(planId);
        }

        int combinationsConsidered = 0;
        int combinationsPushed = 0;
        int combinationsSkippedNoPrice = 0;
        int requestsPosted = 0;
        List<MissingRate> missingRates = new ArrayList<>();

        Set<Long> distinctPlans = new TreeSet<>();
        List<Map<String, Object>> rateControls = new ArrayList<>();

        for (Room room : rooms) {
            if (room == null || room.getRoomNumber() == null || room.getRoomNumber().isBlank()) {
                continue;
            }
            RoomType roomType = room.getRoomType();
            if (roomType == null || roomType.getId() == null) {
                continue;
            }

            String suRoomId = SuRoomIdUtil.buildRoomId(room);
            Set<Long> planIds = planIdsByRoomTypeId.get(roomType.getId());
            if (planIds == null || planIds.isEmpty()) {
                continue;
            }

            for (Long planId : planIds) {
                if (planId == null) {
                    continue;
                }
                combinationsConsidered++;
                distinctPlans.add(planId);

                RoomTypePricePlan rtpp = rtppByKey.get(roomPlanKey(roomType.getId(), planId));

                List<String> dailyPriceValues = new ArrayList<>(effectiveDays);
                int missingDays = 0;
                for (int i = 0; i < effectiveDays; i++) {
                    LocalDate date = startDate.plusDays(i);
                    RoomPrice roomPrice = roomPriceByKey.get(roomPlanDateKey(roomType.getId(), planId, date));
                    LocalBasePriceResolver.Result local = LocalBasePriceResolver.resolve(roomPrice, rtpp, roomType, date);
                    String priceValue = normalizePositivePrice(local.basePrice());
                    if (priceValue == null) {
                        missingDays++;
                    }
                    dailyPriceValues.add(priceValue);
                }

                if (missingDays > 0) {
                    combinationsSkippedNoPrice++;
                    if (missingRates.size() < 200) {
                        missingRates.add(new MissingRate(suRoomId, planId.toString(), missingDays));
                    }
                    continue;
                }

                List<Map<String, Object>> dateSegments = toDateSegments(dailyPriceValues, startDate);
                if (dateSegments.isEmpty()) {
                    combinationsSkippedNoPrice++;
                    if (missingRates.size() < 200) {
                        missingRates.add(new MissingRate(suRoomId, planId.toString(), effectiveDays));
                    }
                    continue;
                }

                Map<String, Object> rateControl = new LinkedHashMap<>();
                rateControl.put("roomid", suRoomId);
                rateControl.put("rateid", planId.toString());
                rateControl.put("date", dateSegments);

                rateControls.add(rateControl);
                combinationsPushed++;
            }
        }

        if (rateControls.isEmpty()) {
            logger.warn(
                    "[SuRateSync] nothing to push (all skipped). storeId={}, hotelId={}, rooms={}, considered={}, skippedNoPrice={}",
                    storeId,
                    hotelId,
                    rooms.size(),
                    combinationsConsidered,
                    combinationsSkippedNoPrice
            );
            pmsPushLogger.warn(
                "[SuRateSync] nothing to push (all skipped). storeId={}, hotelId={}, rooms={}, considered={}, skippedNoPrice={}",
                storeId,
                hotelId,
                rooms.size(),
                combinationsConsidered,
                combinationsSkippedNoPrice
            );
            return new SuRateSyncSummary(
                    rooms.size(),
                    distinctPlans.size(),
                    combinationsConsidered,
                    0,
                    combinationsSkippedNoPrice,
                    0,
                    effectiveDays,
                    startDate,
                    endDate,
                    true,
                    null,
                    missingRates
            );
        }

        try {
            int totalItems = rateControls.size();
            for (int from = 0; from < totalItems; from += BATCH_ITEMS) {
                int to = Math.min(totalItems, from + BATCH_ITEMS);
                List<Map<String, Object>> batch = rateControls.subList(from, to);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("hotelid", hotelId);
                payload.put("ratecontrol", batch);

                logger.info(
                        "[SuRateSync] posting invratecontrol(ratecontrol). storeId={}, hotelId={}, batchItems={}, totalItems={}, batchIndex={}",
                        storeId,
                        hotelId,
                        batch.size(),
                        totalItems,
                        (from / BATCH_ITEMS) + 1
                );
                pmsPushLogger.info(
                    "[SuRateSync] posting invratecontrol(ratecontrol). storeId={}, hotelId={}, batchItems={}, totalItems={}, batchIndex={}",
                    storeId,
                    hotelId,
                    batch.size(),
                    totalItems,
                    (from / BATCH_ITEMS) + 1
                );

                JsonNode response = suAccessTokenService.executeWithTokenRetry(
                        token -> suApiClient.postInvRateControl(token, payload),
                        "invratecontrol(ratecontrol)"
                );
                requestsPosted++;

                if (!suApiClient.isSuSuccess(response)) {
                    String err = suApiClient.extractSuErrorMessage(response);
                    logger.warn(
                            "[SuRateSync] invratecontrol returned fail. storeId={}, hotelId={}, err={}, raw={}",
                            storeId,
                            hotelId,
                            err,
                            response != null ? response.toString() : "null"
                    );
                    pmsPushLogger.error(
                        "[SuRateSync] invratecontrol returned fail. storeId={}, hotelId={}, err={}, raw={}",
                        storeId,
                        hotelId,
                        err,
                        response != null ? response.toString() : "null"
                    );
                    return new SuRateSyncSummary(
                            rooms.size(),
                            distinctPlans.size(),
                            combinationsConsidered,
                            combinationsPushed,
                            combinationsSkippedNoPrice,
                            requestsPosted,
                            effectiveDays,
                            startDate,
                            endDate,
                            false,
                            err,
                            missingRates
                    );
                }
            }

            logger.info(
                    "[SuRateSync] done. storeId={}, hotelId={}, ok=true, pushedItems={}, requests={}",
                    storeId,
                    hotelId,
                    rateControls.size(),
                    requestsPosted
            );
                pmsPushLogger.info(
                    "[SuRateSync] done. storeId={}, hotelId={}, ok=true, pushedItems={}, requests={}",
                    storeId,
                    hotelId,
                    rateControls.size(),
                    requestsPosted
                );
            return new SuRateSyncSummary(
                    rooms.size(),
                    distinctPlans.size(),
                    combinationsConsidered,
                    combinationsPushed,
                    combinationsSkippedNoPrice,
                    requestsPosted,
                    effectiveDays,
                    startDate,
                    endDate,
                    true,
                    null,
                    missingRates
            );
        } catch (RuntimeException e) {
            logger.error("Su rate sync failed. storeId={}, hotelId={}", storeId, hotelId, e);
            pmsPushLogger.error("[SuRateSync] failed. storeId={}, hotelId={}, err={}", storeId, hotelId, e.getMessage());
            return new SuRateSyncSummary(
                    rooms.size(),
                    distinctPlans.size(),
                    combinationsConsidered,
                    combinationsPushed,
                    combinationsSkippedNoPrice,
                    requestsPosted,
                    effectiveDays,
                    startDate,
                    endDate,
                    false,
                    e.getMessage(),
                    missingRates
            );
        }
    }

    private static int normalizeDays(Integer days) {
        int d = days != null ? days : DEFAULT_DAYS;
        if (d < 1) {
            d = 1;
        }
        return Math.min(d, MAX_DAYS);
    }

    private static String normalizePositivePrice(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return normalized.stripTrailingZeros().toPlainString();
    }

    private static String roomPlanKey(Long roomTypeId, Long planId) {
        return roomTypeId + ":" + planId;
    }

    private static String roomPlanDateKey(Long roomTypeId, Long planId, LocalDate date) {
        return roomTypeId + ":" + planId + ":" + date;
    }

    private static List<Map<String, Object>> toDateSegments(List<String> dailyValues, LocalDate start) {
        if (dailyValues == null || dailyValues.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> segments = new ArrayList<>();

        String currentValue = dailyValues.get(0);
        LocalDate segmentStart = start;

        for (int i = 1; i < dailyValues.size(); i++) {
            String value = dailyValues.get(i);
            if (Objects.equals(value, currentValue)) {
                continue;
            }
            LocalDate segmentEnd = start.plusDays(i - 1L);
            segments.add(buildSegment(segmentStart, segmentEnd, currentValue));
            segmentStart = start.plusDays(i);
            currentValue = value;
        }

        LocalDate lastEnd = start.plusDays(dailyValues.size() - 1L);
        segments.add(buildSegment(segmentStart, lastEnd, currentValue));
        return segments;
    }

    private static Map<String, Object> buildSegment(LocalDate from, LocalDate to, String value) {
        Map<String, Object> seg = new LinkedHashMap<>();
        seg.put("from", from.toString());
        seg.put("to", to.toString());
        seg.put("OTARule", List.of(buildOtaRule(value)));
        return seg;
    }

    private static Map<String, Object> buildOtaRule(String value) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", "Fixed");
        rule.put("value", value);

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("OTACode", DEFAULT_SU_OTA_CODES);
        wrapper.put("rule", rule);
        return wrapper;
    }
}
