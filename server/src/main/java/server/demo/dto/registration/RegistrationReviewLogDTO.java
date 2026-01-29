package server.demo.dto.registration;

import server.demo.enums.RegistrationReviewAction;

import java.time.LocalDateTime;

public class RegistrationReviewLogDTO {
    private Long id;
    private RegistrationReviewAction action;
    private Long operatorUserId;
    private String operatorName;
    private String note;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationReviewAction getAction() {
        return action;
    }

    public void setAction(RegistrationReviewAction action) {
        this.action = action;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
