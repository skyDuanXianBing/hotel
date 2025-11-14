package server.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 添加门店成员请求
 */
public class AddStoreMemberRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "基础角色不能为空")
    private String role; // 基础角色: owner, admin, member

    private List<Long> roleIds; // 权限角色ID列表（可选）

    public AddStoreMemberRequest() {}

    public AddStoreMemberRequest(String email, String role) {
        this.email = email;
        this.role = role;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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
}
