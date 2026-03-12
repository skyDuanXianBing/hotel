package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.repository.StoreRepository;
import server.demo.repository.StoreUserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceTransferOwnerTest {

    @Test
    void transferStoreOwner_shouldSwitchOwnerAndActivateNewOwner() {
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);

        Store store = new Store();
        store.setId(9L);
        store.setOwnerEmail("old-owner@example.com");

        StoreUser currentOwner = new StoreUser();
        currentOwner.setRole("owner");
        currentOwner.setStore(store);
        currentOwner.setIsActive(true);

        User newOwnerUser = new User();
        newOwnerUser.setId(22L);
        newOwnerUser.setEmail("new-owner@example.com");

        StoreUser target = new StoreUser();
        target.setRole("member");
        target.setStore(store);
        target.setUser(newOwnerUser);
        target.setIsActive(false);

        when(storeUserRepository.findByStoreIdAndUserId(9L, 1L)).thenReturn(Optional.of(currentOwner));
        when(storeUserRepository.findByStoreIdAndUserId(9L, 22L)).thenReturn(Optional.of(target));

        service.transferStoreOwner(9L, 1L, 22L);

        assertEquals("admin", currentOwner.getRole());
        assertEquals("owner", target.getRole());
        assertEquals(Boolean.TRUE, target.getIsActive());
        assertEquals("new-owner@example.com", store.getOwnerEmail());
        verify(storeUserRepository).save(currentOwner);
        verify(storeUserRepository).save(target);
        verify(storeRepository).save(store);
    }

    @Test
    void transferStoreOwner_shouldRejectNonOwnerOperator() {
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);

        StoreUser operator = new StoreUser();
        operator.setRole("admin");

        when(storeUserRepository.findByStoreIdAndUserId(9L, 2L)).thenReturn(Optional.of(operator));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.transferStoreOwner(9L, 2L, 22L)
        );

        assertEquals("Only owner can transfer store owner", ex.getMessage());
    }

    @Test
    void transferStoreOwner_shouldRejectCurrentOwnerAsTarget() {
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);

        StoreUser operator = new StoreUser();
        operator.setRole("owner");

        when(storeUserRepository.findByStoreIdAndUserId(9L, 3L)).thenReturn(Optional.of(operator));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.transferStoreOwner(9L, 3L, 3L)
        );

        assertEquals("New owner cannot be current owner", ex.getMessage());
    }
}
