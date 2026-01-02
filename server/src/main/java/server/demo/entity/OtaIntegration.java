package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.PriceAdjustmentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OTA直连配置实体
 * 用于管理OTA平台（如Airbnb、Booking.com）的直连配置
 */
@Entity
@Table(name = "ota_integrations")
@EntityListeners(StoreScopedEntityListener.class)
public class OtaIntegration implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 门店ID（门店级架构）
     */
    @Column(name = "store_id")
    private Long storeId;

    /**
     * OTA渠道名称（如 Airbnb, Booking.com）
     */
    @NotBlank(message = "渠道名称不能为空")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * OTA渠道代码（如 AIRBNB, BOOKING）
     */
    @NotBlank(message = "渠道代码不能为空")
    @Column(nullable = false, length = 50)
    private String code;

    /**
     * Logo地址
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /**
     * 是否已连接
     */
    @Column(name = "is_connected")
    private Boolean isConnected = false;

    /**
     * API地址
     */
    @Column(name = "api_url", length = 500)
    private String apiUrl;

    /**
     * API Key
     */
    @Column(name = "api_key", length = 255)
    private String apiKey;

    /**
     * API Secret
     */
    @Column(name = "api_secret", length = 255)
    private String apiSecret;

    /**
     * 物业/房源ID
     */
    @Column(name = "property_id", length = 100)
    private String propertyId;

    /**
     * 是否启用
     */
    @Column(name = "enabled")
    private Boolean enabled = true;

    // ==================== 价格调整字段 ====================

    /**
     * 价格调整类型
     * PERCENTAGE: 百分比调整
     * FIXED: 固定金额调整
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

    /**
     * 默认价格计划
     * 用于推送到 OTA 时选择使用哪个价格计划的价格
     * 如果为 null，则使用房型的默认价格计划
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_price_plan_id")
    private PricePlan defaultPricePlan;

    // ==================== Su Channel Manager 集成字段 ====================

    /**
     * Su平台的property ID (加密后的ID)
     * 格式: STORE{storeId}（默认；具体可由 stores.su_hotel_id 每门店单独配置）
     */
    @Column(name = "su_property_id", length = 200)
    private String suPropertyId;

    /**
     * Su Widget Token (用于加载Widget界面)
     * 此token会过期,每次打开Widget时重新生成
     */
    @Column(name = "su_widget_token", length = 500)
    private String suWidgetToken;

    /**
     * Su Widget Token过期时间
     */
    @Column(name = "su_token_expires_at")
    private LocalDateTime suTokenExpiresAt;

    /**
     * 连接到Su的时间
     */
    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

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
    public OtaIntegration() {}

    public OtaIntegration(String name, String code) {
        this.name = name;
        this.code = code;
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

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Boolean getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(Boolean isConnected) {
        this.isConnected = isConnected;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

    // ==================== 价格调整字段的 Getter/Setter ====================

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

    public PricePlan getDefaultPricePlan() {
        return defaultPricePlan;
    }

    public void setDefaultPricePlan(PricePlan defaultPricePlan) {
        this.defaultPricePlan = defaultPricePlan;
    }

    // ==================== Su Channel Manager 字段的 Getter/Setter ====================

    public String getSuPropertyId() {
        return suPropertyId;
    }

    public void setSuPropertyId(String suPropertyId) {
        this.suPropertyId = suPropertyId;
    }

    public String getSuWidgetToken() {
        return suWidgetToken;
    }

    public void setSuWidgetToken(String suWidgetToken) {
        this.suWidgetToken = suWidgetToken;
    }

    public LocalDateTime getSuTokenExpiresAt() {
        return suTokenExpiresAt;
    }

    public void setSuTokenExpiresAt(LocalDateTime suTokenExpiresAt) {
        this.suTokenExpiresAt = suTokenExpiresAt;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
}
