package server.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import server.demo.entity.base.StoreScopedEntity;

/**
 * 房型价格计划关联实体
 * 用于关联房型和价格计划,并设置该房型在该价格计划下的每日价格
 */
@Entity
@Table(name = "room_type_price_plans", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"store_id", "room_type_id", "price_plan_id"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoomTypePricePlan implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "房型不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @NotNull(message = "价格计划不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_plan_id", nullable = false)
    private PricePlan pricePlan;

    // 每日价格 (周一到周日)
    @Column(name = "monday_price", precision = 10, scale = 2)
    private BigDecimal mondayPrice;

    @Column(name = "tuesday_price", precision = 10, scale = 2)
    private BigDecimal tuesdayPrice;

    @Column(name = "wednesday_price", precision = 10, scale = 2)
    private BigDecimal wednesdayPrice;

    @Column(name = "thursday_price", precision = 10, scale = 2)
    private BigDecimal thursdayPrice;

    @Column(name = "friday_price", precision = 10, scale = 2)
    private BigDecimal fridayPrice;

    @Column(name = "saturday_price", precision = 10, scale = 2)
    private BigDecimal saturdayPrice;

    @Column(name = "sunday_price", precision = 10, scale = 2)
    private BigDecimal sundayPrice;

    @NotNull(message = "最大入住人数不能为空")
    @Min(value = 1, message = "最大入住人数必须大于0")
    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests = 4;

    @Column(name = "included_guests")
    private Integer includedGuests; // 包含人数

    @Column(name = "price_mode", length = 20)
    private String priceMode; // unified 或 multiple

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (maxGuests == null) {
            maxGuests = 4;
        }
        if (priceMode == null) {
            priceMode = "unified";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public RoomTypePricePlan() {}

    public RoomTypePricePlan(RoomType roomType, PricePlan pricePlan) {
        this.roomType = roomType;
        this.pricePlan = pricePlan;
        if (roomType != null) {
            this.storeId = roomType.getStoreId();
        }
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
        if (roomType != null) {
            this.storeId = roomType.getStoreId();
        }
    }

    public PricePlan getPricePlan() {
        return pricePlan;
    }

    public void setPricePlan(PricePlan pricePlan) {
        this.pricePlan = pricePlan;
    }

    public BigDecimal getMondayPrice() {
        return mondayPrice;
    }

    public void setMondayPrice(BigDecimal mondayPrice) {
        this.mondayPrice = mondayPrice;
    }

    public BigDecimal getTuesdayPrice() {
        return tuesdayPrice;
    }

    public void setTuesdayPrice(BigDecimal tuesdayPrice) {
        this.tuesdayPrice = tuesdayPrice;
    }

    public BigDecimal getWednesdayPrice() {
        return wednesdayPrice;
    }

    public void setWednesdayPrice(BigDecimal wednesdayPrice) {
        this.wednesdayPrice = wednesdayPrice;
    }

    public BigDecimal getThursdayPrice() {
        return thursdayPrice;
    }

    public void setThursdayPrice(BigDecimal thursdayPrice) {
        this.thursdayPrice = thursdayPrice;
    }

    public BigDecimal getFridayPrice() {
        return fridayPrice;
    }

    public void setFridayPrice(BigDecimal fridayPrice) {
        this.fridayPrice = fridayPrice;
    }

    public BigDecimal getSaturdayPrice() {
        return saturdayPrice;
    }

    public void setSaturdayPrice(BigDecimal saturdayPrice) {
        this.saturdayPrice = saturdayPrice;
    }

    public BigDecimal getSundayPrice() {
        return sundayPrice;
    }

    public void setSundayPrice(BigDecimal sundayPrice) {
        this.sundayPrice = sundayPrice;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Integer getIncludedGuests() {
        return includedGuests;
    }

    public void setIncludedGuests(Integer includedGuests) {
        this.includedGuests = includedGuests;
    }

    public String getPriceMode() {
        return priceMode;
    }

    public void setPriceMode(String priceMode) {
        this.priceMode = priceMode;
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

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
