package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class MoveToOrderBoxRequest {

    @NotNull(message = "预订ID不能为空")
    private Long reservationId;

    private String notes;

    // Constructors
    public MoveToOrderBoxRequest() {}

    // Getters and Setters
    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}