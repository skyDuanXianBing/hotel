package server.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 消费项DTO
 */
public class ConsumptionItemDTO {

    private Long id;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "消费项名称不能为空")
    private String name;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal price;

    private Boolean enabled = true;

    private String description;

    // Constructors
    public ConsumptionItemDTO() {}

    public ConsumptionItemDTO(String category, String name, BigDecimal price) {
        this.category = category;
        this.name = name;
        this.price = price;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
