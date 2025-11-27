package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.ChannelType;
import server.demo.enums.PriceAdjustmentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "channels")
@EntityListeners(StoreScopedEntityListener.class)
public class Channel implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 门店ID（门店级架构）
     */
    @Column(name = "store_id")
    private Long storeId;

    @NotBlank(message = "渠道名称不能为空")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "渠道代码不能为空")
    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "commission_rate")
    private Double commissionRate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(length = 7)
    private String color = "#409EFF";

    @Column(name = "description", length = 500)
    private String description;

    @Column(length = 500)
    private String notes;

    // ==================== PriceLabs / OTA 集成字段 ====================

    /**
     * OTA API URL
     */
    @Column(name = "ota_api_url", length = 500)
    private String otaApiUrl;

    /**
     * OTA API Key
     */
    @Column(name = "ota_api_key", length = 255)
    private String otaApiKey;

    /**
     * OTA API Secret
     */
    @Column(name = "ota_api_secret", length = 255)
    private String otaApiSecret;

    /**
     * OTA 物业/房源 ID
     */
    @Column(name = "ota_property_id", length = 100)
    private String otaPropertyId;

    /**
     * 价格调整类型
     * COMMISSION: 基于佣金率计算（使用 commissionRate 字段）
     * FIXED: 固定金额调整
     * PERCENTAGE: 百分比调整
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "price_adjustment_type")
    private PriceAdjustmentType priceAdjustmentType = PriceAdjustmentType.PERCENTAGE;

    /**
     * 价格调整值
     * 正数表示加价（更贵），负数表示减价（更便宜）
     * 当 adjustmentType 为 PERCENTAGE 时，表示百分比（如 10 表示 +10%，-5 表示 -5%）
     * 当 adjustmentType 为 FIXED 时，表示固定金额
     */
    @Column(name = "price_adjustment_value", precision = 10, scale = 2)
    private BigDecimal priceAdjustmentValue;

    /**
     * 是否自动同步价格到此渠道
     */
    @Column(name = "auto_sync_price")
    private Boolean autoSyncPrice = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * @deprecated 已废弃，使用门店级架构，由storeId替代
     */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

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
    public Channel() {}

    public Channel(String name, String code, ChannelType type) {
        this.name = name;
        this.code = code;
        this.type = type;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    // ==================== PriceLabs / OTA 集成字段的 Getter/Setter ====================

    public String getOtaApiUrl() {
        return otaApiUrl;
    }

    public void setOtaApiUrl(String otaApiUrl) {
        this.otaApiUrl = otaApiUrl;
    }

    public String getOtaApiKey() {
        return otaApiKey;
    }

    public void setOtaApiKey(String otaApiKey) {
        this.otaApiKey = otaApiKey;
    }

    public String getOtaApiSecret() {
        return otaApiSecret;
    }

    public void setOtaApiSecret(String otaApiSecret) {
        this.otaApiSecret = otaApiSecret;
    }

    public String getOtaPropertyId() {
        return otaPropertyId;
    }

    public void setOtaPropertyId(String otaPropertyId) {
        this.otaPropertyId = otaPropertyId;
    }

    public PriceAdjustmentType getPriceAdjustmentType() {
        return priceAdjustmentType;
    }

    public void setPriceAdjustmentType(PriceAdjustmentType priceAdjustmentType) {
        this.priceAdjustmentType = priceAdjustmentType;
    }

    public BigDecimal getPriceAdjustmentValue() {
        return priceAdjustmentValue;
    }

    public void setPriceAdjustmentValue(BigDecimal priceAdjustmentValue) {
        this.priceAdjustmentValue = priceAdjustmentValue;
    }

    public Boolean getAutoSyncPrice() {
        return autoSyncPrice;
    }

    public void setAutoSyncPrice(Boolean autoSyncPrice) {
        this.autoSyncPrice = autoSyncPrice;
    }

    /**
     * 计算渠道价格
     * @param basePrice 基础价格
     * @return 渠道调整后的价格
     */
    public BigDecimal calculateChannelPrice(BigDecimal basePrice) {
        if (basePrice == null) {
            return null;
        }

        PriceAdjustmentType type = this.priceAdjustmentType;
        if (type == null) {
            type = PriceAdjustmentType.COMMISSION;
        }

        switch (type) {
            case COMMISSION:
                // 使用佣金率: 渠道价格 = 基础价格 × (1 + 佣金率)
                if (commissionRate != null) {
                    return basePrice.multiply(BigDecimal.valueOf(1 + commissionRate / 100));
                }
                return basePrice;

            case PERCENTAGE:
                // 百分比调整: 渠道价格 = 基础价格 × (1 + 百分比/100)
                if (priceAdjustmentValue != null) {
                    BigDecimal multiplier = BigDecimal.ONE.add(
                        priceAdjustmentValue.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)
                    );
                    return basePrice.multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
                }
                return basePrice;

            case FIXED:
                // 固定金额调整: 渠道价格 = 基础价格 + 固定金额
                if (priceAdjustmentValue != null) {
                    return basePrice.add(priceAdjustmentValue).setScale(2, java.math.RoundingMode.HALF_UP);
                }
                return basePrice;

            default:
                return basePrice;
        }
    }
}