package server.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 验证码登录请求DTO
 */
public class LoginByCodeRequest {

    @NotBlank(message = "{api.t.cfe012352180}")
    @Email(message = "{api.t.4f0599f86e3f}")
    private String email;

    @NotBlank(message = "{api.t.5831be51274d}")
    @Size(min = 6, max = 6, message = "{api.t.5fead779f17c}")
    private String verificationCode;

    private Boolean rememberMe;
    private LoginTarget preferredLoginTarget;

    public LoginByCodeRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public LoginTarget getPreferredLoginTarget() { return preferredLoginTarget; }
    public void setPreferredLoginTarget(LoginTarget preferredLoginTarget) { this.preferredLoginTarget = preferredLoginTarget; }
}
