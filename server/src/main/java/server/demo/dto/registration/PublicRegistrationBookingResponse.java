package server.demo.dto.registration;

import java.time.LocalDate;
import java.util.List;

public class PublicRegistrationBookingResponse {
    private String bookingKey;
    private String guestName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private List<PublicRegistrationBookingRoomDTO> rooms;

    public String getBookingKey() {
        return bookingKey;
    }

    public void setBookingKey(String bookingKey) {
        this.bookingKey = bookingKey;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
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

    public List<PublicRegistrationBookingRoomDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<PublicRegistrationBookingRoomDTO> rooms) {
        this.rooms = rooms;
    }
}

