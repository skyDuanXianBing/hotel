package server.demo.dto.internaltask;

public class InternalTaskAssignRequest {
    private Long assigneeUserId;
    private Long version;
    public Long getAssigneeUserId() { return assigneeUserId; } public void setAssigneeUserId(Long value) { this.assigneeUserId = value; }
    public Long getVersion() { return version; } public void setVersion(Long value) { this.version = value; }
}
