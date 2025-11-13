package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import server.demo.dto.auth.CleanerDTO;
import server.demo.dto.auth.CleanerLoginRequest;
import server.demo.dto.auth.CleanerLoginResponse;
import server.demo.entity.Cleaner;
import server.demo.repository.CleanerRepository;
import server.demo.util.JwtUtil;

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

    /**
     * 保洁员登录 - 使用密码
     *
     * @param request 登录请求
     * @return 登录响应
     */
    public CleanerLoginResponse loginByPassword(CleanerLoginRequest request) {
        // 查询保洁员
        Cleaner cleaner = cleanerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("该邮箱未注册保洁员账户"));

        // 验证账号状态
        if (!cleaner.getIsActive()) {
            throw new RuntimeException("保洁员账户已停用，无法登录");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), cleaner.getPassword())) {
            throw new RuntimeException("邮箱或密码不正确");
        }

        // 生成token (使用cleaner id作为用户ID)
        String token = jwtUtil.generateToken(cleaner.getId(), cleaner.getEmail());

        // 返回登录响应
        return new CleanerLoginResponse(token, new CleanerDTO(cleaner));
    }
}
