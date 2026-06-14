package server.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import server.demo.enums.RoomStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyRoomStatusDTO {
    private LocalDate date;
    private RoomStatus status;
    private ReservationInfoDTO reservation;

    /**
     * 房间关房（按房间号+日期落库）
     * true 表示该日期不可售，SU /availability 将推 roomstosell=0
     */
    private Boolean closed;

    /**
     * closeType: stop / maintenance / retain（与前端一致）
     */
    private String closeType;

    private String closeRemark;

    public DailyRoomStatusDTO() {}

    public DailyRoomStatusDTO(LocalDate date, RoomStatus status, ReservationInfoDTO reservation) {
        this.date = date;
        this.status = status;
        this.reservation = reservation;
    }

    public DailyRoomStatusDTO(LocalDate date, RoomStatus status, ReservationInfoDTO reservation, Boolean closed, String closeType, String closeRemark) {
        this.date = date;
        this.status = status;
        this.reservation = reservation;
        this.closed = closed;
        this.closeType = closeType;
        this.closeRemark = closeRemark;
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

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public String getCloseType() {
        return closeType;
    }

    public void setCloseType(String closeType) {
        this.closeType = closeType;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCloseRemark() {
        return closeRemark;
    }

    public void setCloseRemark(String closeRemark) {
        this.closeRemark = closeRemark;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReservationInfoDTO {
        private Long id;
        private String guestName;
        private String channel;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private String orderNumber;
        private String status;
        private BigDecimal totalAmount;
        private String groupOrderNo;
        private String notes;
        private String specialRequests;

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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getGroupOrderNo() {
            return groupOrderNo;
        }

        public void setGroupOrderNo(String groupOrderNo) {
            this.groupOrderNo = groupOrderNo;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getSpecialRequests() {
            return specialRequests;
        }

        public void setSpecialRequests(String specialRequests) {
            this.specialRequests = specialRequests;
        }
    }
}
