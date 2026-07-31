package server.demo.service.admin;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.admin.AdminDtos.ChangePasswordRequest;
import server.demo.dto.admin.AdminDtos.LoginRequest;
import server.demo.dto.admin.AdminDtos.LoginResponse;
import server.demo.entity.admin.AdminUser;
import server.demo.repository.admin.AdminUserRepository;
import server.demo.util.JwtUtil;

import java.util.HashMap;
import java.util.Map;

import server.demo.i18n.ApiMessages;
/**
 * 平台管理员认证：独立表 + BCrypt 校验 + 独立 JWT（claim 带 aud="admin"）。
 * 复制 CleanerAuthService 的独立认证模式，与门店用户体系完全隔离。
 */
@Service
public class AdminAuthService {

    /** 管理端 token 的受众标识，AdminAuthInterceptor 据此区分门店用户 token。 */
    public static final String TOKEN_AUDIENCE = "admin";

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AdminAuthService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.3ef442ce0947")));
        if (!user.isActive()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.049842aa4193"));
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.3ef442ce0947"));
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("aud", TOKEN_AUDIENCE);
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        String token = jwtUtil.generateToken(claims, user.getUsername());

        return new LoginResponse(token, user.getUsername(), user.getRole().name());
    }

    /**
     * 修改当前登录管理员密码。验证原密码后更新为 BCrypt 哈希。
     * 已签发的 token 不因改密失效（与门店用户体系一致，token 有效期 24h）。
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.37f36ca852b6")));
        if (!user.isActive()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.049842aa4193"));
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.aab8905e77bc"));
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        adminUserRepository.save(user);
    }
}
