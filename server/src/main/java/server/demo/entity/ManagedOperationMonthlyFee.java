package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.ManagedOperationFeeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(name = "managed_operation_monthly_fees")
public class ManagedOperationMonthlyFee implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "monthly_data_id", nullable = false)
    private ManagedOperationMonthlyData monthlyData;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 10)
    private ManagedOperationFeeType feeType = ManagedOperationFeeType.DEDUCTION;

    @Column(name = "description", nullable = false, length = 200)
    private String description = "";

    @Column(name = "amount_gross", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountGross = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @Override public Long getStoreId() { return storeId; }
    @Override public void setStoreId(Long storeId) { this.storeId = storeId; }
    public ManagedOperationMonthlyData getMonthlyData() { return monthlyData; }
    public void setMonthlyData(ManagedOperationMonthlyData monthlyData) { this.monthlyData = monthlyData; }
    public ManagedOperationFeeType getFeeType() { return feeType; }
    public void setFeeType(ManagedOperationFeeType feeType) { this.feeType = feeType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmountGross() { return amountGross; }
    public void setAmountGross(BigDecimal amountGross) { this.amountGross = amountGross; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
