package server.demo.dto.registration;

import server.demo.enums.RegistrationMessageType;

public class RegistrationSendMessageRequest {
    private RegistrationMessageType type;
    private String content;
    private String senderName;
    private boolean translateBeforeSend;

    public RegistrationMessageType getType() {
        return type;
    }

    public void setType(RegistrationMessageType type) {
        this.type = type;
    }

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

    public boolean isTranslateBeforeSend() {
        return translateBeforeSend;
    }

    public void setTranslateBeforeSend(boolean translateBeforeSend) {
        this.translateBeforeSend = translateBeforeSend;
    }
}
