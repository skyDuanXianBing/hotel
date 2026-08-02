package server.demo.dto.push;

import jakarta.validation.constraints.NotBlank;

/**
 * 移动设备推送令牌解绑请求（退出登录时调用）。
 */
public class PushDeviceUnregisterRequest {

    @NotBlank(message = "{api.t.8a30ccf0610b}")
    private String deviceToken;

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}
