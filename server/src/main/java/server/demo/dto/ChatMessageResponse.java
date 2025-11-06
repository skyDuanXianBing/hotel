package server.demo.dto;

import java.time.LocalDateTime;

/**
 * 聊天消息响应DTO
 * 用于返回AI生成的回复消息
 * 
 * @author AI Assistant
 */
public class ChatMessageResponse {

    /**
     * AI回复的消息内容
     */
    private String reply;

    /**
     * 回复生成的时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 响应状态（success/error）
     */
    private String status;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 处理耗时（毫秒）
     */
    private Long processingTime;

    // 构造函数
    public ChatMessageResponse() {
        this.timestamp = LocalDateTime.now();
        this.status = "success";
    }

    public ChatMessageResponse(String reply, String sessionId) {
        this();
        this.reply = reply;
        this.sessionId = sessionId;
    }

    /**
     * 创建成功响应
     */
    public static ChatMessageResponse success(String reply, String sessionId) {
        return new ChatMessageResponse(reply, sessionId);
    }

    /**
     * 创建错误响应
     */
    public static ChatMessageResponse error(String errorMessage, String sessionId) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setStatus("error");
        response.setErrorMessage(errorMessage);
        response.setSessionId(sessionId);
        return response;
    }

    // Getter 和 Setter 方法
    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Long processingTime) {
        this.processingTime = processingTime;
    }

    @Override
    public String toString() {
        return "ChatMessageResponse{" +
                "reply='" + reply + '\'' +
                ", timestamp=" + timestamp +
                ", sessionId='" + sessionId + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", processingTime=" + processingTime +
                '}';
    }
}