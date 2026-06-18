package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.util.UtcTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(
        name = "message_knowledge_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_msg_knowledge_item_scope_fact",
                        columnNames = {"store_id", "scope_key", "topic_hash", "fact_hash"}
                )
        }
)
public class MessageKnowledgeItem implements StoreScopedEntity {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANDIDATE = "CANDIDATE";
    public static final String STATUS_CONFLICT = "CONFLICT";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String REDACTION_STATUS_REDACTED = "REDACTED";
    public static final String EMBEDDING_STATUS_PENDING = "PENDING";
    public static final String EMBEDDING_STATUS_READY = "READY";
    public static final String EMBEDDING_STATUS_FAILED = "FAILED";
    public static final String EMBEDDING_STATUS_STALE = "STALE";
    public static final String EMBEDDING_STATUS_DISABLED = "DISABLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @Column(name = "scope_key", nullable = false, length = 80)
    private String scopeKey;

    @Column(name = "thread_id")
    private Long threadId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_number", length = 80)
    private String roomNumber;

    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Column(name = "room_type_name", length = 120)
    private String roomTypeName;

    @Column(name = "channel_id")
    private Integer channelId;

    @Column(name = "topic", nullable = false, length = 120)
    private String topic;

    @Column(name = "topic_hash", nullable = false, length = 64)
    private String topicHash;

    @Column(name = "fact_hash", length = 64)
    private String factHash;

    @Column(name = "extractor_version", length = 40)
    private String extractorVersion;

    @Column(name = "question", columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "normalized_question", columnDefinition = "TEXT", nullable = false)
    private String normalizedQuestion;

    @Column(name = "normalized_answer", columnDefinition = "TEXT", nullable = false)
    private String normalizedAnswer;

    @Column(name = "normalized_answer_hash", nullable = false, length = 64)
    private String normalizedAnswerHash;

    @Column(name = "language", length = 20)
    private String language;

    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence = BigDecimal.valueOf(0.7);

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @Column(name = "pii_redaction_status", nullable = false, length = 20)
    private String piiRedactionStatus = REDACTION_STATUS_REDACTED;

