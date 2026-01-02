package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class SuMessagingSendRequest {
    @NotBlank(message = "消息内容不能为空")
    private String content;

    private String senderName;

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

