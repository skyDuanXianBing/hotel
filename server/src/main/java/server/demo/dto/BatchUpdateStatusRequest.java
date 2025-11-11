package server.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量更新状态请求DTO
 */
public class BatchUpdateStatusRequest {

    @NotEmpty(message = "账号ID列表不能为空")
    private List<Long> accountIds;

    @NotNull(message = "状态不能为空")
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
