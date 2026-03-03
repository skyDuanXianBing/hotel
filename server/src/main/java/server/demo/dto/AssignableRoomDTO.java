package server.demo.dto;

public class AssignableRoomDTO {
    private Long id;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;

    public AssignableRoomDTO() {}

    public AssignableRoomDTO(Long id, String roomNumber, Long roomTypeId, String roomTypeName) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomTypeId = roomTypeId;
        this.roomTypeName = roomTypeName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}

