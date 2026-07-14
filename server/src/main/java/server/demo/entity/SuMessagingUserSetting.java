package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "su_messaging_user_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_su_messaging_user_settings_user",
                columnNames = "user_id"
        )
)
public class SuMessagingUserSetting {

    public static final boolean DEFAULT_TRANSLATION_ENABLED = false;
    public static final String DEFAULT_TRANSLATION_TARGET_LANGUAGE = "zh-CN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "translation_enabled", nullable = false)
    private Boolean translationEnabled = DEFAULT_TRANSLATION_ENABLED;

    @Column(name = "translation_target_language", nullable = false, length = 10)
    private String translationTargetLanguage = DEFAULT_TRANSLATION_TARGET_LANGUAGE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (translationEnabled == null) {
            translationEnabled = DEFAULT_TRANSLATION_ENABLED;
        }
        if (translationTargetLanguage == null || translationTargetLanguage.isBlank()) {
            translationTargetLanguage = DEFAULT_TRANSLATION_TARGET_LANGUAGE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getTranslationEnabled() {
        return translationEnabled;
    }

    public void setTranslationEnabled(Boolean translationEnabled) {
        this.translationEnabled = translationEnabled;
    }

    public String getTranslationTargetLanguage() {
        return translationTargetLanguage;
    }

    public void setTranslationTargetLanguage(String translationTargetLanguage) {
        this.translationTargetLanguage = translationTargetLanguage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
