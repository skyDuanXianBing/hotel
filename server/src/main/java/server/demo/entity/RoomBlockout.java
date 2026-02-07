package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.RoomBlockoutType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "room_blockouts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_blockouts_store_room_date",
                columnNames = {"store_id", "room_id", "block_date"}
        ),
        indexes = {
                @Index(name = "idx_room_blockouts_store_date", columnList = "store_id, block_date"),
                @Index(name = "idx_room_blockouts_room_date", columnList = "room_id, block_date")
        }
)
@EntityListeners(StoreScopedEntityListener.class)
public class RoomBlockout implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "block_date", nullable = false)
    private LocalDate blockDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private RoomBlockoutType blockType;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public RoomBlockout() {}

    public RoomBlockout(Long storeId, Room room, LocalDate blockDate, RoomBlockoutType blockType, String remark) {
        this.storeId = storeId;
        this.room = room;
        this.blockDate = blockDate;
        this.blockType = blockType;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getBlockDate() {
        return blockDate;
    }

    public void setBlockDate(LocalDate blockDate) {
        this.blockDate = blockDate;
    }

    public RoomBlockoutType getBlockType() {
        return blockType;
    }

    public void setBlockType(RoomBlockoutType blockType) {
        this.blockType = blockType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

