package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知设置实体
 */
@Entity
@Table(name = "notification_settings")
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    /**
     * 订单消息弹框提醒
     */
    @Column(nullable = false)
    private Boolean orderPopup = true;

    /**
     * 订单消息声音提醒
     */
    @Column(nullable = false)
    private Boolean orderSound = true;

    /**
     * 聊天信息弹框提醒
     */
    @Column(nullable = false)
    private Boolean chatPopup = true;

    /**
     * 聊天信息声音提醒
     */
    @Column(nullable = false)
    private Boolean chatSound = true;

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

    public Boolean getOrderPopup() {
        return orderPopup;
    }

    public void setOrderPopup(Boolean orderPopup) {
        this.orderPopup = orderPopup;
    }

    public Boolean getOrderSound() {
        return orderSound;
    }

    public void setOrderSound(Boolean orderSound) {
        this.orderSound = orderSound;
    }

    public Boolean getChatPopup() {
        return chatPopup;
    }

    public void setChatPopup(Boolean chatPopup) {
        this.chatPopup = chatPopup;
    }

    public Boolean getChatSound() {
        return chatSound;
    }

    public void setChatSound(Boolean chatSound) {
        this.chatSound = chatSound;
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
