package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import server.demo.entity.base.StoreScopedEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_prices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "room_type_id", "price_plan_id", "price_date"}))
public class RoomPrice implements StoreScopedEntity {
    public static final String PRICE_SOURCE_MANUAL = "MANUAL";
    public static final String PRICE_SOURCE_PRICELABS = "PRICELABS";
    public static final String PRICE_SOURCE_SYSTEM = "SYSTEM";

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
    @DecimalMin(value = "0.0", inclusive = false, message = "浠锋牸蹇呴』澶т簬0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "price_source", length = 32)
    private String priceSource = PRICE_SOURCE_SYSTEM;

    @Column(name = "manual_override", nullable = false)
    private Boolean manualOverride = false;

    @Column(name = "manual_override_until")
    private LocalDate manualOverrideUntil;

    @Column(name = "available_rooms")
    private Integer availableRooms;

    /**
     * Restrictions
     * closeRoom: closed (stop sell)
     * cta: closed to arrival
     * ctd: closed to departure
     */
    @Column(name = "close_room")
    private Boolean closeRoom;

    @Column(name = "cta")
    private Boolean cta;

    @Column(name = "ctd")
    private Boolean ctd;

    @jakarta.validation.constraints.Min(value = 1, message = "鏈€灏忓叆浣忓ぉ鏁板繀椤诲ぇ浜庣瓑浜?")
    @jakarta.validation.constraints.Max(value = 99, message = "鏈€灏忓叆浣忓ぉ鏁板繀椤诲皬浜庣瓑浜?9")
    @Column(name = "min_stay")
    private Integer minStay;

    @jakarta.validation.constraints.Min(value = 1, message = "鏈€澶у叆浣忓ぉ鏁板繀椤诲ぇ浜庣瓑浜?")
    @jakarta.validation.constraints.Max(value = 99, message = "鏈€澶у叆浣忓ぉ鏁板繀椤诲皬浜庣瓑浜?9")
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

    @Column(name = "store_id", nullable = false)
    private Long storeId;

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
        this.storeId = roomType != null ? roomType.getStoreId() : null;
        // 鑷姩鍒ゆ柇鏄惁涓哄懆鏈?
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
        this.storeId = roomType != null ? roomType.getStoreId() : null;
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

    public String getPriceSource() {
        return priceSource;
    }

    public void setPriceSource(String priceSource) {
        this.priceSource = priceSource;
    }

    public Boolean getManualOverride() {
        return manualOverride;
    }

    public void setManualOverride(Boolean manualOverride) {
        this.manualOverride = manualOverride;
    }

    public LocalDate getManualOverrideUntil() {
        return manualOverrideUntil;
    }

    public void setManualOverrideUntil(LocalDate manualOverrideUntil) {
        this.manualOverrideUntil = manualOverrideUntil;
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

    public Boolean getCloseRoom() {
        return closeRoom;
    }

    public void setCloseRoom(Boolean closeRoom) {
        this.closeRoom = closeRoom;
    }

    public Boolean getCta() {
        return cta;
    }

    public void setCta(Boolean cta) {
        this.cta = cta;
    }

    public Boolean getCtd() {
        return ctd;
    }

    public void setCtd(Boolean ctd) {
        this.ctd = ctd;
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

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}

