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

import java.time.LocalDateTime;

/**
 * 门店级 Stripe 密钥配置（一店一套，该店所有独立站共享）。
 * secret_key / webhook_secret 只存 AES-GCM 密文，明文永不落库、永不出服务端；
 * publishable_key 属公开信息，明文存储可回传前端。
 */
@Entity
@Table(
        name = "independent_site_stripe_settings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_independent_site_stripe_settings_store",
                        columnNames = "store_id"
                )
        }
)
@EntityListeners(StoreScopedEntityListener.class)
public class IndependentSiteStripeSettings implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "publishable_key")
    private String publishableKey;

    @Column(name = "secret_key_encrypted", length = 1024)
    private String secretKeyEncrypted;

    @Column(name = "webhook_secret_encrypted", length = 1024)
    private String webhookSecretEncrypted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
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

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public String getSecretKeyEncrypted() {
        return secretKeyEncrypted;
    }

    public void setSecretKeyEncrypted(String secretKeyEncrypted) {
        this.secretKeyEncrypted = secretKeyEncrypted;
    }

    public String getWebhookSecretEncrypted() {
        return webhookSecretEncrypted;
    }

    public void setWebhookSecretEncrypted(String webhookSecretEncrypted) {
        this.webhookSecretEncrypted = webhookSecretEncrypted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
