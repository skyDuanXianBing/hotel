package server.demo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SuMessagingThreadContext {
    public static final String SCOPE_ROOM = "ROOM";
    public static final String SCOPE_ROOM_TYPE = "ROOM_TYPE";
    public static final String SCOPE_BOOKING = "BOOKING";
    public static final String SCOPE_STORE = "STORE";

    private Long reservationId;
    private Long roomId;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private Integer channelId;
    private String channelName;
    private String bookingKey;
    private String guestName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String matchStatus = "NONE";
    private final List<String> warnings = new ArrayList<>();

    public String getBestScopeType() {
        if (roomId != null) {
            return SCOPE_ROOM;
        }
        if (roomTypeId != null) {
            return SCOPE_ROOM_TYPE;
        }
        if (channelId != null && bookingKey != null && !bookingKey.isBlank()) {
            return SCOPE_BOOKING;
        }
        return SCOPE_STORE;
    }

    public Long getBestScopeId() {
        if (roomId != null) {
            return roomId;
        }
        if (roomTypeId != null) {
            return roomTypeId;
        }
        if (reservationId != null) {
            return reservationId;
        }
        return null;
    }

    public void addWarning(String warning) {
        if (warning != null && !warning.isBlank() && !warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
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

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

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

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
