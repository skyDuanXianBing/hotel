package server.demo.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.util.JwtUtil;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理端拦截器：无 token / 非 admin 受众 token 拒绝；合法 admin token 放行并写入请求属性。
 */
class AdminAuthInterceptorTest {

    private JwtUtil jwtUtil;
    private AdminAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "admin-interceptor-test-secret-key-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600_000L);
        interceptor = new AdminAuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);
    }

    private String adminToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("aud", "admin");
        claims.put("username", "admin");
        claims.put("role", "SUPER");
        return jwtUtil.generateToken(claims, "admin");
    }

    private String storeUserToken() {
        // 门店用户 token：无 aud=admin（模拟 JwtUtil.generateToken(userId, email) 的产物）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 42L);
        claims.put("email", "owner@example.com");
        return jwtUtil.generateToken(claims, "owner@example.com");
    }

    @Test
    void preHandle_shouldRejectWhenAuthorizationHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/packages");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("未提供认证令牌"));
        assertNull(request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME));
    }

    @Test
    void preHandle_shouldRejectStoreUserTokenWithoutAdminAudience() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/packages");
        request.addHeader("Authorization", "Bearer " + storeUserToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("非管理端认证令牌"));
        assertNull(request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME));
    }

    @Test
    void preHandle_shouldRejectGarbageToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/packages");
        request.addHeader("Authorization", "Bearer not-a-real-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandle_shouldRejectTenantAudienceToken() throws Exception {
        // P10 反向确认：新签发的租户 token 携带 aud=tenant（JwtUtil.TENANT_TOKEN_AUDIENCE），
        // 管理端链路必须拒绝一切非 aud=admin 的 token
        String tenantToken = jwtUtil.generateToken(42L, "owner@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/packages");
        request.addHeader("Authorization", "Bearer " + tenantToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("非管理端认证令牌"));
        assertNull(request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME));
    }

    @Test
    void preHandle_shouldAllowValidAdminTokenAndExposeAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/packages");
        request.addHeader("Authorization", "Bearer " + adminToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(200, response.getStatus());
        assertEquals("admin", request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME));
        assertEquals("SUPER", request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_ROLE));
    }
}
