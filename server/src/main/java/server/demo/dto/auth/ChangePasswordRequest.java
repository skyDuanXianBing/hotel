package server.demo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求
 */
public class ChangePasswordRequest {

    @NotBlank(message = "{api.t.1aa6721287f0}")
    private String currentPassword;

    @NotBlank(message = "{api.t.689e5a9b3225}")
    @Size(min = 6, max = 64, message = "{api.t.0a0882df29c8}")
    private String newPassword;

    @NotBlank(message = "{api.t.ee353d7ad06c}")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
