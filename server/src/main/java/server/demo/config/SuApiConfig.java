package server.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Su Channel Manager API 配置
 */
@Configuration
@ConfigurationProperties(prefix = "su.api")
public class SuApiConfig {

    /**
     * Su API基础URL
     * 沙箱: https://connect-sandbox.su-api.com
     * 生产: https://connect.su-api.com
     */
    private String baseUrl;

    /**
     * Client ID (Base64编码的域名)
     */
    private String clientId;

    /**
     * Client Secret (从Su Extranet获取)
     */
    private String clientSecret;

    /**
     * PMS名称
     */
    private String pmsName;

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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
