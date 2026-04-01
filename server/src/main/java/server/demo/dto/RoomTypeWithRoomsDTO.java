package server.demo.dto;

import server.demo.entity.Room;
import server.demo.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RoomTypeWithRoomsDTO {
    private Long id;
    private String name;
    private String code;
    private Integer totalRooms;
    private Integer maxGuests;
    private Integer maxChildOccupancy;
    private String description;
    private String checkInGuideLink;
    private String roomTypeAddress;
    private String suRoomType;
    private BigDecimal sizeMeasurement;
    private String sizeMeasurementUnit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RoomInfoDTO> rooms;
    private BigDecimal defaultPrice;
    private BigDecimal weekdayPrice;
    private BigDecimal weekendPrice;
    private BigDecimal mondayPrice;
    private BigDecimal tuesdayPrice;
    private BigDecimal wednesdayPrice;
    private BigDecimal thursdayPrice;
    private BigDecimal fridayPrice;
    private BigDecimal saturdayPrice;
    private BigDecimal sundayPrice;

    public RoomTypeWithRoomsDTO() {}

    public RoomTypeWithRoomsDTO(RoomType roomType, List<RoomInfoDTO> rooms) {
        this.id = roomType.getId();
        this.name = roomType.getName();
        this.code = roomType.getCode();
        this.totalRooms = roomType.getTotalRooms();
        this.maxGuests = roomType.getMaxGuests();
        this.maxChildOccupancy = roomType.getMaxChildOccupancy();
        this.description = roomType.getDescription();
        this.checkInGuideLink = roomType.getCheckInGuideLink();
        this.roomTypeAddress = roomType.getRoomTypeAddress();
        this.suRoomType = roomType.getSuRoomType();
        this.sizeMeasurement = roomType.getSizeMeasurement();
        this.sizeMeasurementUnit = roomType.getSizeMeasurementUnit();
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

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Integer getMaxChildOccupancy() {
        return maxChildOccupancy;
    }

    public void setMaxChildOccupancy(Integer maxChildOccupancy) {
        this.maxChildOccupancy = maxChildOccupancy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCheckInGuideLink() {
        return checkInGuideLink;
    }

    public void setCheckInGuideLink(String checkInGuideLink) {
        this.checkInGuideLink = checkInGuideLink;
    }

    public String getRoomTypeAddress() {
        return roomTypeAddress;
    }

    public void setRoomTypeAddress(String roomTypeAddress) {
        this.roomTypeAddress = roomTypeAddress;
    }

    public String getSuRoomType() {
        return suRoomType;
    }

    public void setSuRoomType(String suRoomType) {
        this.suRoomType = suRoomType;
    }

    public BigDecimal getSizeMeasurement() {
        return sizeMeasurement;
    }

    public void setSizeMeasurement(BigDecimal sizeMeasurement) {
        this.sizeMeasurement = sizeMeasurement;
    }

    public String getSizeMeasurementUnit() {
        return sizeMeasurementUnit;
    }

    public void setSizeMeasurementUnit(String sizeMeasurementUnit) {
        this.sizeMeasurementUnit = sizeMeasurementUnit;
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

    public static class RoomInfoDTO {
        private Long id;
        private String roomNumber;
        private String smartlockPasscode;
        private String status;
        private Integer floor;
        private String notes;

        public RoomInfoDTO() {}

        public RoomInfoDTO(Room room) {
            this.id = room.getId();
            this.roomNumber = room.getRoomNumber();
            this.smartlockPasscode = room.getSmartlockPasscode();
            this.status = room.getStatus() != null ? room.getStatus().name() : "AVAILABLE";
            this.floor = room.getFloor();
            this.notes = room.getNotes();
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

        public String getSmartlockPasscode() {
            return smartlockPasscode;
        }

        public void setSmartlockPasscode(String smartlockPasscode) {
            this.smartlockPasscode = smartlockPasscode;
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
