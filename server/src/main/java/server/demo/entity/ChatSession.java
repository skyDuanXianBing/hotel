package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天会话实体类
 * 用于持久化聊天会话信息（可选功能）
 * 
 * @author AI Assistant
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话ID（UUID）
     */
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;

    /**
     * 用户ID（可选）
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 会话开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 最后活动时间
     */
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    /**
     * 会话状态：active, closed
     */
    @Column(name = "status", nullable = false)
    private String status;

    /**
     * 会话消息列表
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();

    // 构造函数
    public ChatSession() {
        this.startTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.status = "active";
    }

    public ChatSession(String sessionId, String userId) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    /**
     * 更新最后活动时间
     */
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 关闭会话
     */
    public void closeSession() {
        this.status = "closed";
        this.lastActivity = LocalDateTime.now();
    }
}