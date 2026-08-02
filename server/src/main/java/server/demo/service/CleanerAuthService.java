package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import server.demo.dto.auth.CleanerLoginRequest;
import server.demo.dto.auth.CleanerLoginResponse;
import server.demo.dto.auth.LoginResponse;
import server.demo.dto.auth.LoginTarget;
import server.demo.entity.Cleaner;
import server.demo.repository.CleanerRepository;
import server.demo.util.JwtUtil;

import server.demo.i18n.ApiMessages;
/**
 * 保洁员认证服务
 */
@Service
public class CleanerAuthService {

    @Autowired
    private CleanerRepository cleanerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CleanerIdentityService cleanerIdentityService;

    @Autowired
    private AuthService authService;

    /**
     * 保洁员登录 - 使用密码
     *
     * @param request 登录请求
     * @return 登录响应
     */
    public CleanerLoginResponse loginByPassword(CleanerLoginRequest request) {
        // 查询保洁员
        Cleaner cleaner = cleanerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.0457bd515c31")));

        // 验证账号状态
        if (!cleaner.getIsActive()) {
            throw new RuntimeException(ApiMessages.get("api.t.38ef60b60c1f"));
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), cleaner.getPassword())) {
            throw new RuntimeException(ApiMessages.get("api.t.c99e367064c2"));
        }

        cleaner = cleanerIdentityService.ensureCleanerIdentity(cleaner);
        if (cleaner.getUserId() == null) {
            throw new RuntimeException(ApiMessages.get("api.t.ada30da82f84"));
        }

        // 生成token (使用真实 userId 作为用户ID；勾选“记住登录状态”时签发长效 token)
        String token = jwtUtil.generateToken(cleaner.getUserId(), cleaner.getEmail(), Boolean.TRUE.equals(request.getRememberMe()));

        LoginResponse unifiedResponse = authService.buildAuthenticatedLoginResponse(cleaner.getUserId(), token);
        if (unifiedResponse.getLoginTarget() != LoginTarget.CLEANER || unifiedResponse.getCleaner() == null) {
            throw new RuntimeException(ApiMessages.get("api.t.8c63ab089fc3"));
        }

        // 保持旧客户端兼容响应结构
        CleanerLoginResponse response = new CleanerLoginResponse(token, unifiedResponse.getCleaner());
        response.setCleanerContexts(unifiedResponse.getCleanerContexts());
        return response;
    }
}
