package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.CreateStoreRequest;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.repository.StorePolicyRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StoreServiceTimezoneValidationTest {

    @Test
    void createStore_shouldTrimValidTimezoneBeforeSave() {
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        StorePolicyRepository storePolicyRepository = Mockito.mock(StorePolicyRepository.class);
        ChannelBootstrapService channelBootstrapService = Mockito.mock(ChannelBootstrapService.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "storePolicyRepository", storePolicyRepository);
        ReflectionTestUtils.setField(service, "channelBootstrapService", channelBootstrapService);

        User user = new User();
        user.setEmail("owner@example.com");

        CreateStoreRequest request = buildRequest(" Asia/Tokyo ");
        request.setSuHotelId("HOTEL123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            if (store.getId() == null) {
                store.setId(9L);
            }
            return store;
        });
        when(storePolicyRepository.findByStoreId(9L)).thenReturn(Optional.empty());

        service.createStore(1L, request);

        verify(storeRepository, Mockito.atLeastOnce())
                .save(Mockito.argThat(store -> "Asia/Tokyo".equals(store.getTimezone())));
        verify(storeUserRepository).save(any(StoreUser.class));
        verify(channelBootstrapService).ensureDefaultChannelsForStore(9L);
    }

    @Test
    void updateStore_shouldTrimValidTimezoneBeforeSave() {
        StoreServiceFixture fixture = createUpdateFixture("Asia/Shanghai");
        CreateStoreRequest request = buildRequest(" Asia/Tokyo ");

        fixture.service.updateStore(9L, 1L, request);

        assertEquals("Asia/Tokyo", fixture.store.getTimezone());
        verify(fixture.storeRepository).save(fixture.store);
        verify(fixture.priceLabsSyncService).syncListingsUsingStoreAddressAsync(9L);
    }

    @Test
    void updateStore_shouldSaveNullForBlankTimezone() {
        StoreServiceFixture fixture = createUpdateFixture("Asia/Tokyo");
        CreateStoreRequest request = buildRequest(" ");

        fixture.service.updateStore(9L, 1L, request);

        assertNull(fixture.store.getTimezone());
        verify(fixture.storeRepository).save(fixture.store);
        verify(fixture.priceLabsSyncService).syncListingsUsingStoreAddressAsync(9L);
    }

    @Test
    void updateStore_shouldRejectInvalidTimezoneBeforeSave() {
        StoreServiceFixture fixture = createUpdateFixture("Asia/Tokyo");
        CreateStoreRequest request = buildRequest("bad/timezone");

        assertThrows(IllegalArgumentException.class, () -> fixture.service.updateStore(9L, 1L, request));

        verify(fixture.storeRepository, never()).save(any(Store.class));
        verifyNoInteractions(fixture.priceLabsSyncService);
        assertEquals("Asia/Tokyo", fixture.store.getTimezone());
    }

    private static StoreServiceFixture createUpdateFixture(String currentTimezone) {
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StorePolicyRepository storePolicyRepository = Mockito.mock(StorePolicyRepository.class);
        PriceLabsSyncService priceLabsSyncService = Mockito.mock(PriceLabsSyncService.class);
        SuPropertyService suPropertyService = Mockito.mock(SuPropertyService.class);
        SuImageSyncService suImageSyncService = Mockito.mock(SuImageSyncService.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "storePolicyRepository", storePolicyRepository);
        ReflectionTestUtils.setField(service, "priceLabsSyncService", priceLabsSyncService);
        ReflectionTestUtils.setField(service, "suPropertyService", suPropertyService);
        ReflectionTestUtils.setField(service, "suImageSyncService", suImageSyncService);

        Store store = new Store();
        store.setId(9L);
        store.setName("Current");
        store.setTimezone(currentTimezone);
        store.setCountry("JP");
        store.setType("hotel");

        StoreUser operator = new StoreUser();
        operator.setRole("owner");
        operator.setStore(store);

        when(storeUserRepository.findByStoreIdAndUserId(9L, 1L)).thenReturn(Optional.of(operator));
        when(storeRepository.save(store)).thenReturn(store);
        when(storePolicyRepository.findByStoreId(9L)).thenReturn(Optional.empty());
        when(suPropertyService.updateStoreProperty(9L)).thenReturn(
                new SuPropertyService.UpsertResult(true, true, "HOTEL123", "ok")
        );

        StoreServiceFixture fixture = new StoreServiceFixture();
        fixture.service = service;
        fixture.store = store;
        fixture.storeRepository = storeRepository;
        fixture.priceLabsSyncService = priceLabsSyncService;
        return fixture;
    }

    private static CreateStoreRequest buildRequest(String timezone) {
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("Test Store");
        request.setPhone("123");
        request.setPhoneTechType("5");
        request.setType("hotel");
        request.setTimezone(timezone);
        request.setManager("Tester");
        request.setCountry("JP");
        request.setCity("Tokyo");
        request.setState("Tokyo");
        request.setAddress("Address");
        request.setDescription("Desc");
        request.setEmail("hotel@example.com");
        request.setLanguage("ja");
        return request;
    }

    private static class StoreServiceFixture {
        private StoreService service;
        private Store store;
        private StoreRepository storeRepository;
        private PriceLabsSyncService priceLabsSyncService;
    }
}
