package server.demo.config;

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
     * Su 环境：sandbox / production。
     */
    private String env;

    /**
     * Client ID（Base64 编码的域名）。
     */
    private String clientId;

    /**
     * Client Secret（从 Su Extranet 获取）。
     */
    private String clientSecret;

    /**
     * PMS 名称。
     */
    private String pmsName;

    public String getBaseUrl() {
        if (baseUrl != null && !baseUrl.trim().isBlank()) {
            return baseUrl.trim();
        }
        String normalizedEnv = env == null ? SU_ENV_SANDBOX : env.trim().toLowerCase();
        if (normalizedEnv.isBlank() || SU_ENV_SANDBOX.equals(normalizedEnv)) {
            return SU_SANDBOX_BASE_URL;
        }
        if (SU_ENV_PRODUCTION.equals(normalizedEnv)) {
            return SU_PRODUCTION_BASE_URL;
        }
        throw new IllegalStateException(
                "Invalid su.api.env / SU_ENV: " + env + ". Expected: sandbox or production."
        );
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
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPmsName() {
        return pmsName;
    }

    public void setPmsName(String pmsName) {
        this.pmsName = pmsName;
    }
}
