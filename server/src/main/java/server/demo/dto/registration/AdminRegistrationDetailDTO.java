package server.demo.dto.registration;

import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AdminRegistrationDetailDTO {
    private Long formId;
    private Long reservationId;
    private String orderNumber;
    private String channelOrderNumber;
    private RegistrationFormStatus status;
    private ReservationStatus reservationStatus;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String guestName;
    private String roomTypeName;
    private String roomNumber;
    private Integer adults;
    private Integer children;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String reviewNote;

    private List<PublicRegistrationGuestDTO> guests;
    private List<PublicRegistrationAttachmentDTO> attachments;
    private List<RegistrationReviewLogDTO> reviewLogs;
    private List<RegistrationMessageLogDTO> messageLogs;

    public Long getFormId() {
        return formId;
    }

    public void setFormId(Long formId) {
        this.formId = formId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getChannelOrderNumber() {
        return channelOrderNumber;
    }

    public void setChannelOrderNumber(String channelOrderNumber) {
        this.channelOrderNumber = channelOrderNumber;
    }

    public RegistrationFormStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationFormStatus status) {
        this.status = status;
    }

    public ReservationStatus getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(ReservationStatus reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getAdults() {
        return adults;
    }

    public void setAdults(Integer adults) {
        this.adults = adults;
    }

    public Integer getChildren() {
        return children;
    }

    public void setChildren(Integer children) {
        this.children = children;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public List<PublicRegistrationGuestDTO> getGuests() {
        return guests;
    }

    public void setGuests(List<PublicRegistrationGuestDTO> guests) {
        this.guests = guests;
    }

    public List<PublicRegistrationAttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<PublicRegistrationAttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    public List<RegistrationReviewLogDTO> getReviewLogs() {
        return reviewLogs;
    }

    public void setReviewLogs(List<RegistrationReviewLogDTO> reviewLogs) {
        this.reviewLogs = reviewLogs;
    }

    public List<RegistrationMessageLogDTO> getMessageLogs() {
        return messageLogs;
    }

    public void setMessageLogs(List<RegistrationMessageLogDTO> messageLogs) {
        this.messageLogs = messageLogs;
    }
}
