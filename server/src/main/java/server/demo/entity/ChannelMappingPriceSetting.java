package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.ChannelMappingPriceSyncStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "channel_mapping_price_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_channel_mapping_price_current",
                columnNames = {"store_id", "channel_id", "mapping_key"}
        )
)
@EntityListeners(StoreScopedEntityListener.class)
public class ChannelMappingPriceSetting implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "ota_integration_id")
    private Long otaIntegrationId;

    @Column(name = "channel_code", nullable = false, length = 50)
    private String channelCode;

    @Column(name = "su_channel_id", nullable = false, length = 20)
    private String suChannelId;

    @Column(name = "su_property_id", nullable = false, length = 200)
    private String suPropertyId;

    @Column(name = "channel_hotel_id", length = 100)
    private String channelHotelId;

    @Column(name = "mapping_key", nullable = false, length = 700)
    private String mappingKey;

    @Column(
            name = "row_key",
            nullable = false,
            length = 1000,
            columnDefinition = "VARCHAR(1000) CHARACTER SET ascii COLLATE ascii_bin"
    )
    private String rowKey;

    @Column(name = "mapping_key_version", nullable = false, length = 20)
    private String mappingKeyVersion = "v1";

    @Column(name = "room_id", length = 100)
    private String roomId;

    @Column(name = "rate_id", length = 100)
    private String rateId;

    @Column(name = "channel_room_id", length = 100)
    private String channelRoomId;

    @Column(name = "channel_rate_id", length = 100)
    private String channelRateId;

    @Column(name = "listing_id", length = 100)
    private String listingId;

    @Column(name = "applicable_no_of_guest", length = 50)
    private String applicableNoOfGuest;

    @Column(name = "occupancy", length = 50)
    private String occupancy;

    @Column(name = "multiplier", precision = 12, scale = 6)
    private BigDecimal multiplier;

    @Column(name = "surcharge", precision = 12, scale = 2)
    private BigDecimal surcharge;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 32)
    private ChannelMappingPriceSyncStatus syncStatus = ChannelMappingPriceSyncStatus.UNSYNCED;

    @Lob
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_operation_id", length = 64)
    private String lastOperationId;

    @Column(name = "last_batch_id", length = 64)
    private String lastBatchId;

    @Column(name = "last_su_action", length = 100)
    private String lastSuAction;

    @Column(name = "last_su_http_status")
    private Integer lastSuHttpStatus;

    @Column(name = "last_su_response_status", length = 100)
    private String lastSuResponseStatus;

    @Lob
    @Column(name = "last_su_response_message", columnDefinition = "TEXT")
    private String lastSuResponseMessage;

    @Lob
    @Column(name = "last_su_response_errors", columnDefinition = "TEXT")
    private String lastSuResponseErrors;

    @Lob
    @Column(name = "last_su_payload_summary", columnDefinition = "TEXT")
    private String lastSuPayloadSummary;

    @Lob
    @Column(name = "last_su_response_summary", columnDefinition = "TEXT")
    private String lastSuResponseSummary;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (retryCount == null) {
            retryCount = 0;
        }
        if (syncStatus == null) {
            syncStatus = ChannelMappingPriceSyncStatus.UNSYNCED;
        }
        if (mappingKeyVersion == null || mappingKeyVersion.isBlank()) {
            mappingKeyVersion = "v1";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getOtaIntegrationId() {
        return otaIntegrationId;
    }

    public void setOtaIntegrationId(Long otaIntegrationId) {
        this.otaIntegrationId = otaIntegrationId;
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

    public String getSuPropertyId() {
        return suPropertyId;
    }

    public void setSuPropertyId(String suPropertyId) {
        this.suPropertyId = suPropertyId;
    }

    public String getChannelHotelId() {
        return channelHotelId;
    }

    public void setChannelHotelId(String channelHotelId) {
        this.channelHotelId = channelHotelId;
    }

    public String getMappingKey() {
        return mappingKey;
    }

    public void setMappingKey(String mappingKey) {
        this.mappingKey = mappingKey;
    }

    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    public String getMappingKeyVersion() {
        return mappingKeyVersion;
    }

    public void setMappingKeyVersion(String mappingKeyVersion) {
        this.mappingKeyVersion = mappingKeyVersion;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRateId() {
        return rateId;
    }

    public void setRateId(String rateId) {
        this.rateId = rateId;
    }

    public String getChannelRoomId() {
        return channelRoomId;
    }

    public void setChannelRoomId(String channelRoomId) {
        this.channelRoomId = channelRoomId;
    }

    public String getChannelRateId() {
        return channelRateId;
    }

    public void setChannelRateId(String channelRateId) {
        this.channelRateId = channelRateId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getApplicableNoOfGuest() {
        return applicableNoOfGuest;
    }

    public void setApplicableNoOfGuest(String applicableNoOfGuest) {
        this.applicableNoOfGuest = applicableNoOfGuest;
    }

    public String getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(String occupancy) {
        this.occupancy = occupancy;
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

    public ChannelMappingPriceSyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(ChannelMappingPriceSyncStatus syncStatus) {
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

    public String getLastSuAction() {
        return lastSuAction;
    }

    public void setLastSuAction(String lastSuAction) {
        this.lastSuAction = lastSuAction;
    }

    public Integer getLastSuHttpStatus() {
        return lastSuHttpStatus;
    }

    public void setLastSuHttpStatus(Integer lastSuHttpStatus) {
        this.lastSuHttpStatus = lastSuHttpStatus;
    }

    public String getLastSuResponseStatus() {
        return lastSuResponseStatus;
    }

    public void setLastSuResponseStatus(String lastSuResponseStatus) {
        this.lastSuResponseStatus = lastSuResponseStatus;
    }

    public String getLastSuResponseMessage() {
        return lastSuResponseMessage;
    }

    public void setLastSuResponseMessage(String lastSuResponseMessage) {
        this.lastSuResponseMessage = lastSuResponseMessage;
    }

    public String getLastSuResponseErrors() {
        return lastSuResponseErrors;
    }

    public void setLastSuResponseErrors(String lastSuResponseErrors) {
        this.lastSuResponseErrors = lastSuResponseErrors;
    }

    public String getLastSuPayloadSummary() {
        return lastSuPayloadSummary;
    }

    public void setLastSuPayloadSummary(String lastSuPayloadSummary) {
        this.lastSuPayloadSummary = lastSuPayloadSummary;
    }

    public String getLastSuResponseSummary() {
        return lastSuResponseSummary;
    }

    public void setLastSuResponseSummary(String lastSuResponseSummary) {
        this.lastSuResponseSummary = lastSuResponseSummary;
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

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
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
