package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.PricePlan;
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
import java.time.temporal.ChronoUnit;
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

        // Delegate to strict range method (RoomType-level roomid), to avoid sending physical-room roomids.
        return syncAriForDateRange(
                storeId,
                hotelId,
                startDate,
                endDate,
                null,
                null,
                true,
                true,
                true,
                false
        );
    }

    /**
     * PMS -> Su: push base ARI via /SUAPI/jservice/availability, limited to the given scope.
     * <p>
     * Certification strict mode requirement:
     * - payload must include ONLY specified room types / rate plans / date range (no extra data).
     * - roomid uses RoomType-level id (roomTypeId), not physical room number.
     */
    public SuAriSyncSummary syncAriForDateRange(
            Long storeId,
            String hotelId,
            LocalDate startDate,
            LocalDate endDate,
            Set<Long> roomTypeIds,
            Set<Long> ratePlanIds,
            boolean pushAvailability,
            boolean pushRates,
            boolean pushRestrictions,
            boolean deriveClosedFromBlockouts
    ) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId is required");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate/endDate is required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be >= startDate");
        }

        int effectiveDays = (int) Math.min(MAX_DAYS, Math.max(1L, ChronoUnit.DAYS.between(startDate, endDate) + 1L));
        LocalDate effectiveEndDate = startDate.plusDays(effectiveDays - 1L);

        logger.info(
                "[SuAriSync] start(range). storeId={}, hotelId={}, days={}, startDate={}, endDate={}, roomTypes={}, plans={}, pushAvailability={}, pushRates={}, pushRestrictions={}, deriveClosedFromBlockouts={}",
                storeId,
                hotelId,
                effectiveDays,
                startDate,
                effectiveEndDate,
                roomTypeIds != null ? roomTypeIds.size() : null,
                ratePlanIds != null ? ratePlanIds.size() : null,
                pushAvailability,
                pushRates,
                pushRestrictions,
                deriveClosedFromBlockouts
        );

        List<Room> allRooms = roomRepository.findByStoreIdWithRoomType(storeId);
        if (allRooms == null || allRooms.isEmpty()) {
            logger.info("Skip Su ARI sync(range): no rooms. storeId={}, hotelId={}", storeId, hotelId);
            return new SuAriSyncSummary(0, 0, effectiveDays, startDate, effectiveEndDate, true, true, 0, 0, 0, null, List.of());
        }

        List<Room> rooms = allRooms.stream()
                .filter(r -> r != null
                        && r.getId() != null
                        && r.getRoomType() != null
                        && r.getRoomType().getId() != null
                        && (roomTypeIds == null || roomTypeIds.isEmpty() || roomTypeIds.contains(r.getRoomType().getId())))
                .toList();

        List<Long> roomIds = rooms.stream().map(Room::getId).filter(Objects::nonNull).toList();

        Map<Long, RoomType> roomTypeById = new HashMap<>();
        for (Room room : rooms) {
            if (room == null || room.getRoomType() == null || room.getRoomType().getId() == null) {
                continue;
            }
            roomTypeById.putIfAbsent(room.getRoomType().getId(), room.getRoomType());
        }

        if (roomTypeById.isEmpty()) {
            logger.info("Skip Su ARI sync(range): no room types in scope. storeId={}, hotelId={}", storeId, hotelId);
            return new SuAriSyncSummary(0, 0, effectiveDays, startDate, effectiveEndDate, true, true, 0, 0, 0, null, List.of());
        }

        List<Reservation> reservations = roomIds.isEmpty()
                ? List.of()
                : reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                        storeId,
                        roomIds,
                        startDate,
                        effectiveEndDate,
                        BLOCKING_RESERVATION_STATUSES
                );

        Map<Long, List<Reservation>> reservationsByRoomId = reservations.stream()
                .filter(r -> r != null && r.getRoom() != null && r.getRoom().getId() != null)
                .collect(Collectors.groupingBy(r -> r.getRoom().getId()));

        Map<Long, List<RoomBlockout>> blockoutsByRoomId = new HashMap<>();
        if (!roomIds.isEmpty()) {
            List<RoomBlockout> blockouts = roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(
                    storeId,
                    roomIds,
                    startDate,
                    effectiveEndDate
            );
            blockoutsByRoomId = blockouts.stream()
                    .filter(b -> b != null && b.getRoom() != null && b.getRoom().getId() != null && b.getBlockDate() != null)
                    .collect(Collectors.groupingBy(b -> b.getRoom().getId()));
        }

        Map<Long, Integer> totalRoomsByRoomTypeId = new HashMap<>();
        Map<Long, int[]> blockedByRoomTypeId = new HashMap<>();
        for (Room room : rooms) {
            RoomType rt = room != null ? room.getRoomType() : null;
            Long rtId = rt != null ? rt.getId() : null;
            if (rtId == null) {
                continue;
            }
            totalRoomsByRoomTypeId.merge(rtId, 1, Integer::sum);

            if (pushRestrictions && deriveClosedFromBlockouts) {
                boolean[] blocked = buildBlockedOutCalendar(blockoutsByRoomId.get(room.getId()), startDate, effectiveDays);
                int[] counts = blockedByRoomTypeId.computeIfAbsent(rtId, ignored -> new int[effectiveDays]);
                for (int i = 0; i < effectiveDays; i++) {
                    if (blocked[i]) {
                        counts[i]++;
                    }
                }
            }
        }

        Map<Long, Set<Long>> planIdsByRoomTypeId = new HashMap<>();
        Map<String, RoomTypePricePlan> rtppByKey = new HashMap<>();
        Map<Long, PricePlan> pricePlanById = new HashMap<>();

        List<RoomTypePricePlan> rtpps = roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(storeId);
        for (RoomTypePricePlan rtpp : rtpps) {
            if (rtpp == null || rtpp.getRoomType() == null || rtpp.getPricePlan() == null) {
                continue;
            }
            Long rtId = rtpp.getRoomType().getId();
            Long planId = rtpp.getPricePlan().getId();
            if (rtId == null || planId == null) {
                continue;
            }
            if (!roomTypeById.containsKey(rtId)) {
                continue;
            }
            if (ratePlanIds != null && !ratePlanIds.isEmpty() && !ratePlanIds.contains(planId)) {
                continue;
            }
            rtppByKey.put(roomPlanKey(rtId, planId), rtpp);
            planIdsByRoomTypeId.computeIfAbsent(rtId, ignored -> new TreeSet<>()).add(planId);
            pricePlanById.putIfAbsent(planId, rtpp.getPricePlan());
        }

        List<RoomPrice> roomPrices = roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                storeId,
                startDate,
                effectiveEndDate
        );
        Map<String, RoomPrice> roomPriceByKey = new HashMap<>();
        Map<String, Integer> availableRoomsOverrideByRoomTypeDate = new HashMap<>();
        for (RoomPrice rp : roomPrices) {
            if (rp == null || rp.getRoomType() == null || rp.getRoomType().getId() == null) {
                continue;
            }
            if (rp.getPriceDate() == null) {
                continue;
            }
            Long rtId = rp.getRoomType().getId();
            if (!roomTypeById.containsKey(rtId)) {
                continue;
            }

            if (rp.getAvailableRooms() != null && rp.getAvailableRooms() >= 0) {
                String key = rtId + "|" + rp.getPriceDate();
                Integer existing = availableRoomsOverrideByRoomTypeDate.get(key);
                int v = rp.getAvailableRooms();
                availableRoomsOverrideByRoomTypeDate.put(key, existing != null ? Math.min(existing, v) : v);
            }

            if (rp.getPricePlan() == null || rp.getPricePlan().getId() == null) {
                continue;
            }
            Long planId = rp.getPricePlan().getId();
            pricePlanById.putIfAbsent(planId, rp.getPricePlan());
            if (ratePlanIds != null && !ratePlanIds.isEmpty() && !ratePlanIds.contains(planId)) {
                continue;
            }
            roomPriceByKey.put(roomPlanDateKey(rtId, planId, rp.getPriceDate()), rp);
            planIdsByRoomTypeId.computeIfAbsent(rtId, ignored -> new TreeSet<>()).add(planId);
        }

        Map<Long, int[]> sellableByRoomTypeId = new HashMap<>();
        if (pushAvailability) {
            for (Room room : rooms) {
                RoomType rt = room.getRoomType();
                Long rtId = rt != null ? rt.getId() : null;
                if (rtId == null) {
                    continue;
                }
                int[] counts = sellableByRoomTypeId.computeIfAbsent(rtId, ignored -> new int[effectiveDays]);
                boolean[] canSell = buildCanSellCalendar(
                        room,
                        reservationsByRoomId.get(room.getId()),
                        blockoutsByRoomId.get(room.getId()),
                        startDate,
                        effectiveDays
                );
                for (int i = 0; i < effectiveDays; i++) {
                    if (canSell[i]) {
                        counts[i]++;
                    }
                }
            }

            for (Map.Entry<Long, int[]> entry : sellableByRoomTypeId.entrySet()) {
                Long rtId = entry.getKey();
                int[] counts = entry.getValue();
                for (int i = 0; i < effectiveDays; i++) {
                    LocalDate date = startDate.plusDays(i);
                    Integer override = availableRoomsOverrideByRoomTypeDate.get(rtId + "|" + date);
                    if (override != null) {
                        counts[i] = override;
                    }
                }
            }
        }

        List<MissingPrice> missingPrices = new ArrayList<>();
        int availabilitySegments = 0;
        int rateSegments = 0;
        int requestsPosted = 0;

        Set<Long> distinctPlans = new TreeSet<>();
        Set<Long> distinctRoomTypesInPayload = new TreeSet<>();

        List<Map<String, Object>> availabilityRoomNodes = new ArrayList<>();
        List<Map<String, Object>> rateRoomNodes = new ArrayList<>();

        if (pushAvailability) {
            for (Map.Entry<Long, int[]> entry : sellableByRoomTypeId.entrySet()) {
                Long rtId = entry.getKey();
                List<Map<String, Object>> segments = toRoomstosellSegments(entry.getValue(), startDate);
                if (segments.isEmpty()) {
                    continue;
                }
                availabilitySegments += segments.size();
                availabilityRoomNodes.add(buildRoomNode(String.valueOf(rtId), segments));
                distinctRoomTypesInPayload.add(rtId);
            }
        }

        if (pushRates || pushRestrictions) {
            for (Map.Entry<Long, RoomType> it : roomTypeById.entrySet()) {
                Long rtId = it.getKey();
                RoomType roomType = it.getValue();
                if (rtId == null || roomType == null) {
                    continue;
                }
                Set<Long> planIds = planIdsByRoomTypeId.get(rtId);
                if (planIds == null || planIds.isEmpty()) {
                    continue;
                }

                List<Map<String, Object>> rateDateItems = new ArrayList<>();
                for (Long planId : planIds) {
                    if (planId == null) {
                        continue;
                    }
                    if (ratePlanIds != null && !ratePlanIds.isEmpty() && !ratePlanIds.contains(planId)) {
                        continue;
                    }
                    distinctPlans.add(planId);

                    RoomTypePricePlan rtpp = rtppByKey.get(roomPlanKey(rtId, planId));
                    PricePlan pricePlan = (rtpp != null && rtpp.getPricePlan() != null) ? rtpp.getPricePlan() : pricePlanById.get(planId);
                    Integer defaultMinStay = normalizeStayOrNull(pricePlan != null ? pricePlan.getMinNights() : null);
                    Integer defaultMaxStay = normalizeStayOrNull(pricePlan != null ? pricePlan.getMaxNights() : null);
                    int includedGuests = resolveIncludedGuests(rtpp, roomType);
                    String extraAdultRate = formatMoneyAllowZero(rtpp != null ? rtpp.getExtraAdultRate() : null);
                    String extraChildRate = formatMoneyAllowZero(rtpp != null ? rtpp.getExtraChildRate() : null);

                    List<DailyRateState> dailyStates = new ArrayList<>(effectiveDays);
                    int missingDays = 0;
                    for (int i = 0; i < effectiveDays; i++) {
                        LocalDate date = startDate.plusDays(i);
                        RoomPrice rp = roomPriceByKey.get(roomPlanDateKey(rtId, planId, date));

                        String price = null;
                        if (pushRates) {
                            LocalBasePriceResolver.Result local = LocalBasePriceResolver.resolve(rp, rtpp, roomType, date);
                            price = formatPositiveMoney(local.basePrice());
                            if (price == null) {
                                missingDays++;
                            }
                        }

                        Boolean closed = null;
                        Integer minStay = null;
                        Integer maxStay = null;
                        Boolean cta = null;
                        Boolean ctd = null;

                        if (pushRestrictions) {
                            Boolean closeRoom = rp != null ? rp.getCloseRoom() : null;

                            if (deriveClosedFromBlockouts) {
                                int totalRooms = totalRoomsByRoomTypeId.getOrDefault(rtId, 0);
                                int[] blockedCounts = blockedByRoomTypeId.get(rtId);
                                boolean closedByBlockouts = totalRooms > 0 && blockedCounts != null && blockedCounts.length > i && blockedCounts[i] >= totalRooms;
                                closed = closedByBlockouts || Boolean.TRUE.equals(closeRoom);
                            } else {
                                closed = closeRoom;
                            }

                            minStay = normalizeStayOrNull(rp != null ? rp.getMinStay() : null);
                            maxStay = normalizeStayOrNull(rp != null ? rp.getMaxStay() : null);

                            if (minStay == null) {
                                minStay = defaultMinStay;
                            }
                            if (maxStay == null) {
                                maxStay = defaultMaxStay;
                            }
                            if (minStay != null && maxStay != null && maxStay < minStay) {
                                maxStay = minStay;
                            }
                            cta = rp != null ? rp.getCta() : null;
                            ctd = rp != null ? rp.getCtd() : null;
                        }

                        dailyStates.add(new DailyRateState(
                                price,
                                closed,
                                minStay,
                                maxStay,
                                cta,
                                ctd
                        ));
                    }
                    if (pushRates && missingDays > 0 && missingPrices.size() < 200) {
                        missingPrices.add(new MissingPrice(String.valueOf(rtId), planId.toString(), missingDays));
                    }

                    List<Map<String, Object>> segments = toRateSegments(
                            dailyStates,
                            startDate,
                            planId.toString(),
                            includedGuests,
                            extraAdultRate,
                            extraChildRate,
                            pushRates,
                            pushRestrictions,
                            pushRates
                    );
                    if (!segments.isEmpty()) {
                        rateSegments += segments.size();
                        rateDateItems.addAll(segments);
                    }
                }

                if (!rateDateItems.isEmpty()) {
                    rateRoomNodes.add(buildRoomNode(String.valueOf(rtId), rateDateItems));
                    distinctRoomTypesInPayload.add(rtId);
                }
            }
        }

        boolean availabilityOk = !pushAvailability;
        boolean ratesOk = !(pushRates || pushRestrictions);
        String error = null;

        try {
            if (pushAvailability) {
                if (availabilityRoomNodes.isEmpty()) {
                    availabilityOk = true;
                } else {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("hotelid", hotelId);
                    payload.put("room", availabilityRoomNodes);

                    logger.info(
                            "[SuAriSync] posting availability(roomstosell). storeId={}, hotelId={}, roomTypes={}, segments={}",
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
                    } else {
                        availabilityOk = true;
                    }
                }
            }

            if (pushRates || pushRestrictions) {
                if (rateRoomNodes.isEmpty()) {
                    ratesOk = true;
                } else {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("hotelid", hotelId);
                    payload.put("room", rateRoomNodes);

                    logger.info(
                            "[SuAriSync] posting availability(rates). storeId={}, hotelId={}, roomTypes={}, segments={}, plans={}",
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
                    } else {
                        ratesOk = true;
                    }
                }
            }

            logger.info(
                    "[SuAriSync] done(range). storeId={}, hotelId={}, availabilityOk={}, ratesOk={}, requests={}",
                    storeId,
                    hotelId,
                    availabilityOk,
                    ratesOk,
                    requestsPosted
            );

            return new SuAriSyncSummary(
                    distinctRoomTypesInPayload.size(),
                    distinctPlans.size(),
                    effectiveDays,
                    startDate,
                    effectiveEndDate,
                    availabilityOk,
                    ratesOk,
                    availabilitySegments,
                    rateSegments,
                    requestsPosted,
                    error,
                    missingPrices
            );
        } catch (RuntimeException e) {
            logger.error("Su ARI sync(range) failed. storeId={}, hotelId={}", storeId, hotelId, e);
            return new SuAriSyncSummary(
                    distinctRoomTypesInPayload.size(),
                    distinctPlans.size(),
                    effectiveDays,
                    startDate,
                    effectiveEndDate,
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

    private static boolean[] buildBlockedOutCalendar(List<RoomBlockout> blockouts, LocalDate start, int days) {
        boolean[] blocked = new boolean[days];
        if (blockouts == null || blockouts.isEmpty() || start == null || days <= 0) {
            return blocked;
        }
        for (RoomBlockout b : blockouts) {
            if (b == null || b.getBlockDate() == null) {
                continue;
            }
            long offset = b.getBlockDate().toEpochDay() - start.toEpochDay();
            if (offset < 0 || offset >= days) {
                continue;
            }
            blocked[(int) offset] = true;
        }
        return blocked;
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

    private static List<Map<String, Object>> toRoomstosellSegments(int[] roomstosellByDay, LocalDate start) {
        if (roomstosellByDay == null || roomstosellByDay.length == 0) {
            return List.of();
        }

        List<Map<String, Object>> segments = new ArrayList<>();
        int currentValue = roomstosellByDay[0];
        LocalDate segmentStart = start;

        for (int i = 1; i < roomstosellByDay.length; i++) {
            int value = roomstosellByDay[i];
            if (value == currentValue) {
                continue;
            }
            LocalDate segmentEnd = start.plusDays(i - 1L);
            segments.add(buildRoomstosellSegment(segmentStart, segmentEnd, currentValue));
            segmentStart = start.plusDays(i);
            currentValue = value;
        }

        LocalDate lastEnd = start.plusDays(roomstosellByDay.length - 1L);
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
            String extraChildRate,
            boolean includePrice,
            boolean includeRestrictions,
            boolean includeExtraRates
    ) {
        if (dailyStates == null || dailyStates.isEmpty()) {
            return List.of();
        }
        if (ratePlanId == null || ratePlanId.isBlank()) {
            return List.of();
        }

        List<Map<String, Object>> segments = new ArrayList<>();

        LocalDate segmentStart = start;
        DailyRateState current = dailyStates.get(0);
        boolean currentHasPayload = hasAnyRatePayload(current, includePrice, includeRestrictions);

        for (int i = 1; i < dailyStates.size(); i++) {
            DailyRateState next = dailyStates.get(i);
            boolean nextHasPayload = hasAnyRatePayload(next, includePrice, includeRestrictions);

            boolean same = sameRateStateForPayload(current, next, includePrice, includeRestrictions)
                    && nextHasPayload == currentHasPayload;
            if (same) {
                continue;
            }

            LocalDate segmentEnd = start.plusDays(i - 1L);
            if (currentHasPayload) {
                segments.add(buildRateSegment(
                        segmentStart,
                        segmentEnd,
                        ratePlanId,
                        current,
                        includedGuests,
                        extraAdultRate,
                        extraChildRate,
                        includePrice,
                        includeRestrictions,
                        includeExtraRates
                ));
            }
            segmentStart = start.plusDays(i);
            current = next;
            currentHasPayload = nextHasPayload;
        }

        LocalDate lastEnd = start.plusDays(dailyStates.size() - 1L);
        if (currentHasPayload) {
            segments.add(buildRateSegment(
                    segmentStart,
                    lastEnd,
                    ratePlanId,
                    current,
                    includedGuests,
                    extraAdultRate,
                    extraChildRate,
                    includePrice,
                    includeRestrictions,
                    includeExtraRates
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
            String extraChildRate,
            boolean includePrice,
            boolean includeRestrictions,
            boolean includeExtraRates
    ) {
        Map<String, Object> seg = new LinkedHashMap<>();
        seg.put("from", from.toString());
        seg.put("to", to.toString());
        seg.put("rate", List.of(Map.of("rateplanid", ratePlanId)));
        if (includePrice && state != null && state.priceValue() != null) {
            seg.put("price", List.of(Map.of("NumberOfGuests", String.valueOf(includedGuests), "value", state.priceValue())));
        }

        // Restrictions (Room + RatePlan + Date range)
        if (includeRestrictions && state != null) {
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
        }

        if (includeExtraRates) {
            // Extra rates: treat missing as 0 (per user confirmation)
            seg.put("extraadultrate", extraAdultRate);
            seg.put("extrachildrate", extraChildRate);
        }
        return seg;
    }

    private static boolean hasAnyRestriction(DailyRateState state) {
        return state != null && (state.closed() != null
                || state.minStay() != null
                || state.maxStay() != null
                || state.cta() != null
                || state.ctd() != null);
    }

    private static boolean hasAnyRatePayload(DailyRateState state, boolean includePrice, boolean includeRestrictions) {
        if (state == null) {
            return false;
        }
        if (includePrice && state.priceValue() != null) {
            return true;
        }
        return includeRestrictions && hasAnyRestriction(state);
    }

    private static boolean sameRateStateForPayload(DailyRateState a, DailyRateState b, boolean includePrice, boolean includeRestrictions) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (includePrice && !Objects.equals(a.priceValue(), b.priceValue())) {
            return false;
        }
        if (includeRestrictions) {
            if (!Objects.equals(a.closed(), b.closed())) return false;
            if (!Objects.equals(a.minStay(), b.minStay())) return false;
            if (!Objects.equals(a.maxStay(), b.maxStay())) return false;
            if (!Objects.equals(a.cta(), b.cta())) return false;
            if (!Objects.equals(a.ctd(), b.ctd())) return false;
        }
        return true;
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

    private static Integer normalizeStayOrNull(Integer value) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            return null;
        }
        return value;
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
