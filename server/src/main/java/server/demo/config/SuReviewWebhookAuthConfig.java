package server.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Su Review webhook Authorization 配置。
 * <p>
 * Su 的 Review Push 契约只约定 Authorization credentials，不假定具体认证方案。
 * 因此这里保存并原样比较完整的 Authorization header；未配置时必须拒绝所有回调。
 */
@Configuration
@ConfigurationProperties(prefix = "su.review.webhook")
public class SuReviewWebhookAuthConfig {

    private String authorization;

    public boolean isConfigured() {
        return authorization != null && !authorization.isBlank();
    }

    public boolean matches(String presentedAuthorization) {
        if (!isConfigured() || presentedAuthorization == null) {
            return false;
        }
        return MessageDigest.isEqual(
                authorization.getBytes(StandardCharsets.UTF_8),
                presentedAuthorization.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }
}
