package server.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AssignRoomTypePricePlanRequest {
    private BigDecimal mondayPrice;
    private BigDecimal tuesdayPrice;
    private BigDecimal wednesdayPrice;
    private BigDecimal thursdayPrice;
    private BigDecimal fridayPrice;
    private BigDecimal saturdayPrice;
    private BigDecimal sundayPrice;

    @NotNull(message = "最大入住人数不能为空")
    @Min(value = 1, message = "最大入住人数必须大于0")
    private Integer maxGuests;

    private Integer includedGuests;
    private String priceMode;

    // Constructors
    public AssignRoomTypePricePlanRequest() {}

    // Getters and Setters
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

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Integer getIncludedGuests() {
        return includedGuests;
    }

    public void setIncludedGuests(Integer includedGuests) {
        this.includedGuests = includedGuests;
    }

    public String getPriceMode() {
        return priceMode;
    }

    public void setPriceMode(String priceMode) {
        this.priceMode = priceMode;
    }
}
