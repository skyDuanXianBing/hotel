package server.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import server.demo.i18n.ApiMessages;
/**
 * 批量改价请求DTO
 */
public class BulkPriceChangeRequest {

    /**
     * 房型ID列表（必填）
     */
    @NotEmpty(message = "{api.t.4942906b0e3a}")
    private List<Long> roomTypeIds;

    /**
     * 日期范围列表（最多10个）
     */
    @NotEmpty(message = "{api.t.569f7a0b37d1}")
    @Size(max = 10, message = "{api.t.f5f6adce14a7}")
    private List<DateRangeDTO> dateRanges;

    /**
     * 适用的星期几（1=周一, 2=周二, ..., 6=周六, 0=周日）
     * 如果为空或包含所有值，则表示所有日期都适用
     */
    private Set<Integer> weekdays;

    /**
     * 是否区分平日和周末价格
     */
    @NotNull(message = "{api.t.5518bae91051}")
    private Boolean weekendDifferentiation;

    /**
     * 平日价格（weekendDifferentiation=false时为统一价格）
     */
    @NotNull(message = "{api.t.65b3b94588a8}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{api.t.d866edf26493}")
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
        @NotNull(message = "{api.t.897f09b95242}")
        private LocalDate startDate;

        @NotNull(message = "{api.t.6ff06603a351}")
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
            throw new IllegalArgumentException(ApiMessages.get("api.t.65b4094c7678"));
        }

        for (DateRangeDTO range : dateRanges) {
            if (range.getStartDate().isAfter(range.getEndDate())) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.43318cbb9f3c"));
            }
        }

        if (weekdays != null && !weekdays.isEmpty()) {
            for (Integer weekday : weekdays) {
                if (weekday < 0 || weekday > 6) {
                    throw new IllegalArgumentException(ApiMessages.get("api.t.99af22f65434"));
                }
            }
        }
    }
}