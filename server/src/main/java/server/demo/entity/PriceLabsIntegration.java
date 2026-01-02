package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.time.LocalDateTime;

/**
 * PriceLabs 集成配置实体（门店级）
 * 存储每个门店的 PriceLabs 集成配置信息
 */
@Entity
@Table(name = "pricelabs_integrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"store_id"}))
@EntityListeners(StoreScopedEntityListener.class)
public class PriceLabsIntegration implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 是否启用 PriceLabs 集成
     */
    @Column(name = "is_enabled")
    private Boolean isEnabled = false;

    /**
     * PriceLabs 账户邮箱 (user_token)
     * 用于标识在 PriceLabs 注册的账户
     */
    @Column(name = "pricelabs_email", length = 255)
    private String priceLabsEmail;

    /**
     * 价格同步回调 URL
     * PriceLabs 会将价格更新推送到此 URL
     */
    @Column(name = "sync_url", length = 500)
    private String syncUrl;

    /**
     * 日历触发回调 URL
     * PriceLabs 请求日历数据时会调用此 URL
     */
    @Column(name = "calendar_trigger_url", length = 500)
    private String calendarTriggerUrl;

    /**
     * 错误通知回调 URL
     */
    @Column(name = "hook_url", length = 500)
    private String hookUrl;

    /**
     * 最后一次房源同步时间
     */
    @Column(name = "last_listing_sync_at")
    private LocalDateTime lastListingSyncAt;

    /**
     * 最后一次价格同步时间（从 PriceLabs 接收）
     */
    @Column(name = "last_price_sync_at")
    private LocalDateTime lastPriceSyncAt;

    /**
     * 最后一次预订同步时间
     */
    @Column(name = "last_reservation_sync_at")
    private LocalDateTime lastReservationSyncAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public PriceLabsIntegration() {}

    public PriceLabsIntegration(Long storeId) {
        this.storeId = storeId;
    }

    // Getters and Setters
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

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getPriceLabsEmail() {
        return priceLabsEmail;
    }

    public void setPriceLabsEmail(String priceLabsEmail) {
        this.priceLabsEmail = priceLabsEmail;
    }

    public String getSyncUrl() {
        return syncUrl;
    }

    public void setSyncUrl(String syncUrl) {
        this.syncUrl = syncUrl;
    }

    public String getCalendarTriggerUrl() {
        return calendarTriggerUrl;
    }

    public void setCalendarTriggerUrl(String calendarTriggerUrl) {
        this.calendarTriggerUrl = calendarTriggerUrl;
    }

    public String getHookUrl() {
        return hookUrl;
    }

    public void setHookUrl(String hookUrl) {
        this.hookUrl = hookUrl;
    }

    public LocalDateTime getLastListingSyncAt() {
        return lastListingSyncAt;
    }

    public void setLastListingSyncAt(LocalDateTime lastListingSyncAt) {
        this.lastListingSyncAt = lastListingSyncAt;
    }

    public LocalDateTime getLastPriceSyncAt() {
        return lastPriceSyncAt;
    }

    public void setLastPriceSyncAt(LocalDateTime lastPriceSyncAt) {
        this.lastPriceSyncAt = lastPriceSyncAt;
    }

    public LocalDateTime getLastReservationSyncAt() {
        return lastReservationSyncAt;
    }

    public void setLastReservationSyncAt(LocalDateTime lastReservationSyncAt) {
        this.lastReservationSyncAt = lastReservationSyncAt;
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
