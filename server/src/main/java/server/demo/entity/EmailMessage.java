package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.EmailSenderType;

import java.time.LocalDateTime;

/**
 * 邮件消息实体
 * 存储通过虚拟邮箱收发的所有邮件消息
 */
@Entity
@Table(name = "email_messages")
@EntityListeners(StoreScopedEntityListener.class)
public class EmailMessage implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 门店ID(门店级架构)
     */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 关联的虚拟邮箱ID
     */
    @Column(name = "mailbox_id", nullable = false)
    private Long mailboxId;

    /**
     * 邮件Message-ID (用于去重和追踪)
     */
    @Column(name = "message_id", length = 255)
    private String messageId;

    /**
     * 发件人邮箱地址
     */
    @Column(name = "sender_email", nullable = false, length = 255)
    private String senderEmail;

    /**
     * 发件人姓名
     */
    @Column(name = "sender_name", length = 100)
    private String senderName;

    /**
     * 发送者类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private EmailSenderType senderType;

    /**
     * 收件人邮箱地址
     */
    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    /**
     * 邮件主题
     */
    @Column(length = 500)
    private String subject;

    /**
     * 邮件内容(纯文本)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * HTML内容(可选)
     */
    @Column(name = "html_content", columnDefinition = "MEDIUMTEXT")
    private String htmlContent;

    /**
     * 发送时间
     */
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /**
     * 接收时间(对于接收的邮件)
     */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /**
     * 是否已读
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }

    // Constructors
    public EmailMessage() {}

    public EmailMessage(Long mailboxId, String senderEmail, EmailSenderType senderType,
                       String recipientEmail, String content) {
        this.mailboxId = mailboxId;
        this.senderEmail = senderEmail;
        this.senderType = senderType;
        this.recipientEmail = recipientEmail;
        this.content = content;
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

    public Long getMailboxId() {
        return mailboxId;
    }

    public void setMailboxId(Long mailboxId) {
        this.mailboxId = mailboxId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public EmailSenderType getSenderType() {
        return senderType;
    }

    public void setSenderType(EmailSenderType senderType) {
        this.senderType = senderType;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
