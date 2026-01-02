package server.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Su Messaging webhook Basic Auth 配置。
 * <p>
 * 约定：
 * - 生产环境配置用户名/密码后强制校验 Basic Auth
 * - 本地/沙盒可将用户名或密码留空以关闭鉴权（便于调试）
 */
@Configuration
@ConfigurationProperties(prefix = "su.messaging.webhook")
public class SuMessagingWebhookAuthConfig {

    private String username;
    private String password;

    public boolean isAuthEnabled() {
        return username != null
                && !username.isBlank()
                && password != null
                && !password.isBlank();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

