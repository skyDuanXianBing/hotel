package server.demo.dto.auth;

/**
 * 保洁员登录响应
 */
public class CleanerLoginResponse {

    private String token;

    private CleanerDTO cleaner;

    public CleanerLoginResponse() {}

    public CleanerLoginResponse(String token, CleanerDTO cleaner) {
        this.token = token;
        this.cleaner = cleaner;
    }

    // Getters and Setters

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CleanerDTO getCleaner() {
        return cleaner;
    }

    public void setCleaner(CleanerDTO cleaner) {
        this.cleaner = cleaner;
    }
}
