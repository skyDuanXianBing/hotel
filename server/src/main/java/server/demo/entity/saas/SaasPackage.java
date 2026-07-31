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
import server.demo.enums.SaasPackagePeriod;
import server.demo.enums.SaasPackageStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SaaS 套餐模板（版本化：改价 = 上架新行，旧行停售保留）。
 */
@Entity
@Table(name = "saas_package")
public class SaasPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 10)
    private SaasPackagePeriod period = SaasPackagePeriod.MONTH;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SaasPackageStatus status = SaasPackageStatus.ON_SHELF;

    /**
     * 系统兜底套餐标记（V065）：仅迁移/种子可置位。系统套餐不可上架、不可经管理端接口
     * 授予标记，仅供订阅到期后的自动回退（SaasDefaultPackageFallbackService）查找。
     */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "description", length = 500)
    private String description;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public SaasPackagePeriod getPeriod() {
        return period;
    }

    public void setPeriod(SaasPackagePeriod period) {
        this.period = period;
    }

    public SaasPackageStatus getStatus() {
        return status;
    }

    public void setStatus(SaasPackageStatus status) {
        this.status = status;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
