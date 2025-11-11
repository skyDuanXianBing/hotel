package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 保洁配置实体
 */
@Entity
@Table(name = "cleaning_configs")
public class CleaningConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 门店ID
     */
    @Column(nullable = false)
    private Long storeId;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * 续住房开始时间
     */
    @Column(length = 10)
    private String stayStartTime;

    /**
     * 续住房结束时间
     */
    @Column(length = 10)
    private String stayEndTime;

    /**
     * 退房开始时间
     */
    @Column(length = 10)
    private String checkoutStartTime;

    /**
     * 退房结束时间
     */
    @Column(length = 10)
    private String checkoutEndTime;

    /**
     * 自动生成续住任务
     */
    @Column(nullable = false)
    private Boolean autoStayTask = false;

    /**
     * 自动生成转退房任务
     */
    @Column(nullable = false)
    private Boolean autoCheckoutTask = false;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getStayStartTime() {
        return stayStartTime;
    }

    public void setStayStartTime(String stayStartTime) {
        this.stayStartTime = stayStartTime;
    }

    public String getStayEndTime() {
        return stayEndTime;
    }

    public void setStayEndTime(String stayEndTime) {
        this.stayEndTime = stayEndTime;
    }

    public String getCheckoutStartTime() {
        return checkoutStartTime;
    }

    public void setCheckoutStartTime(String checkoutStartTime) {
        this.checkoutStartTime = checkoutStartTime;
    }

    public String getCheckoutEndTime() {
        return checkoutEndTime;
    }

    public void setCheckoutEndTime(String checkoutEndTime) {
        this.checkoutEndTime = checkoutEndTime;
    }

    public Boolean getAutoStayTask() {
        return autoStayTask;
    }

    public void setAutoStayTask(Boolean autoStayTask) {
        this.autoStayTask = autoStayTask;
    }

    public Boolean getAutoCheckoutTask() {
        return autoCheckoutTask;
    }

    public void setAutoCheckoutTask(Boolean autoCheckoutTask) {
        this.autoCheckoutTask = autoCheckoutTask;
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
