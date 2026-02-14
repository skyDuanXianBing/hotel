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
       uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "module", "action", "room_type_id"}))
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

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId = 0L; // 房型ID: 0表示不指定房型(全部/不需要房型范围)

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
        if (roomTypeId == null) {
            roomTypeId = 0L;
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
