package server.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateRoomPriceRequest {
    @NotNull(message = "{api.t.9b3078915166}")
    private Long roomTypeId;

    @NotNull(message = "{api.t.897f09b95242}")
    private LocalDate startDate;

    @NotNull(message = "{api.t.6ff06603a351}")
    private LocalDate endDate;

    @NotNull(message = "{api.t.65b3b94588a8}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{api.t.d866edf26493}")
    private BigDecimal price;

    private Boolean isHoliday;
    private String notes;

    // Constructors
    public UpdateRoomPriceRequest() {}

    public UpdateRoomPriceRequest(Long roomTypeId, LocalDate startDate, LocalDate endDate, BigDecimal price) {
        this.roomTypeId = roomTypeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.price = price;
    }

    // Getters and Setters
    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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