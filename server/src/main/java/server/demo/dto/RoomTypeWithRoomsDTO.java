package server.demo.dto;

import server.demo.entity.RoomType;
import server.demo.entity.Room;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public class RoomTypeWithRoomsDTO {
    private Long id;
    private String name;
    private String code;
    private Integer totalRooms;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RoomInfoDTO> rooms;
    private BigDecimal defaultPrice;
    private BigDecimal weekdayPrice;
    private BigDecimal weekendPrice;

    // 每天的价格
    private BigDecimal mondayPrice;
    private BigDecimal tuesdayPrice;
    private BigDecimal wednesdayPrice;
    private BigDecimal thursdayPrice;
    private BigDecimal fridayPrice;
    private BigDecimal saturdayPrice;
    private BigDecimal sundayPrice;

    // Constructors
    public RoomTypeWithRoomsDTO() {}

    public RoomTypeWithRoomsDTO(RoomType roomType, List<RoomInfoDTO> rooms) {
        this.id = roomType.getId();
        this.name = roomType.getName();
        this.code = roomType.getCode();
        this.totalRooms = roomType.getTotalRooms();
        this.description = roomType.getDescription();
        this.createdAt = roomType.getCreatedAt();
        this.updatedAt = roomType.getUpdatedAt();
        this.rooms = rooms;
        this.defaultPrice = roomType.getDefaultPrice();
        this.weekdayPrice = roomType.getWeekdayPrice();
        this.weekendPrice = roomType.getWeekendPrice();
        this.mondayPrice = roomType.getMondayPrice();
        this.tuesdayPrice = roomType.getTuesdayPrice();
        this.wednesdayPrice = roomType.getWednesdayPrice();
        this.thursdayPrice = roomType.getThursdayPrice();
        this.fridayPrice = roomType.getFridayPrice();
        this.saturdayPrice = roomType.getSaturdayPrice();
        this.sundayPrice = roomType.getSundayPrice();
    }

    // Getters and Setters
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

    public Integer getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(Integer totalRooms) {
        this.totalRooms = totalRooms;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<RoomInfoDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomInfoDTO> rooms) {
        this.rooms = rooms;
    }

    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public BigDecimal getWeekdayPrice() {
        return weekdayPrice;
    }

    public void setWeekdayPrice(BigDecimal weekdayPrice) {
        this.weekdayPrice = weekdayPrice;
    }

    public BigDecimal getWeekendPrice() {
        return weekendPrice;
    }

    public void setWeekendPrice(BigDecimal weekendPrice) {
        this.weekendPrice = weekendPrice;
    }

    public BigDecimal getMondayPrice() {
        return mondayPrice;
    }

    public void setMondayPrice(BigDecimal mondayPrice) {
        this.mondayPrice = mondayPrice;
    }

    public BigDecimal getTuesdayPrice() {
        return tuesdayPrice;
    }

    public void setTuesdayPrice(BigDecimal tuesdayPrice) {
        this.tuesdayPrice = tuesdayPrice;
    }

    public BigDecimal getWednesdayPrice() {
        return wednesdayPrice;
    }

    public void setWednesdayPrice(BigDecimal wednesdayPrice) {
        this.wednesdayPrice = wednesdayPrice;
    }

    public BigDecimal getThursdayPrice() {
        return thursdayPrice;
    }

    public void setThursdayPrice(BigDecimal thursdayPrice) {
        this.thursdayPrice = thursdayPrice;
    }

    public BigDecimal getFridayPrice() {
        return fridayPrice;
    }

    public void setFridayPrice(BigDecimal fridayPrice) {
        this.fridayPrice = fridayPrice;
    }

    public BigDecimal getSaturdayPrice() {
        return saturdayPrice;
    }

    public void setSaturdayPrice(BigDecimal saturdayPrice) {
        this.saturdayPrice = saturdayPrice;
    }

    public BigDecimal getSundayPrice() {
        return sundayPrice;
    }

    public void setSundayPrice(BigDecimal sundayPrice) {
        this.sundayPrice = sundayPrice;
    }

    // 内部类：房间信息DTO
    public static class RoomInfoDTO {
        private Long id;
        private String roomNumber;
        private String status;
        private Integer floor;
        private String notes;

        // Constructors
        public RoomInfoDTO() {}

        public RoomInfoDTO(Room room) {
            this.id = room.getId();
            this.roomNumber = room.getRoomNumber();
            this.status = room.getStatus() != null ? room.getStatus().name() : "AVAILABLE";
            this.floor = room.getFloor();
            this.notes = room.getNotes();
        }

        // Getters and Setters
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getFloor() {
            return floor;
        }

        public void setFloor(Integer floor) {
            this.floor = floor;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}