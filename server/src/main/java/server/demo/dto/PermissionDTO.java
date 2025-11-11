package server.demo.dto;

import server.demo.enums.PermissionModule;
import server.demo.enums.PermissionAction;

/**
 * 权限DTO
 */
public class PermissionDTO {
    private Long id;
    private PermissionModule module;
    private PermissionAction action;
    private Long roomTypeId;
    private String roomTypeName;
    private Boolean allRoomTypes;

    public PermissionDTO() {}

    public PermissionDTO(PermissionModule module, PermissionAction action) {
        this.module = module;
        this.action = action;
    }

    public PermissionDTO(PermissionModule module, PermissionAction action, Boolean allRoomTypes) {
        this.module = module;
        this.action = action;
        this.allRoomTypes = allRoomTypes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PermissionModule getModule() {
        return module;
    }

    public void setModule(PermissionModule module) {
        this.module = module;
    }

    public PermissionAction getAction() {
        return action;
    }

    public void setAction(PermissionAction action) {
        this.action = action;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Boolean getAllRoomTypes() {
        return allRoomTypes;
    }

    public void setAllRoomTypes(Boolean allRoomTypes) {
        this.allRoomTypes = allRoomTypes;
    }
}
