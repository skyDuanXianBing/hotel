package server.demo.dto;

import java.time.LocalDateTime;

public class OrderBoxDTO {

    private Long id;
    private Long reservationId;
    private ReservationDTO reservation;
    private LocalDateTime movedInAt;
    private String movedInBy;
    private String notes;

    // Constructors
    public OrderBoxDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public ReservationDTO getReservation() {
        return reservation;
    }

    public void setReservation(ReservationDTO reservation) {
        this.reservation = reservation;
    }

    public LocalDateTime getMovedInAt() {
        return movedInAt;
    }

    public void setMovedInAt(LocalDateTime movedInAt) {
        this.movedInAt = movedInAt;
    }

    public String getMovedInBy() {
        return movedInBy;
    }

    public void setMovedInBy(String movedInBy) {
        this.movedInBy = movedInBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}