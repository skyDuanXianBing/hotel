package server.demo.enums;

/**
 * 排序类型枚举
 */
public enum SortType {
    ROOM_TYPE("房型排序"),
    ROOM("房间排序"),
    GROUP("分组排序");

    private final String description;

    SortType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
