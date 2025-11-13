package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 保洁员邀请实体
 */
@Entity
@Table(name = "cleaner_invitations")
public class CleanerInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 邀请邮箱
     */
    @Column(nullable = false)
    private String email;

    /**
     * 保洁员姓名
     */
    @Column(nullable = false)
    private String name;

    /**
     * 邀请token
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * 用户ID (邀请人)
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 门店ID
     */
    @Column(nullable = false)
    private Long storeId;

    /**
     * 邀请状态: pending(待接受), accepted(已接受), expired(已过期)
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * 过期时间
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

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

    // Constructors
    public CleanerInvitation() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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
