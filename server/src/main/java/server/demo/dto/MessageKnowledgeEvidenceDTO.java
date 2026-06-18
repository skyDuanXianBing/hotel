package server.demo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageKnowledgeEvidenceDTO {
    private Long id;
    private String sourceType;
    private String sourceKind;
    private String sourceTitle;
    private String sourceText;
    private String content;
    private String messageContent;
    private String guestMessage;
    private String staffMessage;
    private Long threadId;
    private List<Long> sourceMessageIds = new ArrayList<>();
    private Long sourceMessageStartId;
    private Long sourceMessageEndId;
    private String extractorVersion;
    private String sourceFingerprint;
    private String channelName;
    private String topicCode;
    private String language;
    private OffsetDateTime occurredAt;
    private OffsetDateTime createdAt;
    private BigDecimal confidence;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(String sourceKind) {
        this.sourceKind = sourceKind;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getGuestMessage() {
        return guestMessage;
    }

    public void setGuestMessage(String guestMessage) {
        this.guestMessage = guestMessage;
    }

    public String getStaffMessage() {
        return staffMessage;
    }

    public void setStaffMessage(String staffMessage) {
        this.staffMessage = staffMessage;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public List<Long> getSourceMessageIds() {
        return sourceMessageIds;
    }

    public void setSourceMessageIds(List<Long> sourceMessageIds) {
        this.sourceMessageIds = sourceMessageIds == null ? new ArrayList<>() : sourceMessageIds;
    }

    public Long getSourceMessageStartId() {
        return sourceMessageStartId;
    }

    public void setSourceMessageStartId(Long sourceMessageStartId) {
        this.sourceMessageStartId = sourceMessageStartId;
    }

    public Long getSourceMessageEndId() {
        return sourceMessageEndId;
    }

    public void setSourceMessageEndId(Long sourceMessageEndId) {
        this.sourceMessageEndId = sourceMessageEndId;
    }

    public String getExtractorVersion() {
        return extractorVersion;
    }

    public void setExtractorVersion(String extractorVersion) {
        this.extractorVersion = extractorVersion;
    }

    public String getSourceFingerprint() {
        return sourceFingerprint;
    }

    public void setSourceFingerprint(String sourceFingerprint) {
        this.sourceFingerprint = sourceFingerprint;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getTopicCode() {
        return topicCode;
    }

    public void setTopicCode(String topicCode) {
        this.topicCode = topicCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }
}
