package server.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "room_status_shares")
public class RoomStatusShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "分享标题不能为空")
    @Column(name = "share_title", nullable = false, length = 100)
    private String shareTitle;

    @Column(name = "share_token", nullable = false, unique = true, length = 64)
    private String shareToken;

    @Column(name = "share_link", nullable = false, length = 500)
    private String shareLink;

    @Column(name = "view_room_status", nullable = false)
    private Boolean viewRoomStatus = true;

    @Column(name = "query_method", nullable = false)
    private Boolean queryMethod = true;

    @Column(name = "view_type", nullable = false, length = 20)
    private String viewType = "normal";

    @Column(name = "query_mode", nullable = false, length = 20)
    private String queryMode = "enabled";

    @Column(name = "filter_items", columnDefinition = "TEXT")
    private String filterItems;

    @Column(name = "order_items", columnDefinition = "TEXT")
    private String orderItems;

    @Column(name = "associated_room_ids", columnDefinition = "TEXT")
    private String associatedRoomIds;

    /**
     * 分享所属的用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
    public RoomStatusShare() {}

    public RoomStatusShare(String shareTitle, String shareToken, String shareLink) {
        this.shareTitle = shareTitle;
        this.shareToken = shareToken;
        this.shareLink = shareLink;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShareTitle() {
        return shareTitle;
    }

    public void setShareTitle(String shareTitle) {
        this.shareTitle = shareTitle;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }

    public Boolean getViewRoomStatus() {
        return viewRoomStatus;
    }

    public void setViewRoomStatus(Boolean viewRoomStatus) {
        this.viewRoomStatus = viewRoomStatus;
    }

    public Boolean getQueryMethod() {
        return queryMethod;
    }

    public void setQueryMethod(Boolean queryMethod) {
        this.queryMethod = queryMethod;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public String getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(String queryMode) {
        this.queryMode = queryMode;
    }

    public String getFilterItems() {
        return filterItems;
    }

    public void setFilterItems(String filterItems) {
        this.filterItems = filterItems;
    }

    public String getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(String orderItems) {
        this.orderItems = orderItems;
    }

    public String getAssociatedRoomIds() {
        return associatedRoomIds;
    }

    public void setAssociatedRoomIds(String associatedRoomIds) {
        this.associatedRoomIds = associatedRoomIds;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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