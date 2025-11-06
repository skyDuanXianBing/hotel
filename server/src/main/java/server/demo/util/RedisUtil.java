package server.demo.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * 负责验证码的存储和验证
 */
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${verification.code.expiration}")
    private Long verificationCodeExpiration;

    @Value("${verification.code.cooldown}")
    private Long verificationCodeCooldown;

    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";
    private static final String VERIFICATION_CODE_COOLDOWN_PREFIX = "verification_code_cooldown:";
    private static final String BLACKLIST_TOKEN_PREFIX = "blacklist_token:";

    /**
     * 生成6位随机验证码
     *
     * @return 验证码
     */
    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 存储验证码到Redis
     *
     * @param email 邮箱
     * @param code 验证码
     * @param type 类型（login/register/reset_password）
     */
    public void saveVerificationCode(String email, String code, String type) {
        String key = VERIFICATION_CODE_PREFIX + type + ":" + email;
        String cooldownKey = VERIFICATION_CODE_COOLDOWN_PREFIX + type + ":" + email;

        // 保存验证码,有效期为verificationCodeExpiration
        redisTemplate.opsForValue().set(key, code, verificationCodeExpiration, TimeUnit.SECONDS);

        // 保存冷却时间标记,有效期为verificationCodeCooldown
        redisTemplate.opsForValue().set(cooldownKey, "1", verificationCodeCooldown, TimeUnit.SECONDS);
    }

    /**
     * 验证验证码
     *
     * @param email 邮箱
     * @param code 验证码
     * @param type 类型（login/register/reset_password）
     * @return 是否验证通过
     */
    public boolean verifyCode(String email, String code, String type) {
        String key = VERIFICATION_CODE_PREFIX + type + ":" + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功后删除验证码
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    /**
     * 将token加入黑名单（用于登出）
     *
     * @param token JWT token
     * @param expirationTime 过期时间（毫秒）
     */
    public void addTokenToBlacklist(String token, Long expirationTime) {
        String key = BLACKLIST_TOKEN_PREFIX + token;
        long ttl = (expirationTime - System.currentTimeMillis()) / 1000;
        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
        }
    }

    /**
     * 检查token是否在黑名单中
     *
     * @param token JWT token
     * @return 是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 检查验证码是否存在
     *
     * @param email 邮箱
     * @param type 类型（login/register/reset_password）
     * @return 是否存在
     */
    public boolean hasVerificationCode(String email, String type) {
        String key = VERIFICATION_CODE_PREFIX + type + ":" + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 检查是否在冷却时间内
     *
     * @param email 邮箱
     * @param type 类型（login/register/reset_password）
     * @return 是否在冷却时间内
     */
    public boolean isInCooldown(String email, String type) {
        String cooldownKey = VERIFICATION_CODE_COOLDOWN_PREFIX + type + ":" + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey));
    }
}
