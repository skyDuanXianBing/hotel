package server.demo.dto;

import java.util.List;

/**
 * 更新门店成员权限请求
 */
public class UpdateStoreMemberPermissionRequest {

    private String role; // 基础角色: owner, admin, member（可选）
    private List<Long> roleIds; // 权限角色ID列表（可选）
    private Boolean isActive; // 是否激活（可选）
    private List<PermissionDTO> extraPermissions; // 成员额外权限（叠加，可选）
    private String name; // 员工姓名（可选，写入全局 User.name）

    public UpdateStoreMemberPermissionRequest() {}

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

    public List<PermissionDTO> getExtraPermissions() {
        return extraPermissions;
    }

    public void setExtraPermissions(List<PermissionDTO> extraPermissions) {
        this.extraPermissions = extraPermissions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
