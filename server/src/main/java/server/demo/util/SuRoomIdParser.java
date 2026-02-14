package server.demo.util;

/**
 * 解析 Su/IT Provider 回传的 rooms[].id（我们推送的 roomid）。
 * <p>
 * 当前项目约定：roomid = {roomTypeId}-{roomNumber}
 * <p>
 * 注意：roomNumber 本身可能包含 '-'，因此只按第一个 '-' 分割。
 */
public final class SuRoomIdParser {

    private SuRoomIdParser() {}

    public record ParsedRoomId(Long roomTypeId, String roomNumber, String raw) {}

    public static ParsedRoomId parse(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        // RoomType-level format: "21"
        if (trimmed.indexOf('-') < 0) {
            try {
                Long roomTypeId = Long.parseLong(trimmed);
                return new ParsedRoomId(roomTypeId, null, trimmed);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        int idx = trimmed.indexOf('-');
        if (idx <= 0 || idx >= trimmed.length() - 1) {
            return null;
        }
        String left = trimmed.substring(0, idx).trim();
        String right = trimmed.substring(idx + 1).trim();
        if (left.isBlank() || right.isBlank()) {
            return null;
        }
        Long roomTypeId;
        try {
            roomTypeId = Long.parseLong(left);
        } catch (NumberFormatException e) {
            return null;
        }
        return new ParsedRoomId(roomTypeId, right, trimmed);
    }
}

