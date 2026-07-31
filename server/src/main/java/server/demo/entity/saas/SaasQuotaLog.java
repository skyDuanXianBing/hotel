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
import jakarta.persistence.Table;
import server.demo.enums.SaasQuotaAction;

import java.time.LocalDateTime;

/**
 * 配额流水：每次扣减/返还/发放/人工调整/周期重置的审计记录。
 */
@Entity
@Table(
        name = "saas_quota_log",
        indexes = {
                @Index(name = "idx_saas_quota_log_store_feature", columnList = "store_id,feature_code")
        }
)
public class SaasQuotaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "feature_code", nullable = false, length = 64)
    private String featureCode;

    /** used_quota 变动量：DEDUCT 为正，REFUND/RESET 为负，GRANT/ADJUST 为新老 used 差值。 */
    @Column(name = "delta", nullable = false)
    private Long delta;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private SaasQuotaAction action;

    @Column(name = "biz_id", length = 200)
    private String bizId;

    @Column(name = "operator", length = 64)
    private String operator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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

    public Long getDelta() {
        return delta;
    }

    public void setDelta(Long delta) {
        this.delta = delta;
    }

    public SaasQuotaAction getAction() {
        return action;
    }

    public void setAction(SaasQuotaAction action) {
        this.action = action;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
