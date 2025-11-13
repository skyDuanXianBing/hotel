package server.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保洁任务实体类
 */
@Entity
@Table(name = "cleaning_tasks")
public class CleaningTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务日期
     */
    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    /**
     * 房间ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /**
     * 任务类型: checkout(退房), daily(日常清洁), deep(深度清洁)
     */
    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType;

    /**
     * 任务状态: expired(已过期), pending(待分配), assigned(待清洁),
     * in_progress(清洁中), completed(已完成)
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 分配的保洁员
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleaner_id")
    private Cleaner cleaner;

    /**
     * 预计完成时间
     */
    @Column(name = "estimated_time")
    private LocalDateTime estimatedTime;

    /**
     * 实际开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 实际完成时间
     */
    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    /**
     * 审批人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    /**
     * 备注
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public CleaningTask() {}

    public CleaningTask(LocalDate taskDate, Room room, String taskType, String status) {
        this.taskDate = taskDate;
        this.room = room;
        this.taskType = taskType;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Cleaner getCleaner() {
        return cleaner;
    }

    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
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

    public User getApprover() {
        return approver;
    }

    public void setApprover(User approver) {
        this.approver = approver;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
