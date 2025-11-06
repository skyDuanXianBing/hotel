package server.demo.dto;

import server.demo.enums.RoomStatus;
import java.time.LocalDate;

public class DailyRoomStatusDTO {
    private LocalDate date;
    private RoomStatus status;
    private ReservationInfoDTO reservation;

    public DailyRoomStatusDTO() {}

    public DailyRoomStatusDTO(LocalDate date, RoomStatus status, ReservationInfoDTO reservation) {
        this.date = date;
        this.status = status;
        this.reservation = reservation;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public ReservationInfoDTO getReservation() {
        return reservation;
    }

    public void setReservation(ReservationInfoDTO reservation) {
        this.reservation = reservation;
    }

    public static class ReservationInfoDTO {
        private Long id;
        private String guestName;
        private String channel;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private String orderNumber;

        public ReservationInfoDTO() {}

        public ReservationInfoDTO(Long id, String guestName, String channel, 
                                 LocalDate checkIn, LocalDate checkOut, String orderNumber) {
            this.id = id;
            this.guestName = guestName;
            this.channel = channel;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.orderNumber = orderNumber;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getGuestName() {
            return guestName;
        }

        public void setGuestName(String guestName) {
            this.guestName = guestName;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public LocalDate getCheckIn() {
            return checkIn;
        }

        public void setCheckIn(LocalDate checkIn) {
            this.checkIn = checkIn;
        }

        public LocalDate getCheckOut() {
            return checkOut;
        }

        public void setCheckOut(LocalDate checkOut) {
            this.checkOut = checkOut;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }
    }
}