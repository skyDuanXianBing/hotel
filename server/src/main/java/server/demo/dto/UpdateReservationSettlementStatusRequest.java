package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateReservationSettlementStatusRequest {

    @NotNull(message = "{api.t.e705927a94be}")
    private Boolean settled;

    public Boolean getSettled() {
        return settled;
    }

    public void setSettled(Boolean settled) {
        this.settled = settled;
    }
}
