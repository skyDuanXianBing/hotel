package server.demo.dto;

import server.demo.enums.PriceAdjustmentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OTA直连配置数据传输对象
 */
public class OtaIntegrationDTO {

    private Long id;
    private String name;
    private String code;
    private String logoUrl;
    private Boolean isConnected;
    private String apiUrl;
    private String propertyId;
    private Boolean enabled;
    private PriceAdjustmentType priceAdjustmentType;
    private BigDecimal priceAdjustmentValue;
    private Boolean autoSyncPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public OtaIntegrationDTO() {}

    public OtaIntegrationDTO(Long id, String name, String code, String logoUrl, Boolean isConnected) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.logoUrl = logoUrl;
        this.isConnected = isConnected;
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
}
