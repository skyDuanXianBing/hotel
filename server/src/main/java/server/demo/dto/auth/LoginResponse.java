package server.demo.dto.auth;

import server.demo.dto.StoreDTO;

import java.util.List;

/**
 * 登录响应DTO
 */
public class LoginResponse {

    private String token;
    private UserDTO user;
    private List<StoreDTO> stores;
    private LoginTarget loginTarget = LoginTarget.PMS;
    private CleanerDTO cleaner;
    private StoreDTO currentStore;
    private Long targetStoreId;
    private List<CleanerContextDTO> cleanerContexts;
    private List<LoginTarget> availableLoginTargets;

    public LoginResponse() {
    }

    public LoginResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    public LoginResponse(String token, UserDTO user, List<StoreDTO> stores) {
        this.token = token;
        this.user = user;
        this.stores = stores;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public List<StoreDTO> getStores() {
        return stores;
    }

    public void setStores(List<StoreDTO> stores) {
        this.stores = stores;
    }

    public LoginTarget getLoginTarget() {
        return loginTarget;
    }

    public void setLoginTarget(LoginTarget loginTarget) {
        this.loginTarget = loginTarget == null ? LoginTarget.PMS : loginTarget;
    }

    public CleanerDTO getCleaner() {
        return cleaner;
    }

    public void setCleaner(CleanerDTO cleaner) {
        this.cleaner = cleaner;
    }

    public StoreDTO getCurrentStore() {
        return currentStore;
    }

    public void setCurrentStore(StoreDTO currentStore) {
        this.currentStore = currentStore;
    }

    public Long getTargetStoreId() {
        return targetStoreId;
    }

    public void setTargetStoreId(Long targetStoreId) {
        this.targetStoreId = targetStoreId;
    }

    public List<CleanerContextDTO> getCleanerContexts() { return cleanerContexts; }
    public void setCleanerContexts(List<CleanerContextDTO> cleanerContexts) { this.cleanerContexts = cleanerContexts; }

    public List<LoginTarget> getAvailableLoginTargets() { return availableLoginTargets; }
    public void setAvailableLoginTargets(List<LoginTarget> availableLoginTargets) { this.availableLoginTargets = availableLoginTargets; }
}
