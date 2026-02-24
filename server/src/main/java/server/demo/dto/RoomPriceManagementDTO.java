package server.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 房价管理DTO
 * 用于房价管理页面展示
 */
public class RoomPriceManagementDTO {
    private Long id;
    private Long roomTypeId;
    private String roomTypeName;
    private String roomTypeCode;
    private Long pricePlanId;
    private String pricePlanName;
    private LocalDate priceDate;
    private BigDecimal price;
    private Integer availableRooms;
    private Integer minStay;
    private Integer maxStay;
    private Boolean closeRoom;
    private Boolean cta;
    private Boolean ctd;
    private Boolean isWeekend;
    private Boolean isHoliday;
    private String priceSource;
    private Boolean manualOverride;
    private LocalDate manualOverrideUntil;
    private String notes;

    // Constructors
    public RoomPriceManagementDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRoomTypeCode() {
        return roomTypeCode;
    }

    public void setRoomTypeCode(String roomTypeCode) {
        this.roomTypeCode = roomTypeCode;
    }

    public Long getPricePlanId() {
        return pricePlanId;
    }

    public void setPricePlanId(Long pricePlanId) {
        this.pricePlanId = pricePlanId;
    }

    public String getPricePlanName() {
        return pricePlanName;
    }

    public void setPricePlanName(String pricePlanName) {
        this.pricePlanName = pricePlanName;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(Integer availableRooms) {
        this.availableRooms = availableRooms;
    }

    public Integer getMinStay() {
        return minStay;
    }

    public void setMinStay(Integer minStay) {
        this.minStay = minStay;
    }

    public Integer getMaxStay() {
        return maxStay;
    }

    public void setMaxStay(Integer maxStay) {
        this.maxStay = maxStay;
    }

    public Boolean getCloseRoom() {
        return closeRoom;
    }

    public void setCloseRoom(Boolean closeRoom) {
        this.closeRoom = closeRoom;
    }

    public Boolean getCta() {
        return cta;
    }

    public void setCta(Boolean cta) {
        this.cta = cta;
    }

    public Boolean getCtd() {
        return ctd;
    }

    public void setCtd(Boolean ctd) {
        this.ctd = ctd;
    }

    public Boolean getIsWeekend() {
        return isWeekend;
    }

    public void setIsWeekend(Boolean isWeekend) {
        this.isWeekend = isWeekend;
    }

    public Boolean getIsHoliday() {
        return isHoliday;
    }

    public void setIsHoliday(Boolean isHoliday) {
        this.isHoliday = isHoliday;
    }

    public String getPriceSource() {
        return priceSource;
    }

    public void setPriceSource(String priceSource) {
        this.priceSource = priceSource;
    }

    public Boolean getManualOverride() {
        return manualOverride;
    }

    public void setManualOverride(Boolean manualOverride) {
        this.manualOverride = manualOverride;
    }

    public LocalDate getManualOverrideUntil() {
        return manualOverrideUntil;
    }

    public void setManualOverrideUntil(LocalDate manualOverrideUntil) {
        this.manualOverrideUntil = manualOverrideUntil;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
