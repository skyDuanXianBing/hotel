package server.demo.entity.saas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import server.demo.enums.SaasQuotaResetCycle;

import java.time.LocalDateTime;

/**
 * 配额账：total_quota 为 NULL 表示不限；period_start/period_end + reset_cycle 支持惰性周期滚动。
 * 扣减通过数据库原子条件 UPDATE 完成（见 SaasQuotaAccountRepository），version 仅作兜底乐观锁。
 */
@Entity
@Table(
        name = "saas_quota_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_saas_quota_account_store_feature",
                        columnNames = {"store_id", "feature_code"}
                )
        }
)
public class SaasQuotaAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "feature_code", nullable = false, length = 64)
    private String featureCode;

    /** 周期内总额度；NULL = 不限。 */
    @Column(name = "total_quota")
    private Long totalQuota;

    @Column(name = "used_quota", nullable = false)
    private Long usedQuota = 0L;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_cycle", nullable = false, length = 20)
    private SaasQuotaResetCycle resetCycle = SaasQuotaResetCycle.MONTHLY;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

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

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public Long getTotalQuota() {
        return totalQuota;
    }

    public void setTotalQuota(Long totalQuota) {
        this.totalQuota = totalQuota;
    }

    public Long getUsedQuota() {
        return usedQuota;
    }

    public void setUsedQuota(Long usedQuota) {
        this.usedQuota = usedQuota;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public SaasQuotaResetCycle getResetCycle() {
        return resetCycle;
    }

    public void setResetCycle(SaasQuotaResetCycle resetCycle) {
        this.resetCycle = resetCycle;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
