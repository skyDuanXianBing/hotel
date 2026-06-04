package server.demo.dto.registration;

public class AdminRegistrationReviewResponse {
    private boolean messageAttempted;
    private RegistrationMessageLogDTO messageLog;
    private String messageError;

    public boolean isMessageAttempted() {
        return messageAttempted;
    }

    public void setMessageAttempted(boolean messageAttempted) {
        this.messageAttempted = messageAttempted;
    }

    public RegistrationMessageLogDTO getMessageLog() {
        return messageLog;
    }

    public void setMessageLog(RegistrationMessageLogDTO messageLog) {
        this.messageLog = messageLog;
    }

    public String getMessageError() {
        return messageError;
    }

    public void setMessageError(String messageError) {
        this.messageError = messageError;
    }
}
