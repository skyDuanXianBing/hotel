package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天室实体
 */
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_id", unique = true, nullable = false)
    private String roomId;
    
    @Column(name = "guest_name")
    private String guestName;
    
    @Column(name = "guest_room_number")
    private String guestRoomNumber;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ChatStatus status = ChatStatus.ACTIVE;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "last_activity")
    private LocalDateTime lastActivity = LocalDateTime.now();
    
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RealTimeChatMessage> messages = new ArrayList<>();
    
    public enum ChatStatus {
        ACTIVE, CLOSED, WAITING
    }
    
    // 构造函数
    public ChatRoom() {}
    
    public ChatRoom(String roomId, String guestName, String guestRoomNumber) {
        this.roomId = roomId;
        this.guestName = guestName;
        this.guestRoomNumber = guestRoomNumber;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getGuestName() {
        return guestName;
    }
    
    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }
    
    public String getGuestRoomNumber() {
        return guestRoomNumber;
    }
    
    public void setGuestRoomNumber(String guestRoomNumber) {
        this.guestRoomNumber = guestRoomNumber;
    }
    
    public ChatStatus getStatus() {
        return status;
    }
    
    public void setStatus(ChatStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public List<RealTimeChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<RealTimeChatMessage> messages) {
        this.messages = messages;
    }
    
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}