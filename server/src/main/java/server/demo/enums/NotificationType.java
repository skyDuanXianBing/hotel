package server.demo.enums;

/**
 * 通知类型枚举
 */
public enum NotificationType {
    SYSTEM("系统通知"),
    ORDER("订单提醒"),
    CLEANING("保洁通知"),
    TASK("任务待分配");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
