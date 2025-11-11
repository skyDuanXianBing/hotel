package server.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import server.demo.enums.PermissionModule;
import server.demo.enums.PermissionAction;
import java.time.LocalDateTime;

/**
 * 角色权限关联实体类
 */
@Entity
@Table(name = "role_permissions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "module", "action"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnoreProperties("rolePermissions")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PermissionModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private PermissionAction action;

    @Column(name = "room_type_id")
    private Long roomTypeId; // 房型ID,null表示所有房型

    @Column(name = "all_room_types", nullable = false)
    private Boolean allRoomTypes = false; // 是否拥有所有房型权限

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RolePermission() {}

    public RolePermission(Role role, PermissionModule module, PermissionAction action) {
        this.role = role;
        this.module = module;
        this.action = action;
    }

    public RolePermission(Role role, PermissionModule module, PermissionAction action, Long roomTypeId) {
        this.role = role;
        this.module = module;
        this.action = action;
        this.roomTypeId = roomTypeId;
    }

    public RolePermission(Role role, PermissionModule module, PermissionAction action, Boolean allRoomTypes) {
        this.role = role;
        this.module = module;
        this.action = action;
        this.allRoomTypes = allRoomTypes;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (allRoomTypes == null) {
            allRoomTypes = false;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public Boolean getAllRoomTypes() {
        return allRoomTypes;
    }

    public void setAllRoomTypes(Boolean allRoomTypes) {
        this.allRoomTypes = allRoomTypes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RolePermission{" +
                "id=" + id +
                ", module=" + module +
                ", action=" + action +
                ", roomTypeId=" + roomTypeId +
                ", allRoomTypes=" + allRoomTypes +
                ", createdAt=" + createdAt +
                '}';
    }
}
