package server.demo.dto.auth;

/**
 * 保洁员登录请求
 */
public class CleanerLoginRequest {

    private String email;

    private String password;

    private Boolean rememberMe;

    public CleanerLoginRequest() {}

    public CleanerLoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters and Setters

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
}
