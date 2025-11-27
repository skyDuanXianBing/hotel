package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.MailboxStatus;

import java.time.LocalDateTime;

/**
 * 虚拟邮箱实体
 * 每个订单对应一个虚拟邮箱,用于与客人的邮件沟通
 */
@Entity
@Table(name = "virtual_mailboxes")
@EntityListeners(StoreScopedEntityListener.class)
public class VirtualMailbox implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 门店ID(门店级架构)
     */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 关联的订单ID
     */
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    /**
     * 虚拟邮箱地址
     * 格式: order-{订单号}@{酒店域名}.jp
     */
    @Column(name = "email_address", nullable = false, unique = true, length = 255)
    private String emailAddress;

    /**
     * 显示名称
     * 例如: "MyHotel - 订单 RSV1737370001234"
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * 邮箱状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MailboxStatus status = MailboxStatus.ACTIVE;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
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
    public VirtualMailbox() {}

    public VirtualMailbox(Long reservationId, String emailAddress, String displayName) {
        this.reservationId = reservationId;
        this.emailAddress = emailAddress;
        this.displayName = displayName;
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

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public MailboxStatus getStatus() {
        return status;
    }

    public void setStatus(MailboxStatus status) {
        this.status = status;
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
