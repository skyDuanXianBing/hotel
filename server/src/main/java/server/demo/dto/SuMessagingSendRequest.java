package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class SuMessagingSendRequest {
    @NotBlank(message = "{api.t.d86594c188e5}")
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

