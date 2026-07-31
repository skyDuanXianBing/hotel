package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class UpsertRoomBlockoutRequest {

    @NotEmpty(message = "{api.t.db75975a148a}")
    private List<Long> roomIds;

    @NotNull(message = "{api.t.14c70c4c09a7}")
    private LocalDate startDate;

    @NotNull(message = "{api.t.4210bb798cc5}")
    private LocalDate endDate;

    @NotBlank(message = "{api.t.2b871f57db24}")
    private String type;

    private String remark;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

