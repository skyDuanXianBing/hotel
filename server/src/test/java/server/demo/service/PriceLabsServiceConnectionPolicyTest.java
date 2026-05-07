package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.PriceLabsAccount;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.repository.PriceLabsAccountRepository;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsServiceConnectionPolicyTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void createConnection_shouldRejectWhenRoomTypeAlreadyHasEnabledConnection() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        PriceLabsAccountRepository accountRepository = mock(PriceLabsAccountRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);

        PriceLabsAccount account = buildEnabledAccount();
        RoomType roomType = new RoomType();
        roomType.setId(34L);

        PricePlan existingPlan = new PricePlan();
        existingPlan.setId(10L);
        existingPlan.setName("Primary Plan");

        PriceLabsConnection existing = new PriceLabsConnection();
        existing.setId(100L);
        existing.setStoreId(5L);
        existing.setRoomType(roomType);
        existing.setPricePlan(existingPlan);
        existing.setIsEnabled(true);

        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.empty());
        when(accountRepository.findByStoreIdAndId(5L, 7L)).thenReturn(Optional.of(account));
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.of(existing));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "integrationRepository", integrationRepository);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createConnection(7L, 34L, 11L));
        assertTrue(ex.getMessage().contains("Primary Plan"));

        verify(connectionRepository, times(1)).findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L);
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void createConnection_shouldExposeReadableMessageWhenRoomTypeAndPricePlanAlreadyBound() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        PriceLabsAccountRepository accountRepository = mock(PriceLabsAccountRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);

        PriceLabsAccount selectedAccount = buildEnabledAccount();

        PriceLabsAccount existingAccount = new PriceLabsAccount();
        existingAccount.setId(9L);
        existingAccount.setStoreId(5L);
        existingAccount.setAccountName("Secondary Account");
        existingAccount.setPriceLabsEmail("secondary@example.com");
        existingAccount.setIsEnabled(true);

        RoomType roomType = new RoomType();
        roomType.setId(34L);
        roomType.setName("Family Suite");

        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(11L);
        pricePlan.setName("Weekend Plan");

        PriceLabsConnection existing = new PriceLabsConnection();
        existing.setId(101L);
        existing.setStoreId(5L);
        existing.setAccount(existingAccount);
        existing.setRoomType(roomType);
        existing.setPricePlan(pricePlan);
        existing.setIsEnabled(false);

        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.empty());
        when(accountRepository.findByStoreIdAndId(5L, 7L)).thenReturn(Optional.of(selectedAccount));
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.empty());
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(5L, 34L, 11L))
                .thenReturn(Optional.of(existing));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "integrationRepository", integrationRepository);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createConnection(7L, 34L, 11L));
        assertTrue(ex.getMessage().contains("Family Suite"));
        assertTrue(ex.getMessage().contains("Weekend Plan"));
        assertTrue(ex.getMessage().contains("Secondary Account"));

        verify(connectionRepository, never()).save(any());
    }

    @Test
    void createConnection_shouldRejectWhenAccountDisabled() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        PriceLabsAccountRepository accountRepository = mock(PriceLabsAccountRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);

        PriceLabsAccount account = new PriceLabsAccount();
        account.setId(7L);
        account.setStoreId(5L);
        account.setAccountName("Disabled Account");
        account.setPriceLabsEmail("disabled@example.com");
        account.setIsEnabled(false);

        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.empty());
        when(accountRepository.findByStoreIdAndId(5L, 7L)).thenReturn(Optional.of(account));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "integrationRepository", integrationRepository);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createConnection(7L, 34L, 11L));
        assertTrue(ex.getMessage().contains("Please enable the selected PriceLabs account first"));

        verify(connectionRepository, never()).save(any());
    }

    @Test
    void enableConnection_shouldRejectWhenAnotherEnabledConnectionExists() {
        StoreContextHolder.setContext(new StoreContext(1L, 5L, "ADMIN"));

        PriceLabsConnectionRepository connectionRepository = mock(PriceLabsConnectionRepository.class);
        PriceLabsIntegrationRepository integrationRepository = mock(PriceLabsIntegrationRepository.class);

        RoomType roomType = new RoomType();
        roomType.setId(34L);

        PricePlan planToEnable = new PricePlan();
        planToEnable.setId(11L);
        planToEnable.setName("Weekend Plan");

        PriceLabsAccount account = buildEnabledAccount();

        PriceLabsConnection target = new PriceLabsConnection();
        target.setId(200L);
        target.setStoreId(5L);
        target.setRoomType(roomType);
        target.setPricePlan(planToEnable);
        target.setAccount(account);
        target.setIsEnabled(false);

        PricePlan existingPlan = new PricePlan();
        existingPlan.setId(10L);
        existingPlan.setName("Primary Plan");

        PriceLabsConnection existingEnabled = new PriceLabsConnection();
        existingEnabled.setId(100L);
        existingEnabled.setStoreId(5L);
        existingEnabled.setRoomType(roomType);
        existingEnabled.setPricePlan(existingPlan);
        existingEnabled.setIsEnabled(true);

        when(integrationRepository.findByStoreId(5L)).thenReturn(Optional.empty());
        when(connectionRepository.findById(200L)).thenReturn(Optional.of(target));
        when(connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L))
                .thenReturn(Optional.of(existingEnabled));

        PriceLabsService service = new PriceLabsService();
        ReflectionTestUtils.setField(service, "integrationRepository", integrationRepository);
        ReflectionTestUtils.setField(service, "connectionRepository", connectionRepository);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateConnectionStatus(200L, true));
        assertTrue(ex.getMessage().contains("Primary Plan"));

        verify(connectionRepository, times(1)).findById(200L);
        verify(connectionRepository, times(1)).findByStoreIdAndRoomTypeIdAndIsEnabledTrue(5L, 34L);
        verify(connectionRepository, never()).save(any());
    }

    private PriceLabsAccount buildEnabledAccount() {
        PriceLabsAccount account = new PriceLabsAccount();
        account.setId(7L);
        account.setStoreId(5L);
        account.setAccountName("Main Account");
        account.setPriceLabsEmail("main@example.com");
        account.setIsEnabled(true);
        return account;
    }
}
