package server.demo.dto;

import server.demo.entity.RoomPrice;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RoomPriceDTO {
    private Long id;
    private Long roomTypeId;
    private String roomTypeName;
    private String roomTypeCode;
    private LocalDate priceDate;
    private BigDecimal price;
    private Boolean isWeekend;
    private Boolean isHoliday;
    private String notes;

    // Constructors
    public RoomPriceDTO() {}

    public RoomPriceDTO(RoomPrice roomPrice) {
        this.id = roomPrice.getId();
        this.roomTypeId = roomPrice.getRoomType().getId();
        this.roomTypeName = roomPrice.getRoomType().getName();
        this.roomTypeCode = roomPrice.getRoomType().getCode();
        this.priceDate = roomPrice.getPriceDate();
        this.price = roomPrice.getPrice();
        this.isWeekend = roomPrice.getIsWeekend();
        this.isHoliday = roomPrice.getIsHoliday();
        this.notes = roomPrice.getNotes();
    }

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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}