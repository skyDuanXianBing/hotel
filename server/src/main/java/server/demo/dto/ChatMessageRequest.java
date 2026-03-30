package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天消息请求DTO
 * 用于接收前端发送的聊天消息
 * 
 * @author AI Assistant
 */
public class ChatMessageRequest {

    /**
     * 用户发送的消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 8000, message = "message 超过 8000 字符限制")
    private String message;

    /**
     * 会话ID（可选），用于标识同一个聊天会话
     */
    private String sessionId;

    /**
     * 用户ID（可选），用于个性化回复
     */
    private String userId;

    // 构造函数
    public ChatMessageRequest() {}

    public ChatMessageRequest(String message, String sessionId, String userId) {
        this.message = message;
        this.sessionId = sessionId;
        this.userId = userId;
    }

    // Getter 和 Setter 方法
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    @Override
    public String toString() {
        return "ChatMessageRequest{" +
                "message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
