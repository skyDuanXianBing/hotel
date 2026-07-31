package server.demo.service.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import server.demo.dto.admin.AdminDtos.ChangePasswordRequest;
import server.demo.dto.admin.AdminDtos.LoginRequest;
import server.demo.dto.admin.AdminDtos.LoginResponse;
import server.demo.entity.admin.AdminUser;
import server.demo.enums.AdminRole;
import server.demo.repository.admin.AdminUserRepository;
import server.demo.util.JwtUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

/**
 * 管理端登录：成功签发 admin token / 密码错误 / 禁用账号 / 用户不存在。
 */
class AdminAuthServiceTest {

    private AdminUserRepository adminUserRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AdminAuthService service;

    @BeforeEach
    void setUp() {
        adminUserRepository = Mockito.mock(AdminUserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        service = new AdminAuthService(adminUserRepository, passwordEncoder, jwtUtil);
    }

    private AdminUser adminUser(boolean active) {
        AdminUser user = new AdminUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$10$hashed");
        user.setRole(AdminRole.SUPER);
        user.setActive(active);
        return user;
    }

    @Test
    void login_shouldIssueAdminTokenWhenCredentialsValid() {
        Mockito.when(adminUserRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser(true)));
        Mockito.when(passwordEncoder.matches("Admin@123456", "$2a$10$hashed")).thenReturn(true);
        Mockito.when(jwtUtil.generateToken(anyMap(), eq("admin"))).thenReturn("signed-admin-token");

        LoginResponse response = service.login(new LoginRequest("admin", "Admin@123456"));

        assertEquals("signed-admin-token", response.token());
        assertEquals("admin", response.username());
        assertEquals("SUPER", response.role());
    }

    @Test
    void login_shouldRejectWrongPassword() {
        Mockito.when(adminUserRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser(true)));
        Mockito.when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.login(new LoginRequest("admin", "wrong")));
        assertTrue(error.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void login_shouldRejectDisabledAccount() {
        Mockito.when(adminUserRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser(false)));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.login(new LoginRequest("admin", "Admin@123456")));
        assertTrue(error.getMessage().contains("禁用"));
    }

    @Test
    void login_shouldRejectUnknownUsername() {
        Mockito.when(adminUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.login(new LoginRequest("ghost", "whatever")));
    }

    // ---- changePassword：生产交付硬需求（初始口令必须可改） ----

    @Test
    void changePassword_shouldUpdateHashWhenOldPasswordMatches() {
        AdminUser user = adminUser(true);
        Mockito.when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("old-pass", "$2a$10$hashed")).thenReturn(true);
        Mockito.when(passwordEncoder.encode("new-pass-123")).thenReturn("$2a$10$newhash");

        service.changePassword("admin", new ChangePasswordRequest("old-pass", "new-pass-123"));

        assertEquals("$2a$10$newhash", user.getPassword());
        Mockito.verify(adminUserRepository).save(user);
    }

    @Test
    void changePassword_shouldRejectWrongOldPassword() {
        Mockito.when(adminUserRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser(true)));
        Mockito.when(passwordEncoder.matches("bad", "$2a$10$hashed")).thenReturn(false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("admin", new ChangePasswordRequest("bad", "new-pass-123")));
        assertTrue(error.getMessage().contains("原密码"));
        Mockito.verify(adminUserRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void changePassword_shouldRejectDisabledAccount() {
        Mockito.when(adminUserRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser(false)));

        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("admin", new ChangePasswordRequest("old", "new-pass-123")));
        Mockito.verify(adminUserRepository, Mockito.never()).save(Mockito.any());
    }
}
