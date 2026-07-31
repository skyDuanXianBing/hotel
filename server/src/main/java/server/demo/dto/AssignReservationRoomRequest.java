package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class AssignReservationRoomRequest {
    @NotNull(message = "{api.t.174ada196c44}")
    private Long roomId;

    public AssignReservationRoomRequest() {}

    public AssignReservationRoomRequest(Long roomId) {
        this.roomId = roomId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}

