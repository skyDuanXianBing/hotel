package server.demo.dto;

public class RoomBlockoutSummaryDTO {
    private long affectedDays;

    public RoomBlockoutSummaryDTO() {}

    public RoomBlockoutSummaryDTO(long affectedDays) {
        this.affectedDays = affectedDays;
    }

    public long getAffectedDays() {
        return affectedDays;
    }

    public void setAffectedDays(long affectedDays) {
        this.affectedDays = affectedDays;
    }
}

