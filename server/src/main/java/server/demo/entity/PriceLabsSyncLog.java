package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.SyncDirection;
import server.demo.enums.SyncStatus;
import server.demo.enums.SyncType;
import server.demo.util.JsonColumnUtil;

import java.time.LocalDateTime;

/**
 * PriceLabs 同步日志实体
 */
@Entity
@Table(name = "pricelabs_sync_logs",
       indexes = {
           @Index(name = "idx_sync_log_store_type", columnList = "store_id, sync_type"),
           @Index(name = "idx_sync_log_created_at", columnList = "created_at")
       })
@EntityListeners(StoreScopedEntityListener.class)
public class PriceLabsSyncLog implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 同步类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false)
    private SyncType syncType;

    /**
     * 同步方向
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private SyncDirection direction;

    /**
     * 同步状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SyncStatus status;

    /**
     * 受影响的记录数
     */
    @Column(name = "affected_count")
    private Integer affectedCount;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 请求数据（JSON 格式）
     */
    @Column(name = "request_data", columnDefinition = "JSON")
    private String requestData;

    /**
     * 响应数据（JSON 格式）
     */
    @Column(name = "response_data", columnDefinition = "JSON")
    private String responseData;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public PriceLabsSyncLog() {}

    public PriceLabsSyncLog(Long storeId, SyncType syncType, SyncDirection direction, SyncStatus status) {
        this.storeId = storeId;
        this.syncType = syncType;
        this.direction = direction;
        this.status = status;
    }

    /**
     * 创建成功日志
     */
    public static PriceLabsSyncLog success(Long storeId, SyncType syncType, SyncDirection direction, int affectedCount) {
        PriceLabsSyncLog log = new PriceLabsSyncLog(storeId, syncType, direction, SyncStatus.SUCCESS);
        log.setAffectedCount(affectedCount);
        return log;
    }

    /**
     * 创建失败日志
     */
    public static PriceLabsSyncLog failure(Long storeId, SyncType syncType, SyncDirection direction, String errorMessage) {
        PriceLabsSyncLog log = new PriceLabsSyncLog(storeId, syncType, direction, SyncStatus.FAILURE);
        log.setErrorMessage(errorMessage);
        return log;
    }

    // Getters and Setters
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

    public SyncType getSyncType() {
        return syncType;
    }

    public void setSyncType(SyncType syncType) {
        this.syncType = syncType;
    }

    public SyncDirection getDirection() {
        return direction;
    }

    public void setDirection(SyncDirection direction) {
        this.direction = direction;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }

    public Integer getAffectedCount() {
        return affectedCount;
    }

    public void setAffectedCount(Integer affectedCount) {
        this.affectedCount = affectedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = JsonColumnUtil.normalizeJsonText(requestData);
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = JsonColumnUtil.normalizeJsonText(responseData);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
