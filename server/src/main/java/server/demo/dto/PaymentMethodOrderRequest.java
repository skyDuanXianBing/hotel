package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class PaymentMethodOrderRequest {
    @NotNull(message = "收款方式ID不能为空")
    private Long id;

    @NotNull(message = "排序不能为空")
    private Integer displayOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
