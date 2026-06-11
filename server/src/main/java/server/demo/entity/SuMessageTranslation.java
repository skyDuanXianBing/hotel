package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "su_message_translations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_su_msg_trans_store_msg_lang_hash",
                        columnNames = {"store_id", "message_id", "target_language", "source_content_hash"}
                )
        },
        indexes = {
                @Index(name = "idx_su_msg_trans_store_message_lang", columnList = "store_id,message_id,target_language"),
                @Index(name = "idx_su_msg_trans_store_thread", columnList = "store_id,thread_id")
        }
)
public class SuMessageTranslation {
    public static final String STATUS_SUCCESS = "SUCCESS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private SuMessageThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private SuMessage message;

    @Column(name = "target_language", nullable = false, length = 20)
    private String targetLanguage;

    @Column(name = "source_content_hash", nullable = false, length = 64)
    private String sourceContentHash;

    @Column(name = "translated_content", nullable = false, columnDefinition = "TEXT")
    private String translatedContent;

    @Column(name = "translation_status", nullable = false, length = 20)
    private String translationStatus;

    @Column(name = "translated_at", nullable = false)
    private LocalDateTime translatedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (translatedAt == null) {
            translatedAt = now;
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

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public SuMessageThread getThread() {
        return thread;
    }

    public void setThread(SuMessageThread thread) {
        this.thread = thread;
    }

    public SuMessage getMessage() {
        return message;
    }

    public void setMessage(SuMessage message) {
        this.message = message;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public String getSourceContentHash() {
        return sourceContentHash;
    }

    public void setSourceContentHash(String sourceContentHash) {
        this.sourceContentHash = sourceContentHash;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    public String getTranslationStatus() {
        return translationStatus;
    }

    public void setTranslationStatus(String translationStatus) {
        this.translationStatus = translationStatus;
    }

    public LocalDateTime getTranslatedAt() {
        return translatedAt;
    }

    public void setTranslatedAt(LocalDateTime translatedAt) {
        this.translatedAt = translatedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
