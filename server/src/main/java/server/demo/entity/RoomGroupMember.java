package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 房间分组成员实体(房间与分组的关联关系)
 */
@Entity
@Table(name = "room_group_members")
public class RoomGroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "分组ID不能为空")
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @NotNull(message = "房间ID不能为空")
    @Column(name = "room_id", nullable = false)
    private Long roomId;

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
}
