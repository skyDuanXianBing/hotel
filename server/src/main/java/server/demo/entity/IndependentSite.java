package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import jakarta.persistence.Version;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.IndependentSitePaymentProvider;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "independent_sites",
        uniqueConstraints = @UniqueConstraint(name = "uk_independent_sites_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_independent_sites_enabled_slug", columnList = "enabled,slug"),
                @Index(name = "idx_independent_sites_store", columnList = "store_id")
        }
)
@EntityListeners(StoreScopedEntityListener.class)
public class IndependentSite implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "slug", nullable = false, length = 63)
    private String slug;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "theme_key", nullable = false, length = 30)
    private String themeKey = "classic";

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "payment_provider", nullable = false, length = 30)
    private IndependentSitePaymentProvider paymentProvider = IndependentSitePaymentProvider.SIMULATED;

    @Column(name = "simulated_payment_enabled", nullable = false)
    private Boolean simulatedPaymentEnabled = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion = 0L;

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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThemeKey() {
        return themeKey;
    }

    public void setThemeKey(String themeKey) {
        this.themeKey = themeKey;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public IndependentSitePaymentProvider getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(IndependentSitePaymentProvider paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public Boolean getSimulatedPaymentEnabled() {
        return simulatedPaymentEnabled;
    }

    public void setSimulatedPaymentEnabled(Boolean simulatedPaymentEnabled) {
        this.simulatedPaymentEnabled = simulatedPaymentEnabled;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getRowVersion() {
        return rowVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
