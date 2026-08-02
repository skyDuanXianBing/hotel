package server.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 移动推送配置（iOS APNs；Android FCM 发送器预留）。
 *
 * <p>APNs 使用 Token 认证（.p8 Auth Key），支持两种提供方式：
 * <ul>
 *   <li>{@code push.apns.auth-key-path}：.p8 文件路径</li>
 *   <li>{@code push.apns.auth-key-base64}：.p8 文件内容的 Base64（单行，适合放入 .env）</li>
 * </ul>
 * 两者同时存在时优先使用文件路径。
 */
@ConfigurationProperties(prefix = "push")
public class PushProperties {

    /**
     * 推送总开关；关闭时所有推送直接跳过。
     */
    private boolean enabled = false;

    private final Apns apns = new Apns();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Apns getApns() {
        return apns;
    }

    public static class Apns {

        private boolean enabled = false;

        /**
         * Apple Developer Team ID。
         */
        private String teamId = "";

        /**
         * APNs Auth Key 的 Key ID。
         */
        private String keyId = "";

        /**
         * .p8 Auth Key 文件路径。
         */
        private String authKeyPath = "";

        /**
         * .p8 Auth Key 文件内容的 Base64（单行环境变量用）。
         */
        private String authKeyBase64 = "";

        /**
         * 推送 Topic，固定为 App Bundle ID。
         */
        private String topic = "jp.thehost.pms";

        /**
         * 是否走 APNs 生产环境；Xcode 直装的 Debug 包必须为 false（sandbox）。
         */
        private boolean production = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTeamId() {
            return teamId;
        }

        public void setTeamId(String teamId) {
            this.teamId = teamId;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getAuthKeyPath() {
            return authKeyPath;
        }

        public void setAuthKeyPath(String authKeyPath) {
            this.authKeyPath = authKeyPath;
        }

        public String getAuthKeyBase64() {
            return authKeyBase64;
        }

        public void setAuthKeyBase64(String authKeyBase64) {
            this.authKeyBase64 = authKeyBase64;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public boolean isProduction() {
            return production;
        }

        public void setProduction(boolean production) {
            this.production = production;
        }
    }
}
