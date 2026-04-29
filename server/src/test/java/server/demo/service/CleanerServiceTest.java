package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.entity.Cleaner;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.repository.CleanerRepository;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.StoreUserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleanerServiceTest {

    @Test
    void deleteCleaner_shouldRejectWhenCleanerStillHasTasks() {
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        CleaningTaskRepository cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);

        CleanerService service = new CleanerService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);

        Cleaner cleaner = new Cleaner();
        cleaner.setId(7L);
        cleaner.setStoreId(11L);
        cleaner.setUserId(7L);
        cleaner.setIsActive(true);

        when(cleanerRepository.findById(7L)).thenReturn(Optional.of(cleaner));
        when(cleaningTaskRepository.existsByCleanerId(7L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.deleteCleaner(11L, 7L));
        assertEquals("该保洁员仍有关联任务，请先处理任务后再删除", exception.getMessage());
        verify(cleanerRepository, never()).save(any(Cleaner.class));
    }

    @Test
    void deleteCleaner_shouldDisableCleanerAndCurrentStoreMembership() {
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        CleaningTaskRepository cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);

        CleanerService service = new CleanerService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);

        Cleaner cleaner = new Cleaner();
        cleaner.setId(7L);
        cleaner.setStoreId(11L);
        cleaner.setUserId(7L);
        cleaner.setIsActive(true);

        Store store = new Store();
        store.setId(11L);

        User user = new User();
        user.setId(7L);

        StoreUser storeUser = new StoreUser();
        storeUser.setId(16L);
        storeUser.setStore(store);
        storeUser.setUser(user);
        storeUser.setIsActive(true);

        when(cleanerRepository.findById(7L)).thenReturn(Optional.of(cleaner));
        when(cleaningTaskRepository.existsByCleanerId(7L)).thenReturn(false);
        when(cleanerRepository.save(any(Cleaner.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserRepository.findByStoreIdAndUserId(11L, 7L)).thenReturn(Optional.of(storeUser));
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteCleaner(11L, 7L);

        verify(cleanerRepository).save(cleaner);
        verify(storeUserRepository).save(storeUser);
        assertEquals(Boolean.FALSE, cleaner.getIsActive());
        assertEquals(Boolean.FALSE, storeUser.getIsActive());
    }
}
