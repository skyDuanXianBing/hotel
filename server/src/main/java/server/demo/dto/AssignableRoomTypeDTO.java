package server.demo.dto;

public class AssignableRoomTypeDTO {
    private Long id;
    private String name;
    private String code;
    private long availableRooms;

    public AssignableRoomTypeDTO() {}

    public AssignableRoomTypeDTO(Long id, String name, String code, long availableRooms) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.availableRooms = availableRooms;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(long availableRooms) {
        this.availableRooms = availableRooms;
    }
}

