package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.StoreUser;
import server.demo.repository.StoreUserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceSoftDeleteTest {

    @Test
    void softDeleteStore_shouldDeactivateAllStoreUsers_whenOperatorIsOwner() {
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);

        StoreUser operator = new StoreUser();
        operator.setRole("owner");
        operator.setIsActive(true);

        StoreUser member = new StoreUser();
        member.setRole("member");
        member.setIsActive(true);

        when(storeUserRepository.findByStoreIdAndUserId(7L, 1L)).thenReturn(Optional.of(operator));
        when(storeUserRepository.findByStoreId(7L)).thenReturn(List.of(operator, member));

        service.softDeleteStore(7L, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StoreUser>> captor = (ArgumentCaptor<List<StoreUser>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(List.class);
        verify(storeUserRepository).saveAll(captor.capture());

        List<StoreUser> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertFalse(saved.get(0).getIsActive());
        assertFalse(saved.get(1).getIsActive());
    }

    @Test
    void softDeleteStore_shouldRejectNonOwner() {
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);

        StoreUser operator = new StoreUser();
        operator.setRole("admin");

        when(storeUserRepository.findByStoreIdAndUserId(7L, 2L)).thenReturn(Optional.of(operator));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.softDeleteStore(7L, 2L));
        assertEquals("Only owner can delete store", ex.getMessage());
    }

    @Test
    void softDeleteStore_shouldRejectWhenNoMembership() {
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);

        StoreService service = new StoreService();
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);

        when(storeUserRepository.findByStoreIdAndUserId(7L, 3L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.softDeleteStore(7L, 3L));
        assertEquals("No permission", ex.getMessage());
    }
}

