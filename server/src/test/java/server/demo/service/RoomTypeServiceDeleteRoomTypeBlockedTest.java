package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.RoomTypeDeleteBlockInfo;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.ReservationStatus;
import server.demo.exception.RoomTypeDeleteBlockedException;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomTypeServiceDeleteRoomTypeBlockedTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void deleteRoomType_shouldReturnBlockingReservationList() {
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "OWNER"));

        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        RoomTypeService service = new RoomTypeService();
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);

        RoomType roomType = new RoomType();
        roomType.setId(40L);
        roomType.setStoreId(7L);

        Room room = new Room();
        room.setId(100L);
        room.setRoomNumber("101");

        Reservation reservation = new Reservation();
        reservation.setOrderNumber("ORDER-1");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(LocalDate.now().plusDays(1));
        reservation.setCheckOutDate(LocalDate.now().plusDays(2));
        reservation.setRoom(room);

        when(roomTypeRepository.findById(40L)).thenReturn(Optional.of(roomType));
        when(roomRepository.findByStoreIdAndRoomTypeId(7L, 40L)).thenReturn(List.of(room));
        when(reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                Mockito.eq(7L),
                Mockito.eq(List.of(100L)),
                Mockito.any(LocalDate.class),
                Mockito.any(LocalDate.class),
                Mockito.anySet()
        )).thenReturn(List.of(reservation));

        RoomTypeDeleteBlockedException ex = assertThrows(RoomTypeDeleteBlockedException.class, () -> service.deleteRoomType(40L));
        RoomTypeDeleteBlockInfo info = ex.getBlockInfo();
        assertNotNull(info);
        assertEquals(1, info.getTotalBlockingReservations());
        assertEquals(1, info.getSample().size());
        assertEquals("ORDER-1", info.getSample().get(0).getOrderNumber());
        assertEquals(ReservationStatus.CONFIRMED, info.getSample().get(0).getStatus());
        assertEquals("101", info.getSample().get(0).getRoomNumber());

        // should not proceed to delete
        verify(roomRepository, never()).deleteAll(Mockito.anyList());
    }
}
