package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;

import java.time.LocalDateTime;

/**
 * 登记表两段式审查设置（门店级）：
 * 初审通过后，系统在入住日前 leadDays 天自动完成终审并向客人发送 finalMessage。
 */
@Entity
@Table(
        name = "registration_review_settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_registration_review_settings_store", columnNames = {"store_id"})
        }
)
public class RegistrationReviewSettings implements StoreScopedEntity {

    public static final int DEFAULT_LEAD_DAYS = 7;
    public static final int MIN_LEAD_DAYS = 1;
    public static final int MAX_LEAD_DAYS = 30;
    public static final int MAX_FINAL_MESSAGE_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "auto_finalize_enabled", nullable = false)
    private Boolean autoFinalizeEnabled = true;

    @Column(name = "lead_days", nullable = false)
    private Integer leadDays = DEFAULT_LEAD_DAYS;

    @Column(name = "final_message", columnDefinition = "TEXT")
    private String finalMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
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

    @Override
    public Long getStoreId() {
        return storeId;
    }

    @Override
    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Boolean getAutoFinalizeEnabled() {
        return autoFinalizeEnabled;
    }

    public void setAutoFinalizeEnabled(Boolean autoFinalizeEnabled) {
        this.autoFinalizeEnabled = autoFinalizeEnabled;
    }

    public Integer getLeadDays() {
        return leadDays;
    }

    public void setLeadDays(Integer leadDays) {
        this.leadDays = leadDays;
    }

    public String getFinalMessage() {
        return finalMessage;
    }

    public void setFinalMessage(String finalMessage) {
        this.finalMessage = finalMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 未保存过的门店默认值（不落库）。
     */
    public static RegistrationReviewSettings defaultsFor(Long storeId) {
        RegistrationReviewSettings settings = new RegistrationReviewSettings();
        settings.setStoreId(storeId);
        settings.setAutoFinalizeEnabled(true);
        settings.setLeadDays(DEFAULT_LEAD_DAYS);
        settings.setFinalMessage(null);
        return settings;
    }

    public int effectiveLeadDays() {
        if (leadDays == null) {
            return DEFAULT_LEAD_DAYS;
        }
        return Math.max(MIN_LEAD_DAYS, Math.min(MAX_LEAD_DAYS, leadDays));
    }

    public boolean isAutoFinalizeEnabled() {
        return autoFinalizeEnabled == null || autoFinalizeEnabled;
    }
}
