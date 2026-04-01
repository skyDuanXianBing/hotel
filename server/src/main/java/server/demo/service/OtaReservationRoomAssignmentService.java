package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.util.SuRoomIdParser;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Su 预订同步：自动排房（将 reservation 绑定到具体 Room）。
 * <p>
 * 约束（A1）：解析/匹配失败只记录日志，不抛错，不影响拉单整体成功。
 */
@Service
public class OtaReservationRoomAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(OtaReservationRoomAssignmentService.class);
    private static final Logger reservationLogger = LoggerFactory.getLogger("SU_RESERVATION");

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public OtaReservationRoomAssignmentService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public void tryAutoAssignRoom(Long storeId, Reservation reservation, String itProviderRoomId, LocalDate checkIn, LocalDate checkOut) {
        if (storeId == null || reservation == null || checkIn == null || checkOut == null) {
            return;
        }
        if (reservation.getRoom() != null) {
            return;
        }

        SuRoomIdParser.ParsedRoomId parsed = SuRoomIdParser.parse(itProviderRoomId);
        if (parsed == null) {
            logger.warn("[OtaReservationRoomAssign] skip auto-assign: invalid roomid. storeId={}, orderNumber={}, roomid={}",
                    storeId, reservation.getOrderNumber(), itProviderRoomId);
            reservationLogger.warn("[ReservationAssign] invalid roomid. storeId={}, orderNumber={}, roomid={}",
                    storeId, reservation.getOrderNumber(), itProviderRoomId);
            return;
        }

        Room room = null;

        // 1) Room-level format: {roomTypeId}-{roomNumber}
        if (parsed.roomNumber() != null && !parsed.roomNumber().isBlank()) {
            Room matchedRoom = roomRepository.findByStoreIdAndRoomNumber(storeId, parsed.roomNumber())
                    .orElse(null);
            if (matchedRoom == null) {
                logger.warn("[OtaReservationRoomAssign] skip auto-assign: room not found. storeId={}, orderNumber={}, roomNumber={}, roomid={}",
                        storeId, reservation.getOrderNumber(), parsed.roomNumber(), parsed.raw());
                reservationLogger.warn("[ReservationAssign] room not found. storeId={}, orderNumber={}, roomNumber={}, roomid={}",
                        storeId, reservation.getOrderNumber(), parsed.roomNumber(), parsed.raw());
                return;
            }

            room = lockRoomForAssignment(storeId, matchedRoom.getId());
            if (room == null) {
                logger.warn("[OtaReservationRoomAssign] skip auto-assign: room lock failed. storeId={}, orderNumber={}, roomId={}, roomid={}",
                        storeId, reservation.getOrderNumber(), matchedRoom.getId(), parsed.raw());
                reservationLogger.warn("[ReservationAssign] room lock failed. storeId={}, orderNumber={}, roomId={}, roomid={}",
                        storeId, reservation.getOrderNumber(), matchedRoom.getId(), parsed.raw());
                return;
            }

            Long localRoomTypeId = room.getRoomType() != null ? room.getRoomType().getId() : null;
            if (localRoomTypeId == null || !localRoomTypeId.equals(parsed.roomTypeId())) {
                logger.warn("[OtaReservationRoomAssign] skip auto-assign: roomType mismatch. storeId={}, orderNumber={}, roomNumber={}, expectedRoomTypeId={}, actualRoomTypeId={}, roomid={}",
                        storeId, reservation.getOrderNumber(), parsed.roomNumber(), parsed.roomTypeId(), localRoomTypeId, parsed.raw());
                reservationLogger.warn("[ReservationAssign] roomType mismatch. storeId={}, orderNumber={}, roomNumber={}, expectedRoomTypeId={}, actualRoomTypeId={}, roomid={}",
                        storeId, reservation.getOrderNumber(), parsed.roomNumber(), parsed.roomTypeId(), localRoomTypeId, parsed.raw());
                return;
            }
        } else {
            // 2) RoomType-level format: {roomTypeId}
            List<Room> candidates = roomRepository.findByStoreIdAndRoomTypeId(storeId, parsed.roomTypeId());
            if (candidates == null || candidates.isEmpty()) {
                logger.warn("[OtaReservationRoomAssign] skip auto-assign: no rooms under roomType. storeId={}, orderNumber={}, roomTypeId={}, roomid={}",
                        storeId, reservation.getOrderNumber(), parsed.roomTypeId(), parsed.raw());
                reservationLogger.warn("[ReservationAssign] no rooms under roomType. storeId={}, orderNumber={}, roomTypeId={}, roomid={}",
                        storeId, reservation.getOrderNumber(), parsed.roomTypeId(), parsed.raw());
                return;
            }

            // pick the first non-conflicting room
            for (Room candidate : candidates) {
                if (candidate == null || candidate.getId() == null) {
                    continue;
                }

                Room lockedCandidate = lockRoomForAssignment(storeId, candidate.getId());
                if (lockedCandidate == null) {
                    continue;
                }

                List<Reservation> conflicts = findConflictsExcludingCurrent(
                        storeId,
                        lockedCandidate.getId(),
                        checkIn,
                        checkOut,
                        reservation
                );
                if (conflicts.isEmpty()) {
                    room = lockedCandidate;
                    break;
                }
            }

            if (room == null) {
                logger.warn("[OtaReservationRoomAssign] skip auto-assign: no available room found under roomType. storeId={}, orderNumber={}, roomTypeId={}, roomid={}",
                        storeId, reservation.getOrderNumber(), parsed.roomTypeId(), parsed.raw());
                reservationLogger.warn("[ReservationAssign] no available room found. storeId={}, orderNumber={}, roomTypeId={}, roomid={}",
                        storeId, reservation.getOrderNumber(), parsed.roomTypeId(), parsed.raw());
                return;
            }
        }

        // final conflict check (defensive)
        List<Reservation> conflicts = findConflictsExcludingCurrent(
                storeId,
                room.getId(),
                checkIn,
                checkOut,
                reservation
        );
        if (!conflicts.isEmpty()) {
            String conflictSummary = conflicts.stream()
                    .limit(5)
                    .map(r -> "{" +
                            "id=" + r.getId() +
                            ",orderNumber=" + r.getOrderNumber() +
                            ",status=" + (r.getStatus() != null ? r.getStatus().name() : null) +
                            ",checkIn=" + r.getCheckInDate() +
                            ",checkOut=" + r.getCheckOutDate() +
                            ",guest=" + r.getGuestName() +
                            "}")
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            logger.warn("[OtaReservationRoomAssign] skip auto-assign: conflicting reservations exist. storeId={}, orderNumber={}, roomId={}, roomid={}",
                    storeId, reservation.getOrderNumber(), room.getId(), parsed.raw());
            reservationLogger.warn("[ReservationAssign] conflict detected. storeId={}, orderNumber={}, roomId={}, roomNumber={}, checkIn={}, checkOut={}, conflicts={}, conflictSample={}",
                    storeId,
                    reservation.getOrderNumber(),
                    room.getId(),
                    room.getRoomNumber(),
                    checkIn,
                    checkOut,
                    conflicts.size(),
                    conflictSummary);
            return;
        }

        reservation.setRoom(room);
        logger.info("[OtaReservationRoomAssign] auto-assigned room. storeId={}, orderNumber={}, roomId={}, roomNumber={}, roomid={}",
                storeId, reservation.getOrderNumber(), room.getId(), room.getRoomNumber(), parsed.raw());
        reservationLogger.info("[ReservationAssign] assigned. storeId={}, orderNumber={}, roomId={}, roomNumber={}, checkIn={}, checkOut={}, roomid={}",
                storeId, reservation.getOrderNumber(), room.getId(), room.getRoomNumber(), checkIn, checkOut, parsed.raw());
    }

    private Room lockRoomForAssignment(Long storeId, Long roomId) {
        return roomRepository.findByStoreIdAndIdForUpdate(storeId, roomId).orElse(null);
    }

    private List<Reservation> findConflictsExcludingCurrent(
            Long storeId,
            Long roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            Reservation reservation
    ) {
        Long currentReservationId = reservation.getId();
        return reservationRepository.findByStoreIdAndRoomIdAndDateRange(storeId, roomId, checkIn, checkOut)
                .stream()
                .filter(r -> !Objects.equals(r.getId(), currentReservationId))
                .toList();
    }
}

