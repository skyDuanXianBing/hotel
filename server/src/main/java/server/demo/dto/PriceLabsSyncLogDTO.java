package server.demo.dto;

import server.demo.enums.SyncDirection;
import server.demo.enums.SyncStatus;
import server.demo.enums.SyncType;

import java.time.LocalDateTime;

import server.demo.i18n.ApiMessages;
/**
 * PriceLabs 同步日志 DTO
 */
public class PriceLabsSyncLogDTO {

    private Long id;
    private Long storeId;
    private SyncType syncType;
    private SyncDirection direction;
    private SyncStatus status;
    private Integer affectedCount;
    private String errorMessage;
    private String requestData;
    private String responseData;
    private LocalDateTime createdAt;

    // 显示用的中文描述
    private String syncTypeDisplay;
    private String directionDisplay;
    private String statusDisplay;

    // Constructors
    public PriceLabsSyncLogDTO() {}

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

    public SyncType getSyncType() {
        return syncType;
    }

    public void setSyncType(SyncType syncType) {
        this.syncType = syncType;
        this.syncTypeDisplay = getSyncTypeDisplayText(syncType);
    }

    public SyncDirection getDirection() {
        return direction;
    }

    public void setDirection(SyncDirection direction) {
        this.direction = direction;
        this.directionDisplay = getDirectionDisplayText(direction);
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
        this.statusDisplay = getStatusDisplayText(status);
    }

    public Integer getAffectedCount() {
        return affectedCount;
    }

    public void setAffectedCount(Integer affectedCount) {
        this.affectedCount = affectedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSyncTypeDisplay() {
        return syncTypeDisplay;
    }

    public void setSyncTypeDisplay(String syncTypeDisplay) {
        this.syncTypeDisplay = syncTypeDisplay;
    }

    public String getDirectionDisplay() {
        return directionDisplay;
    }

    public void setDirectionDisplay(String directionDisplay) {
        this.directionDisplay = directionDisplay;
    }

    public String getStatusDisplay() {
        return statusDisplay;
    }

    public void setStatusDisplay(String statusDisplay) {
        this.statusDisplay = statusDisplay;
    }

    // Helper methods for display text
    private String getSyncTypeDisplayText(SyncType type) {
        if (type == null) return "";
        switch (type) {
            case LISTING: return ApiMessages.get("api.t.a45305bfebc8");
            case RATE_PLAN: return ApiMessages.get("api.t.5f1fa3159043");
            case CALENDAR: return ApiMessages.get("api.t.fa0bc354f237");
            case RESERVATION: return ApiMessages.get("api.t.2c7f056889ef");
            case PRICE_UPDATE: return ApiMessages.get("api.t.3f799ab84162");
            default: return type.name();
        }
    }

    private String getDirectionDisplayText(SyncDirection direction) {
        if (direction == null) return "";
        switch (direction) {
            case OUTBOUND: return ApiMessages.get("api.t.a71772f65f62");
            case INBOUND: return ApiMessages.get("api.t.de7b4c9eebf8");
            default: return direction.name();
        }
    }

    private String getStatusDisplayText(SyncStatus status) {
        if (status == null) return "";
        switch (status) {
            case SUCCESS: return ApiMessages.get("api.t.51991a5d111a");
            case FAILURE: return ApiMessages.get("api.t.3e3c8068bb0e");
            case PARTIAL: return ApiMessages.get("api.t.2f2b7e05e3ea");
            default: return status.name();
        }
    }
}
