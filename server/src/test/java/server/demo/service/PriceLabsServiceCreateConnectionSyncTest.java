package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.constants.PriceLabsSyncDefaults;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsServiceCreateConnectionSyncTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void createConnection_shouldSyncListingRatePlanAndCalendar() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = mock(PricePlanRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        PriceLabsSyncService syncService = mock(PriceLabsSyncService.class);

        RoomType roomType = new RoomType();
        roomType.setId(34L);
        roomType.setStoreId(5L);
        roomType.setName("Room 34");
        roomType.setTotalRooms(3);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(10L);
        pricePlan.setStoreId(5L);
        pricePlan.setName("Plan 10");

        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.empty());
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(Optional.empty());
        when(roomTypeRepository.findByStoreIdAndId(5L, 34L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(5L, 10L)).thenReturn(Optional.of(pricePlan));
        when(roomRepository.findByStoreIdAndRoomTypeId(5L, 34L)).thenReturn(List.of());
        when(roomTypePricePlanRepository.existsByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L)).thenReturn(true);
        when(connectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "priceLabsSyncService", syncService);

        var dto = service.createConnection(34L, 10L);
        assertNotNull(dto);

        verify(connectionRepository, times(1)).save(any());
        verify(syncService, times(1)).syncListingRatePlanAndCalendar(
                5L,
                roomType,
                pricePlan,
                PriceLabsSyncDefaults.DEFAULT_SYNC_DAYS
        );
    }

    @Test
    void createConnection_shouldThrowWhenSyncFails() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = mock(PricePlanRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        PriceLabsSyncService syncService = mock(PriceLabsSyncService.class);

        RoomType roomType = new RoomType();
        roomType.setId(34L);
        roomType.setStoreId(5L);
        roomType.setName("Room 34");
        roomType.setTotalRooms(3);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(10L);
        pricePlan.setStoreId(5L);
        pricePlan.setName("Plan 10");

        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.empty());
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(Optional.empty());
        when(roomTypeRepository.findByStoreIdAndId(5L, 34L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(5L, 10L)).thenReturn(Optional.of(pricePlan));
        when(roomRepository.findByStoreIdAndRoomTypeId(5L, 34L)).thenReturn(List.of());
        when(roomTypePricePlanRepository.existsByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L)).thenReturn(true);
        when(connectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("boom")).when(syncService).syncListingRatePlanAndCalendar(
                5L,
                roomType,
                pricePlan,
                PriceLabsSyncDefaults.DEFAULT_SYNC_DAYS
        );

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "priceLabsSyncService", syncService);

        assertThrows(RuntimeException.class, () -> service.createConnection(34L, 10L));
        verify(connectionRepository, times(1)).save(any());
    }

    @Test
    void createConnection_shouldThrowWhenRoomTypeAndPricePlanNotBound() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = mock(PricePlanRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        PriceLabsSyncService syncService = mock(PriceLabsSyncService.class);

        RoomType roomType = new RoomType();
        roomType.setId(34L);
        roomType.setStoreId(5L);
        roomType.setName("Room 34");
        roomType.setTotalRooms(3);

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(10L);
        pricePlan.setStoreId(5L);
        pricePlan.setName("Plan 10");

        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.empty());
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(Optional.empty());
        when(roomTypeRepository.findByStoreIdAndId(5L, 34L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(5L, 10L)).thenReturn(Optional.of(pricePlan));
        when(roomRepository.findByStoreIdAndRoomTypeId(5L, 34L)).thenReturn(List.of());
        when(roomTypePricePlanRepository.existsByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L)).thenReturn(false);

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "priceLabsSyncService", syncService);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createConnection(34L, 10L));
        assertTrue(ex.getMessage().contains("房型绑定"));
    }
}
