package server.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建保洁任务DTO
 */
public class CleaningTaskCreateDTO {

    @NotNull(message = "任务日期不能为空")
    private LocalDate taskDate;

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "任务类型不能为空")
    private String taskType;

    private Long cleanerId;
    private LocalDateTime estimatedTime;
    private String notes;

    // Constructors
    public CleaningTaskCreateDTO() {}

    // Getters and Setters
    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
