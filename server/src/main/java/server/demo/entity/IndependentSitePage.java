package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.IndependentSitePageFormat;
import server.demo.enums.IndependentSitePageType;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "independent_site_pages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_independent_site_pages_path",
                columnNames = {"site_id", "path"}
        ),
        indexes = @Index(
                name = "idx_independent_site_pages_store_site",
                columnList = "store_id,site_id"
        )
)
@EntityListeners(StoreScopedEntityListener.class)
public class IndependentSitePage implements StoreScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private IndependentSite site;

    @Column(name = "path", nullable = false)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private IndependentSitePageType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 20)
    private IndependentSitePageFormat format = IndependentSitePageFormat.BLOCKS;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "seo_description", length = 300)
    private String seoDescription;

    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Column(name = "draft_schema_json", columnDefinition = "LONGTEXT")
    private String draftSchemaJson;

    @Column(name = "draft_backup_schema_json", columnDefinition = "LONGTEXT")
    private String draftBackupSchemaJson;

    @Column(name = "published_schema_json", columnDefinition = "LONGTEXT")
    private String publishedSchemaJson;

    @Column(name = "draft_version", nullable = false)
    private Long draftVersion = 0L;

    @Column(name = "draft_updated_at")
    private LocalDateTime draftUpdatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

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

    public IndependentSite getSite() {
        return site;
    }

    public void setSite(IndependentSite site) {
        this.site = site;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public IndependentSitePageType getType() {
        return type;
    }

    public void setType(IndependentSitePageType type) {
        this.type = type;
    }

    /**
     * 页面格式；与数据库列默认值一致，未显式设置（含历史数据/测试中手工构造的实体）
     * 时按 BLOCKS 处理，保证旧管线零回归。
     */
    public IndependentSitePageFormat getFormat() {
        return format == null ? IndependentSitePageFormat.BLOCKS : format;
    }

    public void setFormat(IndependentSitePageFormat format) {
        this.format = format;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getDraftSchemaJson() {
        return draftSchemaJson;
    }

    public void setDraftSchemaJson(String draftSchemaJson) {
        this.draftSchemaJson = draftSchemaJson;
    }

    public String getDraftBackupSchemaJson() {
        return draftBackupSchemaJson;
    }

    public void setDraftBackupSchemaJson(String draftBackupSchemaJson) {
        this.draftBackupSchemaJson = draftBackupSchemaJson;
    }

    public String getPublishedSchemaJson() {
        return publishedSchemaJson;
    }

    public void setPublishedSchemaJson(String publishedSchemaJson) {
        this.publishedSchemaJson = publishedSchemaJson;
    }

    public Long getDraftVersion() {
        return draftVersion;
    }

    public void setDraftVersion(Long draftVersion) {
        this.draftVersion = draftVersion;
    }

    public LocalDateTime getDraftUpdatedAt() {
        return draftUpdatedAt;
    }

    public void setDraftUpdatedAt(LocalDateTime draftUpdatedAt) {
        this.draftUpdatedAt = draftUpdatedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
