package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.enums.SuAriSyncEventStatus;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "su_ari_sync_events",
        indexes = {
                @Index(name = "idx_su_ari_sync_events_due", columnList = "status, not_before_at, next_retry_at"),
                @Index(name = "idx_su_ari_sync_events_store_hotel", columnList = "store_id, hotel_id, status")
        }
)
public class SuAriSyncEvent implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "hotel_id", nullable = false, length = 50)
    private String hotelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SuAriSyncEventStatus status = SuAriSyncEventStatus.QUEUED;

    @Column(name = "source", length = 50)
    private String source;

    /**
     * JSON array of date ranges: [{"from":"2026-02-10","to":"2026-02-12"}, ...]
     * Kept as TEXT to support strict minimal payload requirements (no extra dates).
     */
    @Column(name = "date_ranges", columnDefinition = "TEXT")
    private String dateRanges;

    /**
     * JSON array of roomTypeIds. NULL/blank means "all room types".
     */
    @Column(name = "room_type_ids", columnDefinition = "TEXT")
    private String roomTypeIds;

    /**
     * JSON array of ratePlanIds. NULL/blank means "all rate plans".
     */
    @Column(name = "rate_plan_ids", columnDefinition = "TEXT")
    private String ratePlanIds;

    @Column(name = "push_availability", nullable = false)
    private Boolean pushAvailability = true;

    @Column(name = "push_rates", nullable = false)
    private Boolean pushRates = true;

    @Column(name = "push_restrictions", nullable = false)
    private Boolean pushRestrictions = true;

    @Column(name = "derive_closed_from_blockouts", nullable = false)
    private Boolean deriveClosedFromBlockouts = false;

    @Column(name = "coalesced_count", nullable = false)
    private Integer coalescedCount = 0;

    @Column(name = "not_before_at")
    private LocalDateTime notBeforeAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = SuAriSyncEventStatus.QUEUED;
        }
        if (pushAvailability == null) {
            pushAvailability = true;
        }
        if (pushRates == null) {
            pushRates = true;
        }
        if (pushRestrictions == null) {
            pushRestrictions = true;
        }
        if (deriveClosedFromBlockouts == null) {
            deriveClosedFromBlockouts = false;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (coalescedCount == null) {
            coalescedCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (pushAvailability == null) {
            pushAvailability = true;
        }
        if (pushRates == null) {
            pushRates = true;
        }
        if (pushRestrictions == null) {
            pushRestrictions = true;
        }
        if (deriveClosedFromBlockouts == null) {
            deriveClosedFromBlockouts = false;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (coalescedCount == null) {
            coalescedCount = 0;
        }
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getId() {
        return id;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public SuAriSyncEventStatus getStatus() {
        return status;
    }

    public void setStatus(SuAriSyncEventStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDateRanges() {
        return dateRanges;
    }

    public void setDateRanges(String dateRanges) {
        this.dateRanges = dateRanges;
    }

    public String getRoomTypeIds() {
        return roomTypeIds;
    }

    public void setRoomTypeIds(String roomTypeIds) {
        this.roomTypeIds = roomTypeIds;
    }

    public String getRatePlanIds() {
        return ratePlanIds;
    }

    public void setRatePlanIds(String ratePlanIds) {
        this.ratePlanIds = ratePlanIds;
    }

    public Boolean getPushAvailability() {
        return pushAvailability;
    }

    public void setPushAvailability(Boolean pushAvailability) {
        this.pushAvailability = pushAvailability;
    }

    public Boolean getPushRates() {
        return pushRates;
    }

    public void setPushRates(Boolean pushRates) {
        this.pushRates = pushRates;
    }

    public Boolean getPushRestrictions() {
        return pushRestrictions;
    }

    public void setPushRestrictions(Boolean pushRestrictions) {
        this.pushRestrictions = pushRestrictions;
    }

    public Boolean getDeriveClosedFromBlockouts() {
        return deriveClosedFromBlockouts;
    }

    public void setDeriveClosedFromBlockouts(Boolean deriveClosedFromBlockouts) {
        this.deriveClosedFromBlockouts = deriveClosedFromBlockouts;
    }

    public Integer getCoalescedCount() {
        return coalescedCount;
    }

    public void setCoalescedCount(Integer coalescedCount) {
        this.coalescedCount = coalescedCount;
    }

    public LocalDateTime getNotBeforeAt() {
        return notBeforeAt;
    }

    public void setNotBeforeAt(LocalDateTime notBeforeAt) {
        this.notBeforeAt = notBeforeAt;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(LocalDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

