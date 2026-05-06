package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.Cleaner;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.entity.User;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.CleanerRepository;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;

import java.util.Collections;
import java.util.List;
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
        StoreUserPermissionRepository storeUserPermissionRepository =
                Mockito.mock(StoreUserPermissionRepository.class);

        CleanerService service = new CleanerService();
        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(
                service,
                "storeUserPermissionRepository",
                storeUserPermissionRepository
        );

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
        verify(storeUserRepository, never()).save(any(StoreUser.class));
    }

    @Test
    void deleteCleaner_shouldDisableCleanerOnlyStoreMembership() {
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        CleaningTaskRepository cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StoreUserPermissionRepository storeUserPermissionRepository =
                Mockito.mock(StoreUserPermissionRepository.class);

        CleanerService service = new CleanerService();
        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(
                service,
                "storeUserPermissionRepository",
                storeUserPermissionRepository
        );

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
        storeUser.setRole("member");
        storeUser.setIsActive(true);

        StoreUserPermission taskListPermission = new StoreUserPermission();
        taskListPermission.setModule(PermissionModule.ACCOMMODATION);
        taskListPermission.setAction(PermissionAction.TASK_LIST);

        when(cleanerRepository.findById(7L)).thenReturn(Optional.of(cleaner));
        when(cleaningTaskRepository.existsByCleanerId(7L)).thenReturn(false);
        when(cleanerRepository.save(any(Cleaner.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserRepository.findByStoreIdAndUserId(11L, 7L)).thenReturn(Optional.of(storeUser));
        when(storeUserPermissionRepository.findByStoreUser_Id(16L))
                .thenReturn(List.of(taskListPermission));
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteCleaner(11L, 7L);

        verify(cleanerRepository).save(cleaner);
        verify(storeUserRepository).save(storeUser);
        assertEquals(Boolean.FALSE, cleaner.getIsActive());
        assertEquals(Boolean.FALSE, storeUser.getIsActive());
    }

    @Test
    void deleteCleaner_shouldKeepOwnerStoreMembershipActive() {
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        CleaningTaskRepository cleaningTaskRepository = Mockito.mock(CleaningTaskRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StoreUserPermissionRepository storeUserPermissionRepository =
                Mockito.mock(StoreUserPermissionRepository.class);

        CleanerService service = new CleanerService();
        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "cleaningTaskRepository", cleaningTaskRepository);
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(
                service,
                "storeUserPermissionRepository",
                storeUserPermissionRepository
        );

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
        storeUser.setRole("owner");
        storeUser.setIsActive(true);

        when(cleanerRepository.findById(7L)).thenReturn(Optional.of(cleaner));
        when(cleaningTaskRepository.existsByCleanerId(7L)).thenReturn(false);
        when(cleanerRepository.save(any(Cleaner.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserRepository.findByStoreIdAndUserId(11L, 7L)).thenReturn(Optional.of(storeUser));
        when(storeUserPermissionRepository.findByStoreUser_Id(16L)).thenReturn(Collections.emptyList());

        service.deleteCleaner(11L, 7L);

        verify(cleanerRepository).save(cleaner);
        verify(storeUserRepository, never()).save(any(StoreUser.class));
        assertEquals(Boolean.FALSE, cleaner.getIsActive());
        assertEquals(Boolean.TRUE, storeUser.getIsActive());
    }
}
