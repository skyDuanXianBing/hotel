package server.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Su Channel Manager API 配置。
 */
@Configuration
@ConfigurationProperties(prefix = "su.api")
public class SuApiConfig {

    private static final String SU_PRODUCTION_BASE_URL = "https://connect.su-api.com";
    private static final String SU_SANDBOX_BASE_URL = "https://connect-sandbox.su-api.com";
    private static final String SU_ENV_PRODUCTION = "production";
    private static final String SU_ENV_SANDBOX = "sandbox";

    /**
     * Su API 基础 URL。若为空则由 env 自动推导。
     */
    private String baseUrl;

    /**
     * 是否启用本地 Mock Su API，只由 channel.e2e.local-enabled 统一驱动。
     */
    @Value("${channel.e2e.local-enabled:false}")
    private boolean channelE2ELocalEnabled;

    /**
     * 本地 Mock Su API 基础 URL。
     */
    private String localMockBaseUrl;

    /**
     * 本地 Mock Su API Client ID。
     */
    private String localMockClientId;

    /**
     * 本地 Mock Su API Client Secret。
     */
    private String localMockClientSecret;

    /**
     * Su 环境：sandbox / production。
     */
    private String env;

    /**
     * Client ID（Base64 编码的域名）。
     */
    private String clientId;

    /**
     * 单密钥模式：若非空则优先使用此密钥。
     */
    private String clientSecret;

    /**
     * 双密钥模式：沙盒密钥。
     */
    private String clientSecretSandbox;

    /**
     * 双密钥模式：生产密钥。
     */
    private String clientSecretProduction;

    /**
     * PMS 名称。
     */
    private String pmsName;

    public String getBaseUrl() {
        if (isLocalMockEnabled()) {
            return requireLocalMockValue(localMockBaseUrl, "base-url");
        }
        if (isNotBlank(baseUrl)) {
            return baseUrl.trim();
        }
        String normalizedEnv = resolveEnv();
        if (SU_ENV_SANDBOX.equals(normalizedEnv)) {
            return SU_SANDBOX_BASE_URL;
        }
        return SU_PRODUCTION_BASE_URL;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getClientId() {
        if (isLocalMockEnabled()) {
            return requireLocalMockValue(localMockClientId, "client-id");
        }
        return getRealClientId();
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        if (isLocalMockEnabled()) {
            return requireLocalMockValue(localMockClientSecret, "client-secret");
        }
        return getRealClientSecret();
    }

    public String getRealClientId() {
        return clientId;
    }

    public String getRealClientSecret() {
        return getRealClientSecretForEnv(resolveEnv());
    }

    public String getRealProductionClientSecret() {
        return getRealClientSecretForEnv(SU_ENV_PRODUCTION);
    }

    public String getRealSandboxClientSecret() {
        return getRealClientSecretForEnv(SU_ENV_SANDBOX);
    }

    private String getRealClientSecretForEnv(String normalizedEnv) {
        if (isNotBlank(clientSecret)) {
            return clientSecret.trim();
        }

        if (SU_ENV_SANDBOX.equals(normalizedEnv)) {
            if (!isNotBlank(clientSecretSandbox)) {
                throw new IllegalStateException(
                        "Missing su.api.client-secret-sandbox / SU_CLIENT_SECRET_SANDBOX for sandbox environment."
                );
            }
            return clientSecretSandbox.trim();
        }

        if (!isNotBlank(clientSecretProduction)) {
            throw new IllegalStateException(
                    "Missing su.api.client-secret-production / SU_CLIENT_SECRET_PRODUCTION for production environment."
            );
        }
        return clientSecretProduction.trim();
    }

    public String getRequiredLocalMockBaseUrl() {
        return requireLocalMockValue(localMockBaseUrl, "base-url");
    }

    public String getRequiredLocalMockClientId() {
        return requireLocalMockValue(localMockClientId, "client-id");
    }

    public String getRequiredLocalMockClientSecret() {
        return requireLocalMockValue(localMockClientSecret, "client-secret");
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientSecretSandbox() {
        return clientSecretSandbox;
    }

    public void setClientSecretSandbox(String clientSecretSandbox) {
        this.clientSecretSandbox = clientSecretSandbox;
    }

    public String getClientSecretProduction() {
        return clientSecretProduction;
    }

    public void setClientSecretProduction(String clientSecretProduction) {
        this.clientSecretProduction = clientSecretProduction;
    }

    public String getPmsName() {
        return pmsName;
    }

    public void setPmsName(String pmsName) {
        this.pmsName = pmsName;
    }

    public boolean isLocalMockEnabled() {
        return channelE2ELocalEnabled;
    }

    public void setChannelE2ELocalEnabled(boolean channelE2ELocalEnabled) {
        this.channelE2ELocalEnabled = channelE2ELocalEnabled;
    }

    public String getLocalMockBaseUrl() {
        return localMockBaseUrl;
    }

    public void setLocalMockBaseUrl(String localMockBaseUrl) {
        this.localMockBaseUrl = localMockBaseUrl;
    }

    public String getLocalMockClientId() {
        return localMockClientId;
    }

    public void setLocalMockClientId(String localMockClientId) {
        this.localMockClientId = localMockClientId;
    }

    public String getLocalMockClientSecret() {
        return localMockClientSecret;
    }

    public void setLocalMockClientSecret(String localMockClientSecret) {
        this.localMockClientSecret = localMockClientSecret;
    }

    private String resolveEnv() {
        String normalizedEnv = env == null ? SU_ENV_SANDBOX : env.trim().toLowerCase();
        if (normalizedEnv.isBlank() || SU_ENV_SANDBOX.equals(normalizedEnv)) {
            return SU_ENV_SANDBOX;
        }
        if (SU_ENV_PRODUCTION.equals(normalizedEnv)) {
            return SU_ENV_PRODUCTION;
        }
        throw new IllegalStateException(
                "Invalid su.api.env / SU_ENV: " + env + ". Expected: sandbox or production."
        );
    }

    private String requireLocalMockValue(String value, String propertyName) {
        if (isNotBlank(value)) {
            return value.trim();
        }
        throw new IllegalStateException(
                "Missing su.api.local-mock-" + propertyName
                        + " while channel.e2e.local-enabled / CHANNEL_E2E_LOCAL_ENABLED is true."
        );
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isBlank();
    }
}
