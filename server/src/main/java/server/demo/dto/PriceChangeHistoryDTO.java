package server.demo.dto;

import java.math.BigDecimal;

/**
 * 改价历史记录DTO
 */
public class PriceChangeHistoryDTO {
    private Long id;
    private String roomTypeName;
    private String pricePlanName;
    private String priceDate; // 价格日期范围,格式: "2025/01/01至2025/01/07"
    private String applyDays; // 适用周几,如: "全部" 或 "周一,周二"
    private String changeType; // 修改类型,如: "价格"
    private BigDecimal changeValue; // 修改后的值
    private BigDecimal previousValue; // 修改前的值
    private String operator; // 操作人
    private String operateTime; // 操作时间,格式: "2025/01/01 10:00:00"
    private String pmsPushTime; // PMS推送时间
    private String notes;

    // Constructors
    public PriceChangeHistoryDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public String getPricePlanName() {
        return pricePlanName;
    }

    public void setPricePlanName(String pricePlanName) {
        this.pricePlanName = pricePlanName;
    }

    public String getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(String priceDate) {
        this.priceDate = priceDate;
    }

    public String getApplyDays() {
        return applyDays;
    }

    public void setApplyDays(String applyDays) {
        this.applyDays = applyDays;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public BigDecimal getChangeValue() {
        return changeValue;
    }

    public void setChangeValue(BigDecimal changeValue) {
        this.changeValue = changeValue;
    }

    public BigDecimal getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(BigDecimal previousValue) {
        this.previousValue = previousValue;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(String operateTime) {
        this.operateTime = operateTime;
    }

    public String getPmsPushTime() {
        return pmsPushTime;
    }

    public void setPmsPushTime(String pmsPushTime) {
        this.pmsPushTime = pmsPushTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
