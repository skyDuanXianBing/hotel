package server.demo.dto.home;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HomeWorkbenchItemDTO {
    private String id;
    private String type;
    private String sourceType;
    private String sourceId;
    private String sourceStatus;
    private String statusGroup;
    private String priority;
    private LocalDateTime dueAt;
    private String title;
    private String subtitle;
    private List<HomeWorkbenchMetaItemDTO> metaItems = new ArrayList<>();
    private HomeWorkbenchTargetDTO target;
    private List<HomeWorkbenchActionDTO> actions = new ArrayList<>();
    private Long assigneeId;
    private String assigneeName;
    private Long unreadCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceStatus() {
        return sourceStatus;
    }

    public void setSourceStatus(String sourceStatus) {
        this.sourceStatus = sourceStatus;
    }

    public String getStatusGroup() {
        return statusGroup;
    }

    public void setStatusGroup(String statusGroup) {
        this.statusGroup = statusGroup;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public List<HomeWorkbenchMetaItemDTO> getMetaItems() {
        return metaItems;
    }

    public void setMetaItems(List<HomeWorkbenchMetaItemDTO> metaItems) {
        this.metaItems = metaItems;
    }

    public HomeWorkbenchTargetDTO getTarget() {
        return target;
    }

    public void setTarget(HomeWorkbenchTargetDTO target) {
        this.target = target;
    }

    public List<HomeWorkbenchActionDTO> getActions() {
        return actions;
    }

    public void setActions(List<HomeWorkbenchActionDTO> actions) {
        this.actions = actions;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
