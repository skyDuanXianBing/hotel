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
import server.demo.enums.SaasBillingOrderStatus;
import server.demo.enums.SaasBillingProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐购买订单：当前为 DIRECT 直连（点击购买即成功），provider 预留 STRIPE。
 */
@Entity
@Table(
        name = "saas_billing_order",
        indexes = {
                @Index(name = "idx_saas_billing_order_store", columnList = "store_id")
        }
)
public class SaasBillingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private SaasBillingProvider provider = SaasBillingProvider.DIRECT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SaasBillingOrderStatus status = SaasBillingOrderStatus.PAID;

    /**
     * 客户端生成的幂等键（V064，uk(store_id, idempotency_key) 兜底防重复下单）。
     * 可空：历史订单与未带键调用不冲突（MySQL 唯一索引允许多个 NULL）。
     */
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    /**
     * 订单备注（V065）：管理端人工开通/调控时写入操作人与业务备注
     * （格式「操作人:{operator}；{remark}」）；直连购买与自动兜底（不下订单）为 NULL。
     */
    @Column(name = "remark", length = 500)
    private String remark;

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

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public SaasBillingProvider getProvider() {
        return provider;
    }

    public void setProvider(SaasBillingProvider provider) {
        this.provider = provider;
    }

    public SaasBillingOrderStatus getStatus() {
        return status;
    }

    public void setStatus(SaasBillingOrderStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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
}
