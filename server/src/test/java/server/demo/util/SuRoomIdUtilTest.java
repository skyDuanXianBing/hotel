package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.entity.Room;
import server.demo.entity.RoomType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuRoomIdUtilTest {

    @Test
    void buildRoomId_andDisplayName_shouldUseRoomTypeAndRoomNumber() {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("Deluxe");

        Room room = new Room();
        room.setId(10L);
        room.setRoomType(roomType);
        room.setRoomNumber(" 101 ");

        assertEquals("1-101", SuRoomIdUtil.buildRoomId(room));
        assertEquals("Deluxe", SuRoomIdUtil.buildDisplayName(room));
    }

    @Test
    void buildDisplayName_shouldFallbackToRoomTypeIdWhenNameBlank() {
        RoomType roomType = new RoomType();
        roomType.setId(99L);
        roomType.setName("  ");

        Room room = new Room();
        room.setId(10L);
        room.setRoomType(roomType);
        room.setRoomNumber("101");

        assertEquals("99", SuRoomIdUtil.buildDisplayName(room));
    }

    @Test
    void assertRoomIdsWithinLimit_shouldThrowWithTooLongList() {
        RoomType roomType = new RoomType();
        roomType.setId(Long.parseLong("1234567890123456789"));
        roomType.setName("RT");

        Room room = new Room();
        room.setId(1L);
        room.setRoomType(roomType);
        room.setRoomNumber("1");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SuRoomIdUtil.assertRoomIdsWithinLimit(List.of(room))
        );
        assertTrue(ex.getMessage().contains("超过 20 字符"));
        assertTrue(ex.getMessage().contains("1234567890123456789-1"));
        assertTrue(ex.getMessage().contains("len=21"));
    }
}
