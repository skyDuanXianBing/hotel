package server.demo.util;

import server.demo.entity.Room;
import server.demo.entity.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import server.demo.i18n.ApiMessages;
/**
 * Su roomid 生成与校验工具。
 * <p>
 * 规则（方案 1）：roomid = {roomTypeId}-{roomNumber}
 * <p>
 * 约束：Su roomid 最大长度为 20 字符；超长时直接失败并给出清单，避免推送过程中产生数据割裂。
 */
public final class SuRoomIdUtil {

    public static final int MAX_SU_ROOM_ID_LENGTH = 20;

    private SuRoomIdUtil() {}

    public record TooLongRoomId(
            Long roomId,
            Long roomTypeId,
            String roomTypeName,
            String roomNumber,
            String suRoomId,
            int length
    ) {}

    public static String buildRoomId(Room room) {
        if (room == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.09f7f9543841"));
        }

        RoomType roomType = room.getRoomType();
        if (roomType == null || roomType.getId() == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.8d36940175a1"));
        }

        String roomNumber = normalizeRoomNumber(room.getRoomNumber());
        String suRoomId = roomType.getId() + "-" + roomNumber;
        if (suRoomId.length() > MAX_SU_ROOM_ID_LENGTH) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.0e80b6e5b3c8") + suRoomId);
        }
        return suRoomId;
    }

    public static String buildDisplayName(Room room) {
        if (room == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.09f7f9543841"));
        }

        RoomType roomType = room.getRoomType();
        if (roomType == null || roomType.getId() == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.e91aba90ec45"));
        }

        String roomTypeName = roomType.getName();
        String prefix = roomTypeName != null && !roomTypeName.isBlank()
                ? roomTypeName.trim()
                : String.valueOf(roomType.getId());
        return prefix;
    }

    public static void assertRoomIdsWithinLimit(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }

        List<TooLongRoomId> tooLong = new ArrayList<>();
        for (Room room : rooms) {
            if (room == null) {
                continue;
            }
            RoomType roomType = room.getRoomType();
            if (roomType == null || roomType.getId() == null) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.8d36940175a1"));
            }
            String roomNumber = normalizeRoomNumber(room.getRoomNumber());
            String suRoomId = roomType.getId() + "-" + roomNumber;
            if (suRoomId.length() > MAX_SU_ROOM_ID_LENGTH) {
                tooLong.add(new TooLongRoomId(
                        room.getId(),
                        roomType.getId(),
                        roomType.getName(),
                        roomNumber,
                        suRoomId,
                        suRoomId.length()
                ));
            }
        }

        if (!tooLong.isEmpty()) {
            throw new IllegalArgumentException(buildTooLongMessage(tooLong));
        }
    }

    private static String normalizeRoomNumber(String roomNumber) {
        if (roomNumber == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d1eaae8bb920"));
        }
        String trimmed = roomNumber.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d1eaae8bb920"));
        }
        return trimmed;
    }

    private static String buildTooLongMessage(List<TooLongRoomId> tooLong) {
        StringBuilder sb = new StringBuilder();
        sb.append(ApiMessages.get("api.t.6e3784cb5c82"));
        for (TooLongRoomId it : tooLong) {
            if (it == null) {
                continue;
            }
            sb.append(" [");
            sb.append(Objects.toString(it.roomTypeId(), "null"));
            sb.append("-");
            sb.append(Objects.toString(it.roomNumber(), "null"));
            sb.append(" => ");
            sb.append(Objects.toString(it.suRoomId(), "null"));
            sb.append(", len=");
            sb.append(it.length());
            if (it.roomId() != null) {
                sb.append(", roomId=").append(it.roomId());
            }
            if (it.roomTypeName() != null && !it.roomTypeName().isBlank()) {
                sb.append(", roomTypeName=").append(it.roomTypeName().trim());
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
