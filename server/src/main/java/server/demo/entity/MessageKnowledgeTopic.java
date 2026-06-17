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
        name = "message_knowledge_topics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_msg_knowledge_topic_store_code",
                        columnNames = {"store_id", "topic_code"}
                )
        }
)
public class MessageKnowledgeTopic implements StoreScopedEntity {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_NEEDS_REVIEW = "NEEDS_REVIEW";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String SOURCE_DEFAULT = "DEFAULT";
    public static final String SOURCE_AI_CANDIDATE = "AI_CANDIDATE";
    public static final String SOURCE_MANUAL = "MANUAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "topic_code", nullable = false, length = 120)
    private String topicCode;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "aliases_json", columnDefinition = "TEXT")
    private String aliasesJson;

    @Column(name = "example_questions_json", columnDefinition = "TEXT")
    private String exampleQuestionsJson;

    @Column(name = "example_answers_json", columnDefinition = "TEXT")
    private String exampleAnswersJson;

    @Column(name = "scope_preference", nullable = false, length = 30)
    private String scopePreference = "STORE";

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @Column(name = "source", nullable = false, length = 30)
    private String source = SOURCE_DEFAULT;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        if (scopePreference == null || scopePreference.isBlank()) {
            scopePreference = "STORE";
        }
        if (status == null || status.isBlank()) {
            status = STATUS_ACTIVE;
        }
        if (source == null || source.isBlank()) {
            source = SOURCE_DEFAULT;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
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

    public String getTopicCode() {
        return topicCode;
    }

    public void setTopicCode(String topicCode) {
        this.topicCode = topicCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAliasesJson() {
        return aliasesJson;
    }

    public void setAliasesJson(String aliasesJson) {
        this.aliasesJson = aliasesJson;
    }

    public String getExampleQuestionsJson() {
        return exampleQuestionsJson;
    }

    public void setExampleQuestionsJson(String exampleQuestionsJson) {
        this.exampleQuestionsJson = exampleQuestionsJson;
    }

    public String getExampleAnswersJson() {
        return exampleAnswersJson;
    }

    public void setExampleAnswersJson(String exampleAnswersJson) {
        this.exampleAnswersJson = exampleAnswersJson;
    }

    public String getScopePreference() {
        return scopePreference;
    }

    public void setScopePreference(String scopePreference) {
        this.scopePreference = scopePreference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
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
}
