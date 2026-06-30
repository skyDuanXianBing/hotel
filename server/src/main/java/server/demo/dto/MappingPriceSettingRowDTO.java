package server.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MappingPriceSettingRowDTO {

    private String rowKey;
    private String channelCode;
    private String suChannelId;
    private String displayName;
    private String mappingStatus;
    private String channelHotelId;
    private String localRoomId;
    private String localRatePlanId;
    private String remoteRoomId;
    private String remoteRatePlanId;
    private String listingId;
    private String occupancy;
    private String applicableNoOfGuest;
    private BigDecimal multiplier;
    private BigDecimal surcharge;
    private String syncStatus;
    private String lastError;
    private Integer retryCount;
    private String lastOperationId;
    private String lastBatchId;
    private LocalDateTime lastAttemptedAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastFailedAt;

    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getSuChannelId() {
        return suChannelId;
    }

    public void setSuChannelId(String suChannelId) {
        this.suChannelId = suChannelId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMappingStatus() {
        return mappingStatus;
    }

    public void setMappingStatus(String mappingStatus) {
        this.mappingStatus = mappingStatus;
    }

    public String getChannelHotelId() {
        return channelHotelId;
    }

    public void setChannelHotelId(String channelHotelId) {
        this.channelHotelId = channelHotelId;
    }

    public String getLocalRoomId() {
        return localRoomId;
    }

    public void setLocalRoomId(String localRoomId) {
        this.localRoomId = localRoomId;
    }

    public String getLocalRatePlanId() {
        return localRatePlanId;
    }

    public void setLocalRatePlanId(String localRatePlanId) {
        this.localRatePlanId = localRatePlanId;
    }

    public String getRemoteRoomId() {
        return remoteRoomId;
    }

    public void setRemoteRoomId(String remoteRoomId) {
        this.remoteRoomId = remoteRoomId;
    }

    public String getRemoteRatePlanId() {
        return remoteRatePlanId;
    }

    public void setRemoteRatePlanId(String remoteRatePlanId) {
        this.remoteRatePlanId = remoteRatePlanId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(String occupancy) {
        this.occupancy = occupancy;
    }

    public String getApplicableNoOfGuest() {
        return applicableNoOfGuest;
    }

    public void setApplicableNoOfGuest(String applicableNoOfGuest) {
        this.applicableNoOfGuest = applicableNoOfGuest;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public BigDecimal getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(BigDecimal surcharge) {
        this.surcharge = surcharge;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastOperationId() {
        return lastOperationId;
    }

    public void setLastOperationId(String lastOperationId) {
        this.lastOperationId = lastOperationId;
    }

    public String getLastBatchId() {
        return lastBatchId;
    }

    public void setLastBatchId(String lastBatchId) {
        this.lastBatchId = lastBatchId;
    }

    public LocalDateTime getLastAttemptedAt() {
        return lastAttemptedAt;
    }

    public void setLastAttemptedAt(LocalDateTime lastAttemptedAt) {
        this.lastAttemptedAt = lastAttemptedAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public LocalDateTime getLastFailedAt() {
        return lastFailedAt;
    }

    public void setLastFailedAt(LocalDateTime lastFailedAt) {
        this.lastFailedAt = lastFailedAt;
    }
}
