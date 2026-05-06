package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.auth.LoginResponse;
import server.demo.dto.auth.UserDTO;
import server.demo.entity.Cleaner;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.entity.User;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.CleanerRepository;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;
import server.demo.util.JwtUtil;
import server.demo.util.RedisUtil;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class AuthServicePmsLoginAccessTest {

    @Test
    void ensurePmsLoginAllowed_shouldRejectCleanerOnlyAccount() {
        AuthService service = buildService();
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StoreUserPermissionRepository permissionRepository =
                Mockito.mock(StoreUserPermissionRepository.class);

        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "storeUserPermissionRepository", permissionRepository);

        Store store = new Store();
        store.setId(7L);

        StoreUser storeUser = new StoreUser();
        storeUser.setId(17L);
        storeUser.setStore(store);
        storeUser.setRole("member");
        storeUser.setIsActive(true);

        StoreUserPermission taskListPermission = new StoreUserPermission();
        taskListPermission.setModule(PermissionModule.ACCOMMODATION);
        taskListPermission.setAction(PermissionAction.TASK_LIST);

        Cleaner cleaner = new Cleaner();
        cleaner.setId(6L);
        cleaner.setStoreId(7L);
        cleaner.setUserId(6L);
        cleaner.setIsActive(true);

        when(storeUserRepository.findByUserIdWithStoreAndRoles(6L)).thenReturn(List.of(storeUser));
        when(permissionRepository.findByStoreUser_Id(17L)).thenReturn(List.of(taskListPermission));
        when(cleanerRepository.findByUserId(6L)).thenReturn(List.of(cleaner));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.ensurePmsLoginAllowed(buildLoginResponse(6L, "cleaner@example.com"))
        );

        assertEquals("请前往保洁员登录入口", exception.getMessage());
    }

    @Test
    void ensurePmsLoginAllowed_shouldAllowOwnerWhoIsAlsoCleaner() {
        AuthService service = buildService();
        CleanerRepository cleanerRepository = Mockito.mock(CleanerRepository.class);
        StoreUserRepository storeUserRepository = Mockito.mock(StoreUserRepository.class);
        StoreUserPermissionRepository permissionRepository =
                Mockito.mock(StoreUserPermissionRepository.class);

        ReflectionTestUtils.setField(service, "cleanerRepository", cleanerRepository);
        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "storeUserPermissionRepository", permissionRepository);

        Store store = new Store();
        store.setId(7L);

        StoreUser storeUser = new StoreUser();
        storeUser.setId(18L);
        storeUser.setStore(store);
        storeUser.setRole("owner");
        storeUser.setIsActive(true);

        Cleaner cleaner = new Cleaner();
        cleaner.setId(2L);
        cleaner.setStoreId(7L);
        cleaner.setUserId(2L);
        cleaner.setIsActive(true);

        when(storeUserRepository.findByUserIdWithStoreAndRoles(2L)).thenReturn(List.of(storeUser));
        when(permissionRepository.findByStoreUser_Id(18L)).thenReturn(Collections.emptyList());
        when(cleanerRepository.findByUserId(2L)).thenReturn(List.of(cleaner));

        assertDoesNotThrow(() -> service.ensurePmsLoginAllowed(buildLoginResponse(2L, "owner@example.com")));
    }

    private AuthService buildService() {
        AuthService service = new AuthService();
        ReflectionTestUtils.setField(service, "userRepository", Mockito.mock(UserRepository.class));
        ReflectionTestUtils.setField(service, "jwtUtil", Mockito.mock(JwtUtil.class));
        ReflectionTestUtils.setField(service, "redisUtil", Mockito.mock(RedisUtil.class));
        ReflectionTestUtils.setField(service, "emailService", Mockito.mock(EmailService.class));
        ReflectionTestUtils.setField(service, "storeService", Mockito.mock(StoreService.class));
        return service;
    }

    private LoginResponse buildLoginResponse(Long userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setUsername("user" + userId);
        user.setPassword("encoded");

        return new LoginResponse("token", new UserDTO(user));
    }
}
