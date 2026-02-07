package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomBlockout;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.util.LocalBasePriceResolver;
import server.demo.util.SuRoomIdUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * PMS -> Su: push base ARI (Rates & Availability) via /SUAPI/jservice/availability.
 * <p>
 * This is required for Su extranet pages like "Listing Type" and "Rate Plan" to show
 * "availability pushed" / "rates pushed", while invratecontrol is OTA-specific control.
 */
@Service
public class SuAriSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SuAriSyncService.class);

    private static final int DEFAULT_DAYS = 365;
    private static final int MAX_DAYS = 500;

    private static final int GUESTS = 2;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private static final Set<ReservationStatus> BLOCKING_RESERVATION_STATUSES = EnumSet.of(
            ReservationStatus.REQUESTED,
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN
    );

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final RoomTypePricePlanRepository roomTypePricePlanRepository;
    private final RoomPriceRepository roomPriceRepository;
    private final RoomBlockoutRepository roomBlockoutRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;

    public SuAriSyncService(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            RoomTypePricePlanRepository roomTypePricePlanRepository,
            RoomPriceRepository roomPriceRepository,
            RoomBlockoutRepository roomBlockoutRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService
    ) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.roomTypePricePlanRepository = roomTypePricePlanRepository;
        this.roomPriceRepository = roomPriceRepository;
        this.roomBlockoutRepository = roomBlockoutRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
    }

    public record MissingPrice(
            String roomId,
            String ratePlanId,
            int missingDays
    ) {}

    private record DailyRateState(
            String priceValue,
            Boolean closed,
            Integer minStay,
            Integer maxStay,
            Boolean cta,
            Boolean ctd
    ) {}

    public record SuAriSyncSummary(
            int roomCount,
            int ratePlanCount,
            int days,
            LocalDate startDate,
            LocalDate endDate,
            boolean availabilityPushed,
            boolean ratesPushed,
            int availabilitySegments,
            int rateSegments,
            int requestsPosted,
            String error,
            List<MissingPrice> missingPrices
    ) {}

    public SuAriSyncSummary syncBaseAriForNextDays(Long storeId, String hotelId, Integer days) {
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
                "[SuAriSync] start. storeId={}, hotelId={}, days={}, startDate={}, endDate={}",
                storeId,
                hotelId,
                effectiveDays,
                startDate,
                endDate
        );

        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        if (rooms == null || rooms.isEmpty()) {
            logger.info("Skip Su ARI sync: no rooms. storeId={}, hotelId={}", storeId, hotelId);
            return new SuAriSyncSummary(0, 0, effectiveDays, startDate, endDate, true, true, 0, 0, 0, null, List.of());
        }

        List<Room> eligibleRooms = rooms.stream()
                .filter(r -> r != null
                        && r.getRoomNumber() != null
                        && !r.getRoomNumber().isBlank()
                        && r.getRoomType() != null
                        && r.getRoomType().getId() != null)
                .toList();
        SuRoomIdUtil.assertRoomIdsWithinLimit(eligibleRooms);

        List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .filter(Objects::nonNull)
                .toList();

        List<Reservation> reservations = roomIds.isEmpty()
                ? List.of()
                : reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                        storeId,
                        roomIds,
                        startDate,
                        endDate,
                        BLOCKING_RESERVATION_STATUSES
                );

        Map<Long, List<Reservation>> reservationsByRoomId = reservations.stream()
                .filter(r -> r.getRoom() != null && r.getRoom().getId() != null)
                .collect(Collectors.groupingBy(r -> r.getRoom().getId()));

        Map<Long, List<RoomBlockout>> blockoutsByRoomId = new HashMap<>();
        if (!roomIds.isEmpty()) {
            List<RoomBlockout> blockouts = roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                    storeId,
                    roomIds,
                    startDate,
                    endDate
            );
            blockoutsByRoomId = blockouts.stream()
                    .filter(b -> b != null && b.getRoom() != null && b.getRoom().getId() != null && b.getBlockDate() != null)
                    .collect(Collectors.groupingBy(b -> b.getRoom().getId()));
        }

        Map<Long, Set<Long>> planIdsByRoomTypeId = new HashMap<>();
        Map<String, RoomTypePricePlan> rtppByKey = new HashMap<>();

        List<RoomTypePricePlan> rtpps = roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(storeId);
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

        List<MissingPrice> missingPrices = new ArrayList<>();
        int availabilitySegments = 0;
        int rateSegments = 0;
        int requestsPosted = 0;

        Set<Long> distinctPlans = new TreeSet<>();

        List<Map<String, Object>> availabilityRoomNodes = new ArrayList<>();
        List<Map<String, Object>> rateRoomNodes = new ArrayList<>();

        for (Room room : rooms) {
            if (room == null || room.getId() == null) {
                continue;
            }
            if (room.getRoomNumber() == null || room.getRoomNumber().isBlank()) {
                continue;
            }
            RoomType roomType = room.getRoomType();
            if (roomType == null || roomType.getId() == null) {
                continue;
            }

            String suRoomId = SuRoomIdUtil.buildRoomId(room);

            boolean[] canSell = buildCanSellCalendar(
                    room,
                    reservationsByRoomId.get(room.getId()),
                    blockoutsByRoomId.get(room.getId()),
                    startDate,
                    effectiveDays
            );
            List<Map<String, Object>> roomstosellSegments = toRoomstosellSegments(canSell, startDate);
            if (!roomstosellSegments.isEmpty()) {
                availabilitySegments += roomstosellSegments.size();
                availabilityRoomNodes.add(buildRoomNode(suRoomId, roomstosellSegments));
            }

            Set<Long> planIds = planIdsByRoomTypeId.get(roomType.getId());
            if (planIds == null || planIds.isEmpty()) {
                continue;
            }

            List<Map<String, Object>> rateDateItems = new ArrayList<>();
            for (Long planId : planIds) {
                if (planId == null) {
                    continue;
                }
                distinctPlans.add(planId);
                RoomTypePricePlan rtpp = rtppByKey.get(roomPlanKey(roomType.getId(), planId));

                int includedGuests = resolveIncludedGuests(rtpp, roomType);
                String extraAdultRate = formatMoneyAllowZero(rtpp != null ? rtpp.getExtraAdultRate() : null);
                String extraChildRate = formatMoneyAllowZero(rtpp != null ? rtpp.getExtraChildRate() : null);

                List<DailyRateState> dailyStates = new ArrayList<>(effectiveDays);
                int missingDays = 0;
                for (int i = 0; i < effectiveDays; i++) {
                    LocalDate date = startDate.plusDays(i);
                    RoomPrice rp = roomPriceByKey.get(roomPlanDateKey(roomType.getId(), planId, date));
                    LocalBasePriceResolver.Result local = LocalBasePriceResolver.resolve(rp, rtpp, roomType, date);
                    String price = formatPositiveMoney(local.basePrice());
                    if (price == null) {
                        missingDays++;
                    }
                    dailyStates.add(new DailyRateState(
                            price,
                            rp != null ? rp.getCloseRoom() : null,
                            rp != null ? rp.getMinStay() : null,
                            rp != null ? rp.getMaxStay() : null,
                            rp != null ? rp.getCta() : null,
                            rp != null ? rp.getCtd() : null
                    ));
                }
                if (missingDays > 0 && missingPrices.size() < 200) {
                    missingPrices.add(new MissingPrice(suRoomId, planId.toString(), missingDays));
                }

                List<Map<String, Object>> segments = toRateSegments(
                        dailyStates,
                        startDate,
                        planId.toString(),
                        includedGuests,
                        extraAdultRate,
                        extraChildRate
                );
                if (!segments.isEmpty()) {
                    rateSegments += segments.size();
                    rateDateItems.addAll(segments);
                }
            }

            if (!rateDateItems.isEmpty()) {
                rateRoomNodes.add(buildRoomNode(suRoomId, rateDateItems));
            }
        }

        boolean availabilityOk = true;
        boolean ratesOk = true;
        String error = null;

        try {
            if (!availabilityRoomNodes.isEmpty()) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("hotelid", hotelId);
                payload.put("room", availabilityRoomNodes);

                logger.info(
                        "[SuAriSync] posting availability(roomstosell). storeId={}, hotelId={}, rooms={}, segments={}",
                        storeId,
                        hotelId,
                        availabilityRoomNodes.size(),
                        availabilitySegments
                );
                JsonNode resp = suAccessTokenService.executeWithTokenRetry(
                        token -> suApiClient.postAvailability(token, payload),
                        "availability(roomstosell)"
                );
                requestsPosted++;

                if (!suApiClient.isSuSuccess(resp)) {
                    availabilityOk = false;
                    error = suApiClient.extractSuErrorMessage(resp);
                    logger.warn(
                            "[SuAriSync] availability(roomstosell) returned fail. storeId={}, hotelId={}, err={}, raw={}",
                            storeId,
                            hotelId,
                            error,
                            resp != null ? resp.toString() : "null"
                    );
                }
            }

            if (!rateRoomNodes.isEmpty()) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("hotelid", hotelId);
                payload.put("room", rateRoomNodes);

                logger.info(
                        "[SuAriSync] posting availability(rates). storeId={}, hotelId={}, rooms={}, segments={}, plans={}",
                        storeId,
                        hotelId,
                        rateRoomNodes.size(),
                        rateSegments,
                        distinctPlans.size()
                );
                JsonNode resp = suAccessTokenService.executeWithTokenRetry(
                        token -> suApiClient.postAvailability(token, payload),
                        "availability(rates)"
                );
                requestsPosted++;

                if (!suApiClient.isSuSuccess(resp)) {
                    ratesOk = false;
                    String err = suApiClient.extractSuErrorMessage(resp);
                    error = error != null ? error + "; " + err : err;
                    logger.warn(
                            "[SuAriSync] availability(rates) returned fail. storeId={}, hotelId={}, err={}, raw={}",
                            storeId,
                            hotelId,
                            err,
                            resp != null ? resp.toString() : "null"
                    );
                }
            }

            logger.info(
                    "[SuAriSync] done. storeId={}, hotelId={}, availabilityOk={}, ratesOk={}, requests={}",
                    storeId,
                    hotelId,
                    availabilityOk,
                    ratesOk,
                    requestsPosted
            );

            return new SuAriSyncSummary(
                    rooms.size(),
                    distinctPlans.size(),
                    effectiveDays,
                    startDate,
                    endDate,
                    availabilityOk,
                    ratesOk,
                    availabilitySegments,
                    rateSegments,
                    requestsPosted,
                    error,
                    missingPrices
            );
        } catch (RuntimeException e) {
            logger.error("Su ARI sync failed. storeId={}, hotelId={}", storeId, hotelId, e);
            return new SuAriSyncSummary(
                    rooms.size(),
                    distinctPlans.size(),
                    effectiveDays,
                    startDate,
                    endDate,
                    false,
                    false,
                    availabilitySegments,
                    rateSegments,
                    requestsPosted,
                    e.getMessage(),
                    missingPrices
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

    private static boolean[] buildCanSellCalendar(
            Room room,
            List<Reservation> reservations,
            List<RoomBlockout> blockouts,
            LocalDate start,
            int days
    ) {
        boolean roomAvailable = room.getStatus() == RoomStatus.AVAILABLE;
        boolean[] canSell = new boolean[days];

        for (int i = 0; i < days; i++) {
            canSell[i] = roomAvailable;
        }
        if (!roomAvailable) {
            return canSell;
        }

        if (blockouts != null && !blockouts.isEmpty()) {
            for (RoomBlockout b : blockouts) {
                if (b == null || b.getBlockDate() == null) {
                    continue;
                }
                long offset = b.getBlockDate().toEpochDay() - start.toEpochDay();
                if (offset < 0 || offset >= days) {
                    continue;
                }
                canSell[(int) offset] = false;
            }
        }

        if (reservations == null || reservations.isEmpty()) {
            return canSell;
        }

        LocalDate end = start.plusDays(days - 1L);

        for (Reservation r : reservations) {
            if (r == null || r.getCheckInDate() == null || r.getCheckOutDate() == null) {
                continue;
            }
            if (r.getStatus() == null || !BLOCKING_RESERVATION_STATUSES.contains(r.getStatus())) {
                continue;
            }

            LocalDate from = r.getCheckInDate().isAfter(start) ? r.getCheckInDate() : start;
            LocalDate toInclusive = r.getCheckOutDate().minusDays(1);
            if (toInclusive.isAfter(end)) {
                toInclusive = end;
            }
            if (toInclusive.isBefore(from)) {
                continue;
            }

            long offsetFrom = from.toEpochDay() - start.toEpochDay();
            long offsetTo = toInclusive.toEpochDay() - start.toEpochDay();

            int iFrom = (int) Math.max(0, offsetFrom);
            int iTo = (int) Math.min(days - 1L, offsetTo);

            for (int i = iFrom; i <= iTo; i++) {
                canSell[i] = false;
            }
        }

        return canSell;
    }

    private static List<Map<String, Object>> toRoomstosellSegments(boolean[] canSell, LocalDate start) {
        if (canSell == null || canSell.length == 0) {
            return List.of();
        }

        List<Map<String, Object>> segments = new ArrayList<>();
        int currentValue = canSell[0] ? 1 : 0;
        LocalDate segmentStart = start;

        for (int i = 1; i < canSell.length; i++) {
            int value = canSell[i] ? 1 : 0;
            if (value == currentValue) {
                continue;
            }
            LocalDate segmentEnd = start.plusDays(i - 1L);
            segments.add(buildRoomstosellSegment(segmentStart, segmentEnd, currentValue));
            segmentStart = start.plusDays(i);
            currentValue = value;
        }

        LocalDate lastEnd = start.plusDays(canSell.length - 1L);
        segments.add(buildRoomstosellSegment(segmentStart, lastEnd, currentValue));
        return segments;
    }

    private static Map<String, Object> buildRoomstosellSegment(LocalDate from, LocalDate to, int value) {
        Map<String, Object> seg = new LinkedHashMap<>();
        seg.put("from", from.toString());
        seg.put("to", to.toString());
        seg.put("roomstosell", String.valueOf(value));
        return seg;
    }

    private static List<Map<String, Object>> toRateSegments(
            List<DailyRateState> dailyStates,
            LocalDate start,
            String ratePlanId,
            int includedGuests,
            String extraAdultRate,
            String extraChildRate
    ) {
        if (dailyStates == null || dailyStates.isEmpty()) {
            return List.of();
        }
        if (ratePlanId == null || ratePlanId.isBlank()) {
            return List.of();
        }

        List<Map<String, Object>> segments = new ArrayList<>();

        DailyRateState current = dailyStates.get(0);
        LocalDate segmentStart = start;
        boolean currentHasPrice = current != null && current.priceValue() != null;

        for (int i = 1; i < dailyStates.size(); i++) {
            DailyRateState next = dailyStates.get(i);
            boolean hasPrice = next != null && next.priceValue() != null;

            boolean same = Objects.equals(next, current) && hasPrice == currentHasPrice;
            if (same) {
                continue;
            }

            LocalDate segmentEnd = start.plusDays(i - 1L);
            if (currentHasPrice) {
                segments.add(buildRateSegment(
                        segmentStart,
                        segmentEnd,
                        ratePlanId,
                        current,
                        includedGuests,
                        extraAdultRate,
                        extraChildRate
                ));
            }
            segmentStart = start.plusDays(i);
            current = next;
            currentHasPrice = hasPrice;
        }

        LocalDate lastEnd = start.plusDays(dailyStates.size() - 1L);
        if (currentHasPrice) {
            segments.add(buildRateSegment(
                    segmentStart,
                    lastEnd,
                    ratePlanId,
                    current,
                    includedGuests,
                    extraAdultRate,
                    extraChildRate
            ));
        }
        return segments;
    }

    private static Map<String, Object> buildRateSegment(
            LocalDate from,
            LocalDate to,
            String ratePlanId,
            DailyRateState state,
            int includedGuests,
            String extraAdultRate,
            String extraChildRate
    ) {
        Map<String, Object> seg = new LinkedHashMap<>();
        seg.put("from", from.toString());
        seg.put("to", to.toString());
        seg.put("rate", List.of(Map.of("rateplanid", ratePlanId)));
        seg.put("price", List.of(Map.of("NumberOfGuests", String.valueOf(includedGuests), "value", state.priceValue())));

        // Restrictions (Room + RatePlan + Date range)
        if (state.closed() != null) {
            seg.put("closed", state.closed() ? "1" : "0");
        }
        if (state.minStay() != null) {
            seg.put("minimumstay", String.valueOf(state.minStay()));
        }
        if (state.maxStay() != null) {
            seg.put("maximumstay", String.valueOf(state.maxStay()));
        }
        if (state.cta() != null) {
            seg.put("closedonarrival", state.cta() ? "1" : "0");
        }
        if (state.ctd() != null) {
            seg.put("closedondeparture", state.ctd() ? "1" : "0");
        }

        // Extra rates: treat missing as 0 (per user confirmation)
        seg.put("extraadultrate", extraAdultRate);
        seg.put("extrachildrate", extraChildRate);
        return seg;
    }

    private static Map<String, Object> buildRoomNode(String roomId, List<Map<String, Object>> dateItems) {
        Map<String, Object> roomNode = new LinkedHashMap<>();
        roomNode.put("roomid", roomId);
        roomNode.put("date", dateItems);
        return roomNode;
    }

    private static String formatPositiveMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return normalized.toPlainString();
    }

    private static String formatMoneyAllowZero(BigDecimal value) {
        BigDecimal normalized = value == null ? ZERO_MONEY : value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            normalized = ZERO_MONEY;
        }
        return normalized.toPlainString();
    }

    private static int resolveIncludedGuests(RoomTypePricePlan roomTypePricePlan, RoomType roomType) {
        Integer configured = roomTypePricePlan != null ? roomTypePricePlan.getIncludedGuests() : null;
        if (configured != null && configured > 0) {
            return clampGuests(configured);
        }

        Integer fallbackMax = roomTypePricePlan != null ? roomTypePricePlan.getMaxGuests() : null;
        if (fallbackMax == null || fallbackMax <= 0) {
            fallbackMax = roomType != null ? roomType.getMaxGuests() : null;
        }
        if (fallbackMax != null && fallbackMax > 0) {
            return clampGuests(fallbackMax);
        }

        return clampGuests(GUESTS);
    }

    private static int clampGuests(Integer guests) {
        if (guests == null || guests <= 0) {
            return GUESTS;
        }
        return Math.min(30, guests);
    }

    private static String roomPlanKey(Long roomTypeId, Long planId) {
        return roomTypeId + ":" + planId;
    }

    private static String roomPlanDateKey(Long roomTypeId, Long planId, LocalDate date) {
        return roomTypeId + ":" + planId + ":" + date;
    }
}
