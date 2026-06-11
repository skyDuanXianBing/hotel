package server.demo.dto;

import java.time.OffsetDateTime;

public class SuMessagingTranslationResponse {
    private Long messageId;
    private String targetLanguage;
    private String translatedContent;
    private String sourceContentHash;
    private String status;
    private boolean cached;
    private OffsetDateTime translatedAt;

    public SuMessagingTranslationResponse() {
    }

    public SuMessagingTranslationResponse(
            Long messageId,
            String targetLanguage,
            String translatedContent,
            String sourceContentHash,
            String status,
            boolean cached,
            OffsetDateTime translatedAt
    ) {
        this.messageId = messageId;
        this.targetLanguage = targetLanguage;
        this.translatedContent = translatedContent;
        this.sourceContentHash = sourceContentHash;
        this.status = status;
        this.cached = cached;
        this.translatedAt = translatedAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    public String getSourceContentHash() {
        return sourceContentHash;
    }

    public void setSourceContentHash(String sourceContentHash) {
        this.sourceContentHash = sourceContentHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public OffsetDateTime getTranslatedAt() {
        return translatedAt;
    }

    public void setTranslatedAt(OffsetDateTime translatedAt) {
        this.translatedAt = translatedAt;
    }
}
