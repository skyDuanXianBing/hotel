package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Service
public class RegistrationLinkService {

    public static class Claims {
        private final Long storeId;
        private final long expEpochSeconds;

        public Claims(Long storeId, long expEpochSeconds) {
            this.storeId = storeId;
            this.expEpochSeconds = expEpochSeconds;
        }

        public Long getStoreId() {
            return storeId;
        }

        public long getExpEpochSeconds() {
            return expEpochSeconds;
        }
    }

    private final String secret;
    private final long ttlDays;

    public RegistrationLinkService(
            @Value("${registration.link.secret}") String secret,
            @Value("${registration.link.ttl-days:90}") long ttlDays
    ) {
        this.secret = secret;
        this.ttlDays = ttlDays;
    }

    public String generateToken(Long storeId, String orderNumber) {
        long exp = Instant.now().plusSeconds(ttlDays * 86400L).getEpochSecond();
        String payload = payload(storeId, orderNumber, exp);
        String sig = hmacSha256Hex(payload);
        return storeId + "." + exp + "." + sig;
    }

    public Claims verifyToken(String orderNumber, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("缺少token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("token格式错误");
        }
        Long storeId;
        long exp;
        try {
            storeId = Long.parseLong(parts[0]);
            exp = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("token格式错误");
        }

        long now = Instant.now().getEpochSecond();
        if (now > exp) {
            throw new IllegalArgumentException("链接已过期");
        }

        String expectedSig = hmacSha256Hex(payload(storeId, orderNumber, exp));
        if (!constantTimeEquals(expectedSig, parts[2])) {
            throw new IllegalArgumentException("token无效");
        }

        return new Claims(storeId, exp);
    }

    private String payload(Long storeId, String orderNumber, long exp) {
        return storeId + ":" + orderNumber + ":" + exp;
    }

    private String hmacSha256Hex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC计算失败", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit((b & 0xF), 16));
        }
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
