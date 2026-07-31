package server.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量更新状态请求DTO
 */
public class BatchUpdateStatusRequest {

    @NotEmpty(message = "{api.t.f3594450f917}")
    private List<Long> accountIds;

    @NotNull(message = "{api.t.1318b551d6ba}")
    private Boolean isActive;

    public BatchUpdateStatusRequest() {}

    // Getters and Setters
    public List<Long> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<Long> accountIds) {
        this.accountIds = accountIds;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
