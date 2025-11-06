package server.demo.enums;

public enum RoomStatus {
    AVAILABLE("可用"),
    OCCUPIED("已入住"),
    RESERVED("已预订"),
    MAINTENANCE("维修"),
    OUT_OF_ORDER("停用");

    private final String description;

    RoomStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}