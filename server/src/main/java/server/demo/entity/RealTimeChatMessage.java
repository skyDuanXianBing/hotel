package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 实时聊天消息实体
 */
@Entity
@Table(name = "realtime_chat_messages")
public class RealTimeChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;
    
    @Column(name = "sender_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SenderType senderType;
    
    @Column(name = "sender_name")
    private String senderName;
    
    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;
    
    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt = LocalDateTime.now();
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    public enum SenderType {
        GUEST, STAFF
    }
    
    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
    
    // 构造函数
    public RealTimeChatMessage() {}
    
    public RealTimeChatMessage(ChatRoom chatRoom, SenderType senderType, String senderName, String messageContent) {
        this.chatRoom = chatRoom;
        this.senderType = senderType;
        this.senderName = senderName;
        this.messageContent = messageContent;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ChatRoom getChatRoom() {
        return chatRoom;
    }
    
    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }
    
    public SenderType getSenderType() {
        return senderType;
    }
    
    public void setSenderType(SenderType senderType) {
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
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
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