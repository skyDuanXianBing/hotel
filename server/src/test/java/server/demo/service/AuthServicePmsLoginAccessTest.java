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
    void loginByPassword_shouldReturnCleanerTargetForActiveCleanerIdentityWithoutTaskListPermission() {
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
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(6L, 7L))
                .thenReturn(Optional.of(cleaner));

        LoginResponse response = fixture.service.loginByPassword(buildPasswordRequest("cleaner@example.com", "secret"));

        assertEquals(LoginTarget.CLEANER, response.getLoginTarget());
        assertEquals("token-123", response.getToken());
        assertEquals(7L, response.getTargetStoreId());
        assertEquals(List.of(LoginTarget.CLEANER), response.getAvailableLoginTargets());
        assertEquals(1, response.getCleanerContexts().size());
        assertEquals(7L, response.getCleanerContexts().get(0).getStoreId());
        assertEquals(storeDTO, response.getCurrentStore());
        assertNotNull(response.getCleaner());
        assertEquals(3L, response.getCleaner().getId());
        assertEquals(6L, response.getCleaner().getUserId());
        assertEquals(7L, response.getCleaner().getStoreId());
        Mockito.verifyNoInteractions(fixture.storeUserPermissionRepository, fixture.rolePermissionRepository);
    }

    @Test
    void loginByPassword_shouldReturnAllActiveCleanerContexts() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(6L, "multi.cleaner@example.com", "secret");
        Store firstStore = buildStore(7L); Store secondStore = buildStore(9L);
        StoreUser firstMembership = buildStoreUser(17L, firstStore, "member");
        StoreUser secondMembership = buildStoreUser(19L, secondStore, "member");
        Cleaner firstCleaner = buildCleaner(3L, 6L, 7L, true);
        Cleaner secondCleaner = buildCleaner(5L, 6L, 9L, true);

        when(fixture.userRepository.findByEmail("multi.cleaner@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(6L, "multi.cleaner@example.com")).thenReturn("token-multi");
        when(fixture.storeService.getUserStores(6L)).thenReturn(List.of(
                buildStoreDTO(7L, "First Store"), buildStoreDTO(9L, "Second Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(6L))
                .thenReturn(List.of(secondMembership, firstMembership));
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(6L, 7L)).thenReturn(Optional.of(firstCleaner));
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(6L, 9L)).thenReturn(Optional.of(secondCleaner));

        LoginResponse response = fixture.service.loginByPassword(
                buildPasswordRequest("multi.cleaner@example.com", "secret"));

        assertEquals(2, response.getCleanerContexts().size());
        assertEquals(7L, response.getCleanerContexts().get(0).getStoreId());
        assertEquals(9L, response.getCleanerContexts().get(1).getStoreId());
        assertEquals(7L, response.getTargetStoreId());
    }

    @Test
    void loginByPassword_shouldDefaultOwnerCleanerToPmsAndAllowCleanerSelection() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(50L, "owner.cleaner@example.com", "secret");
        Store store = buildStore(60L);
        StoreUser membership = buildStoreUser(70L, store, "owner");
        Cleaner cleaner = buildCleaner(80L, 50L, 60L, true);
        StoreDTO storeDTO = buildStoreDTO(60L, "Owner Store");
        when(fixture.userRepository.findByEmail("owner.cleaner@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(50L, "owner.cleaner@example.com")).thenReturn("token-owner-cleaner");
        when(fixture.storeService.getUserStores(50L)).thenReturn(List.of(storeDTO));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(50L)).thenReturn(List.of(membership));
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(50L, 60L))
                .thenReturn(Optional.of(cleaner));

        LoginByPasswordRequest defaultRequest = buildPasswordRequest("owner.cleaner@example.com", "secret");
        LoginResponse pmsResponse = fixture.service.loginByPassword(defaultRequest);
        assertEquals(LoginTarget.PMS, pmsResponse.getLoginTarget());
        assertEquals(List.of(LoginTarget.PMS, LoginTarget.CLEANER), pmsResponse.getAvailableLoginTargets());
        assertEquals(1, pmsResponse.getCleanerContexts().size());
        assertNull(pmsResponse.getCleaner());

        LoginByPasswordRequest cleanerRequest = buildPasswordRequest("owner.cleaner@example.com", "secret");
        cleanerRequest.setPreferredLoginTarget(LoginTarget.CLEANER);
        LoginResponse cleanerResponse = fixture.service.loginByPassword(cleanerRequest);
        assertEquals(LoginTarget.CLEANER, cleanerResponse.getLoginTarget());
        assertNotNull(cleanerResponse.getCleaner());
        assertEquals(60L, cleanerResponse.getTargetStoreId());
    }

    @Test
    void loginByPassword_shouldKeepPmsReachableForCleanerWithAnotherStaffStore() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(51L, "mixed@example.com", "secret");
        Store cleanerStore = buildStore(61L); Store staffStore = buildStore(62L);
        StoreUser cleanerMembership = buildStoreUser(71L, cleanerStore, "member");
        StoreUser staffMembership = buildStoreUser(72L, staffStore, "member");
        Cleaner cleaner = buildCleaner(81L, 51L, 61L, true);
        when(fixture.userRepository.findByEmail("mixed@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(51L, "mixed@example.com")).thenReturn("token-mixed");
        when(fixture.storeService.getUserStores(51L)).thenReturn(List.of(
                buildStoreDTO(61L, "Cleaner Store"), buildStoreDTO(62L, "Staff Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(51L))
                .thenReturn(List.of(cleanerMembership, staffMembership));
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(51L, 61L))
                .thenReturn(Optional.of(cleaner));
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(51L, 62L))
                .thenReturn(Optional.empty());

        LoginResponse response = fixture.service.loginByPassword(
                buildPasswordRequest("mixed@example.com", "secret"));

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertEquals(List.of(LoginTarget.PMS, LoginTarget.CLEANER), response.getAvailableLoginTargets());
        assertEquals(1, response.getCleanerContexts().size());
    }

    @Test
    void loginByPassword_shouldRejectDisabledUserBeforeResolvingWorkspaces() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(52L, "disabled@example.com", "secret"); user.setIsActive(false);
        when(fixture.userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> fixture.service.loginByPassword(
                buildPasswordRequest("disabled@example.com", "secret")));

        assertEquals("用户账号已停用，无法登录", exception.getMessage());
        Mockito.verifyNoInteractions(fixture.storeUserRepository, fixture.cleanerIdentityService);
    }

    @Test
    void loginByPassword_shouldIgnoreInactiveCleanerMembership() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(53L, "inactive.membership@example.com", "secret");
        Store store = buildStore(63L); StoreUser membership = buildStoreUser(73L, store, "member");
        membership.setIsActive(false);
        when(fixture.userRepository.findByEmail("inactive.membership@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(53L, "inactive.membership@example.com")).thenReturn("token-inactive");
        when(fixture.storeService.getUserStores(53L)).thenReturn(List.of());
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(53L)).thenReturn(List.of(membership));

        LoginResponse response = fixture.service.loginByPassword(
                buildPasswordRequest("inactive.membership@example.com", "secret"));

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertEquals(List.of(LoginTarget.PMS), response.getAvailableLoginTargets());
        assertEquals(List.of(), response.getCleanerContexts());
        Mockito.verifyNoInteractions(fixture.cleanerIdentityService);
    }

    @Test
    void loginByPassword_shouldKeepRoleTaskListPermissionWithoutCleanerInPms() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(8L, "role.manager@example.com", "secret");
        Store store = buildStore(10L);
        StoreUser storeUser = buildStoreUser(18L, store, "member");
        Role role = new Role();
        role.setId(30L);
        storeUser.getRoles().add(role);

        when(fixture.userRepository.findByEmail("role.manager@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(8L, "role.manager@example.com")).thenReturn("token-role");
        when(fixture.storeService.getUserStores(8L)).thenReturn(List.of(buildStoreDTO(10L, "Role Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(8L)).thenReturn(List.of(storeUser));
        when(fixture.rolePermissionRepository.existsByRoleIdInAndModuleAndAction(
                List.of(30L),
                PermissionModule.ACCOMMODATION,
                PermissionAction.TASK_LIST
        )).thenReturn(true);
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(8L, 10L))
                .thenReturn(Optional.empty());

        LoginResponse response = fixture.service.loginByPassword(
                buildPasswordRequest("role.manager@example.com", "secret")
        );

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertNull(response.getTargetStoreId());
        assertNull(response.getCleaner());
    }

    @Test
    void loginByPassword_shouldKeepOwnerWithoutCleanerIdentityInPms() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(2L, "owner@example.com", "secret");
        Store store = buildStore(7L);
        StoreUser storeUser = buildStoreUser(18L, store, "owner");

        when(fixture.userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(2L, "owner@example.com")).thenReturn("token-owner");
        when(fixture.storeService.getUserStores(2L)).thenReturn(List.of(buildStoreDTO(7L, "Owner Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(2L)).thenReturn(List.of(storeUser));
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(2L, 7L))
                .thenReturn(Optional.empty());

        LoginResponse response = fixture.service.loginByPassword(buildPasswordRequest("owner@example.com", "secret"));

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertNull(response.getCleaner());
        assertNull(response.getTargetStoreId());
    }

    @Test
    void loginByPassword_shouldKeepInactiveCleanerIdentityInPms() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(11L, "plain.member@example.com", "secret");
        Store store = buildStore(12L);
        StoreUser storeUser = buildStoreUser(31L, store, "member");
        Cleaner cleaner = buildCleaner(9L, 11L, 12L, false);

        when(fixture.userRepository.findByEmail("plain.member@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(11L, "plain.member@example.com")).thenReturn("token-member");
        when(fixture.storeService.getUserStores(11L)).thenReturn(List.of(buildStoreDTO(12L, "Member Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(11L)).thenReturn(List.of(storeUser));
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(11L, 12L))
                .thenReturn(Optional.of(cleaner));

        LoginResponse response = fixture.service.loginByPassword(
                buildPasswordRequest("plain.member@example.com", "secret")
        );

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertNull(response.getCleaner());
        assertNull(response.getTargetStoreId());
    }

    @Test
    void loginByPassword_shouldKeepDirectTaskListPermissionWithoutCleanerInPms() {
        ServiceFixture fixture = buildServiceFixture();
        User user = buildUser(20L, "task.manager@example.com", "secret");
        Store store = buildStore(21L);
        StoreUser storeUser = buildStoreUser(22L, store, "member");

        when(fixture.userRepository.findByEmail("task.manager@example.com")).thenReturn(Optional.of(user));
        when(fixture.jwtUtil.generateToken(20L, "task.manager@example.com")).thenReturn("token-task");
        when(fixture.storeService.getUserStores(20L)).thenReturn(List.of(buildStoreDTO(21L, "Broken Store")));
        when(fixture.storeUserRepository.findByUserIdWithStoreAndRoles(20L)).thenReturn(List.of(storeUser));
        when(fixture.storeUserPermissionRepository.existsByStoreUser_IdAndModuleAndAction(
                22L,
                PermissionModule.ACCOMMODATION,
                PermissionAction.TASK_LIST
        )).thenReturn(true);
        when(fixture.cleanerIdentityService.findCleanerByUserIdAndStoreId(20L, 21L))
                .thenReturn(Optional.empty());

        LoginResponse response = fixture.service.loginByPassword(
                buildPasswordRequest("task.manager@example.com", "secret")
        );

        assertEquals(LoginTarget.PMS, response.getLoginTarget());
        assertEquals("token-task", response.getToken());
        assertNull(response.getCleaner());
        assertNull(response.getTargetStoreId());
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
