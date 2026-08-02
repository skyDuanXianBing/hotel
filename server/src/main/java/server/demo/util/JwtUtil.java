package server.demo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 负责生成和验证JWT token
 */
@Component
public class JwtUtil {

    /**
     * 租户端（门店用户/保洁员等 /api/v1/** 体系）token 的受众标识。
     * 新签发的租户 token 携带 aud=tenant，JwtInterceptor 据此与管理端 token（aud=admin）隔离。
     */
    public static final String TENANT_TOKEN_AUDIENCE = "tenant";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 勾选“记住登录状态”时签发的长效 token 有效期（默认 30 天）。
     */
    @Value("${jwt.remember-me-expiration:2592000000}")
    private Long rememberMeExpiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT token（默认有效期）
     *
     * @param userId 用户ID
     * @param email 用户邮箱
     * @return JWT token
     */
    public String generateToken(Long userId, String email) {
        return generateToken(userId, email, expiration);
    }

    /**
     * 按“记住登录状态”生成JWT token：rememberMe=true 时使用长效有效期，否则使用默认有效期。
     *
     * @param userId 用户ID
     * @param email 用户邮箱
     * @param rememberMe 是否记住登录状态
     * @return JWT token
     */
    public String generateToken(Long userId, String email, boolean rememberMe) {
        return generateToken(userId, email, rememberMe ? rememberMeExpiration : expiration);
    }

    private String generateToken(Long userId, String email, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        // 受众标记：与管理端 token（aud=admin）隔离，JwtInterceptor 校验 aud 白名单
        claims.put("aud", TENANT_TOKEN_AUDIENCE);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成带自定义claims的JWT token（供管理端等非用户体系认证使用，不破坏既有 generateToken(userId, email) 调用）。
     *
     * @param claims 自定义claims（如 aud/username/role）
     * @param subject token主体
     * @return JWT token
     */
    public String generateToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析token获取全部Claims（校验签名；过期/非法会抛异常，调用方需自行捕获）。
     *
     * @param token JWT token
     * @return Claims
     */
    public Claims getClaims(String token) {
        return getClaimsFromToken(token);
    }

    /**
     * 从token中获取用户ID
     *
     * @param token JWT token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从token中获取用户邮箱
     *
     * @param token JWT token
     * @return 用户邮箱
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 获取token的过期时间
     *
     * @param token JWT token
     * @return 过期时间
     */
    public Date getExpirationDate(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 从token中获取Claims
     *
     * @param token JWT token
     * @return Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证token是否有效
     *
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断token是否过期
     *
     * @param claims Claims
     * @return 是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
}
