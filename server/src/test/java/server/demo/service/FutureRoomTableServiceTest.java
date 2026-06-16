package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.FutureDateRoomData;
import server.demo.dto.FutureRoomTableResponse;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class FutureRoomTableServiceTest {
    private static final Long STORE_ID = 26L;

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getFutureRoomTableData_shouldReleaseCheckedOutRoomOnActualCheckoutDate() {
        StoreContextHolder.setContext(new StoreContext(7L, STORE_ID, "ADMIN"));

        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);

        FutureRoomTableService service = new FutureRoomTableService();
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);

        LocalDate startDate = LocalDate.of(2026, 6, 15);
        RoomType roomType = new RoomType();
        roomType.setId(10L);
        roomType.setStoreId(STORE_ID);
        roomType.setName("标准房");
        roomType.setTotalRooms(1);
        roomType.setDefaultPrice(new BigDecimal("100.00"));

        Room room = new Room();
        room.setId(101L);
        room.setStoreId(STORE_ID);
        room.setRoomType(roomType);
        room.setRoomNumber("101");

        Reservation reservation = new Reservation();
        reservation.setId(501L);
        reservation.setStoreId(STORE_ID);
        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setCheckInDate(LocalDate.of(2026, 6, 15));
        reservation.setCheckOutDate(LocalDate.of(2026, 6, 18));
        reservation.setActualCheckOut(LocalDateTime.of(2026, 6, 16, 9, 0));

        when(roomTypeRepository.findByStoreId(STORE_ID)).thenReturn(List.of(roomType));
        when(roomRepository.findByStoreIdAndRoomTypeId(STORE_ID, 10L)).thenReturn(List.of(room));
        when(reservationRepository.findByStoreIdAndDateRange(STORE_ID, startDate, startDate.plusDays(2)))
                .thenReturn(List.of(reservation));
        when(reservationRepository.findByStoreIdAndDateRange(STORE_ID, startDate, startDate.plusDays(1)))
                .thenReturn(List.of(reservation));
        when(reservationRepository.findByStoreIdAndDateRange(STORE_ID, startDate.plusDays(1), startDate.plusDays(2)))
                .thenReturn(List.of(reservation));
        when(reservationRepository.findByStoreIdAndDateRange(STORE_ID, startDate.plusDays(2), startDate.plusDays(3)))
                .thenReturn(List.of(reservation));

        FutureRoomTableResponse response = service.getFutureRoomTableData(startDate, 3);

        List<FutureDateRoomData> dates = response.getRoomTypes().get(0).getDates();
        assertEquals(1, dates.get(0).getOccupied());
        assertEquals(0, dates.get(0).getAvailable());
        assertEquals(0, dates.get(1).getOccupied());
        assertEquals(1, dates.get(1).getAvailable());
        assertEquals(0, dates.get(2).getOccupied());
        assertEquals(1, dates.get(2).getAvailable());
    }
}
