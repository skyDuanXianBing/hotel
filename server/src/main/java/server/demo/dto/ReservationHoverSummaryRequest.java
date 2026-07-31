package server.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ReservationHoverSummaryRequest {

    @NotEmpty(message = "{api.t.8ccfedc31412}")
    @Size(max = 200, message = "{api.t.944ea1c7d652}")
    @Valid
    private List<@NotNull(message = "{api.t.55fe1d5495bc}") @Positive(message = "{api.t.b37170977a99}") Long> reservationIds;

    public List<Long> getReservationIds() {
        return reservationIds;
    }

    public void setReservationIds(List<Long> reservationIds) {
        this.reservationIds = reservationIds;
    }
}
