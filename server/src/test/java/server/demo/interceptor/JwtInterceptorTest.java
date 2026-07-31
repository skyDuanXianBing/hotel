package server.demo.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.util.JwtUtil;
import server.demo.util.RedisUtil;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * 租户端拦截器受众隔离（P10 修复）：管理端 token（aud=admin）与租户 token 共用签名密钥，
 * 未校验 aud 时管理端 token 可调通 /api/v1/**（越权）。修复后：
 * aud 存在且非租户值 → 401；无 aud 的旧 token 按既有租户声明 userId 判定，缺失 → 401。
 */
class JwtInterceptorTest {

    private JwtUtil jwtUtil;
    private RedisUtil redisUtil;
    private JwtInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "jwt-interceptor-test-secret-key-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600_000L);
        redisUtil = Mockito.mock(RedisUtil.class);
        Mockito.when(redisUtil.isTokenBlacklisted(anyString())).thenReturn(false);
        interceptor = new JwtInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(interceptor, "redisUtil", redisUtil);
    }

    /** 新签发租户 token 的形态：aud=tenant + userId（JwtUtil.generateToken(userId, email)）。 */
    private String tenantToken() {
        return jwtUtil.generateToken(42L, "owner@example.com");
    }

    /** 旧版租户 token（修复前签发、仍在有效期内）：无 aud，仅有 userId/email。 */
    private String legacyTenantTokenWithoutAudience() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 42L);
        claims.put("email", "owner@example.com");
        return jwtUtil.generateToken(claims, "owner@example.com");
    }

    /** 管理端 token 的形态：aud=admin + username/role，无 userId（AdminAuthService.login）。 */
    private String adminToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("aud", "admin");
        claims.put("username", "admin");
        claims.put("role", "SUPER");
        return jwtUtil.generateToken(claims, "admin");
    }

    @Test
    void preHandle_shouldRejectWhenAuthorizationHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("未提供认证令牌"));
        assertNull(request.getAttribute("userId"));
    }

    @Test
    void preHandle_shouldRejectGarbageToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        request.addHeader("Authorization", "Bearer not-a-real-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandle_shouldRejectBlacklistedToken() throws Exception {
        String token = tenantToken();
        Mockito.when(redisUtil.isTokenBlacklisted(token)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("认证令牌已失效"));
    }

    @Test
    void preHandle_shouldRejectAdminAudienceToken() throws Exception {
        // 核心回归：admin JWT（aud=admin）调租户端 /api/v1/stores 必须 401
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        request.addHeader("Authorization", "Bearer " + adminToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("非租户端认证令牌"));
        assertNull(request.getAttribute("userId"));
    }

    @Test
    void preHandle_shouldRejectForgedAdminAudienceTokenEvenWithUserId() throws Exception {
        // 伪造场景：token 同时带 aud=admin 与 userId——aud 白名单优先拦截
        Map<String, Object> claims = new HashMap<>();
        claims.put("aud", "admin");
        claims.put("userId", 42L);
        String forged = jwtUtil.generateToken(claims, "owner@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        request.addHeader("Authorization", "Bearer " + forged);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("非租户端认证令牌"));
        assertNull(request.getAttribute("userId"));
    }

    @Test
    void preHandle_shouldRejectTokenWithoutUserIdAndAudience() throws Exception {
        // 既无 aud 又无 userId：不是租户令牌（userId 声明兜底拦截）
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "nobody@example.com");
        String token = jwtUtil.generateToken(claims, "nobody@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertNull(request.getAttribute("userId"));
    }

    @Test
    void preHandle_shouldAllowLegacyTenantTokenWithoutAudience() throws Exception {
        // 兼容策略：无 aud 的旧 token 凭既有租户声明 userId 放行（不强制全量重登）
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        request.addHeader("Authorization", "Bearer " + legacyTenantTokenWithoutAudience());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(200, response.getStatus());
        assertEquals(42L, request.getAttribute("userId"));
        assertEquals("owner@example.com", request.getAttribute("email"));
    }

    @Test
    void preHandle_shouldAllowNewTenantTokenWithTenantAudience() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        request.addHeader("Authorization", "Bearer " + tenantToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertEquals(200, response.getStatus());
        assertEquals(42L, request.getAttribute("userId"));
        assertEquals("owner@example.com", request.getAttribute("email"));
        // 新签发的租户 token 必须携带 aud=tenant（aud 白名单才有长期意义）
        assertTrue(jwtUtil.getClaims(tenantToken()).getAudience().contains(JwtUtil.TENANT_TOKEN_AUDIENCE));
    }
}
