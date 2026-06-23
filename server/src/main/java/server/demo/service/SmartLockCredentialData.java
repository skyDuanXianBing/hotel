package server.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import server.demo.dto.SmartLockRequests;
import server.demo.enums.SmartLockProvider;
import server.demo.util.SmartLockCredentialCrypto;
import server.demo.util.SmartLockMaskingUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class SmartLockCredentialData {
    private SmartLockProvider provider;
    private String switchBotToken;
    private String switchBotSecret;
    private String ttLockClientId;
    private String ttLockClientSecret;
    private String ttLockUsername;
    private String ttLockPasswordMd5;
    private String ttLockAccessToken;
    private String ttLockRefreshToken;
    private LocalDateTime ttLockTokenExpiresAt;

    public static SmartLockCredentialData fromRequest(
            SmartLockProvider provider,
            SmartLockRequests.UpsertIntegrationRequest request,
            SmartLockCredentialData existing
    ) {
        SmartLockCredentialData data = existing != null ? existing.copy() : new SmartLockCredentialData();
        data.setProvider(provider);
        if (provider == SmartLockProvider.SWITCHBOT) {
            data.applySwitchBot(request);
            data.requireSwitchBotFields();
            return data;
        }
        if (provider == SmartLockProvider.TTLOCK) {
            data.applyTtLock(request);
            data.requireTtLockFields();
            return data;
        }
        throw new IllegalArgumentException("不支持的门锁服务商");
    }

    public SmartLockCredentialData copy() {
        SmartLockCredentialData copy = new SmartLockCredentialData();
        copy.provider = provider;
        copy.switchBotToken = switchBotToken;
        copy.switchBotSecret = switchBotSecret;
        copy.ttLockClientId = ttLockClientId;
        copy.ttLockClientSecret = ttLockClientSecret;
        copy.ttLockUsername = ttLockUsername;
        copy.ttLockPasswordMd5 = ttLockPasswordMd5;
        copy.ttLockAccessToken = ttLockAccessToken;
        copy.ttLockRefreshToken = ttLockRefreshToken;
        copy.ttLockTokenExpiresAt = ttLockTokenExpiresAt;
        return copy;
    }

    @JsonIgnore
    public boolean isConfigured() {
        if (provider == SmartLockProvider.SWITCHBOT) {
            return hasText(switchBotToken) && hasText(switchBotSecret);
        }
        if (provider == SmartLockProvider.TTLOCK) {
            return hasText(ttLockClientId)
                    && hasText(ttLockClientSecret)
                    && hasText(ttLockUsername)
                    && hasText(ttLockPasswordMd5);
        }
        return false;
    }

    public Map<String, String> maskedFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        if (provider == SmartLockProvider.SWITCHBOT) {
            fields.put("token", SmartLockMaskingUtils.maskSecret(switchBotToken));
            fields.put("secret", SmartLockMaskingUtils.maskSecret(switchBotSecret));
            return fields;
        }
        if (provider == SmartLockProvider.TTLOCK) {
            fields.put("clientId", SmartLockMaskingUtils.maskSecret(ttLockClientId));
            fields.put("clientSecret", SmartLockMaskingUtils.maskSecret(ttLockClientSecret));
            fields.put("username", SmartLockMaskingUtils.maskSecret(ttLockUsername));
            fields.put("password", hasText(ttLockPasswordMd5) ? "****" : null);
            fields.put("accessToken", SmartLockMaskingUtils.maskSecret(ttLockAccessToken));
        }
        return fields;
    }

    public String fingerprint(SmartLockCredentialCrypto crypto) {
        String payload = String.valueOf(provider)
                + "|" + safe(switchBotToken)
                + "|" + safe(switchBotSecret)
                + "|" + safe(ttLockClientId)
                + "|" + safe(ttLockClientSecret)
                + "|" + safe(ttLockUsername)
                + "|" + safe(ttLockPasswordMd5);
        return crypto.sha256Hex(payload);
    }

    public boolean shouldRefreshTtLockToken(LocalDateTime now) {
        if (provider != SmartLockProvider.TTLOCK) {
            return false;
        }
        if (!hasText(ttLockAccessToken)) {
            return true;
        }
        if (ttLockTokenExpiresAt == null) {
            return true;
        }
        return !ttLockTokenExpiresAt.isAfter(now.plusMinutes(5));
    }

    private void applySwitchBot(SmartLockRequests.UpsertIntegrationRequest request) {
        String token = SmartLockMaskingUtils.trimToNull(request.getSwitchBotToken());
        String secret = SmartLockMaskingUtils.trimToNull(request.getSwitchBotSecret());
        if (token != null) {
            switchBotToken = token;
        }
        if (secret != null) {
            switchBotSecret = secret;
        }
    }

    private void applyTtLock(SmartLockRequests.UpsertIntegrationRequest request) {
        String clientId = SmartLockMaskingUtils.trimToNull(request.getTtLockClientId());
        String clientSecret = SmartLockMaskingUtils.trimToNull(request.getTtLockClientSecret());
        String username = SmartLockMaskingUtils.trimToNull(request.getTtLockUsername());
        String password = SmartLockMaskingUtils.trimToNull(request.getTtLockPassword());
        String passwordMd5 = SmartLockMaskingUtils.trimToNull(request.getTtLockPasswordMd5());
        if (clientId != null) {
            ttLockClientId = clientId;
        }
        if (clientSecret != null) {
            ttLockClientSecret = clientSecret;
        }
        if (username != null) {
            ttLockUsername = username;
        }
        if (password != null) {
            ttLockPasswordMd5 = md5Lower(password);
        }
        if (passwordMd5 != null) {
            if (!passwordMd5.matches("(?i)[0-9a-f]{32}")) {
                throw new IllegalArgumentException("TTLock passwordMd5 必须是 32 位十六进制小写 MD5");
            }
            ttLockPasswordMd5 = passwordMd5.toLowerCase();
        }
    }

    private void requireSwitchBotFields() {
        if (!hasText(switchBotToken) || !hasText(switchBotSecret)) {
            throw new IllegalArgumentException("SwitchBot token 和 secret 不能为空");
        }
    }

    private void requireTtLockFields() {
        if (!hasText(ttLockClientId) || !hasText(ttLockClientSecret)
                || !hasText(ttLockUsername) || !hasText(ttLockPasswordMd5)) {
            throw new IllegalArgumentException("TTLock clientId、clientSecret、username 和 password 不能为空");
        }
    }

    private static String md5Lower(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("MD5 不可用", ex);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    public SmartLockProvider getProvider() {
        return provider;
    }

    public void setProvider(SmartLockProvider provider) {
        this.provider = provider;
    }

    public String getSwitchBotToken() {
        return switchBotToken;
    }

    public void setSwitchBotToken(String switchBotToken) {
        this.switchBotToken = switchBotToken;
    }

    public String getSwitchBotSecret() {
        return switchBotSecret;
    }

    public void setSwitchBotSecret(String switchBotSecret) {
        this.switchBotSecret = switchBotSecret;
    }

    public String getTtLockClientId() {
        return ttLockClientId;
    }

    public void setTtLockClientId(String ttLockClientId) {
        this.ttLockClientId = ttLockClientId;
    }

    public String getTtLockClientSecret() {
        return ttLockClientSecret;
    }

    public void setTtLockClientSecret(String ttLockClientSecret) {
        this.ttLockClientSecret = ttLockClientSecret;
    }

    public String getTtLockUsername() {
        return ttLockUsername;
    }

    public void setTtLockUsername(String ttLockUsername) {
        this.ttLockUsername = ttLockUsername;
    }

    public String getTtLockPasswordMd5() {
        return ttLockPasswordMd5;
    }

    public void setTtLockPasswordMd5(String ttLockPasswordMd5) {
        this.ttLockPasswordMd5 = ttLockPasswordMd5;
    }

    public String getTtLockAccessToken() {
        return ttLockAccessToken;
    }

    public void setTtLockAccessToken(String ttLockAccessToken) {
        this.ttLockAccessToken = ttLockAccessToken;
    }

    public String getTtLockRefreshToken() {
        return ttLockRefreshToken;
    }

    public void setTtLockRefreshToken(String ttLockRefreshToken) {
        this.ttLockRefreshToken = ttLockRefreshToken;
    }

    public LocalDateTime getTtLockTokenExpiresAt() {
        return ttLockTokenExpiresAt;
    }

    public void setTtLockTokenExpiresAt(LocalDateTime ttLockTokenExpiresAt) {
        this.ttLockTokenExpiresAt = ttLockTokenExpiresAt;
    }
}
