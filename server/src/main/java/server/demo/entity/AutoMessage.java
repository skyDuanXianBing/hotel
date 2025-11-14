package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;

import java.time.LocalDateTime;

/**
 * 自动化消息实体
 */
@Entity
@Table(name = "auto_messages")
@EntityListeners(StoreScopedEntityListener.class)
public class AutoMessage implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 门店ID（门店级架构）
     */
    @Column(name = "store_id")
    private Long storeId;

    /**
     * @deprecated 已废弃，使用门店级架构，由storeId替代
     */
    @Deprecated
    @Column(nullable = true)
    private Long userId;

    /**
     * 标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 消息内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 自动化规则: 订单确认时, 入住前24小时, 入住当天, 退房当天, 退房后
     */
    @Column(nullable = false, length = 50)
    private String automationRule;

    /**
     * 渠道: 全部渠道, Booking.com, Airbnb, Agoda, 等
     */
    @Column(nullable = false, length = 100)
    private String channel;

    /**
     * 房间: 全部房间, 或具体房间名称
     */
    @Column(nullable = false, length = 100)
    private String room;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAutomationRule() {
        return automationRule;
    }

    public void setAutomationRule(String automationRule) {
        this.automationRule = automationRule;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