    @Column(name = "evidence_count", nullable = false)
    private Integer evidenceCount = 0;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "refined_at")
    private LocalDateTime refinedAt;

    @Column(name = "semantic_text", columnDefinition = "MEDIUMTEXT")
    private String semanticText;

    @Column(name = "search_intents_json", columnDefinition = "MEDIUMTEXT")
    private String searchIntentsJson;

    @Column(name = "embedding_vector", columnDefinition = "MEDIUMTEXT")
    private String embeddingVector;

    @Column(name = "embedding_status", nullable = false, length = 20)
    private String embeddingStatus = EMBEDDING_STATUS_PENDING;

    @Column(name = "embedding_provider", length = 60)
    private String embeddingProvider;

    @Column(name = "embedding_model", length = 120)
    private String embeddingModel;

    @Column(name = "embedding_dimensions")
    private Integer embeddingDimensions;

    @Column(name = "embedding_input_hash", length = 64)
    private String embeddingInputHash;

    @Column(name = "embedding_error", length = 500)
    private String embeddingError;

    @Column(name = "embedding_updated_at")
    private LocalDateTime embeddingUpdatedAt;

    @Column(name = "embedding_attempt_count", nullable = false)
    private Integer embeddingAttemptCount = 0;

    @Column(name = "embedding_next_attempt_at")
    private LocalDateTime embeddingNextAttemptAt;

    @Column(name = "embedding_lease_owner", length = 120)
    private String embeddingLeaseOwner;

    @Column(name = "embedding_lease_until")
    private LocalDateTime embeddingLeaseUntil;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        if (status == null) {
            status = STATUS_ACTIVE;
        }
        if (piiRedactionStatus == null) {
            piiRedactionStatus = REDACTION_STATUS_REDACTED;
        }
        if (confidence == null) {
            confidence = BigDecimal.valueOf(0.7);
        }
        if (evidenceCount == null) {
            evidenceCount = 0;
        }
        if (embeddingStatus == null) {
            embeddingStatus = EMBEDDING_STATUS_PENDING;
        }
        if (embeddingAttemptCount == null) {
            embeddingAttemptCount = 0;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (refinedAt == null) {
            refinedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UtcTimeUtil.nowLocalDateTime();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTopicHash() {
        return topicHash;
    }

    public void setTopicHash(String topicHash) {
        this.topicHash = topicHash;
    }

    public String getFactHash() {
        return factHash;
    }

    public void setFactHash(String factHash) {
        this.factHash = factHash;
    }

    public String getExtractorVersion() {
        return extractorVersion;
    }

    public void setExtractorVersion(String extractorVersion) {
        this.extractorVersion = extractorVersion;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getNormalizedQuestion() {
        return normalizedQuestion;
    }

    public void setNormalizedQuestion(String normalizedQuestion) {
        this.normalizedQuestion = normalizedQuestion;
    }

    public String getNormalizedAnswer() {
        return normalizedAnswer;
    }

    public void setNormalizedAnswer(String normalizedAnswer) {
        this.normalizedAnswer = normalizedAnswer;
    }

    public String getNormalizedAnswerHash() {
        return normalizedAnswerHash;
    }

    public void setNormalizedAnswerHash(String normalizedAnswerHash) {
        this.normalizedAnswerHash = normalizedAnswerHash;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPiiRedactionStatus() {
        return piiRedactionStatus;
    }

    public void setPiiRedactionStatus(String piiRedactionStatus) {
        this.piiRedactionStatus = piiRedactionStatus;
    }

    public Integer getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(Integer evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getRefinedAt() {
        return refinedAt;
    }

    public void setRefinedAt(LocalDateTime refinedAt) {
        this.refinedAt = refinedAt;
    }

    public String getSemanticText() {
        return semanticText;
    }

    public void setSemanticText(String semanticText) {
        this.semanticText = semanticText;
    }

    public String getSearchIntentsJson() {
        return searchIntentsJson;
    }

    public void setSearchIntentsJson(String searchIntentsJson) {
        this.searchIntentsJson = searchIntentsJson;
    }

    public String getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(String embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public String getEmbeddingStatus() {
        return embeddingStatus;
    }

    public void setEmbeddingStatus(String embeddingStatus) {
        this.embeddingStatus = embeddingStatus;
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public void setEmbeddingDimensions(Integer embeddingDimensions) {
        this.embeddingDimensions = embeddingDimensions;
    }

    public String getEmbeddingInputHash() {
        return embeddingInputHash;
    }

    public void setEmbeddingInputHash(String embeddingInputHash) {
        this.embeddingInputHash = embeddingInputHash;
    }

    public String getEmbeddingError() {
        return embeddingError;
    }

    public void setEmbeddingError(String embeddingError) {
        this.embeddingError = embeddingError;
    }

    public LocalDateTime getEmbeddingUpdatedAt() {
        return embeddingUpdatedAt;
    }

    public void setEmbeddingUpdatedAt(LocalDateTime embeddingUpdatedAt) {
        this.embeddingUpdatedAt = embeddingUpdatedAt;
    }

    public Integer getEmbeddingAttemptCount() {
        return embeddingAttemptCount;
    }

    public void setEmbeddingAttemptCount(Integer embeddingAttemptCount) {
        this.embeddingAttemptCount = embeddingAttemptCount;
    }

    public LocalDateTime getEmbeddingNextAttemptAt() {
        return embeddingNextAttemptAt;
    }

    public void setEmbeddingNextAttemptAt(LocalDateTime embeddingNextAttemptAt) {
        this.embeddingNextAttemptAt = embeddingNextAttemptAt;
    }

    public String getEmbeddingLeaseOwner() {
        return embeddingLeaseOwner;
    }

    public void setEmbeddingLeaseOwner(String embeddingLeaseOwner) {
        this.embeddingLeaseOwner = embeddingLeaseOwner;
    }

    public LocalDateTime getEmbeddingLeaseUntil() {
        return embeddingLeaseUntil;
    }

    public void setEmbeddingLeaseUntil(LocalDateTime embeddingLeaseUntil) {
        this.embeddingLeaseUntil = embeddingLeaseUntil;
    }
}
