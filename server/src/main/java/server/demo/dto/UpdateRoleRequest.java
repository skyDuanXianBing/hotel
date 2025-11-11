package server.demo.dto;

/**
 * 更新角色请求DTO
 */
public class UpdateRoleRequest {

    private String name;

    private String description;

    public UpdateRoleRequest() {}

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
