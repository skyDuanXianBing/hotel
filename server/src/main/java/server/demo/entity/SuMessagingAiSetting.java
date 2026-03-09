package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "su_messaging_ai_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_su_messaging_ai_settings_store", columnNames = "store_id")
)
@EntityListeners(StoreScopedEntityListener.class)
public class SuMessagingAiSetting implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "auto_reply_enabled", nullable = false)
    private Boolean autoReplyEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (autoReplyEnabled == null) {
            autoReplyEnabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Boolean getAutoReplyEnabled() {
        return autoReplyEnabled;
    }

    public void setAutoReplyEnabled(Boolean autoReplyEnabled) {
        this.autoReplyEnabled = autoReplyEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
