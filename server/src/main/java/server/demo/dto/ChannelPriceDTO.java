package server.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 渠道价格 DTO
 */
public class ChannelPriceDTO {

    private Long id;
    private Long storeId;
    private Long roomTypeId;
    private String roomTypeName;
    private Long pricePlanId;
    private String pricePlanName;
    private Long channelId;
    private String channelName;
    private String channelCode;
    private LocalDate priceDate;
    private BigDecimal basePrice;
    private BigDecimal channelPrice;
    private Integer minStay;
    private Integer maxStay;
    private Boolean isSyncedToOta;
    private LocalDateTime otaSyncAt;
    private LocalDateTime priceLabsUpdatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public ChannelPriceDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Long getPricePlanId() {
        return pricePlanId;
    }

    public void setPricePlanId(Long pricePlanId) {
        this.pricePlanId = pricePlanId;
    }

    public String getPricePlanName() {
        return pricePlanName;
    }

    public void setPricePlanName(String pricePlanName) {
        this.pricePlanName = pricePlanName;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
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
