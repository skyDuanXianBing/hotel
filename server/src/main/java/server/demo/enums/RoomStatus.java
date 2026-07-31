package server.demo.enums;

import server.demo.i18n.ApiMessages;

public enum RoomStatus {
    AVAILABLE("api.t.e91365cf9ed9"),
    OCCUPIED("api.t.e822694b11c0"),
    RESERVED("api.t.cdddae4919a3"),
    MAINTENANCE("api.t.2b83a4cb9ca5"),
    OUT_OF_ORDER("api.t.d989e55188c9");

    private final String descriptionKey;

    RoomStatus(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getDescription() {
        return ApiMessages.get(descriptionKey);
    }
}
