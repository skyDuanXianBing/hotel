package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 发送消息请求DTO
 */
public class SendMessageRequest {

    @NotBlank(message = "{api.t.d86594c188e5}")
    private String content;

    private String senderName;

    // Constructors
    public SendMessageRequest() {}

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
