package server.demo.dto;

import java.util.List;

public class BatchCreateReservationResponse {

    private String groupOrderNo;
    private Integer reservationCount;
    private List<ReservationDTO> reservations;

    public BatchCreateReservationResponse() {
    }

    public BatchCreateReservationResponse(String groupOrderNo, Integer reservationCount, List<ReservationDTO> reservations) {
        this.groupOrderNo = groupOrderNo;
        this.reservationCount = reservationCount;
        this.reservations = reservations;
    }

    public String getGroupOrderNo() {
        return groupOrderNo;
    }

    public void setGroupOrderNo(String groupOrderNo) {
        this.groupOrderNo = groupOrderNo;
    }

    public Integer getReservationCount() {
        return reservationCount;
    }

    public void setReservationCount(Integer reservationCount) {
        this.reservationCount = reservationCount;
    }

    public List<ReservationDTO> getReservations() {
        return reservations;
    }

    public void setReservations(List<ReservationDTO> reservations) {
        this.reservations = reservations;
    }
}

