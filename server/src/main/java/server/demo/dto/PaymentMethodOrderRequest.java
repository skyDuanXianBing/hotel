package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class PaymentMethodOrderRequest {
    @NotNull(message = "{api.t.d2f270636ec0}")
    private Long id;

    @NotNull(message = "{api.t.3218602aa9fb}")
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
