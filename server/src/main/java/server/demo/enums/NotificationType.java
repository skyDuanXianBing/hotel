package server.demo.enums;

import server.demo.i18n.ApiMessages;

/**
 * 通知类型枚举
 */
public enum NotificationType {
    SYSTEM("api.t.b7575d6f5557"),
    ORDER("api.t.2801f5fb25b6"),
    CLEANING("api.t.060198af05ea"),
    TASK("api.t.0116bb1fadab");

    private final String descriptionKey;

    NotificationType(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getDescription() {
        return ApiMessages.get(descriptionKey);
    }
}
