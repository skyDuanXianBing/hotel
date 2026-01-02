package server.demo.dto;

import java.time.LocalDateTime;

/**
 * PriceLabs 集成配置 DTO
 */
public class PriceLabsIntegrationDTO {

    private Long id;
    private Long storeId;
    private Boolean isEnabled;
    private String priceLabsEmail;
    private String syncUrl;
    private String calendarTriggerUrl;
    private String hookUrl;
    private LocalDateTime lastListingSyncAt;
    private LocalDateTime lastPriceSyncAt;
    private LocalDateTime lastReservationSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 附加统计信息
    private Long connectedRoomTypeCount;
    private Long totalSyncCount;
    private Long successSyncCount;

    // Constructors
    public PriceLabsIntegrationDTO() {}

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

    public Long getConnectedRoomTypeCount() {
        return connectedRoomTypeCount;
    }

    public void setConnectedRoomTypeCount(Long connectedRoomTypeCount) {
        this.connectedRoomTypeCount = connectedRoomTypeCount;
    }

    public Long getTotalSyncCount() {
        return totalSyncCount;
    }

    public void setTotalSyncCount(Long totalSyncCount) {
        this.totalSyncCount = totalSyncCount;
    }

    public Long getSuccessSyncCount() {
        return successSyncCount;
    }

    public void setSuccessSyncCount(Long successSyncCount) {
        this.successSyncCount = successSyncCount;
    }
}
