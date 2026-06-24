package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.StoreDTO;
import server.demo.dto.auth.LoginByCodeRequest;
import server.demo.dto.auth.LoginByPasswordRequest;
import server.demo.dto.auth.LoginResponse;
import server.demo.dto.auth.LoginTarget;
import server.demo.entity.Cleaner;
import server.demo.entity.Role;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.RolePermissionRepository;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;
import server.demo.util.JwtUtil;
import server.demo.util.RedisUtil;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class AuthServicePmsLoginAccessTest {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Test
    void loginByPassword_shouldReturnCleanerTargetForDirectTaskListPermission() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(6L, "cleaner@example.com", "secret");
        Store store = buildStore(7L);
        StoreUser storeUser = buildStoreUser(17L, store, "member");
        Cleaner cleaner = buildCleaner(3L, 6L, 7L, true);
        StoreDTO storeDTO = buildStoreDTO(7L, "Target Store");

        when(fixture.userRepository.findByEmail("cleaner@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(6L, "cleaner@example.com")).thenReturn("token-123");
        when(fixture.storeService.getUserStores(6L)).thenReturn(List.of(storeDTO));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(6L)).thenReturn(List.of(storeUser));
        when(fixture.storeUserPermissionRepository.existsByStoreUser_IdAndModuleAndAction(
                17L,
                PermissionModule.ACCOMMODATION,
                PermissionAction.TASK_LIST
        )).thenReturn(true);
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(6L, 7L))
                .thenReturn(Optional.of(cleaner));

        LoginResponse response = fixture.service.loginByPassword(buildPasswordRequest("cleaner@example.com", "secret"));

        assertEquals(LoginTarget.CLEANER, response.getLoginTarget());
        assertEquals("token-123", response.getToken());
        assertEquals(7L, response.getTargetStoreId());
        assertEquals(storeDTO, response.getCurrentStore());
        assertNotNull(response.getCleaner());
        assertEquals(3L, response.getCleaner().getId());
        assertEquals(6L, response.getCleaner().getUserId());
        assertEquals(7L, response.getCleaner().getStoreId());
    }

    @Test
    void loginByPassword_shouldReturnCleanerTargetForRoleTaskListPermission() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(8L, "role.cleaner@example.com", "secret");
        Store store = buildStore(10L);
        StoreUser storeUser = buildStoreUser(18L, store, "member");
        Role role = new Role();
        role.setId(30L);
        storeUser.getRoles().add(role);
        Cleaner cleaner = buildCleaner(4L, 8L, 10L, true);

        when(fixture.userRepository.findByEmail("role.cleaner@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(8L, "role.cleaner@example.com")).thenReturn("token-role");
        when(fixture.storeService.getUserStores(8L)).thenReturn(List.of(buildStoreDTO(10L, "Role Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(8L)).thenReturn(List.of(storeUser));
        when(fixture.rolePermissionRepository.existsByRoleIdInAndModuleAndAction(
                List.of(30L),
                PermissionModule.ACCOMMODATION,
                PermissionAction.TASK_LIST
        )).thenReturn(true);
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(8L, 10L))
                .thenReturn(Optional.of(cleaner));

        LoginResponse response = fixture.service.loginByPassword(
                buildPasswordRequest("role.cleaner@example.com", "secret")
        );

        assertEquals(LoginTarget.CLEANER, response.getLoginTarget());
        assertEquals(10L, response.getTargetStoreId());
        assertEquals(4L, response.getCleaner().getId());
    }

    @Test
    void loginByPassword_shouldKeepOwnerWithoutExplicitTaskListInPms() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(2L, "owner@example.com", "secret");
        Store store = buildStore(7L);
        StoreUser storeUser = buildStoreUser(18L, store, "owner");

        when(fixture.userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(2L, "owner@example.com")).thenReturn("token-owner");
        when(fixture.storeService.getUserStores(2L)).thenReturn(List.of(buildStoreDTO(7L, "Owner Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(2L)).thenReturn(List.of(storeUser));

        LoginResponse response = fixture.service.loginByPassword(buildPasswordRequest("owner@example.com", "secret"));

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertNull(response.getCleaner());
        assertNull(response.getTargetStoreId());
        Mockito.verifyNoInteractions(fixture.cleanerIdentityService);
    }

    @Test
    void loginByPassword_shouldKeepActiveCleanerWithoutExplicitTaskListInPms() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(11L, "plain.member@example.com", "secret");
        Store store = buildStore(12L);
        StoreUser storeUser = buildStoreUser(31L, store, "member");

        when(fixture.userRepository.findByEmail("plain.member@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(11L, "plain.member@example.com")).thenReturn("token-member");
        when(fixture.storeService.getUserStores(11L)).thenReturn(List.of(buildStoreDTO(12L, "Member Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(11L)).thenReturn(List.of(storeUser));

        LoginResponse response = fixture.service.loginByPassword(
                buildPasswordRequest("plain.member@example.com", "secret")
        );

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertNull(response.getCleaner());
        Mockito.verifyNoInteractions(fixture.cleanerIdentityService);
    }

    @Test
    void loginByPassword_shouldFailWhenTaskListPermissionHasNoActiveCleaner() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(20L, "broken.cleaner@example.com", "secret");
        Store store = buildStore(21L);
        StoreUser storeUser = buildStoreUser(22L, store, "member");

        when(fixture.userRepository.findByEmail("broken.cleaner@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(20L, "broken.cleaner@example.com")).thenReturn("token-broken");
        when(fixture.storeService.getUserStores(20L)).thenReturn(List.of(buildStoreDTO(21L, "Broken Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(20L)).thenReturn(List.of(storeUser));
        when(fixture.storeUserPermissionRepository.existsByStoreUser_IdAndModuleAndAction(
                22L,
                PermissionModule.ACCOMMODATION,
                PermissionAction.TASK_LIST
        )).thenReturn(true);
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(20L, 21L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> fixture.service.loginByPassword(buildPasswordRequest("broken.cleaner@example.com", "secret"))
        );

        assertEquals(
                "账号已配置查看保洁任务权限，但未找到对应的有效保洁员档案，请联系管理员检查保洁员配置",
                exception.getMessage()
        );
    }

    @Test
    void loginByCode_shouldPopulatePmsLoginTarget() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(40L, "code.user@example.com", "secret");

        when(fixture.redisUtil.verifyCode("code.user@example.com", "123456", "login")).thenReturn(true);
        when(fixture.userRepository.findByEmail("code.user@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(40L, "code.user@example.com")).thenReturn("token-code");
        when(fixture.storeService.getUserStores(40L)).thenReturn(List.of());
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(40L)).thenReturn(List.of());

        LoginResponse response = fixture.service.loginByCode(buildCodeRequest("code.user@example.com", "123456"));

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertEquals("token-code", response.getToken());
        assertNull(response.getCleaner());
    }

    private ServiceFixture buildServiceFixture() {
        ServiceFixture fixture = new ServiceFixture();
        fixture.service = new AuthService();
        fixture.userRepository = Mockito.mock(UserRepository.class);
        fixture.jwtUtil = Mockito.mock(JwtUtil.class);
        fixture.redisUtil = Mockito.mock(RedisUtil.class);
        fixture.emailService = Mockito.mock(EmailService.class);
        fixture.storeService = Mockito.mock(StoreService.class);
        fixture.storeUserRepository = Mockito.mock(StoreUserRepository.class);
        fixture.storeUserPermissionRepository = Mockito.mock(StoreUserPermissionRepository.class);
        fixture.rolePermissionRepository = Mockito.mock(RolePermissionRepository.class);
        fixture.cleanerIdentityService = Mockito.mock(CleanerIdentityService.class);

        ReflectionTestUtils.setField(fixture.service, "userRepository", fixture.userRepository);
        ReflectionTestUtils.setField(fixture.service, "jwtUtil", fixture.jwtUtil);
        ReflectionTestUtils.setField(fixture.service, "redisUtil", fixture.redisUtil);
        ReflectionTestUtils.setField(fixture.service, "emailService", fixture.emailService);
        ReflectionTestUtils.setField(fixture.service, "storeService", fixture.storeService);
        ReflectionTestUtils.setField(fixture.service, "storeUserRepository", fixture.storeUserRepository);
        ReflectionTestUtils.setField(
                fixture.service,
                "storeUserPermissionRepository",
                fixture.storeUserPermissionRepository
        );
        ReflectionTestUtils.setField(fixture.service, "rolePermissionRepository", fixture.rolePermissionRepository);
        ReflectionTestUtils.setField(fixture.service, "cleanerIdentityService", fixture.cleanerIdentityService);
        return fixture;
    }

    private LoginByPasswordRequest buildPasswordRequest(String email, String password) {
        LoginByPasswordRequest request = new LoginByPasswordRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginByCodeRequest buildCodeRequest(String email, String code) {
        LoginByCodeRequest request = new LoginByCodeRequest();
        request.setEmail(email);
        request.setVerificationCode(code);
        return request;
    }

    private User buildUser(Long id, String email, String password) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername("user" + id);
        user.setPassword(PASSWORD_ENCODER.encode(password));
        return user;
    }

    private Store buildStore(Long id) {
        Store store = new Store();
        store.setId(id);
        return store;
    }

    private StoreUser buildStoreUser(Long id, Store store, String role) {
        StoreUser storeUser = new StoreUser();
        storeUser.setId(id);
        storeUser.setStore(store);
        storeUser.setRole(role);
        storeUser.setIsActive(true);
        return storeUser;
    }

    private StoreDTO buildStoreDTO(Long id, String name) {
        StoreDTO storeDTO = new StoreDTO();
        storeDTO.setId(id);
        storeDTO.setName(name);
        return storeDTO;
    }

    private Cleaner buildCleaner(Long id, Long userId, Long storeId, boolean active) {
        Cleaner cleaner = new Cleaner();
        cleaner.setId(id);
        cleaner.setUserId(userId);
        cleaner.setStoreId(storeId);
        cleaner.setName("Cleaner " + id);
        cleaner.setEmail("cleaner" + id + "@example.com");
        cleaner.setIsActive(active);
        return cleaner;
    }

    private static class ServiceFixture {
        private AuthService service;
        private UserRepository userRepository;
        private JwtUtil jwtUtil;
        private RedisUtil redisUtil;
        private EmailService emailService;
        private StoreService storeService;
        private StoreUserRepository storeUserRepository;
        private StoreUserPermissionRepository storeUserPermissionRepository;
        private RolePermissionRepository rolePermissionRepository;
        private CleanerIdentityService cleanerIdentityService;
    }
}
