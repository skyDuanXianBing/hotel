package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 渠道价格实体
 * 存储各渠道调整后的价格
 */
@Entity
@Table(name = "channel_prices",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"store_id", "room_type_id", "channel_id", "price_date"}),
       indexes = {
           @Index(name = "idx_channel_price_room_type_date", columnList = "room_type_id, price_date"),
           @Index(name = "idx_channel_price_channel_date", columnList = "channel_id, price_date"),
           @Index(name = "idx_channel_price_sync_status", columnList = "is_synced_to_ota")
       })
@EntityListeners(StoreScopedEntityListener.class)
public class ChannelPrice implements StoreScopedEntity {

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
     * 关联的渠道
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    /**
     * 价格日期
     */
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    /**
     * PriceLabs 推送的基础价格
     */
    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;

    /**
     * 渠道调整后的售价
     * = basePrice × (1 + channel.priceAdjustmentValue/100) 或
     * = basePrice + channel.priceAdjustmentValue
     */
    @Column(name = "channel_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal channelPrice;

    /**
     * 最小入住天数
     */
    @Column(name = "min_stay")
    private Integer minStay;

    /**
     * 最大入住天数
     */
    @Column(name = "max_stay")
    private Integer maxStay;

    /**
     * 是否已同步到 OTA
     */
    @Column(name = "is_synced_to_ota")
    private Boolean isSyncedToOta = false;

    /**
     * OTA 同步时间
     */
    @Column(name = "ota_sync_at")
    private LocalDateTime otaSyncAt;

    /**
     * PriceLabs 更新时间
     */
    @Column(name = "pricelabs_updated_at")
    private LocalDateTime priceLabsUpdatedAt;

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
    public ChannelPrice() {}

    public ChannelPrice(RoomType roomType, Channel channel, LocalDate priceDate,
                        BigDecimal basePrice, BigDecimal channelPrice) {
        this.roomType = roomType;
        this.channel = channel;
        this.priceDate = priceDate;
        this.basePrice = basePrice;
        this.channelPrice = channelPrice;
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

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getChannelPrice() {
        return channelPrice;
    }

    public void setChannelPrice(BigDecimal channelPrice) {
        this.channelPrice = channelPrice;
    }

    public Integer getMinStay() {
        return minStay;
    }

    public void setMinStay(Integer minStay) {
        this.minStay = minStay;
    }

    public Integer getMaxStay() {
        return maxStay;
    }

    public void setMaxStay(Integer maxStay) {
        this.maxStay = maxStay;
    }

    public Boolean getIsSyncedToOta() {
        return isSyncedToOta;
    }

    public void setIsSyncedToOta(Boolean isSyncedToOta) {
        this.isSyncedToOta = isSyncedToOta;
    }

    public LocalDateTime getOtaSyncAt() {
        return otaSyncAt;
    }

    public void setOtaSyncAt(LocalDateTime otaSyncAt) {
        this.otaSyncAt = otaSyncAt;
    }

    public LocalDateTime getPriceLabsUpdatedAt() {
        return priceLabsUpdatedAt;
    }

    public void setPriceLabsUpdatedAt(LocalDateTime priceLabsUpdatedAt) {
        this.priceLabsUpdatedAt = priceLabsUpdatedAt;
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
