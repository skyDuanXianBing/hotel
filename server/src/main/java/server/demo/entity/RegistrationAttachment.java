package server.demo.entity;

import jakarta.persistence.*;
import server.demo.enums.RegistrationAttachmentType;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registration_attachments",
        indexes = {
                @Index(name = "idx_registration_attachments_form", columnList = "form_id"),
                @Index(name = "idx_registration_attachments_guest", columnList = "guest_id")
        }
)
public class RegistrationAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private RegistrationForm form;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = true)
    private RegistrationGuest guest;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private RegistrationAttachmentType type;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "sha256", length = 64)
    private String sha256;

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

    public RegistrationGuest getGuest() {
        return guest;
    }

    public void setGuest(RegistrationGuest guest) {
        this.guest = guest;
    }

    public RegistrationAttachmentType getType() {
        return type;
    }

    public void setType(RegistrationAttachmentType type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
