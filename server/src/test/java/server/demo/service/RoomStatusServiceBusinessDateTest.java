package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.Store;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomStatusServiceBusinessDateTest {

    @BeforeEach
    void setUp() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("Asia/Shanghai"));
    }

    @AfterEach
    void tearDown() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(
                ZoneId.of(StoreTimeZoneUtil.DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE)
        );
    }

    @Test
    void statisticsForStore_shouldDefaultToTokyoBusinessDateAndStorageWindow() {
        Long storeId = 26L;
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        RoomBlockoutRepository roomBlockoutRepository = Mockito.mock(RoomBlockoutRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);

        RoomStatusService service = new RoomStatusService();
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "roomBlockoutRepository", roomBlockoutRepository);
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(
                service,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );

        Store store = new Store();
        store.setId(storeId);
        store.setTimezone("Asia/Tokyo");
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(roomRepository.findByStoreId(storeId)).thenReturn(List.of());

        service.getRoomStatusStatisticsForStore(storeId, null);

        verify(reservationRepository).countTodayArrivalsByStoreId(storeId, LocalDate.of(2026, 4, 8));
        verify(reservationRepository).countByStoreIdAndCheckOutDate(storeId, LocalDate.of(2026, 4, 8));
        verify(reservationRepository).countTodayNewOrdersByStoreId(
                storeId,
                LocalDateTime.of(2026, 4, 7, 23, 0),
                LocalDateTime.of(2026, 4, 8, 23, 0)
        );
        verify(reservationRepository).countUnassignedOrUnmappedByStoreId(storeId, LocalDate.of(2026, 4, 8));
    }
}
