package server.demo.enums;

public enum ChannelType {
    DIRECT("直销"),
    OTA("在线旅行社"),
    TRAVEL_AGENCY("旅行社"),
    CORPORATE("企业客户");

    private final String description;

    ChannelType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}