package server.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量调整角色请求DTO
 */
public class BatchUpdateRolesRequest {

    @NotEmpty(message = "{api.t.f3594450f917}")
    private List<Long> accountIds;

    @NotNull(message = "{api.t.50d4c131475f}")
    private List<Long> roleIds;

    public BatchUpdateRolesRequest() {}

    // Getters and Setters
    public List<Long> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<Long> accountIds) {
        this.accountIds = accountIds;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
