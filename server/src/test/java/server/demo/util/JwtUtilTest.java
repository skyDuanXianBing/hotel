package server.demo.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-jwt-util-tests-0123456789abcdef";
    private static final long DEFAULT_EXPIRATION_MS = 86_400_000L;
    private static final long REMEMBER_ME_EXPIRATION_MS = 2_592_000_000L;
    private static final long TOLERANCE_MS = 5_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", DEFAULT_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtUtil, "rememberMeExpiration", REMEMBER_ME_EXPIRATION_MS);
    }

    @Test
    void generateToken_defaultOverload_shouldUseDefaultExpiration() {
        long before = System.currentTimeMillis();
        String token = jwtUtil.generateToken(1L, "user@example.com");

        Date expiresAt = jwtUtil.getExpirationDate(token);
        long ttl = expiresAt.getTime() - before;

        assertTrue(Math.abs(ttl - DEFAULT_EXPIRATION_MS) <= TOLERANCE_MS,
                "默认有效期应接近 24 小时，实际 ttl=" + ttl);
    }

    @Test
    void generateToken_rememberMeFalse_shouldUseDefaultExpiration() {
        long before = System.currentTimeMillis();
        String token = jwtUtil.generateToken(1L, "user@example.com", false);

        Date expiresAt = jwtUtil.getExpirationDate(token);
        long ttl = expiresAt.getTime() - before;

        assertTrue(Math.abs(ttl - DEFAULT_EXPIRATION_MS) <= TOLERANCE_MS,
                "未勾选记住登录时应接近 24 小时，实际 ttl=" + ttl);
    }

    @Test
    void generateToken_rememberMeTrue_shouldUseRememberMeExpiration() {
        long before = System.currentTimeMillis();
        String token = jwtUtil.generateToken(1L, "user@example.com", true);

        Date expiresAt = jwtUtil.getExpirationDate(token);
        long ttl = expiresAt.getTime() - before;

        assertTrue(Math.abs(ttl - REMEMBER_ME_EXPIRATION_MS) <= TOLERANCE_MS,
                "勾选记住登录时应接近 30 天，实际 ttl=" + ttl);
        assertEquals(1L, jwtUtil.getUserIdFromToken(token));
        assertEquals("user@example.com", jwtUtil.getEmailFromToken(token));
        assertEquals(JwtUtil.TENANT_TOKEN_AUDIENCE, jwtUtil.getClaims(token).getAudience().iterator().next());
    }

    @Test
    void generateToken_rememberMeTrue_shouldRemainValid() {
        String token = jwtUtil.generateToken(1L, "user@example.com", true);

        assertTrue(jwtUtil.validateToken(token));
    }
}
