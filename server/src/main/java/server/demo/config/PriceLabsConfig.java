package server.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * PriceLabs API 配置
 */
@Configuration
public class PriceLabsConfig {

    @Value("${pricelabs.api.base-url:https://api.pricelabs.co/v1/integration/api}")
    private String baseUrl;

    @Value("${pricelabs.api.integration-name:thehosthub}")
    private String integrationName;

    @Value("${pricelabs.api.integration-token}")
    private String integrationToken;

    @Value("${pricelabs.api.regenerate-token:false}")
    private boolean regenerateToken;

    @Value("${pricelabs.api.features.delta-only:true}")
    private boolean deltaOnly;

    @Value("${server.base-url:http://localhost:8092}")
    private String serverBaseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getIntegrationName() {
        return integrationName;
    }

    public String getIntegrationToken() {
        return integrationToken;
    }

    public boolean isRegenerateToken() {
        return regenerateToken;
    }

    public boolean isDeltaOnly() {
        return deltaOnly;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    /**
     * Webhook URLs
     */
    public String getSyncUrl() {
        return serverBaseUrl + "/api/v1/pricelabs/webhook/sync";
    }

    public String getCalendarTriggerUrl() {
        return serverBaseUrl + "/api/v1/pricelabs/webhook/calendar-trigger";
    }

    public String getHookUrl() {
        return serverBaseUrl + "/api/v1/pricelabs/webhook/hook";
    }

    @Bean
    public RestTemplate priceLabsRestTemplate() {
        return new RestTemplate();
    }
}
