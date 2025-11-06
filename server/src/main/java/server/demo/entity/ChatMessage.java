package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 用于持久化聊天消息记录（可选功能）
 * 
 * @author AI Assistant
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的聊天会话
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private ChatSession session;

    /**
     * 消息类型：user, ai, system
     */
    @Column(name = "message_type", nullable = false)
    private String messageType;

    /**
     * 消息内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 发送时间
     */
    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime;

    /**
     * 发送者（用户ID或AI标识）
     */
    @Column(name = "sender")
    private String sender;

    // 构造函数
    public ChatMessage() {
        this.sendTime = LocalDateTime.now();
    }

    public ChatMessage(ChatSession session, String messageType, String content, String sender) {
        this();
        this.session = session;
        this.messageType = messageType;
        this.content = content;
        this.sender = sender;
    }

    /**
     * 创建用户消息
     */
    public static ChatMessage createUserMessage(ChatSession session, String content, String userId) {
        return new ChatMessage(session, "user", content, userId);
    }

    /**
     * 创建AI回复消息
     */
    public static ChatMessage createAiMessage(ChatSession session, String content) {
        return new ChatMessage(session, "ai", content, "ai-assistant");
    }

    /**
     * 创建系统消息
     */
    public static ChatMessage createSystemMessage(ChatSession session, String content) {
        return new ChatMessage(session, "system", content, "system");
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}