package server.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmartLockConfig {
    private static final String SWITCHBOT_DEFAULT_BASE_URL = "https://api.switch-bot.com";
    private static final String TTLOCK_DEFAULT_BASE_URL = "https://api.sciener.com";
    private static final String LOCAL_DEV_ENCRYPTION_KEY =
            "local-smart-lock-dev-encryption-key-change-me";

    @Value("${smart-lock.switchbot.base-url:" + SWITCHBOT_DEFAULT_BASE_URL + "}")
    private String switchBotBaseUrl;

    @Value("${smart-lock.ttlock.base-url:" + TTLOCK_DEFAULT_BASE_URL + "}")
    private String ttLockBaseUrl;

    @Value("${SMART_LOCK_ENCRYPTION_KEY:${smart-lock.encryption.key:}}")
    private String encryptionKey;

    @Value("${SMART_LOCK_SWITCHBOT_WEBHOOK_TOKEN:${smart-lock.switchbot.webhook-token:}}")
    private String switchBotWebhookToken;

    @Value("${smart-lock.confirmation.ttl-seconds:300}")
    private long confirmationTtlSeconds;

    @Value("${smart-lock.passcode-reconcile.enabled:true}")
    private boolean passcodeReconcileEnabled;

    @Value("${smart-lock.passcode-reconcile.batch-size:20}")
    private int passcodeReconcileBatchSize;

    @Value("${smart-lock.passcode-reconcile.timeout-minutes:5}")
    private long passcodeReconcileTimeoutMinutes;

    public String getSwitchBotBaseUrl() {
        return trimOrDefault(switchBotBaseUrl, SWITCHBOT_DEFAULT_BASE_URL);
    }

    public String getTtLockBaseUrl() {
        return trimOrDefault(ttLockBaseUrl, TTLOCK_DEFAULT_BASE_URL);
    }

    public String getEncryptionKey() {
        if (hasText(encryptionKey)) {
            return encryptionKey.trim();
        }
        return LOCAL_DEV_ENCRYPTION_KEY;
    }

    public String getSwitchBotWebhookToken() {
        if (hasText(switchBotWebhookToken)) {
            return switchBotWebhookToken.trim();
        }
        return null;
    }

    public long getConfirmationTtlSeconds() {
        return confirmationTtlSeconds;
    }

    public boolean isPasscodeReconcileEnabled() {
        return passcodeReconcileEnabled;
    }

    public int getPasscodeReconcileBatchSize() {
        return Math.max(1, passcodeReconcileBatchSize);
    }

    public long getPasscodeReconcileTimeoutMinutes() {
        return Math.max(1, passcodeReconcileTimeoutMinutes);
    }

    private static String trimOrDefault(String value, String defaultValue) {
        if (!hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
