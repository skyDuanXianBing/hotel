package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.CreateStoreRequest;
import server.demo.entity.Store;
import server.demo.entity.StorePolicy;
import server.demo.entity.StoreUser;
import server.demo.repository.StoreRepository;
import server.demo.repository.StorePolicyRepository;
import server.demo.repository.StoreUserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceUpdateStoreSyncTest {

    @Test
    void updateStore_shouldSyncStoreToSuAfterSave() {
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StorePolicyRepository storePolicyRepository = Mockito.mock(StorePolicyRepository.class);
        SuPropertyService suPropertyService = Mockito.mock(SuPropertyService.class);
        SuImageSyncService suImageSyncService = Mockito.mock(SuImageSyncService.class);
        PriceLabsSyncService priceLabsSyncService = Mockito.mock(PriceLabsSyncService.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "storePolicyRepository", storePolicyRepository);
        ReflectionTestUtils.setField(service, "suPropertyService", suPropertyService);
        ReflectionTestUtils.setField(service, "suImageSyncService", suImageSyncService);
        ReflectionTestUtils.setField(service, "priceLabsSyncService", priceLabsSyncService);

        Store store = new Store();
        store.setId(9L);
        store.setName("Old");
        store.setCountry("JP");
        store.setType("hotel");

        StoreUser operator = new StoreUser();
        operator.setRole("owner");
        operator.setStore(store);

        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("New Name");
        request.setPhone("123");
        request.setType("hotel");
        request.setTimezone("Asia/Tokyo");
        request.setManager("Tester");
        request.setCountry("JP");
        request.setCity("Tokyo");
        request.setState("Tokyo");
        request.setAddress("Address");
        request.setDescription("Desc");
        request.setEmail("hotel@example.com");
        request.setPhoneTechType("5");
        request.setLanguage("ja");
        request.setCheckinTime("15:00");
        request.setCheckoutTime("11:00");

        when(storeUserRepository.findByStoreIdAndUserId(9L, 1L)).thenReturn(Optional.of(operator));
        when(storeRepository.save(store)).thenReturn(store);
        when(storePolicyRepository.findByStoreId(9L)).thenReturn(Optional.empty());
        when(storePolicyRepository.save(Mockito.any(StorePolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(suPropertyService.updateStoreProperty(9L)).thenReturn(
                new SuPropertyService.UpsertResult(true, true, "HOTEL1234567890", "ok")
        );

        service.updateStore(9L, 1L, request);

        assertEquals("New Name", store.getName());
        assertEquals("5", store.getPhoneTechType());
        verify(storeRepository).save(store);
        verify(storePolicyRepository).save(Mockito.any(StorePolicy.class));
        verify(priceLabsSyncService).syncListingsUsingStoreAddressAsync(9L);
        verify(suPropertyService).updateStoreProperty(9L);
        verify(suImageSyncService).syncStoreImagesStrict(9L);
    }
}
