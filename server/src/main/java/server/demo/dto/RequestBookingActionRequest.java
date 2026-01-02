package server.demo.dto;

public class RequestBookingActionRequest {
    private String bookingId;
    private String declineReason;
    private String messageGuest;
    private String messageAirbnb;

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public String getMessageGuest() {
        return messageGuest;
    }

    public void setMessageGuest(String messageGuest) {
        this.messageGuest = messageGuest;
    }

    public String getMessageAirbnb() {
        return messageAirbnb;
    }

    public void setMessageAirbnb(String messageAirbnb) {
        this.messageAirbnb = messageAirbnb;
    }
}

