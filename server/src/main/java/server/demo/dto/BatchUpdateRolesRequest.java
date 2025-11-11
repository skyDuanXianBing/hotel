package server.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量调整角色请求DTO
 */
public class BatchUpdateRolesRequest {

    @NotEmpty(message = "账号ID列表不能为空")
    private List<Long> accountIds;

    @NotNull(message = "角色ID列表不能为空")
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
