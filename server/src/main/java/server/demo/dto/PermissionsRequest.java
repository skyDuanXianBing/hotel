package server.demo.dto;

import java.util.List;

/**
 * 权限设置请求DTO
 */
public class PermissionsRequest {
    private List<PermissionDTO> permissions;
    private List<Long> roomTypeIds; // 房型权限
    private Boolean allRoomTypes; // 是否拥有所有房型权限

    public PermissionsRequest() {}

    // Getters and Setters
    public List<PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
    }

    public List<Long> getRoomTypeIds() {
        return roomTypeIds;
    }

    public void setRoomTypeIds(List<Long> roomTypeIds) {
        this.roomTypeIds = roomTypeIds;
    }

    public Boolean getAllRoomTypes() {
        return allRoomTypes;
    }

    public void setAllRoomTypes(Boolean allRoomTypes) {
        this.allRoomTypes = allRoomTypes;
    }
}
