package server.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CreateRoomTypeRequest {
    @NotBlank(message = "{api.t.b81e3dd1b013}")
    private String name;

    @NotBlank(message = "{api.t.a5d68ad681d9}")
    private String code;

    @NotNull(message = "{api.t.c2c401d97003}")
    @Min(value = 1, message = "{api.t.d5822641bf3f}")
    private Integer totalRooms;

    @NotNull(message = "{api.t.aa29dec0ec6d}")
    @Min(value = 1, message = "{api.t.010360759242}")
    private Integer maxGuests;

    @Min(value = 0, message = "{api.t.331df5d8df78}")
    private Integer maxChildOccupancy;

    private String description;
    private String checkInGuideLink;
    private String roomTypeAddress;
    private String nearbyStation;
    private String suRoomType;
    private BigDecimal sizeMeasurement;
    private String sizeMeasurementUnit;

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

    @Size(min = 1, message = "{api.t.69de290b8fec}")
    private List<String> roomNumbers;
    private List<RoomInput> rooms;
    private List<FacilityDTO> facilities;
    private List<String> desktopPhotoUrls;
    private List<String> mobilePhotoUrls;
    private Map<String, LocalizedContentDTO> localizedContent;

    public static class RoomInput {
        @NotBlank(message = "{api.t.d1eaae8bb920}")
        private String roomNumber;
        private String smartlockPasscode;

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

    public String getNearbyStation() {
        return nearbyStation;
    }

    public void setNearbyStation(String nearbyStation) {
        this.nearbyStation = nearbyStation;
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

    public List<String> getRoomNumbers() {
        return roomNumbers;
    }

    public void setRoomNumbers(List<String> roomNumbers) {
        this.roomNumbers = roomNumbers;
    }

    public List<RoomInput> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomInput> rooms) {
        this.rooms = rooms;
    }

    public List<FacilityDTO> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<FacilityDTO> facilities) {
        this.facilities = facilities;
    }

    public List<String> getDesktopPhotoUrls() {
        return desktopPhotoUrls;
    }

    public void setDesktopPhotoUrls(List<String> desktopPhotoUrls) {
        this.desktopPhotoUrls = desktopPhotoUrls;
    }

    public List<String> getMobilePhotoUrls() {
        return mobilePhotoUrls;
    }

    public void setMobilePhotoUrls(List<String> mobilePhotoUrls) {
        this.mobilePhotoUrls = mobilePhotoUrls;
    }

    public Map<String, LocalizedContentDTO> getLocalizedContent() {
        return localizedContent;
    }

    public void setLocalizedContent(Map<String, LocalizedContentDTO> localizedContent) {
        this.localizedContent = localizedContent;
    }
}
