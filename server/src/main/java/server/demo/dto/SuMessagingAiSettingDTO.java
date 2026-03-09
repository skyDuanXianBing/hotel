package server.demo.dto;

public class SuMessagingAiSettingDTO {

    private Boolean autoReplyEnabled;

    public SuMessagingAiSettingDTO() {
    }

    public SuMessagingAiSettingDTO(Boolean autoReplyEnabled) {
        this.autoReplyEnabled = autoReplyEnabled;
    }

    public Boolean getAutoReplyEnabled() {
        return autoReplyEnabled;
    }

    public void setAutoReplyEnabled(Boolean autoReplyEnabled) {
        this.autoReplyEnabled = autoReplyEnabled;
    }
}
