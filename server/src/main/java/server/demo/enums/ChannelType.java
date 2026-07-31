package server.demo.enums;

import server.demo.i18n.ApiMessages;

public enum ChannelType {
    DIRECT("api.t.b4451b961899"),
    OTA("api.t.f2e9e5295329"),
    TRAVEL_AGENCY("api.t.5b943f50bca8"),
    CORPORATE("api.t.8035a71757dd");

    private final String descriptionKey;

    ChannelType(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getDescription() {
        return ApiMessages.get(descriptionKey);
    }
}
