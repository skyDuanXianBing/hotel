package server.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 房间分组成员DTO(用于批量操作)
 */
public class RoomGroupMemberDTO {

    @NotEmpty(message = "房间ID列表不能为空")
    private List<Long> roomIds;

    // Constructors
    public RoomGroupMemberDTO() {}

    public RoomGroupMemberDTO(List<Long> roomIds) {
        this.roomIds = roomIds;
    }

    // Getters and Setters
    public List<Long> getRoomIds() {
        return roomIds;
    }

    public void setRoomIds(List<Long> roomIds) {
        this.roomIds = roomIds;
    }
}
