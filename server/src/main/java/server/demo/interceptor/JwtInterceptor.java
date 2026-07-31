package server.demo.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import server.demo.i18n.ApiMessages;
import server.demo.util.ApiResponseHttpWriter;
import server.demo.util.JwtUtil;
import server.demo.util.RedisUtil;

import java.util.Set;

/**
 * JWT拦截器
 * 负责验证JWT token并注入用户信息到请求属性中
 *
 * <p>受众隔离（P10 修复）：管理端 token（aud=admin）与门店用户 token 共用同一签名密钥，
 * 不校验 audience 时管理端 token 可调通 /api/v1/** 租户链路（越权）。此处强制：
 * aud 存在时必须为租户值（{@link JwtUtil#TENANT_TOKEN_AUDIENCE}），否则一律 401；
 * 无 aud 的旧 token 按既有租户声明（userId）判定——userId 缺失同样 401（管理端 token 无 userId，
 * 被两道检查同时拦截）。</p>
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, ApiMessages.get("api.auth.token.missing"));
            return false;
        }

        String token = authHeader.substring(7);

        if (redisUtil.isTokenBlacklisted(token)) {
            writeUnauthorized(response, ApiMessages.get("api.auth.token.blacklisted"));
            return false;
        }

        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(response, ApiMessages.get("api.auth.token.invalid"));
            return false;
        }

        try {
            Claims claims = jwtUtil.getClaims(token);

            Set<String> audience = claims.getAudience();
            if (audience != null && !audience.isEmpty()
                    && !audience.contains(JwtUtil.TENANT_TOKEN_AUDIENCE)) {
                writeUnauthorized(response, ApiMessages.get("api.auth.token.not_tenant"));
                return false;
            }

            Long userId = claims.get("userId", Long.class);
            if (userId == null) {
                writeUnauthorized(response, ApiMessages.get("api.auth.token.not_tenant"));
                return false;
            }
            String email = claims.getSubject();

            request.setAttribute("userId", userId);
            request.setAttribute("email", email);
            request.setAttribute("token", token);

            return true;
        } catch (Exception e) {
            writeUnauthorized(response, ApiMessages.get("api.auth.token.parse_failed"));
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        ApiResponseHttpWriter.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, message);
    }
}
