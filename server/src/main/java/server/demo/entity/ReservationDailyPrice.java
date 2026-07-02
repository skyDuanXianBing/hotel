package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.util.StoreTimeZoneUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservation_daily_prices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_daily_prices_store_res_date",
                columnNames = {"store_id", "reservation_id", "price_date"}
        ),
        indexes = {
                @Index(name = "idx_reservation_daily_prices_store_date", columnList = "store_id,price_date"),
                @Index(name = "idx_reservation_daily_prices_reservation", columnList = "reservation_id"),
                @Index(
                        name = "idx_reservation_daily_prices_su_res_date",
                        columnList = "store_id,su_reservation_id,room_reservation_id,price_date"
                )
        }
)
@EntityListeners(StoreScopedEntityListener.class)
public class ReservationDailyPrice implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "su_hotel_id", length = 50)
    private String suHotelId;

    @Column(name = "su_reservation_id", length = 100)
    private String suReservationId;

    @Column(name = "room_reservation_id", length = 100)
    private String roomReservationId;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "rate_id", length = 100)
    private String rateId;

    @Column(name = "mealplan_id", length = 100)
    private String mealplanId;

    @Column(name = "mealplan", length = 255)
    private String mealplan;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "price_before_tax", precision = 12, scale = 2)
    private BigDecimal priceBeforeTax;

    @Column(name = "price_after_tax", precision = 12, scale = 2)
    private BigDecimal priceAfterTax;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = StoreTimeZoneUtil.nowReservationTimestampLocalDateTime();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = StoreTimeZoneUtil.nowReservationTimestampLocalDateTime();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        if (reservation != null && reservation.getStoreId() != null) {
            this.storeId = reservation.getStoreId();
        }
    }

    public String getSuHotelId() {
        return suHotelId;
    }

    public void setSuHotelId(String suHotelId) {
        this.suHotelId = suHotelId;
    }

    public String getSuReservationId() {
        return suReservationId;
    }

    public void setSuReservationId(String suReservationId) {
        this.suReservationId = suReservationId;
    }

    public String getRoomReservationId() {
        return roomReservationId;
    }

    public void setRoomReservationId(String roomReservationId) {
        this.roomReservationId = roomReservationId;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getRateId() {
        return rateId;
    }

    public void setRateId(String rateId) {
        this.rateId = rateId;
    }

    public String getMealplanId() {
        return mealplanId;
    }

    public void setMealplanId(String mealplanId) {
        this.mealplanId = mealplanId;
    }

    public String getMealplan() {
        return mealplan;
    }

    public void setMealplan(String mealplan) {
        this.mealplan = mealplan;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getPriceBeforeTax() {
        return priceBeforeTax;
    }

    public void setPriceBeforeTax(BigDecimal priceBeforeTax) {
        this.priceBeforeTax = priceBeforeTax;
    }

    public BigDecimal getPriceAfterTax() {
        return priceAfterTax;
    }

    public void setPriceAfterTax(BigDecimal priceAfterTax) {
        this.priceAfterTax = priceAfterTax;
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
}
