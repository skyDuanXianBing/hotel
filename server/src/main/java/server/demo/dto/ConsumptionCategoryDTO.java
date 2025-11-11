package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 消费项分类DTO
 */
public class ConsumptionCategoryDTO {

    private Long id;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String description;

    private Integer count;

    // Constructors
    public ConsumptionCategoryDTO() {}

    public ConsumptionCategoryDTO(String name, String description) {
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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
