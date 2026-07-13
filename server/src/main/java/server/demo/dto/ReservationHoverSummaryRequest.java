package server.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ReservationHoverSummaryRequest {

    @NotEmpty(message = "reservationIds 不能为空")
    @Size(max = 200, message = "reservationIds 最多包含 200 项")
    @Valid
    private List<@NotNull(message = "reservationId 不能为空") @Positive(message = "reservationId 必须为正数") Long> reservationIds;

    public List<Long> getReservationIds() {
        return reservationIds;
    }

    public void setReservationIds(List<Long> reservationIds) {
        this.reservationIds = reservationIds;
    }
}
