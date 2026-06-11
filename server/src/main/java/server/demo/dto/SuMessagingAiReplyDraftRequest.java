package server.demo.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SuMessagingAiReplyDraftRequest {
    private Long reservationId;
    private String bookingId;
    private String externalThreadId;
    private String channel;
    private String guestName;
    private Long roomId;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private Long latestGuestMessageId;
    private List<RecentMessage> recentMessages = new ArrayList<>();
    private String language;

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getExternalThreadId() {
        return externalThreadId;
    }

    public void setExternalThreadId(String externalThreadId) {
        this.externalThreadId = externalThreadId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Long getLatestGuestMessageId() {
        return latestGuestMessageId;
    }

    public void setLatestGuestMessageId(Long latestGuestMessageId) {
        this.latestGuestMessageId = latestGuestMessageId;
    }

    public List<RecentMessage> getRecentMessages() {
        return recentMessages;
    }

    public void setRecentMessages(List<RecentMessage> recentMessages) {
        this.recentMessages = recentMessages == null ? new ArrayList<>() : recentMessages;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public static class RecentMessage {
        private String direction;
        private String content;
        private LocalDateTime sentAt;

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public LocalDateTime getSentAt() {
            return sentAt;
        }

        public void setSentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
        }
    }
}
