package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.util.SuRoomIdParser;

import java.time.LocalDate;
import java.util.List;

/**
 * Su 预订同步：自动排房（将 reservation 绑定到具体 Room）。
 * <p>
 * 约束（A1）：解析/匹配失败只记录日志，不抛错，不影响拉单整体成功。
 */
@Service
public class OtaReservationRoomAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(OtaReservationRoomAssignmentService.class);

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public OtaReservationRoomAssignmentService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

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
            return;
        }

        Room room = roomRepository.findByStoreIdAndRoomNumber(storeId, parsed.roomNumber())
                .orElse(null);
        if (room == null) {
            logger.warn("[OtaReservationRoomAssign] skip auto-assign: room not found. storeId={}, orderNumber={}, roomNumber={}, roomid={}",
                    storeId, reservation.getOrderNumber(), parsed.roomNumber(), parsed.raw());
            return;
        }

        Long localRoomTypeId = room.getRoomType() != null ? room.getRoomType().getId() : null;
        if (localRoomTypeId == null || !localRoomTypeId.equals(parsed.roomTypeId())) {
            logger.warn("[OtaReservationRoomAssign] skip auto-assign: roomType mismatch. storeId={}, orderNumber={}, roomNumber={}, expectedRoomTypeId={}, actualRoomTypeId={}, roomid={}",
                    storeId, reservation.getOrderNumber(), parsed.roomNumber(), parsed.roomTypeId(), localRoomTypeId, parsed.raw());
            return;
        }

        List<Reservation> conflicts = reservationRepository.findByStoreIdAndRoomIdAndDateRange(
                storeId,
                room.getId(),
                checkIn,
                checkOut
        );
        boolean hasConflict = conflicts.stream().anyMatch(r -> reservation.getId() == null || r.getId() == null || !r.getId().equals(reservation.getId()));
        if (hasConflict) {
            logger.warn("[OtaReservationRoomAssign] skip auto-assign: conflicting reservations exist. storeId={}, orderNumber={}, roomNumber={}, roomid={}",
                    storeId, reservation.getOrderNumber(), parsed.roomNumber(), parsed.raw());
            return;
        }

        reservation.setRoom(room);
        logger.info("[OtaReservationRoomAssign] auto-assigned room. storeId={}, orderNumber={}, roomNumber={}, roomid={}",
                storeId, reservation.getOrderNumber(), parsed.roomNumber(), parsed.raw());
    }
}

