package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class MoveOutOrderBoxRequest {

    @NotNull(message = "{api.t.87d6a6dafe1c}")
    private Long orderBoxItemId;

    private String notes;

    // Constructors
    public MoveOutOrderBoxRequest() {}

    // Getters and Setters
    public Long getOrderBoxItemId() {
        return orderBoxItemId;
    }

    public void setOrderBoxItemId(Long orderBoxItemId) {
        this.orderBoxItemId = orderBoxItemId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}