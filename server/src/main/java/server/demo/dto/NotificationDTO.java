package server.demo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 通知DTO
 */
public class NotificationDTO {

    private Long id;

    @NotBlank(message = "{api.t.531d8f6a5266}")
    private String notificationType;

    @NotBlank(message = "{api.t.ee41a31c9061}")
    private String title;

    @NotBlank(message = "{api.t.53f37fa58742}")
    private String content;

    private Long relatedId;

    // Constructors
    public NotificationDTO() {}

    public NotificationDTO(String notificationType, String title, String content) {
        this.notificationType = notificationType;
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }
}
