package server.demo.dto;

import jakarta.validation.constraints.Size;

public class UpdateReservationNotesRequest {

    @Size(max = 1000, message = "{api.t.ddfd1b6ab954}")
    private String notes;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
