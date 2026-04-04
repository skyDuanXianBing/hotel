package server.demo.dto;

import server.demo.enums.SuMessagingSenderType;

import java.time.OffsetDateTime;

public class SuMessagingMessageDTO {
    private Long id;
    private Long threadId;
    private SuMessagingSenderType senderType;
    private String senderName;
    private String content;
    private String deliveryStatus;
    private OffsetDateTime timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
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

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

