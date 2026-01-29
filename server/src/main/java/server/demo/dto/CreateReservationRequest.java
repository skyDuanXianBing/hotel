package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreateReservationRequest {
    
    @NotBlank(message = "客人姓名不能为空")
    private String guestName;
    
    private String guestPhone;
    
    private String guestIdCard;
    
    @NotNull(message = "房间ID不能为空")
    private Long roomId;
    
    @NotNull(message = "渠道ID不能为空")
    private Long channelId;
    
    @NotNull(message = "入住日期不能为空")
    private LocalDate checkInDate;
    
    @NotNull(message = "离店日期不能为空")
    private LocalDate checkOutDate;
    
    @NotNull(message = "成人数量不能为空")
    @Min(value = 1, message = "成人数量必须大于0")
    private Integer adults = 1;
    
    private Integer children = 0;
    
    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;
    
    private String channelOrderNumber;

    private String paymentMethod;

    private BigDecimal commission;

    private BigDecimal otherFees;

    private String pricePlan;

    private String specialRequests;

    private LocalDateTime bookingDate;

    private String notes;

    // 是否直接入住（创建时直接设置为已入住状态）
    private Boolean directCheckIn = false;

    // Getters and Setters
    public String getGuestName() {
        return guestName;
    }
    
    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }
    
    public String getGuestPhone() {
        return guestPhone;
    }
    
    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }
    
    public String getGuestIdCard() {
        return guestIdCard;
    }
    
    public void setGuestIdCard(String guestIdCard) {
        this.guestIdCard = guestIdCard;
    }
    
    public Long getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
    
    public Long getChannelId() {
        return channelId;
    }
    
    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }
    
    public LocalDate getCheckInDate() {
        return checkInDate;
    }
    
    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }
    
    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }
    
    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }
    
    public Integer getAdults() {
        return adults;
    }
    
    public void setAdults(Integer adults) {
        this.adults = adults;
    }
    
    public Integer getChildren() {
        return children;
    }
    
    public void setChildren(Integer children) {
        this.children = children;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getChannelOrderNumber() {
        return channelOrderNumber;
    }
    
    public void setChannelOrderNumber(String channelOrderNumber) {
        this.channelOrderNumber = channelOrderNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public BigDecimal getOtherFees() {
        return otherFees;
    }

    public void setOtherFees(BigDecimal otherFees) {
        this.otherFees = otherFees;
    }

    public String getPricePlan() {
        return pricePlan;
    }

    public void setPricePlan(String pricePlan) {
        this.pricePlan = pricePlan;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getDirectCheckIn() {
        return directCheckIn;
    }

    public void setDirectCheckIn(Boolean directCheckIn) {
        this.directCheckIn = directCheckIn;
    }
}
