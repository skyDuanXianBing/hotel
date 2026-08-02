package server.demo.dto.registration;

public class RegistrationReviewSettingsRequest {
    private Boolean autoFinalizeEnabled;
    private Integer leadDays;
    private String finalMessage;

    public Boolean getAutoFinalizeEnabled() {
        return autoFinalizeEnabled;
    }

    public void setAutoFinalizeEnabled(Boolean autoFinalizeEnabled) {
        this.autoFinalizeEnabled = autoFinalizeEnabled;
    }

    public Integer getLeadDays() {
        return leadDays;
    }

    public void setLeadDays(Integer leadDays) {
        this.leadDays = leadDays;
    }

    public String getFinalMessage() {
        return finalMessage;
    }

    public void setFinalMessage(String finalMessage) {
        this.finalMessage = finalMessage;
    }
}
