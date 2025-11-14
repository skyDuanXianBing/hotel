package server.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 改价历史记录实体
 * 记录每次价格修改操作
 */
@Entity
@Table(name = "price_change_history", indexes = {
    @Index(name = "idx_room_type", columnList = "room_type_id"),
    @Index(name = "idx_price_plan", columnList = "price_plan_id"),
    @Index(name = "idx_operate_time", columnList = "operate_time"),
    @Index(name = "idx_price_date", columnList = "price_date_start,price_date_end")
})
@EntityListeners(StoreScopedEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PriceChangeHistory implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_plan_id")
    private PricePlan pricePlan;

    @NotNull
    @Column(name = "price_date_start", nullable = false)
    private LocalDate priceDateStart;

    @NotNull
    @Column(name = "price_date_end", nullable = false)
    private LocalDate priceDateEnd;

    @Column(name = "apply_weekdays", length = 50)
    private String applyWeekdays; // 适用周几,例如: "1,2,3,4,5" 或 "全部"

    @NotNull
    @Column(name = "change_type", length = 50, nullable = false)
    private String changeType; // 修改类型,例如: "价格"

    @NotNull
    @Column(name = "change_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal changeValue; // 修改后的价格

    @Column(name = "previous_value", precision = 10, scale = 2)
    private BigDecimal previousValue; // 修改前的价格

    @NotNull
    @Column(name = "operator", length = 100, nullable = false)
    private String operator; // 操作人

    @NotNull
    @Column(name = "operate_time", nullable = false)
    private LocalDateTime operateTime;

    @Column(name = "pms_push_time")
    private LocalDateTime pmsPushTime; // PMS推送时间

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (operateTime == null) {
            operateTime = LocalDateTime.now();
        }
    }

    // Constructors
    public PriceChangeHistory() {}

    public PriceChangeHistory(RoomType roomType, PricePlan pricePlan,
                             LocalDate priceDateStart, LocalDate priceDateEnd,
                             String changeType, BigDecimal changeValue, String operator) {
        this.roomType = roomType;
        this.pricePlan = pricePlan;
        this.priceDateStart = priceDateStart;
        this.priceDateEnd = priceDateEnd;
        this.changeType = changeType;
        this.changeValue = changeValue;
        this.operator = operator;
        this.operateTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public PricePlan getPricePlan() {
        return pricePlan;
    }

    public void setPricePlan(PricePlan pricePlan) {
        this.pricePlan = pricePlan;
    }

    public LocalDate getPriceDateStart() {
        return priceDateStart;
    }

    public void setPriceDateStart(LocalDate priceDateStart) {
        this.priceDateStart = priceDateStart;
    }

    public LocalDate getPriceDateEnd() {
        return priceDateEnd;
    }

    public void setPriceDateEnd(LocalDate priceDateEnd) {
        this.priceDateEnd = priceDateEnd;
    }

    public String getApplyWeekdays() {
        return applyWeekdays;
    }

    public void setApplyWeekdays(String applyWeekdays) {
        this.applyWeekdays = applyWeekdays;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public BigDecimal getChangeValue() {
        return changeValue;
    }

    public void setChangeValue(BigDecimal changeValue) {
        this.changeValue = changeValue;
    }

    public BigDecimal getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(BigDecimal previousValue) {
        this.previousValue = previousValue;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public LocalDateTime getPmsPushTime() {
        return pmsPushTime;
    }

    public void setPmsPushTime(LocalDateTime pmsPushTime) {
        this.pmsPushTime = pmsPushTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
