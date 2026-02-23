package server.demo.dto.registration;

import server.demo.enums.RegistrationFormStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PublicRegistrationResponse {
    private Long formId;
    private String orderNumber;
    private RegistrationFormStatus status;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String guestName;
    private Integer adults;
    private Integer children;
    private Integer maxGuests;
    private Integer guestCount;
    private LocalDateTime lastSavedAt;
    private String checkInGuideLink;
    private List<PublicRegistrationGuestDTO> guests;
    private List<PublicRegistrationAttachmentDTO> attachments;

    public Long getFormId() {
        return formId;
    }

    public void setFormId(Long formId) {
        this.formId = formId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public RegistrationFormStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationFormStatus status) {
        this.status = status;
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

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    public void setLastSavedAt(LocalDateTime lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    public String getCheckInGuideLink() {
        return checkInGuideLink;
    }

    public void setCheckInGuideLink(String checkInGuideLink) {
        this.checkInGuideLink = checkInGuideLink;
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
}
