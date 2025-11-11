package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建角色请求DTO
 */
public class CreateRoleRequest {

    @NotBlank(message = "角色名称不能为空")
    private String name;

    private String description;

    public CreateRoleRequest() {}

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
