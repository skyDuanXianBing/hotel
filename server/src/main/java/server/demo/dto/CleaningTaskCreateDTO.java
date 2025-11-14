package server.demo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 创建保洁任务DTO
 */
public class CleaningTaskCreateDTO {

    @NotNull(message = "任务日期不能为空")
    private String taskDate;  // 改为String类型,接受 "YYYY-MM-DD" 格式

    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @NotNull(message = "任务类型不能为空")
    private String taskType;

    private Long cleanerId;
    private String estimatedTime;  // 改为String类型,接受 "HH:MM-HH:MM" 格式
    private String notes;

    // Constructors
    public CleaningTaskCreateDTO() {}

    // Getters and Setters
    public String getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(String taskDate) {
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

    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
