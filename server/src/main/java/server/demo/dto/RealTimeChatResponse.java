package server.demo.dto;

import server.demo.entity.RealTimeChatMessage;
import java.time.LocalDateTime;

/**
 * 实时聊天响应DTO
 */
public class RealTimeChatResponse {
    
    private Long messageId;
    private String roomId;
    private RealTimeChatMessage.SenderType senderType;
    private String senderName;
    private String messageContent;
    private RealTimeChatMessage.MessageType messageType;
    private LocalDateTime sentAt;
    private Boolean isRead;
    
    // 构造函数
    public RealTimeChatResponse() {}
    
    public RealTimeChatResponse(RealTimeChatMessage message) {
        this.messageId = message.getId();
        this.roomId = message.getChatRoom().getRoomId();
        this.senderType = message.getSenderType();
        this.senderName = message.getSenderName();
        this.messageContent = message.getMessageContent();
        this.messageType = message.getMessageType();
        this.sentAt = message.getSentAt();
        this.isRead = message.getIsRead();
    }
    
    // 静态工厂方法
    public static RealTimeChatResponse fromEntity(RealTimeChatMessage message) {
        return new RealTimeChatResponse(message);
    }
    
    // Getters and Setters
    public Long getMessageId() {
        return messageId;
    }
    
    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public RealTimeChatMessage.SenderType getSenderType() {
        return senderType;
    }
    
    public void setSenderType(RealTimeChatMessage.SenderType senderType) {
        this.senderType = senderType;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getMessageContent() {
        return messageContent;
    }
    
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    
    public RealTimeChatMessage.MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(RealTimeChatMessage.MessageType messageType) {
        this.messageType = messageType;
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
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}