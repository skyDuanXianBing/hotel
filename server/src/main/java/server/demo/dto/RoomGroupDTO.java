package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 房间分组DTO
 */
public class RoomGroupDTO {

    private Long id;

    @NotBlank(message = "分组名称不能为空")
    private String name;

    private String description;

    // Constructors
    public RoomGroupDTO() {}

    public RoomGroupDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
