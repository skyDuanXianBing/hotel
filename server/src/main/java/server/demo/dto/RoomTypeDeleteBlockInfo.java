package server.demo.dto;

import server.demo.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 房型删除阻塞信息：用于在删除失败时给前端展示“哪些订单还在占用/阻塞删除”。
 */
public class RoomTypeDeleteBlockInfo {

    private long totalBlockingReservations;
    private List<BlockingReservationSummary> sample;

    public RoomTypeDeleteBlockInfo() {
    }

    public RoomTypeDeleteBlockInfo(long totalBlockingReservations, List<BlockingReservationSummary> sample) {
        this.totalBlockingReservations = totalBlockingReservations;
        this.sample = sample;
    }

    public long getTotalBlockingReservations() {
        return totalBlockingReservations;
    }

    public void setTotalBlockingReservations(long totalBlockingReservations) {
        this.totalBlockingReservations = totalBlockingReservations;
    }

    public List<BlockingReservationSummary> getSample() {
        return sample;
    }

    public void setSample(List<BlockingReservationSummary> sample) {
        this.sample = sample;
    }

    public static class BlockingReservationSummary {
        private String orderNumber;
        private ReservationStatus status;
        private String roomNumber;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;

        public BlockingReservationSummary() {
        }

        public BlockingReservationSummary(
                String orderNumber,
                ReservationStatus status,
                String roomNumber,
                LocalDate checkInDate,
                LocalDate checkOutDate
        ) {
            this.orderNumber = orderNumber;
            this.status = status;
            this.roomNumber = roomNumber;
            this.checkInDate = checkInDate;
            this.checkOutDate = checkOutDate;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public ReservationStatus getStatus() {
            return status;
        }

        public void setStatus(ReservationStatus status) {
            this.status = status;
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
    }
}

