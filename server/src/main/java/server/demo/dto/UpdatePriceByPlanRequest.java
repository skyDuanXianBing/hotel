package server.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 按价格计划更新价格请求
 */
public class UpdatePriceByPlanRequest {
    @NotNull(message = "房型ID不能为空")
    private Long roomTypeId;

    @NotNull(message = "价格计划ID不能为空")
    private Long pricePlanId;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private List<Integer> weekdays; // 0=全部, 1=周一, 2=周二, ..., 7=周日

    private BigDecimal price;

    private Integer availableRooms;

    @jakarta.validation.constraints.Min(value = 1, message = "最小入住天数必须大于等于1")
    @jakarta.validation.constraints.Max(value = 99, message = "最小入住天数必须小于等于99")
    private Integer minStay;

    @jakarta.validation.constraints.Min(value = 1, message = "最大入住天数必须大于等于1")
    @jakarta.validation.constraints.Max(value = 99, message = "最大入住天数必须小于等于99")
    private Integer maxStay;

    private String notes;

    // Constructors
    public UpdatePriceByPlanRequest() {}

    // Getters and Setters
    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public Long getPricePlanId() {
        return pricePlanId;
    }

    public void setPricePlanId(Long pricePlanId) {
        this.pricePlanId = pricePlanId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<Integer> getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(List<Integer> weekdays) {
        this.weekdays = weekdays;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
