package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_user_type_status", columnList = "user_id,notification_type,is_read")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank(message = "通知类型不能为空")
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType; // SYSTEM, ORDER, CLEANING, TASK

    @NotBlank(message = "通知标题不能为空")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "related_id")
    private Long relatedId; // 关联的订单ID、任务ID等

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = UtcTimeUtil.nowLocalDateTime();
        }
    }

    // Constructors
    public Notification() {}

    public Notification(Long userId, String notificationType, String title, String content) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.content = content;
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

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
        if (isRead && readAt == null) {
            readAt = UtcTimeUtil.nowLocalDateTime();
        }
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
