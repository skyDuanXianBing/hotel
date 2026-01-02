package server.demo.util;

import server.demo.entity.Room;
import server.demo.entity.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            throw new IllegalArgumentException("房间不能为空");
        }

        RoomType roomType = room.getRoomType();
        if (roomType == null || roomType.getId() == null) {
            throw new IllegalArgumentException("生成 Su roomid 需要 roomTypeId（请检查 Room 是否已加载 RoomType）");
        }

        String roomNumber = normalizeRoomNumber(room.getRoomNumber());
        String suRoomId = roomType.getId() + "-" + roomNumber;
        if (suRoomId.length() > MAX_SU_ROOM_ID_LENGTH) {
            throw new IllegalArgumentException("Su roomid 超过 20 字符: " + suRoomId);
        }
        return suRoomId;
    }

    public static String buildDisplayName(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("房间不能为空");
        }

        RoomType roomType = room.getRoomType();
        if (roomType == null || roomType.getId() == null) {
            throw new IllegalArgumentException("生成显示名需要 roomTypeId（请检查 Room 是否已加载 RoomType）");
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
                throw new IllegalArgumentException("生成 Su roomid 需要 roomTypeId（请检查 Room 是否已加载 RoomType）");
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
            throw new IllegalArgumentException("房间号不能为空");
        }
        String trimmed = roomNumber.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("房间号不能为空");
        }
        return trimmed;
    }

    private static String buildTooLongMessage(List<TooLongRoomId> tooLong) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下房间生成的 Su roomid 超过 20 字符（规则：roomTypeId-roomNumber），请回到 PMS 缩短房型标识/房间号：");
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
