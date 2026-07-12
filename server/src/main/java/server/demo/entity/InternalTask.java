package server.demo.entity;

import jakarta.persistence.*;
import server.demo.entity.base.StoreScopedEntity;
import server.demo.entity.listener.StoreScopedEntityListener;
import server.demo.enums.InternalTaskStatus;

import java.time.LocalDateTime;

@Entity
@EntityListeners(StoreScopedEntityListener.class)
@Table(name = "internal_tasks")
public class InternalTask implements StoreScopedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "store_id", nullable = false)
    private Long storeId;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private InternalTaskStatus status = InternalTaskStatus.UNASSIGNED;
    @Column(name = "assignee_user_id")
    private Long assigneeUserId;
    @Column(name = "assignee_name_snapshot", length = 100)
    private String assigneeNameSnapshot;
    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;
    @Column(name = "created_by_name_snapshot", nullable = false, length = 100)
    private String createdByNameSnapshot;
    @Column(name = "completed_by_user_id")
    private Long completedByUserId;
    @Column(name = "completed_by_name_snapshot", length = 100)
    private String completedByNameSnapshot;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
    @Column(name = "archived_by_user_id")
    private Long archivedByUserId;
    @Version @Column(nullable = false)
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getStoreId() { return storeId; } public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public InternalTaskStatus getStatus() { return status; } public void setStatus(InternalTaskStatus status) { this.status = status; }
    public Long getAssigneeUserId() { return assigneeUserId; } public void setAssigneeUserId(Long value) { this.assigneeUserId = value; }
    public String getAssigneeNameSnapshot() { return assigneeNameSnapshot; } public void setAssigneeNameSnapshot(String value) { this.assigneeNameSnapshot = value; }
    public Long getCreatedByUserId() { return createdByUserId; } public void setCreatedByUserId(Long value) { this.createdByUserId = value; }
    public String getCreatedByNameSnapshot() { return createdByNameSnapshot; } public void setCreatedByNameSnapshot(String value) { this.createdByNameSnapshot = value; }
    public Long getCompletedByUserId() { return completedByUserId; } public void setCompletedByUserId(Long value) { this.completedByUserId = value; }
    public String getCompletedByNameSnapshot() { return completedByNameSnapshot; } public void setCompletedByNameSnapshot(String value) { this.completedByNameSnapshot = value; }
    public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
    public LocalDateTime getArchivedAt() { return archivedAt; } public void setArchivedAt(LocalDateTime value) { this.archivedAt = value; }
    public Long getArchivedByUserId() { return archivedByUserId; } public void setArchivedByUserId(Long value) { this.archivedByUserId = value; }
    public Long getVersion() { return version; } public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
