package server.demo.enums;

import server.demo.i18n.ApiMessages;

/**
 * 排序类型枚举
 */
public enum SortType {
    ROOM_TYPE("api.t.370ebdd8fd25"),
    ROOM("api.t.db772e3c32bd"),
    GROUP("api.t.2fe8d2baa5ba");

    private final String descriptionKey;

    SortType(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getDescription() {
        return ApiMessages.get(descriptionKey);
    }
}
