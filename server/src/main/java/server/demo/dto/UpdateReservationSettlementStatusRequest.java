package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateReservationSettlementStatusRequest {

    @NotNull(message = "settled 不能为空")
    private Boolean settled;

    public Boolean getSettled() {
        return settled;
    }

    public void setSettled(Boolean settled) {
        this.settled = settled;
    }
}
