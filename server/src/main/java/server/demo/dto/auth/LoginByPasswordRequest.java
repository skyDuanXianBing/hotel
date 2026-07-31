package server.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 密码登录请求DTO
 */
public class LoginByPasswordRequest {

    @NotBlank(message = "{api.t.cfe012352180}")
    @Email(message = "{api.t.4f0599f86e3f}")
    private String email;

    @NotBlank(message = "{api.t.4d81424b0110}")
    private String password;

    private Boolean rememberMe;
    private LoginTarget preferredLoginTarget;

    public LoginByPasswordRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
