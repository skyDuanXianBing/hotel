package server.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建账号请求DTO
 */
public class CreateAccountRequest {

    @NotBlank(message = "{api.t.ecb38cb09941}")
    @Size(min = 3, max = 50, message = "{api.t.5b973f53ba5f}")
    private String username;

    @NotBlank(message = "{api.t.fb45634dbd6e}")
    private String name;

    @NotBlank(message = "{api.t.cfe012352180}")
    @Email(message = "{api.t.4f0599f86e3f}")
    private String email;

    private String password; // 可选,如果为空则生成默认密码

    private List<Long> roleIds; // 角色ID列表

    private PermissionsRequest permissions; // 权限设置

    public CreateAccountRequest() {}

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public PermissionsRequest getPermissions() {
        return permissions;
    }

    public void setPermissions(PermissionsRequest permissions) {
        this.permissions = permissions;
    }
}
