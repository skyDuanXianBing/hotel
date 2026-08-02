package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.demo.enums.PushPlatform;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;

/**
 * 移动设备推送令牌实体（APNs / 预留 FCM）。
 * 一台设备一条记录，按 device_token 唯一；换账号登录会更新归属用户/门店。
 */
@Entity
@Table(name = "push_device_tokens", indexes = {
        @Index(name = "idx_push_device_user", columnList = "user_id,enabled"),
        @Index(name = "idx_push_device_store", columnList = "store_id")
})
public class PushDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "{api.t.855ea23e7df8}")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "{api.t.db3bc300a9da}")
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @NotNull(message = "{api.t.c4179d74f0ba}")
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private PushPlatform platform;

    @NotBlank(message = "{api.t.8a30ccf0610b}")
    @Column(name = "device_token", nullable = false, unique = true, length = 512)
    private String deviceToken;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UtcTimeUtil.nowLocalDateTime();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public PushPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(PushPlatform platform) {
        this.platform = platform;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
