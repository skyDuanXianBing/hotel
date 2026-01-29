package server.demo.dto.registration;

import server.demo.enums.RegistrationFormStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PublicRegistrationBookingRoomDTO {
    private String orderNumber;
    private String roomTypeName;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer maxGuests;
    private Integer guestCount;
    private RegistrationFormStatus status;
    private LocalDateTime lastSavedAt;
    private String roomRegistrationLink;

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    public RegistrationFormStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationFormStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    public void setLastSavedAt(LocalDateTime lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    public String getRoomRegistrationLink() {
        return roomRegistrationLink;
    }

    public void setRoomRegistrationLink(String roomRegistrationLink) {
        this.roomRegistrationLink = roomRegistrationLink;
    }
}

