package server.demo.dto.registration;

import server.demo.enums.RegistrationMessageType;
import server.demo.enums.RegistrationSendStatus;

import java.time.LocalDateTime;

public class RegistrationMessageLogDTO {
    private Long id;
    private RegistrationMessageType type;
    private String channel;
    private String toIdentifier;
    private String content;
    private RegistrationSendStatus sendStatus;
    private String errorMessage;
    private String targetLanguageCode;
    private String targetLanguageName;
    private String targetLanguageFallbackReason;
    private Boolean translated;
    private String translationError;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationMessageType getType() {
        return type;
    }

    public void setType(RegistrationMessageType type) {
        this.type = type;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getToIdentifier() {
        return toIdentifier;
    }

    public void setToIdentifier(String toIdentifier) {
        this.toIdentifier = toIdentifier;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public RegistrationSendStatus getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(RegistrationSendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTargetLanguageCode() {
        return targetLanguageCode;
    }

    public void setTargetLanguageCode(String targetLanguageCode) {
        this.targetLanguageCode = targetLanguageCode;
    }

    public String getTargetLanguageName() {
        return targetLanguageName;
    }

    public void setTargetLanguageName(String targetLanguageName) {
        this.targetLanguageName = targetLanguageName;
    }

    public String getTargetLanguageFallbackReason() {
        return targetLanguageFallbackReason;
    }

    public void setTargetLanguageFallbackReason(String targetLanguageFallbackReason) {
        this.targetLanguageFallbackReason = targetLanguageFallbackReason;
    }

    public Boolean getTranslated() {
        return translated;
    }

    public void setTranslated(Boolean translated) {
        this.translated = translated;
    }

    public String getTranslationError() {
        return translationError;
    }

    public void setTranslationError(String translationError) {
        this.translationError = translationError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
