package server.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Su Webhook 回调地址生成（用于内网穿透/生产域名配置）
 */
@Configuration
public class SuWebhookConfig {

    @Value("${server.base-url:http://localhost:8092}")
    private String serverBaseUrl;

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public String getReservationNotifWebhookUrl(String hotelId) {
        String base = serverBaseUrl != null ? serverBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/v1/su/webhook/reservation-notif/" + hotelId;
    }
}

