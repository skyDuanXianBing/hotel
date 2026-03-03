package server.demo.dto;

import jakarta.validation.constraints.NotNull;

public class AssignReservationRoomRequest {
    @NotNull(message = "roomId不能为空")
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

