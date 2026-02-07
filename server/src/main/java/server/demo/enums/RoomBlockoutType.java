package server.demo.enums;

public enum RoomBlockoutType {
    STOP,
    MAINTENANCE,
    RETAIN;

    public static RoomBlockoutType fromUiValue(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if (t.isEmpty()) {
            return null;
        }
        return switch (t.toLowerCase()) {
            case "stop" -> STOP;
            case "maintenance" -> MAINTENANCE;
            case "retain" -> RETAIN;
            default -> null;
        };
    }

    public String toUiValue() {
        return switch (this) {
            case STOP -> "stop";
            case MAINTENANCE -> "maintenance";
            case RETAIN -> "retain";
        };
    }
}

