package server.demo.enums;

public enum ReservationStatus {
    CONFIRMED("已确认"),
    CHECKED_IN("已入住"),
    CHECKED_OUT("已退房"),
    CANCELLED("已取消"),
    NO_SHOW("未到");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}