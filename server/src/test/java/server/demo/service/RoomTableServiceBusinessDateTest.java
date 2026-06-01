package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomTableServiceBusinessDateTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void roomTableCancelledWindow_shouldUseStoreBusinessDateBoundary() {
        Long storeId = 26L;
        StoreContextHolder.setContext(new StoreContext(7L, storeId, "ADMIN"));

        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);

        RoomTableService service = new RoomTableService();
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        roomType.setName("标准房");

        Room room = new Room();
        room.setId(101L);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setRoomType(roomType);

        Store store = new Store();
        store.setId(storeId);
        store.setTimezone("Asia/Tokyo");

        when(roomTypeRepository.findByStoreId(storeId)).thenReturn(List.of(roomType));
        when(roomRepository.findByStoreIdAndRoomTypeId(storeId, 10L)).thenReturn(List.of(room));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(reservationRepository.findByStoreIdAndRoomTypeAndDate(storeId, 10L, LocalDate.of(2026, 4, 8), "ARRIVAL"))
                .thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndRoomTypeAndDate(storeId, 10L, LocalDate.of(2026, 4, 8), "DEPARTURE"))
                .thenReturn(List.of());
        when(reservationRepository.findOccupiedWithoutDepartureByStore(storeId, 10L, LocalDate.of(2026, 4, 8)))
                .thenReturn(List.of());
        when(reservationRepository.findCancelledByStoreAndRoomTypeAndDate(
                storeId,
                10L,
                LocalDateTime.of(2026, 4, 7, 15, 0),
                LocalDateTime.of(2026, 4, 8, 15, 0)
        )).thenReturn(List.of());

        service.getRoomTableStatistics(LocalDate.of(2026, 4, 8));

        verify(reservationRepository).findCancelledByStoreAndRoomTypeAndDate(
                storeId,
                10L,
                LocalDateTime.of(2026, 4, 7, 15, 0),
                LocalDateTime.of(2026, 4, 8, 15, 0)
        );
    }
}
