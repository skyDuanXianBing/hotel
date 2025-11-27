package server.demo.dto;

import server.demo.enums.EmailSenderType;

import java.time.LocalDateTime;

/**
 * 邮件消息DTO
 */
public class EmailMessageDTO {

    private Long id;
    private Long mailboxId;
    private String senderEmail;
    private String senderName;
    private EmailSenderType senderType;
    private String content;
    private LocalDateTime timestamp;
    private Boolean isRead;

    // Constructors
    public EmailMessageDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMailboxId() {
        return mailboxId;
    }

    public void setMailboxId(Long mailboxId) {
        this.mailboxId = mailboxId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
