package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.demo.enums.ChannelType;

public class CreateChannelRequest {
    @NotBlank(message = "渠道名称不能为空")
    private String name;

    @NotBlank(message = "渠道代码不能为空")
    private String code;

    @NotNull(message = "渠道类型不能为空")
    private ChannelType type;

    private String color = "#409EFF";
    private Boolean enabled = true;
    private String description;

    // Constructors
    public CreateChannelRequest() {}

    public CreateChannelRequest(String name, String code, ChannelType type, String color, Boolean enabled, String description) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.color = color;
        this.enabled = enabled;
        this.description = description;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}