package server.demo.entity.saas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import server.demo.enums.SaasSubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店订阅：成交时冻结权益快照（entitlement_snapshot_json），改模板不影响存量。
 */
@Entity
@Table(
        name = "saas_subscription",
        indexes = {
                @Index(name = "idx_saas_subscription_store_status", columnList = "store_id,status")
        }
)
public class SaasSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "package_name", nullable = false, length = 128)
    private String packageName;

    @Column(name = "entitlement_snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String entitlementSnapshotJson;

    @Column(name = "price_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePaid;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SaasSubscriptionStatus status = SaasSubscriptionStatus.ACTIVE;

    /**
     * 订阅备注（V065）：自动兜底订阅标记 'auto-fallback-after-expiry'，
     * 区分人工开通/购买与到期自动回退；其余来源为 NULL。
     */
    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
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

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getEntitlementSnapshotJson() {
        return entitlementSnapshotJson;
    }

    public void setEntitlementSnapshotJson(String entitlementSnapshotJson) {
        this.entitlementSnapshotJson = entitlementSnapshotJson;
    }

    public BigDecimal getPricePaid() {
        return pricePaid;
    }

    public void setPricePaid(BigDecimal pricePaid) {
        this.pricePaid = pricePaid;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public SaasSubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SaasSubscriptionStatus status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
