package server.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BatchCreateReservationRequest {

    private String groupOrderNo;

    @Valid
    @NotEmpty(message = "{api.t.10a7e0f9210f}")
    private List<CreateReservationRequest> reservations;

    public String getGroupOrderNo() {
        return groupOrderNo;
    }

    public void setGroupOrderNo(String groupOrderNo) {
        this.groupOrderNo = groupOrderNo;
    }

    public List<CreateReservationRequest> getReservations() {
        return reservations;
    }

    public void setReservations(List<CreateReservationRequest> reservations) {
        this.reservations = reservations;
    }
}

