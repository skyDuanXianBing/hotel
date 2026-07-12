package server.demo.dto.internaltask;

import server.demo.entity.InternalTask;
import server.demo.enums.InternalTaskStatus;
import java.time.LocalDateTime;

public class InternalTaskDTO {
    private Long id; private String title; private String description; private InternalTaskStatus status;
    private Long assigneeUserId; private String assigneeName; private Long createdByUserId; private String createdByName;
    private Long completedByUserId; private String completedByName; private LocalDateTime completedAt;
    private LocalDateTime archivedAt; private LocalDateTime createdAt; private LocalDateTime updatedAt; private Long version;
    private boolean canComplete; private boolean canManage;

    public static InternalTaskDTO from(InternalTask task, boolean canComplete, boolean canManage) {
        InternalTaskDTO dto = new InternalTaskDTO(); dto.id = task.getId(); dto.title = task.getTitle();
        dto.description = task.getDescription(); dto.status = task.getStatus(); dto.assigneeUserId = task.getAssigneeUserId();
        dto.assigneeName = task.getAssigneeNameSnapshot(); dto.createdByUserId = task.getCreatedByUserId();
        dto.createdByName = task.getCreatedByNameSnapshot(); dto.completedByUserId = task.getCompletedByUserId();
        dto.completedByName = task.getCompletedByNameSnapshot(); dto.completedAt = task.getCompletedAt();
        dto.archivedAt = task.getArchivedAt(); dto.createdAt = task.getCreatedAt(); dto.updatedAt = task.getUpdatedAt();
        dto.version = task.getVersion(); dto.canComplete = canComplete; dto.canManage = canManage; return dto;
    }
    public Long getId() { return id; } public String getTitle() { return title; } public String getDescription() { return description; }
    public InternalTaskStatus getStatus() { return status; } public Long getAssigneeUserId() { return assigneeUserId; }
    public String getAssigneeName() { return assigneeName; } public Long getCreatedByUserId() { return createdByUserId; }
    public String getCreatedByName() { return createdByName; } public Long getCompletedByUserId() { return completedByUserId; }
    public String getCompletedByName() { return completedByName; } public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getArchivedAt() { return archivedAt; } public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public Long getVersion() { return version; }
    public boolean isCanComplete() { return canComplete; } public boolean isCanManage() { return canManage; }
}
