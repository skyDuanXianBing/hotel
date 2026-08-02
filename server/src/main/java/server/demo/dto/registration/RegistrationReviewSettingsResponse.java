package server.demo.dto.registration;

public class RegistrationReviewSettingsResponse {
    private boolean autoFinalizeEnabled;
    private int leadDays;
    private String finalMessage;
    private String defaultFinalMessage;

    public boolean isAutoFinalizeEnabled() {
        return autoFinalizeEnabled;
    }

    public void setAutoFinalizeEnabled(boolean autoFinalizeEnabled) {
        this.autoFinalizeEnabled = autoFinalizeEnabled;
    }

    public int getLeadDays() {
        return leadDays;
    }

    public void setLeadDays(int leadDays) {
        this.leadDays = leadDays;
    }

    public String getFinalMessage() {
        return finalMessage;
    }

    public void setFinalMessage(String finalMessage) {
        this.finalMessage = finalMessage;
    }

    public String getDefaultFinalMessage() {
        return defaultFinalMessage;
    }

    public void setDefaultFinalMessage(String defaultFinalMessage) {
        this.defaultFinalMessage = defaultFinalMessage;
    }
}
