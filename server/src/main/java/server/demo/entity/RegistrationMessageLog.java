package server.demo.entity;

import jakarta.persistence.*;
import server.demo.enums.RegistrationMessageType;
import server.demo.enums.RegistrationSendStatus;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registration_message_logs",
        indexes = {
                @Index(name = "idx_registration_message_logs_form", columnList = "form_id"),
                @Index(name = "idx_registration_message_logs_created", columnList = "created_at")
        }
)
public class RegistrationMessageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private RegistrationForm form;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private RegistrationMessageType type;

    @Column(name = "channel", length = 30)
    private String channel;

    @Column(name = "to_identifier", length = 200)
    private String toIdentifier;

    @Column(name = "content", length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false, length = 30)
    private RegistrationSendStatus sendStatus;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

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

    public RegistrationMessageType getType() {
        return type;
    }

    public void setType(RegistrationMessageType type) {
        this.type = type;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getToIdentifier() {
        return toIdentifier;
    }

    public void setToIdentifier(String toIdentifier) {
        this.toIdentifier = toIdentifier;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public RegistrationSendStatus getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(RegistrationSendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
