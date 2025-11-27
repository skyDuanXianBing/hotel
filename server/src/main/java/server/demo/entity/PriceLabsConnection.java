package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.time.LocalDateTime;

/**
 * PriceLabs 连接配置实体
 * 存储房型+价格计划与 PriceLabs Listing 的映射关系
 */
@Entity
@Table(name = "pricelabs_connections",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"store_id", "room_type_id", "price_plan_id"}))
@EntityListeners(StoreScopedEntityListener.class)
public class PriceLabsConnection implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 关联的房型
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    /**
     * 关联的价格计划
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_plan_id", nullable = false)
    private PricePlan pricePlan;

    /**
     * PriceLabs 中的 listing_id
     * 格式: store_{storeId}_room_type_{roomTypeId}_plan_{pricePlanId}
     */
    @Column(name = "pricelabs_listing_id", length = 100)
    private String priceLabsListingId;

    /**
     * 是否启用此连接
     */
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    /**
     * 最后同步时间
     */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    /**
     * 同步状态: connected, disconnected, error
     */
    @Column(name = "sync_status", length = 20)
    private String syncStatus = "disconnected";

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // 自动生成 PriceLabs listing_id
        if (priceLabsListingId == null && roomType != null && pricePlan != null) {
            generatePriceLabsListingId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 生成 PriceLabs listing_id
     */
    public void generatePriceLabsListingId() {
        if (storeId != null && roomType != null && pricePlan != null) {
            this.priceLabsListingId = String.format("store_%d_rt_%d_plan_%d",
                storeId, roomType.getId(), pricePlan.getId());
        }
    }

    // Constructors
    public PriceLabsConnection() {}

    public PriceLabsConnection(RoomType roomType, PricePlan pricePlan) {
        this.roomType = roomType;
        this.pricePlan = pricePlan;
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

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public PricePlan getPricePlan() {
        return pricePlan;
    }

    public void setPricePlan(PricePlan pricePlan) {
        this.pricePlan = pricePlan;
    }

    public String getPriceLabsListingId() {
        return priceLabsListingId;
    }

    public void setPriceLabsListingId(String priceLabsListingId) {
        this.priceLabsListingId = priceLabsListingId;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
