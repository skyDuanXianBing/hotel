package server.demo.dto;

import java.util.List;

/**
 * 更新门店成员权限请求
 */
public class UpdateStoreMemberPermissionRequest {

    private String role; // 基础角色: owner, admin, member（可选）
    private List<Long> roleIds; // 权限角色ID列表
    private Boolean isActive; // 是否激活（可选）

    public UpdateStoreMemberPermissionRequest() {}

    // Getters and Setters
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
