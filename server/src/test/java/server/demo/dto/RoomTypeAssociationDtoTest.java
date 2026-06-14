package server.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.RoomStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomTypeAssociationDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void roomJson_shouldExposeStableRoomTypeFlatFields() {
        Room room = buildRoom();

        JsonNode json = objectMapper.valueToTree(room);

        assertEquals(9L, json.get("roomTypeId").asLong());
        assertEquals("Local Standard", json.get("roomTypeName").asText());
        assertEquals("STD", json.get("roomTypeCode").asText());
    }

    @Test
    void roomTypeWithRoomsItem_shouldExposeRoomTypeIdAndName() {
        RoomTypeWithRoomsDTO.RoomInfoDTO dto = new RoomTypeWithRoomsDTO.RoomInfoDTO(buildRoom());

        assertEquals(9L, dto.getRoomTypeId());
        assertEquals("Local Standard", dto.getRoomTypeName());
    }

    private Room buildRoom() {
        RoomType roomType = new RoomType();
        roomType.setId(9L);
        roomType.setName("Local Standard");
        roomType.setCode("STD");

        Room room = new Room();
        room.setId(101L);
        room.setRoomNumber("E2E-101");
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);
        return room;
    }
}
