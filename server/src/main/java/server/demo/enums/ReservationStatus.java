package server.demo.enums;

import server.demo.i18n.ApiMessages;

public enum ReservationStatus {
    REQUESTED("api.t.e73bb89b6633"),
    CONFIRMED("api.t.d9fea67ad2be"),
    CHECKED_IN("api.t.e822694b11c0"),
    CHECKED_OUT("api.t.55ecbad1f909"),
    CANCELLED("api.t.a5ffdc95eeb0"),
    NO_SHOW("api.t.1814bd3efb93");

    private final String descriptionKey;

    ReservationStatus(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getDescription() {
        return ApiMessages.get(descriptionKey);
    }
}
