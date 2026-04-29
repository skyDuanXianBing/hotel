package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.Cleaner;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.entity.User;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.CleanerRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleanerIdentityServiceTest {

    @Test
    void createCleanerUserAccount_shouldCreateUserStoreUserAndTaskPermission() {
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StoreUserPermissionRepository storeUserPermissionRepository = Mockito.mock(StoreUserPermissionRepository.class);

        CleanerIdentityService service = new CleanerIdentityService(
                cleanerRepository,
                userRepository,
                storeRepository,
                storeUserRepository,
                storeUserPermissionRepository
        );

        Store store = new Store();
        store.setId(26L);
        store.setUserId(7L);

        when(userRepository.findByEmail("cleaner@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("cleaner")).thenReturn(false);
        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(88L);
            return user;
        });
        when(storeUserRepository.findByStoreIdAndUserId(26L, 88L)).thenReturn(Optional.empty());
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> {
            StoreUser storeUser = invocation.getArgument(0);
            storeUser.setId(108L);
            return storeUser;
        });
        when(storeUserPermissionRepository.findByStoreUser_Id(108L)).thenReturn(List.of());

        User user = service.createCleanerUserAccount(
                "cleaner@example.com",
                "Cleaner One",
                "encoded-password",
                26L,
                7L
        );

        assertEquals(88L, user.getId());
        assertEquals("cleaner@example.com", user.getEmail());
        assertEquals("Cleaner One", user.getName());
        assertEquals("Cleaner One", user.getNickname());

        ArgumentCaptor<StoreUser> storeUserCaptor = ArgumentCaptor.forClass(StoreUser.class);
        verify(storeUserRepository).save(storeUserCaptor.capture());
        assertEquals("member", storeUserCaptor.getValue().getRole());
        assertEquals(7L, storeUserCaptor.getValue().getInvitedBy());

        ArgumentCaptor<StoreUserPermission> permissionCaptor = ArgumentCaptor.forClass(StoreUserPermission.class);
        verify(storeUserPermissionRepository).save(permissionCaptor.capture());
        assertEquals(PermissionModule.ACCOMMODATION, permissionCaptor.getValue().getModule());
        assertEquals(PermissionAction.TASK_LIST, permissionCaptor.getValue().getAction());
    }

    @Test
    void createOrReuseCleanerUserAccount_shouldReuseExistingUserAndReactivateStoreMembership() {
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StoreUserPermissionRepository storeUserPermissionRepository = Mockito.mock(StoreUserPermissionRepository.class);

        CleanerIdentityService service = new CleanerIdentityService(
                cleanerRepository,
                userRepository,
                storeRepository,
                storeUserRepository,
                storeUserPermissionRepository
        );

        Store store = new Store();
        store.setId(26L);
        store.setUserId(7L);

        User existingUser = new User();
        existingUser.setId(88L);
        existingUser.setUsername("cleaner");
        existingUser.setEmail("cleaner@example.com");
        existingUser.setIsActive(false);

        StoreUser existingStoreUser = new StoreUser();
        existingStoreUser.setId(108L);
        existingStoreUser.setStore(store);
        existingStoreUser.setUser(existingUser);
        existingStoreUser.setRole("member");
        existingStoreUser.setIsActive(false);

        when(userRepository.findByEmail("cleaner@example.com")).thenReturn(Optional.of(existingUser));
        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserRepository.findByStoreIdAndUserId(26L, 88L)).thenReturn(Optional.of(existingStoreUser));
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserPermissionRepository.findByStoreUser_Id(108L)).thenReturn(List.of());

        User reusedUser = service.createOrReuseCleanerUserAccount(
                "cleaner@example.com",
                "Cleaner Reused",
                "new-password",
                26L,
                7L
        );

        assertEquals(88L, reusedUser.getId());
        assertEquals("Cleaner Reused", reusedUser.getName());
        assertEquals("new-password", reusedUser.getPassword());
        assertTrue(Boolean.TRUE.equals(reusedUser.getIsActive()));
        assertTrue(Boolean.TRUE.equals(existingStoreUser.getIsActive()));
    }

    @Test
    void ensureCleanerIdentity_shouldRepairLegacyCleanerUserBinding() {
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StoreUserPermissionRepository storeUserPermissionRepository = Mockito.mock(StoreUserPermissionRepository.class);

        CleanerIdentityService service = new CleanerIdentityService(
                cleanerRepository,
                userRepository,
                storeRepository,
                storeUserRepository,
                storeUserPermissionRepository
        );

        Cleaner cleaner = new Cleaner();
        cleaner.setId(3L);
        cleaner.setUserId(1L);
        cleaner.setStoreId(26L);
        cleaner.setName("Cleaner Legacy");
        cleaner.setEmail("legacy.cleaner@example.com");
        cleaner.setPassword("encoded-password");
        cleaner.setIsActive(true);

        User wrongLinkedUser = new User();
        wrongLinkedUser.setId(1L);
        wrongLinkedUser.setEmail("owner@example.com");

        Store store = new Store();
        store.setId(26L);
        store.setUserId(9L);

        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));
        when(userRepository.findById(1L)).thenReturn(Optional.of(wrongLinkedUser));
        when(userRepository.findByEmail("legacy.cleaner@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("legacy.cleaner")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(88L);
            }
            return user;
        });
        when(cleanerRepository.save(any(Cleaner.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserRepository.findByStoreIdAndUserId(26L, 88L)).thenReturn(Optional.empty());
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> {
            StoreUser storeUser = invocation.getArgument(0);
            storeUser.setId(208L);
            return storeUser;
        });
        when(storeUserPermissionRepository.findByStoreUser_Id(208L)).thenReturn(List.of());

        Cleaner repairedCleaner = service.ensureCleanerIdentity(cleaner);

        assertNotNull(repairedCleaner);
        assertEquals(88L, repairedCleaner.getUserId());

        ArgumentCaptor<Cleaner> cleanerCaptor = ArgumentCaptor.forClass(Cleaner.class);
        verify(cleanerRepository).save(cleanerCaptor.capture());
        assertEquals(88L, cleanerCaptor.getValue().getUserId());
    }
}
