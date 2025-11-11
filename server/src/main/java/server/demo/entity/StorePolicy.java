package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 门店政策实体
 */
@Entity
@Table(name = "store_policies")
public class StorePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(length = 50)
    private String checkinTime; // 入住时间

    @Column(length = 50)
    private String checkoutTime; // 退房时间

    @Column(columnDefinition = "TEXT")
    private String childPolicy; // 儿童政策

    @Column(length = 100)
    private String smokingPolicy; // 吸烟政策

    @Column(length = 100)
    private String petPolicy; // 宠物政策

    @Column(columnDefinition = "TEXT")
    private String additionalRules; // 附加规则

    @Column(columnDefinition = "TEXT")
    private String hotelTerms; // 酒店条款

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
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

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public String getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(String checkinTime) {
        this.checkinTime = checkinTime;
    }

    public String getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(String checkoutTime) {
        this.checkoutTime = checkoutTime;
    }

    public String getChildPolicy() {
        return childPolicy;
    }

    public void setChildPolicy(String childPolicy) {
        this.childPolicy = childPolicy;
    }

    public String getSmokingPolicy() {
        return smokingPolicy;
    }

    public void setSmokingPolicy(String smokingPolicy) {
        this.smokingPolicy = smokingPolicy;
    }

    public String getPetPolicy() {
        return petPolicy;
    }

    public void setPetPolicy(String petPolicy) {
        this.petPolicy = petPolicy;
    }

    public String getAdditionalRules() {
        return additionalRules;
    }

    public void setAdditionalRules(String additionalRules) {
        this.additionalRules = additionalRules;
    }

    public String getHotelTerms() {
        return hotelTerms;
    }

    public void setHotelTerms(String hotelTerms) {
        this.hotelTerms = hotelTerms;
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
