package server.demo.dto;

import server.demo.enums.PriceAdjustmentType;

import java.math.BigDecimal;

/**
 * 渠道价格调整 DTO
 * 用于前端编辑渠道的价格调整设置
 */
public class ChannelPriceAdjustmentDTO {

    private Long channelId;
    private String channelName;
    private String channelCode;
    private PriceAdjustmentType adjustmentType;
    private BigDecimal adjustmentValue;
    private Boolean autoSyncPrice;

    // 用于显示的计算示例
    private BigDecimal exampleBasePrice;
    private BigDecimal exampleChannelPrice;

    // Constructors
    public ChannelPriceAdjustmentDTO() {}

    public ChannelPriceAdjustmentDTO(Long channelId, String channelName, String channelCode,
                                      PriceAdjustmentType adjustmentType, BigDecimal adjustmentValue) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelCode = channelCode;
        this.adjustmentType = adjustmentType;
        this.adjustmentValue = adjustmentValue;
    }

    // Getters and Setters
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

    public PriceAdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(PriceAdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public BigDecimal getAdjustmentValue() {
        return adjustmentValue;
    }

    public void setAdjustmentValue(BigDecimal adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }

    public Boolean getAutoSyncPrice() {
        return autoSyncPrice;
    }

    public void setAutoSyncPrice(Boolean autoSyncPrice) {
        this.autoSyncPrice = autoSyncPrice;
    }

    public BigDecimal getExampleBasePrice() {
        return exampleBasePrice;
    }

    public void setExampleBasePrice(BigDecimal exampleBasePrice) {
        this.exampleBasePrice = exampleBasePrice;
    }

    public BigDecimal getExampleChannelPrice() {
        return exampleChannelPrice;
    }

    public void setExampleChannelPrice(BigDecimal exampleChannelPrice) {
        this.exampleChannelPrice = exampleChannelPrice;
    }
}
