package server.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class OpenRoomBlockoutRequest {

    @NotEmpty(message = "roomIds 不能为空")
    private List<Long> roomIds;

    @NotNull(message = "startDate 不能为空")
    private LocalDate startDate;

    @NotNull(message = "endDate 不能为空")
    private LocalDate endDate;

    public List<Long> getRoomIds() {
        return roomIds;
    }

    public void setRoomIds(List<Long> roomIds) {
        this.roomIds = roomIds;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}

