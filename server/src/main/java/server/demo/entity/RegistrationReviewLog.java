package server.demo.entity;

import jakarta.persistence.*;
import server.demo.enums.RegistrationReviewAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registration_review_logs",
        indexes = {
                @Index(name = "idx_registration_review_logs_form", columnList = "form_id"),
                @Index(name = "idx_registration_review_logs_created", columnList = "created_at")
        }
)
public class RegistrationReviewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private RegistrationForm form;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private RegistrationReviewAction action;

    @Column(name = "operator_user_id")
    private Long operatorUserId;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationForm getForm() {
        return form;
    }

    public void setForm(RegistrationForm form) {
        this.form = form;
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
}
