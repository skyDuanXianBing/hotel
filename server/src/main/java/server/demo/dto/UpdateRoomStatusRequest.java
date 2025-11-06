package server.demo.dto;

import jakarta.validation.constraints.NotNull;
import server.demo.enums.RoomStatus;
import java.time.LocalDate;

public class UpdateRoomStatusRequest {
    
    @NotNull(message = "日期不能为空")
    private LocalDate date;
    
    @NotNull(message = "房间状态不能为空")
    private RoomStatus status;
    
    private String reason;

    public UpdateRoomStatusRequest() {}

    public UpdateRoomStatusRequest(LocalDate date, RoomStatus status, String reason) {
        this.date = date;
        this.status = status;
        this.reason = reason;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}