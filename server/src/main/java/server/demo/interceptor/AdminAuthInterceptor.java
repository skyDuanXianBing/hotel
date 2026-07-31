package server.demo.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import server.demo.service.admin.AdminAuthService;
import server.demo.util.JwtUtil;

import java.io.IOException;

/**
 * 平台管理端认证拦截器：保护 /api/admin/**（仅放行登录端点）。
 * 仅接受 claim 中 aud="admin" 的独立管理端 token，门店用户 token 一律拒绝。
 * 认证通过后将 adminUsername/adminRole 写入 request attribute。
 *
 * 注意：SecurityConfig 为全放行，本拦截器是 /api/admin/** 的唯一认证屏障。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN_USERNAME = "adminUsername";
    public static final String ATTR_ADMIN_ROLE = "adminRole";

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "未提供认证令牌");
            return false;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "认证令牌无效或已过期");
            return false;
        }

        try {
            Claims claims = jwtUtil.getClaims(token);
            // 注意：JJWT 解析时将注册 claim aud 归一化为 Set，必须用 getAudience() 判断
            if (claims.getAudience() == null
                    || !claims.getAudience().contains(AdminAuthService.TOKEN_AUDIENCE)) {
                writeUnauthorized(response, "非管理端认证令牌");
                return false;
            }
            String username = claims.get("username", String.class);
            if (username == null || username.isBlank()) {
                username = claims.getSubject();
            }
            request.setAttribute(ATTR_ADMIN_USERNAME, username);
            request.setAttribute(ATTR_ADMIN_ROLE, claims.get("role", String.class));
            return true;
        } catch (Exception e) {
            writeUnauthorized(response, "认证令牌解析失败");
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}
