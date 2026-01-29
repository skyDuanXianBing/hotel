package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.util.SuRoomIdUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PMS -> Su：推送“房间号级别可用性（1/0）”到 Su（InventoryControl）。
 * <p>
 * 规则：
 * - room.status != AVAILABLE 视为不可售（0）
 * - 若房间在某日期被预订占用（REQUESTED/CONFIRMED/CHECKED_IN），则该日期不可售（0）
 * - 其他日期可售（1）
 */
@Service
public class SuAvailabilitySyncService {

    private static final Logger logger = LoggerFactory.getLogger(SuAvailabilitySyncService.class);

    private static final int DEFAULT_DAYS = 365;
    private static final int MAX_DAYS = 500;

    // Su OTACode：Booking=19，Airbnb=244（与现有 OtaSyncService 口径保持一致）
    private static final List<Integer> DEFAULT_SU_OTA_CODES = List.of(19, 244);

    private static final Set<ReservationStatus> BLOCKING_RESERVATION_STATUSES = EnumSet.of(
            ReservationStatus.REQUESTED,
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN
    );

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;

    public SuAvailabilitySyncService(
            RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService
    ) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
    }

    public SuAvailabilitySyncSummary syncRoomAvailabilityForNextDays(Long storeId, String hotelId, Integer days) {
        return syncRoomAvailabilityForNextDays(storeId, hotelId, days, DEFAULT_SU_OTA_CODES);
    }

    public SuAvailabilitySyncSummary syncRoomAvailabilityForNextDays(Long storeId, String hotelId, Integer days, List<Integer> otaCodes) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId is required");
        }

        List<Integer> effectiveOtaCodes = normalizeOtaCodes(otaCodes);

        int effectiveDays = normalizeDays(days);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(effectiveDays - 1L);

        logger.info(
                "[SuAvailabilitySync] start. storeId={}, hotelId={}, days={}, startDate={}, endDate={}",
                storeId,
                hotelId,
                effectiveDays,
                startDate,
                endDate
        );

        List<Room> rooms = roomRepository.findByStoreIdWithRoomType(storeId);
        if (rooms == null || rooms.isEmpty()) {
            logger.info("Skip Su availability sync: no rooms. storeId={}, hotelId={}", storeId, hotelId);
            return new SuAvailabilitySyncSummary(0, effectiveDays, startDate, endDate, true, null);
        }

        List<Room> eligibleRooms = rooms.stream()
                .filter(r -> r != null && r.getRoomNumber() != null && !r.getRoomNumber().isBlank())
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

        List<Map<String, Object>> inventoryControls = new ArrayList<>();

        for (Room room : rooms) {
            if (room == null || room.getId() == null) {
                continue;
            }
            if (room.getRoomNumber() == null || room.getRoomNumber().isBlank()) {
                continue;
            }
            if (room.getRoomType() == null || room.getRoomType().getId() == null) {
                continue;
            }
            String roomId = SuRoomIdUtil.buildRoomId(room);
            boolean[] canSell = buildCanSellCalendar(room, reservationsByRoomId.get(room.getId()), startDate, effectiveDays);
            List<Map<String, Object>> dateSegments = toDateSegments(canSell, startDate, effectiveOtaCodes);

            if (dateSegments.isEmpty()) {
                continue;
            }

            Map<String, Object> control = new LinkedHashMap<>();
            control.put("roomid", roomId);
            control.put("date", dateSegments);
            inventoryControls.add(control);
        }

        if (inventoryControls.isEmpty()) {
            logger.info("Skip Su availability sync: no inventorycontrol generated. storeId={}, hotelId={}", storeId, hotelId);
            return new SuAvailabilitySyncSummary(rooms.size(), effectiveDays, startDate, endDate, true, null);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelid", hotelId.trim());
        payload.put("inventorycontrol", inventoryControls);

        try {
            logger.info(
                    "[SuAvailabilitySync] posting invratecontrol(inventorycontrol). storeId={}, hotelId={}, rooms={}, inventoryControlItems={}",
                    storeId,
                    hotelId,
                    rooms.size(),
                    inventoryControls.size()
            );
            JsonNode response = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.postInvRateControl(token, payload),
                    "invratecontrol(inventorycontrol)"
            );
            if (!suApiClient.isSuSuccess(response)) {
                String err = suApiClient.extractSuErrorMessage(response);
                logger.warn(
                        "[SuAvailabilitySync] invratecontrol returned fail. storeId={}, hotelId={}, err={}, raw={}",
                        storeId,
                        hotelId,
                        err,
                        response != null ? response.toString() : "null"
                );
                return new SuAvailabilitySyncSummary(rooms.size(), effectiveDays, startDate, endDate, false, err);
            }
            logger.info("[SuAvailabilitySync] done. storeId={}, hotelId={}, ok=true", storeId, hotelId);
            return new SuAvailabilitySyncSummary(rooms.size(), effectiveDays, startDate, endDate, true, null);
        } catch (RuntimeException e) {
            logger.error("Su availability sync failed. storeId={}, hotelId={}", storeId, hotelId, e);
            return new SuAvailabilitySyncSummary(rooms.size(), effectiveDays, startDate, endDate, false, e.getMessage());
        }
    }

    private static int normalizeDays(Integer days) {
        int d = days != null ? days : DEFAULT_DAYS;
        if (d < 1) {
            d = 1;
        }
        return Math.min(d, MAX_DAYS);
    }

    private static boolean[] buildCanSellCalendar(Room room, List<Reservation> reservations, LocalDate start, int days) {
        boolean roomAvailable = room.getStatus() == RoomStatus.AVAILABLE;
        boolean[] canSell = new boolean[days];

        for (int i = 0; i < days; i++) {
            canSell[i] = roomAvailable;
        }
        if (!roomAvailable) {
            return canSell;
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

    private static List<Map<String, Object>> toDateSegments(boolean[] canSell, LocalDate start, List<Integer> otaCodes) {
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
            segments.add(buildSegment(segmentStart, segmentEnd, currentValue, otaCodes));
            segmentStart = start.plusDays(i);
            currentValue = value;
        }

        LocalDate lastEnd = start.plusDays(canSell.length - 1L);
        segments.add(buildSegment(segmentStart, lastEnd, currentValue, otaCodes));

        return segments;
    }

    private static Map<String, Object> buildSegment(LocalDate from, LocalDate to, int value, List<Integer> otaCodes) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", "Fixed");
        rule.put("value", String.valueOf(value));

        Map<String, Object> otaRule = new LinkedHashMap<>();
        otaRule.put("OTACode", normalizeOtaCodes(otaCodes));
        otaRule.put("rule", rule);

        Map<String, Object> segment = new LinkedHashMap<>();
        segment.put("from", from.toString());
        segment.put("to", to.toString());
        segment.put("OTARule", List.of(otaRule));
        return segment;
    }

    private static List<Integer> normalizeOtaCodes(List<Integer> otaCodes) {
        if (otaCodes == null || otaCodes.isEmpty()) {
            return DEFAULT_SU_OTA_CODES;
        }
        List<Integer> out = otaCodes.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return out.isEmpty() ? DEFAULT_SU_OTA_CODES : out;
    }

    public record SuAvailabilitySyncSummary(
            int roomCount,
            int days,
            LocalDate startDate,
            LocalDate endDate,
            boolean availabilitySynced,
            String availabilityError
    ) {}
}
