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
     * @deprecated 使用 channels 替代，支持多选
     */
    @Deprecated
    @Column(nullable = true, length = 100)
    private String channel;

    /**
     * 渠道列表（JSON数组格式，支持多选）
     * 例如: ["Booking.com", "Airbnb"]
     */
    @Column(columnDefinition = "TEXT")
    private String channels;

    /**
     * 过时补发: 如果消息在触发时间之后创建，是否仍然发送
     */
    @Column(nullable = false)
    private Boolean resendOnExpire = false;

    /**
     * 房间: 全部房间, 或具体房间名称
     * @deprecated 使用 roomSelectionType 和 roomSelection 替代
     */
    @Deprecated
    @Column(nullable = true, length = 100)
    private String room;

    /**
     * 房间选择类型: ALL_LOCAL(全部本地房型), BY_ROOM_TYPE(根据房型), BY_GROUP(根据分组), BY_ROOM(按房间)
     */
    @Column(length = 20)
    private String roomSelectionType;

    /**
     * 房间选择值（JSON数组格式）
     * 根据 roomSelectionType 存储不同内容:
     * - ALL_LOCAL: 空或null
     * - BY_ROOM_TYPE: 房型ID数组 ["1", "2"]
     * - BY_GROUP: 分组ID数组 ["1", "2"]
     * - BY_ROOM: 房间ID数组 ["1", "2"]
     */
    @Column(columnDefinition = "TEXT")
    private String roomSelection;

    /**
     * 操作类型: BOOKING_CONFIRM(预订确认), CHECK_IN(入住), CHECK_OUT(离店)
     */
    @Column(length = 30)
    private String action;

    /**
     * 发送时机: IMMEDIATELY(立即发送), 5_MIN(5分钟后), 10_MIN(10分钟后),
     * 15_MIN(15分钟后), 30_MIN(30分钟后), 1_HOUR(1小时后), 2_HOUR(2小时后),
     * 4_HOUR(4小时后), 8_HOUR(8小时后), 16_HOUR(16小时后), 24_HOUR(24小时后)
     */
    @Column(length = 20)
    private String sendTiming;

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

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public Boolean getResendOnExpire() {
        return resendOnExpire;
    }

    public void setResendOnExpire(Boolean resendOnExpire) {
        this.resendOnExpire = resendOnExpire;
    }

    public String getRoomSelectionType() {
        return roomSelectionType;
    }

    public void setRoomSelectionType(String roomSelectionType) {
        this.roomSelectionType = roomSelectionType;
    }

    public String getRoomSelection() {
        return roomSelection;
    }

    public void setRoomSelection(String roomSelection) {
        this.roomSelection = roomSelection;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSendTiming() {
        return sendTiming;
    }

    public void setSendTiming(String sendTiming) {
        this.sendTiming = sendTiming;
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
