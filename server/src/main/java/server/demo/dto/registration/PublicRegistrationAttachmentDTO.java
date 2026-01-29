package server.demo.dto.registration;

import server.demo.enums.RegistrationAttachmentType;

public class PublicRegistrationAttachmentDTO {
    private Long id;
    private Long guestId;
    private RegistrationAttachmentType type;
    private String originalName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGuestId() {
        return guestId;
    }

    public void setGuestId(Long guestId) {
        this.guestId = guestId;
    }

    public RegistrationAttachmentType getType() {
        return type;
    }

    public void setType(RegistrationAttachmentType type) {
        this.type = type;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
}
