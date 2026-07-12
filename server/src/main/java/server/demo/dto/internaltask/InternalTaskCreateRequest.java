package server.demo.dto.internaltask;

public class InternalTaskCreateRequest {
    private String title;
    private String description;
    private Long assigneeUserId;
    public String getTitle() { return title; } public void setTitle(String value) { this.title = value; }
    public String getDescription() { return description; } public void setDescription(String value) { this.description = value; }
    public Long getAssigneeUserId() { return assigneeUserId; } public void setAssigneeUserId(Long value) { this.assigneeUserId = value; }
}
