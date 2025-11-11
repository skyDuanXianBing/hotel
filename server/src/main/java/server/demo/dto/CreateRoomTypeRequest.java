package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class CreateRoomTypeRequest {
    @NotBlank(message = "房型名称不能为空")
    private String name;

    @NotBlank(message = "房型代码不能为空")
    private String code;

    @NotNull(message = "房间总数不能为空")
    @Min(value = 1, message = "房间总数必须大于0")
    private Integer totalRooms;

    private String description;

    private BigDecimal defaultPrice;
    private BigDecimal weekdayPrice;
    private BigDecimal weekendPrice;

    // 周价格字段
    private BigDecimal mondayPrice;
    private BigDecimal tuesdayPrice;
    private BigDecimal wednesdayPrice;
    private BigDecimal thursdayPrice;
    private BigDecimal fridayPrice;
    private BigDecimal saturdayPrice;
    private BigDecimal sundayPrice;

    @Size(min = 1, message = "至少需要一个房间号")
    private List<String> roomNumbers;

    // Constructors
    public CreateRoomTypeRequest() {}

    public CreateRoomTypeRequest(String name, String code, Integer totalRooms, String description, List<String> roomNumbers) {
        this.name = name;
        this.code = code;
        this.totalRooms = totalRooms;
        this.description = description;
        this.roomNumbers = roomNumbers;
    }

    // Getters and Setters
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

    public List<String> getRoomNumbers() {
        return roomNumbers;
    }

    public void setRoomNumbers(List<String> roomNumbers) {
        this.roomNumbers = roomNumbers;
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
}