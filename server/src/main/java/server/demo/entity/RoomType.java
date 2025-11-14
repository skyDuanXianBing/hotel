package server.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(name = "room_types",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "code"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoomType implements StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "房型名称不能为空")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "房型代码不能为空")
    @Column(nullable = false, length = 20)
    private String code;

    @NotNull(message = "房间总数不能为空")
    @Min(value = 1, message = "房间总数必须大于0")
    @Column(nullable = false)
    private Integer totalRooms;

    @Column(length = 500)
    private String description;

    @Column(name = "default_price", precision = 10, scale = 2)
    private BigDecimal defaultPrice;

    @Column(name = "weekday_price", precision = 10, scale = 2)
    private BigDecimal weekdayPrice;

    @Column(name = "weekend_price", precision = 10, scale = 2)
    private BigDecimal weekendPrice;

    // 每天的价格
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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
    public RoomType() {}

    public RoomType(String name, String code, Integer totalRooms, String description) {
        this.name = name;
        this.code = code;
        this.totalRooms = totalRooms;
        this.description = description;
    }

    // Getters and Setters
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(Integer totalRooms) {
        this.totalRooms = totalRooms;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public BigDecimal getWeekdayPrice() {
        return weekdayPrice;
    }

    public void setWeekdayPrice(BigDecimal weekdayPrice) {
        this.weekdayPrice = weekdayPrice;
    }

    public BigDecimal getWeekendPrice() {
        return weekendPrice;
    }

    public void setWeekendPrice(BigDecimal weekendPrice) {
        this.weekendPrice = weekendPrice;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
