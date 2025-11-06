package server.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 批量改价请求DTO
 */
public class BulkPriceChangeRequest {

    /**
     * 房型ID列表（必填）
     */
    @NotEmpty(message = "房型ID列表不能为空")
    private List<Long> roomTypeIds;

    /**
     * 日期范围列表（最多10个）
     */
    @NotEmpty(message = "日期范围列表不能为空")
    @Size(max = 10, message = "最多只能添加10个日期范围")
    private List<DateRangeDTO> dateRanges;

    /**
     * 适用的星期几（1=周一, 2=周二, ..., 6=周六, 0=周日）
     * 如果为空或包含所有值，则表示所有日期都适用
     */
    private Set<Integer> weekdays;

    /**
     * 是否区分平日和周末价格
     */
    @NotNull(message = "是否区分平日周末不能为空")
    private Boolean weekendDifferentiation;

    /**
     * 平日价格（weekendDifferentiation=false时为统一价格）
     */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "价格必须大于0")
    private BigDecimal weekdayPrice;

    /**
     * 周末价格（仅当weekendDifferentiation=true时使用）
     */
    private BigDecimal weekendPrice;

    /**
     * 备注
     */
    private String notes;

    /**
     * 日期范围DTO
     */
    public static class DateRangeDTO {
        @NotNull(message = "开始日期不能为空")
        private LocalDate startDate;

        @NotNull(message = "结束日期不能为空")
        private LocalDate endDate;

        public DateRangeDTO() {}

        public DateRangeDTO(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
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
    }

    // Constructors
    public BulkPriceChangeRequest() {}

    // Getters and Setters
    public List<Long> getRoomTypeIds() {
        return roomTypeIds;
    }

    public void setRoomTypeIds(List<Long> roomTypeIds) {
        this.roomTypeIds = roomTypeIds;
    }

    public List<DateRangeDTO> getDateRanges() {
        return dateRanges;
    }

    public void setDateRanges(List<DateRangeDTO> dateRanges) {
        this.dateRanges = dateRanges;
    }

    public Set<Integer> getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(Set<Integer> weekdays) {
        this.weekdays = weekdays;
    }

    public Boolean getWeekendDifferentiation() {
        return weekendDifferentiation;
    }

    public void setWeekendDifferentiation(Boolean weekendDifferentiation) {
        this.weekendDifferentiation = weekendDifferentiation;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * 验证请求数据
     */
    public void validate() {
        if (weekendDifferentiation && weekendPrice == null) {
            throw new IllegalArgumentException("区分平日周末时，周末价格不能为空");
        }

        for (DateRangeDTO range : dateRanges) {
            if (range.getStartDate().isAfter(range.getEndDate())) {
                throw new IllegalArgumentException("开始日期不能晚于结束日期");
            }
        }

        if (weekdays != null && !weekdays.isEmpty()) {
            for (Integer weekday : weekdays) {
                if (weekday < 0 || weekday > 6) {
                    throw new IllegalArgumentException("星期几的值必须在0-6之间");
                }
            }
        }
    }
}