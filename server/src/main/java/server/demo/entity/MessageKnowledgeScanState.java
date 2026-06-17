package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(
        name = "message_knowledge_scan_states",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_msg_knowledge_scan_state_store",
                        columnNames = {"store_id"}
                )
        }
)
public class MessageKnowledgeScanState implements StoreScopedEntity {
    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_IDLE;

    @Column(name = "last_scanned_message_id", nullable = false)
    private Long lastScannedMessageId = 0L;

    @Column(name = "lease_owner", length = 120)
    private String leaseOwner;

    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    @Column(name = "next_scan_after")
    private LocalDateTime nextScanAfter;

    @Column(name = "backoff_until")
    private LocalDateTime backoffUntil;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "last_started_at")
    private LocalDateTime lastStartedAt;

    @Column(name = "last_finished_at")
    private LocalDateTime lastFinishedAt;

    @Column(name = "processed_message_count", nullable = false)
    private Long processedMessageCount = 0L;

    @Column(name = "extracted_pair_count", nullable = false)
    private Long extractedPairCount = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        if (status == null) {
            status = STATUS_IDLE;
        }
        if (lastScannedMessageId == null) {
            lastScannedMessageId = 0L;
        }
        if (failureCount == null) {
            failureCount = 0;
        }
        if (processedMessageCount == null) {
            processedMessageCount = 0L;
        }
        if (extractedPairCount == null) {
            extractedPairCount = 0L;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UtcTimeUtil.nowLocalDateTime();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getLastScannedMessageId() {
        return lastScannedMessageId;
    }

    public void setLastScannedMessageId(Long lastScannedMessageId) {
        this.lastScannedMessageId = lastScannedMessageId;
    }

    public String getLeaseOwner() {
        return leaseOwner;
    }

    public void setLeaseOwner(String leaseOwner) {
        this.leaseOwner = leaseOwner;
    }

    public LocalDateTime getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(LocalDateTime leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public LocalDateTime getNextScanAfter() {
        return nextScanAfter;
    }

    public void setNextScanAfter(LocalDateTime nextScanAfter) {
        this.nextScanAfter = nextScanAfter;
    }

    public LocalDateTime getBackoffUntil() {
        return backoffUntil;
    }

    public void setBackoffUntil(LocalDateTime backoffUntil) {
        this.backoffUntil = backoffUntil;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getLastStartedAt() {
        return lastStartedAt;
    }

    public void setLastStartedAt(LocalDateTime lastStartedAt) {
        this.lastStartedAt = lastStartedAt;
    }

    public LocalDateTime getLastFinishedAt() {
        return lastFinishedAt;
    }

    public void setLastFinishedAt(LocalDateTime lastFinishedAt) {
        this.lastFinishedAt = lastFinishedAt;
    }

    public Long getProcessedMessageCount() {
        return processedMessageCount;
    }

    public void setProcessedMessageCount(Long processedMessageCount) {
        this.processedMessageCount = processedMessageCount;
    }

    public Long getExtractedPairCount() {
        return extractedPairCount;
    }

    public void setExtractedPairCount(Long extractedPairCount) {
        this.extractedPairCount = extractedPairCount;
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
