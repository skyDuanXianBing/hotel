package server.demo.dto;

import java.time.LocalDateTime;

/**
 * 更新保洁任务DTO
 */
public class CleaningTaskUpdateDTO {

    private String status;
    private Long cleanerId;
    private LocalDateTime estimatedTime;
    private LocalDateTime startTime;
    private LocalDateTime completeTime;
    private Long approverId;
    private String notes;

    // Constructors
    public CleaningTaskUpdateDTO() {}

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCleanerId() {
        return cleanerId;
    }

    public void setCleanerId(Long cleanerId) {
        this.cleanerId = cleanerId;
    }

    public LocalDateTime getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(LocalDateTime estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
