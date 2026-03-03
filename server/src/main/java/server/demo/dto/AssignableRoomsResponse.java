package server.demo.dto;

import java.util.List;

public class AssignableRoomsResponse {
    private Long reservationId;
    private String checkInDate;
    private String checkOutDate;
    private List<AssignableRoomTypeDTO> roomTypes;
    private List<AssignableRoomDTO> rooms;

    public AssignableRoomsResponse() {}

    public AssignableRoomsResponse(
            Long reservationId,
            String checkInDate,
            String checkOutDate,
            List<AssignableRoomTypeDTO> roomTypes,
            List<AssignableRoomDTO> rooms
    ) {
        this.reservationId = reservationId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomTypes = roomTypes;
        this.rooms = rooms;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public List<AssignableRoomTypeDTO> getRoomTypes() {
        return roomTypes;
    }

    public void setRoomTypes(List<AssignableRoomTypeDTO> roomTypes) {
        this.roomTypes = roomTypes;
    }

    public List<AssignableRoomDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<AssignableRoomDTO> rooms) {
        this.rooms = rooms;
    }
}

