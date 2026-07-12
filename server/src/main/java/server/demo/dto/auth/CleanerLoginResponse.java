package server.demo.dto.auth;

import java.util.List;

/**
 * 保洁员登录响应
 */
public class CleanerLoginResponse {

    private String token;

    private CleanerDTO cleaner;
    private List<CleanerContextDTO> cleanerContexts;

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

    public List<CleanerContextDTO> getCleanerContexts() { return cleanerContexts; }
    public void setCleanerContexts(List<CleanerContextDTO> cleanerContexts) { this.cleanerContexts = cleanerContexts; }
}
