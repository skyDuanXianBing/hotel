package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(name = "managed_operation_rooms")
public class ManagedOperationRoom implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "store_id", nullable = false)
    private Long storeId;
    @ManyToOne(optional = false)
    @JoinColumn(name = "settings_id", nullable = false)
    private ManagedOperationSettings settings;
    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    @Override public Long getStoreId() { return storeId; }
    @Override public void setStoreId(Long storeId) { this.storeId = storeId; }
    public ManagedOperationSettings getSettings() { return settings; }
    public void setSettings(ManagedOperationSettings settings) { this.settings = settings; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
}
