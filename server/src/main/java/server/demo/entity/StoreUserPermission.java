package server.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;

import java.time.LocalDateTime;

/**
 * 门店成员额外权限（叠加在角色权限之上）
 */
@Entity
@Table(
        name = "store_user_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_user_id", "module", "action", "room_type_id"}),
        indexes = @Index(name = "idx_store_user_permissions_store_user_id", columnList = "store_user_id")
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StoreUserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_user_id", nullable = false)
    private StoreUser storeUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PermissionModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private PermissionAction action;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId = 0L;

    @Column(name = "all_room_types", nullable = false)
    private Boolean allRoomTypes = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StoreUserPermission() {}

    public StoreUserPermission(StoreUser storeUser, PermissionModule module, PermissionAction action) {
        this.storeUser = storeUser;
        this.module = module;
        this.action = action;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StoreUser getStoreUser() {
        return storeUser;
    }

    public void setStoreUser(StoreUser storeUser) {
        this.storeUser = storeUser;
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
}

