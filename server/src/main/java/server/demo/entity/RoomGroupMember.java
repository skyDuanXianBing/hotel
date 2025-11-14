package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

/**
 * 房间分组成员实体(房间与分组的关联关系)
 */
@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(name = "room_group_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "group_id", "room_id"}))
public class RoomGroupMember implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "分组ID不能为空")
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @NotNull(message = "房间ID不能为空")
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public RoomGroupMember() {}

    public RoomGroupMember(Long groupId, Long roomId) {
        this.groupId = groupId;
        this.roomId = roomId;
    }

    public RoomGroupMember(Long groupId, Long roomId, Long storeId) {
        this.groupId = groupId;
        this.roomId = roomId;
        this.storeId = storeId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
