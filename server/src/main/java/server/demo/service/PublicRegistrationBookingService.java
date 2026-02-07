package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;
import server.demo.dto.registration.PublicRegistrationBookingResponse;
import server.demo.dto.registration.PublicRegistrationBookingRoomDTO;
import server.demo.dto.registration.PublicRegistrationResponse;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.repository.ReservationRepository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PublicRegistrationBookingService {

    private final ReservationRepository reservationRepository;
    private final PublicRegistrationService publicRegistrationService;
    private final RegistrationLinkService registrationLinkService;

    public PublicRegistrationBookingService(
            ReservationRepository reservationRepository,
            PublicRegistrationService publicRegistrationService,
            RegistrationLinkService registrationLinkService
    ) {
        this.reservationRepository = reservationRepository;
        this.publicRegistrationService = publicRegistrationService;
        this.registrationLinkService = registrationLinkService;
    }

    @Transactional
    public PublicRegistrationBookingResponse getBooking(Long storeId, String bookingKey) {
        if (storeId == null || bookingKey == null || bookingKey.isBlank()) {
            throw new RuntimeException("bookingKey 不能为空");
        }

        List<Reservation> reservations = reservationRepository.findByStoreIdAndChannelOrderNumber(storeId, bookingKey.trim());
        if (reservations == null || reservations.isEmpty()) {
            // fallback: single-room bookingKey might be orderNumber (local booking)
            Reservation single = reservationRepository.findByStoreIdAndOrderNumber(storeId, bookingKey.trim()).orElse(null);
            reservations = single == null ? List.of() : List.of(single);
        }

        if (reservations.isEmpty()) {
            throw new RuntimeException("预订不存在");
        }

        List<Reservation> sorted = new ArrayList<>(reservations);
        sorted.sort(Comparator.comparing(Reservation::getCheckInDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Reservation::getCheckOutDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Reservation::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        PublicRegistrationBookingResponse resp = new PublicRegistrationBookingResponse();
        resp.setBookingKey(bookingKey.trim());

        Reservation first = sorted.get(0);
        resp.setGuestName(first != null ? nullToEmpty(first.getGuestName()) : "");
        resp.setCheckInDate(first != null ? first.getCheckInDate() : null);
        resp.setCheckOutDate(first != null ? first.getCheckOutDate() : null);

        List<PublicRegistrationBookingRoomDTO> rooms = new ArrayList<>();
        for (Reservation r : sorted) {
            if (r == null || r.getOrderNumber() == null) {
                continue;
            }

            PublicRegistrationResponse roomForm = publicRegistrationService.getOrCreate(storeId, r.getOrderNumber());

            PublicRegistrationBookingRoomDTO room = new PublicRegistrationBookingRoomDTO();
            room.setOrderNumber(r.getOrderNumber());
            room.setRoomTypeName(resolveRoomTypeName(r));
            room.setRoomNumber(resolveRoomNumber(r));
            room.setCheckInDate(roomForm.getCheckInDate());
            room.setCheckOutDate(roomForm.getCheckOutDate());
            room.setMaxGuests(roomForm.getMaxGuests());
            room.setGuestCount(roomForm.getGuestCount());
            room.setStatus(roomForm.getStatus());
            room.setLastSavedAt(roomForm.getLastSavedAt());
            room.setRoomRegistrationLink(buildRoomRegistrationLink(storeId, r.getOrderNumber()));
            rooms.add(room);
        }

        resp.setRooms(rooms);
        return resp;
    }

    @Transactional(readOnly = true)
    public void assertRoomBelongsToBooking(Long storeId, String bookingKey, String orderNumber) {
        if (storeId == null || bookingKey == null || bookingKey.isBlank() || orderNumber == null || orderNumber.isBlank()) {
            throw new RuntimeException("参数错误");
        }

        Reservation reservation = reservationRepository.findByStoreIdAndOrderNumber(storeId, orderNumber.trim()).orElse(null);
        if (reservation == null) {
            throw new RuntimeException("订单不存在");
        }

        String key = bookingKey.trim();
        String channelBookingId = reservation.getChannelOrderNumber();
        if (channelBookingId != null && !channelBookingId.isBlank()) {
            if (!channelBookingId.trim().equals(key)) {
                throw new RuntimeException("订单不属于该预订");
            }
            return;
        }

        // local booking fallback: bookingKey = orderNumber
        if (!orderNumber.trim().equals(key)) {
            throw new RuntimeException("订单不属于该预订");
        }
    }

    private String buildRoomRegistrationLink(Long storeId, String orderNumber) {
        String token = registrationLinkService.generateToken(storeId, orderNumber);
        String encodedOrder = UriUtils.encodePathSegment(orderNumber, StandardCharsets.UTF_8);
        return "/r/" + encodedOrder + "?t=" + token;
    }

    private static String resolveRoomTypeName(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null && room.getRoomType() != null) {
            RoomType rt = room.getRoomType();
            return nullToEmpty(rt.getName());
        }
        return "";
    }

    private static String resolveRoomNumber(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null) {
            return nullToEmpty(room.getRoomNumber());
        }
        return nullToEmpty(reservation.getOtaRoomNumber());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
