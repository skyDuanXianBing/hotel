package server.demo.dto.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.demo.enums.PushPlatform;

/**
 * 移动设备推送令牌注册请求。
 */
public class PushDeviceRegisterRequest {

    @NotNull(message = "{api.t.c4179d74f0ba}")
    private PushPlatform platform;

    @NotBlank(message = "{api.t.8a30ccf0610b}")
    private String deviceToken;

    public PushPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(PushPlatform platform) {
        this.platform = platform;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}
