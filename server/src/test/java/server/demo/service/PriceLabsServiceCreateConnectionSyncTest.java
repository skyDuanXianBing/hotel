package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.constants.PriceLabsSyncDefaults;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.PriceLabsAccount;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.repository.PriceLabsAccountRepository;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        PriceLabsAccountRepository accountRepository = mock(PriceLabsAccountRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);
        RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = mock(PricePlanRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        PriceLabsSyncService syncService = mock(PriceLabsSyncService.class);

        PriceLabsAccount account = buildEnabledAccount();
        RoomType roomType = buildRoomType();
        PricePlan pricePlan = buildPricePlan();

        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.empty());
        when(accountRepository.findByStoreIdAndId(5L, 8L)).thenReturn(Optional.of(account));
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.empty());
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(Optional.empty());
        when(roomTypeRepository.findByStoreIdAndId(5L, 34L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(5L, 10L)).thenReturn(Optional.of(pricePlan));
        when(roomRepository.findByStoreIdAndRoomTypeId(5L, 34L)).thenReturn(List.of());
        when(roomTypePricePlanRepository.existsByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(true);
        when(connectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PriceLabsService service = buildService(
                connectionRepository,
                accountRepository,
                integrationRepository,
                roomTypeRepository,
                pricePlanRepository,
                roomRepository,
                roomTypePricePlanRepository,
                syncService
        );

        var dto = service.createConnection(8L, 34L, 10L);

        assertNotNull(dto);
        assertEquals(8L, dto.getAccountId());
        assertEquals("Main Account", dto.getAccountName());

        ArgumentCaptor<PriceLabsConnection> captor = ArgumentCaptor.forClass(PriceLabsConnection.class);
        verify(connectionRepository, times(1)).save(captor.capture());
        assertSame(account, captor.getValue().getAccount());

        verify(syncService, times(1)).syncListingRatePlanAndCalendar(
                eq(5L),
                eq(roomType),
                eq(pricePlan),
                anyString(),
                eq(PriceLabsSyncDefaults.DEFAULT_SYNC_DAYS)
        );
    }

    @Test
    void createConnection_shouldThrowWhenSyncFails() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        PriceLabsAccountRepository accountRepository = mock(PriceLabsAccountRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);
        RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = mock(PricePlanRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        PriceLabsSyncService syncService = mock(PriceLabsSyncService.class);

        PriceLabsAccount account = buildEnabledAccount();
        RoomType roomType = buildRoomType();
        PricePlan pricePlan = buildPricePlan();

        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.empty());
        when(accountRepository.findByStoreIdAndId(5L, 8L)).thenReturn(Optional.of(account));
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.empty());
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(Optional.empty());
        when(roomTypeRepository.findByStoreIdAndId(5L, 34L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(5L, 10L)).thenReturn(Optional.of(pricePlan));
        when(roomRepository.findByStoreIdAndRoomTypeId(5L, 34L)).thenReturn(List.of());
        when(roomTypePricePlanRepository.existsByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(true);
        when(connectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("boom")).when(syncService).syncListingRatePlanAndCalendar(
                eq(5L),
                eq(roomType),
                eq(pricePlan),
                anyString(),
                eq(PriceLabsSyncDefaults.DEFAULT_SYNC_DAYS)
        );

        PriceLabsService service = buildService(
                connectionRepository,
                accountRepository,
                integrationRepository,
                roomTypeRepository,
                pricePlanRepository,
                roomRepository,
                roomTypePricePlanRepository,
                syncService
        );

        assertThrows(RuntimeException.class, () -> service.createConnection(8L, 34L, 10L));
        verify(connectionRepository, times(1)).save(any());
    }

    @Test
    void createConnection_shouldThrowWhenRoomTypeAndPricePlanNotBound() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        PriceLabsAccountRepository accountRepository = mock(PriceLabsAccountRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);
        RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
        PricePlanRepository pricePlanRepository = mock(PricePlanRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomTypePricePlanRepository roomTypePricePlanRepository = mock(RoomTypePricePlanRepository.class);
        PriceLabsSyncService syncService = mock(PriceLabsSyncService.class);

        PriceLabsAccount account = buildEnabledAccount();
        RoomType roomType = buildRoomType();
        PricePlan pricePlan = buildPricePlan();

        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.empty());
        when(accountRepository.findByStoreIdAndId(5L, 8L)).thenReturn(Optional.of(account));
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.empty());
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(Optional.empty());
        when(roomTypeRepository.findByStoreIdAndId(5L, 34L)).thenReturn(Optional.of(roomType));
        when(pricePlanRepository.findByStoreIdAndId(5L, 10L)).thenReturn(Optional.of(pricePlan));
        when(roomRepository.findByStoreIdAndRoomTypeId(5L, 34L)).thenReturn(List.of());
        when(roomTypePricePlanRepository.existsByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 10L))
                .thenReturn(false);

        PriceLabsService service = buildService(
                connectionRepository,
                accountRepository,
                integrationRepository,
                roomTypeRepository,
                pricePlanRepository,
                roomRepository,
                roomTypePricePlanRepository,
                syncService
        );

        assertThrows(RuntimeException.class, () -> service.createConnection(8L, 34L, 10L));
    }

    private PriceLabsService buildService(
            PriceLabsConnectionRepository connectionRepository,
            PriceLabsAccountRepository accountRepository,
            PriceLabsIntegrationRepository integrationRepository,
            RoomTypeRepository roomTypeRepository,
            PricePlanRepository pricePlanRepository,
            RoomRepository roomRepository,
            RoomTypePricePlanRepository roomTypePricePlanRepository,
            PriceLabsSyncService syncService
    ) {
        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "integrationRepository", integrationRepository);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);
        ReflectionTestUtils.setField(service, "roomTypeRepository", roomTypeRepository);
        ReflectionTestUtils.setField(service, "pricePlanRepository", pricePlanRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "roomTypePricePlanRepository", roomTypePricePlanRepository);
        ReflectionTestUtils.setField(service, "priceLabsSyncService", syncService);
        return service;
    }

    private PriceLabsAccount buildEnabledAccount() {
        PriceLabsAccount account = new PriceLabsAccount();
        account.setId(8L);
        account.setStoreId(5L);
        account.setAccountName("Main Account");
        account.setPriceLabsEmail("main@example.com");
        account.setIsEnabled(true);
        return account;
    }

    private RoomType buildRoomType() {
        RoomType roomType = new RoomType();
        roomType.setId(34L);
        roomType.setStoreId(5L);
        roomType.setName("Room 34");
        roomType.setTotalRooms(3);
        return roomType;
    }

    private PricePlan buildPricePlan() {
        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(10L);
        pricePlan.setStoreId(5L);
        pricePlan.setName("Plan 10");
        return pricePlan;
    }
}
