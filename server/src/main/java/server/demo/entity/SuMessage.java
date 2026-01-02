package server.demo.entity;

import jakarta.persistence.*;
import server.demo.enums.SuMessagingSenderType;

import java.time.LocalDateTime;

/**
 * Su Messaging 单条消息。
 */
@Entity
@Table(
        name = "su_messages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_su_msg_store_external_id", columnNames = {"store_id", "external_message_id"})
        },
        indexes = {
                @Index(name = "idx_su_msg_thread_sent", columnList = "thread_id,sent_at"),
                @Index(name = "idx_su_msg_store_sent", columnList = "store_id,sent_at")
        }
)
public class SuMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private SuMessageThread thread;

    /**
     * 外部消息 ID（文档字段：messageid）。用于幂等去重。
     * PMS 自己发送的消息也会生成一个本地 ID（这里也可以为空）。
     */
    @Column(name = "external_message_id", length = 255)
    private String externalMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SuMessagingSenderType senderType;

    @Column(name = "sender_name", length = 120)
    private String senderName;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "raw_json", columnDefinition = "MEDIUMTEXT")
    private String rawJson;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public SuMessageThread getThread() {
        return thread;
    }

    public void setThread(SuMessageThread thread) {
        this.thread = thread;
    }

    public String getExternalMessageId() {
        return externalMessageId;
    }

    public void setExternalMessageId(String externalMessageId) {
        this.externalMessageId = externalMessageId;
    }

    public SuMessagingSenderType getSenderType() {
        return senderType;
    }

    public void setSenderType(SuMessagingSenderType senderType) {
        this.senderType = senderType;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean read) {
        isRead = read;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }
}

