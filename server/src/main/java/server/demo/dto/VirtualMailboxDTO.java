package server.demo.dto;

import server.demo.enums.MailboxStatus;

import java.time.LocalDateTime;

/**
 * 虚拟邮箱DTO
 */
public class VirtualMailboxDTO {

    private Long id;
    private Long reservationId;
    private String emailAddress;
    private String displayName;
    private MailboxStatus status;
    private String guestName;
    private String guestRoomNumber;
    private LocalDateTime lastActivity;
    private Long unreadCount;

    // Constructors
    public VirtualMailboxDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public MailboxStatus getStatus() {
        return status;
    }

    public void setStatus(MailboxStatus status) {
        this.status = status;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestRoomNumber() {
        return guestRoomNumber;
    }

    public void setGuestRoomNumber(String guestRoomNumber) {
        this.guestRoomNumber = guestRoomNumber;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
