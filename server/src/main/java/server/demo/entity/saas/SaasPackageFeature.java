package server.demo.entity.saas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 套餐权益模板行：quota_limit 为 NULL 表示不限。
 */
@Entity
@Table(
        name = "saas_package_feature",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_saas_package_feature",
                        columnNames = {"package_id", "feature_code"}
                )
        }
)
public class SaasPackageFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "feature_code", nullable = false, length = 64)
    private String featureCode;

    /** 权益额度上限；NULL = 不限。BOOLEAN 权益恒为 NULL。 */
    @Column(name = "quota_limit")
    private Long quotaLimit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public Long getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(Long quotaLimit) {
        this.quotaLimit = quotaLimit;
    }
}
