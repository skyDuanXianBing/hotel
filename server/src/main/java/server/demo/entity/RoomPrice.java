package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_prices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_type_id", "price_plan_id", "price_date"}))
public class RoomPrice {
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
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "价格必须大于0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "available_rooms")
    private Integer availableRooms;

    @jakarta.validation.constraints.Min(value = 1, message = "最小入住天数必须大于等于1")
    @jakarta.validation.constraints.Max(value = 99, message = "最小入住天数必须小于等于99")
    @Column(name = "min_stay")
    private Integer minStay;

    @jakarta.validation.constraints.Min(value = 1, message = "最大入住天数必须大于等于1")
    @jakarta.validation.constraints.Max(value = 99, message = "最大入住天数必须小于等于99")
    @Column(name = "max_stay")
    private Integer maxStay;

    @Column(name = "is_weekend")
    private Boolean isWeekend = false;

    @Column(name = "is_holiday")
    private Boolean isHoliday = false;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public RoomPrice() {}

    public RoomPrice(RoomType roomType, LocalDate priceDate, BigDecimal price) {
        this.roomType = roomType;
        this.priceDate = priceDate;
        this.price = price;
        // 自动判断是否为周末
        this.isWeekend = priceDate.getDayOfWeek().getValue() >= 6;
    }

    public RoomPrice(RoomType roomType, LocalDate priceDate, BigDecimal price, Boolean isHoliday) {
        this(roomType, priceDate, price);
        this.isHoliday = isHoliday;
    }

    public RoomPrice(RoomType roomType, PricePlan pricePlan, LocalDate priceDate, BigDecimal price) {
        this.roomType = roomType;
        this.pricePlan = pricePlan;
        this.priceDate = priceDate;
        this.price = price;
        this.isWeekend = priceDate.getDayOfWeek().getValue() >= 6;
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

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getIsWeekend() {
        return isWeekend;
    }

    public void setIsWeekend(Boolean isWeekend) {
        this.isWeekend = isWeekend;
    }

    public Boolean getIsHoliday() {
        return isHoliday;
    }

    public void setIsHoliday(Boolean isHoliday) {
        this.isHoliday = isHoliday;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public PricePlan getPricePlan() {
        return pricePlan;
    }

    public void setPricePlan(PricePlan pricePlan) {
        this.pricePlan = pricePlan;
    }

    public Integer getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(Integer availableRooms) {
        this.availableRooms = availableRooms;
    }

    public Integer getMinStay() {
        return minStay;
    }

    public void setMinStay(Integer minStay) {
        this.minStay = minStay;
    }

    public Integer getMaxStay() {
        return maxStay;
    }

    public void setMaxStay(Integer maxStay) {
        this.maxStay = maxStay;
    }
}