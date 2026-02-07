package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomBlockout;
import server.demo.entity.RoomType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomBlockoutType;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class RoomStatusServiceTest {

    @Test
    void getRoomStatusCalendarForStore_shouldTreatRequestedAsReservedAndShowReservationInfo() {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        RoomBlockoutRepository roomBlockoutRepository = Mockito.mock(RoomBlockoutRepository.class);

        RoomStatusService service = new RoomStatusService();
        inject(service, "roomRepository", roomRepository);
        inject(service, "reservationRepository", reservationRepository);
        inject(service, "roomBlockoutRepository", roomBlockoutRepository);

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("RT1");

        Room room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);

        when(roomRepository.findByStoreIdWithRoomType(5L)).thenReturn(List.of(room));

        Channel channel = new Channel();
        channel.setName("BOOKING");

        Reservation reservation = new Reservation();
        reservation.setId(99L);
        reservation.setStatus(ReservationStatus.REQUESTED);
        reservation.setGuestName("GuestA");
        reservation.setChannel(channel);
        reservation.setCheckInDate(LocalDate.of(2025, 1, 1));
        reservation.setCheckOutDate(LocalDate.of(2025, 1, 2));
        reservation.setOrderNumber("SU5-1");

        when(reservationRepository.findByStoreIdAndRoomIdAndDate(eq(5L), eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(reservation));

        when(roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(eq(5L), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 1);
        RoomStatusCalendarDTO calendar = service.getRoomStatusCalendarForStore(5L, start, end);

        assertNotNull(calendar);
        assertEquals(1, calendar.getRooms().size());
        RoomStatusCalendarDTO.CalendarRoomDataDTO r = calendar.getRooms().get(0);
        assertEquals("101", r.getRoomNumber());
        assertEquals(1, r.getDailyStatus().size());
        assertEquals(RoomStatus.RESERVED, r.getDailyStatus().get(0).getStatus());
        assertNotNull(r.getDailyStatus().get(0).getReservation());
        assertEquals("GuestA", r.getDailyStatus().get(0).getReservation().getGuestName());
    }

    @Test
    void getRoomStatusCalendarForStore_shouldExposeClosedAndCloseTypeFromBlockouts() {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        RoomBlockoutRepository roomBlockoutRepository = Mockito.mock(RoomBlockoutRepository.class);

        RoomStatusService service = new RoomStatusService();
        inject(service, "roomRepository", roomRepository);
        inject(service, "reservationRepository", reservationRepository);
        inject(service, "roomBlockoutRepository", roomBlockoutRepository);

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("RT1");

        Room room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);

        when(roomRepository.findByStoreIdWithRoomType(5L)).thenReturn(List.of(room));
        when(reservationRepository.findByStoreIdAndRoomIdAndDate(eq(5L), eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        RoomBlockout blockout = new RoomBlockout(5L, room, LocalDate.of(2025, 1, 1), RoomBlockoutType.STOP, "x");
        when(roomBlockoutRepository.findByStoreIdAndRoom_IdInAndBlockDateBetween(eq(5L), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(blockout));

        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 1);
        RoomStatusCalendarDTO calendar = service.getRoomStatusCalendarForStore(5L, start, end);

        assertNotNull(calendar);
        assertEquals(1, calendar.getRooms().size());
        RoomStatusCalendarDTO.CalendarRoomDataDTO r = calendar.getRooms().get(0);
        assertEquals(1, r.getDailyStatus().size());
        assertEquals(Boolean.TRUE, r.getDailyStatus().get(0).getClosed());
        assertEquals("stop", r.getDailyStatus().get(0).getCloseType());
        assertEquals("x", r.getDailyStatus().get(0).getCloseRemark());
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
