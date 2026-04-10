package server.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservationDTO {
    
    private Long id;
    private String orderNumber;
    private String groupOrderNo;
    private String guestName;
    private String phone;
    private Long roomId;
    private String roomNumber;
    private String roomTypeName;
    private Long channelId;
    private String channelName;
    private String channelOrderNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status; // RESERVED, CHECKED_IN, CHECKED_OUT, CANCELLED
    private Integer adults;
    private Integer children;
    private String paymentMethod;
    private java.math.BigDecimal commission;
    private java.math.BigDecimal paidAmount;
    private Boolean settled;
    private String pricePlan;
    private String createdBy;
    private String notes;
    
    // Su / OTA fields (for 未排房/未映射排查)
    private String reservationNotifId;
    private String suReservationId;
    private String otaRoomId;
    private Long otaRoomTypeId;
    private java.math.BigDecimal totalAmount;
    private java.math.BigDecimal currentRoomPrice; // 当前房型价格
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public ReservationDTO() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getGroupOrderNo() {
        return groupOrderNo;
    }

    public void setGroupOrderNo(String groupOrderNo) {
        this.groupOrderNo = groupOrderNo;
    }
    
    public String getGuestName() {
        return guestName;
    }
    
    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Long getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
    
    public String getRoomNumber() {
        return roomNumber;
    }
    
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
    
    public String getRoomTypeName() {
        return roomTypeName;
    }
    
    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }
    
    public Long getChannelId() {
        return channelId;
    }
    
    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
    
    public String getChannelOrderNumber() {
        return channelOrderNumber;
    }
    
    public void setChannelOrderNumber(String channelOrderNumber) {
        this.channelOrderNumber = channelOrderNumber;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public java.math.BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(java.math.BigDecimal commission) {
        this.commission = commission;
    }

    public java.math.BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(java.math.BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Boolean getSettled() {
        return settled;
    }

    public void setSettled(Boolean settled) {
        this.settled = settled;
    }

    public String getPricePlan() {
        return pricePlan;
    }

    public void setPricePlan(String pricePlan) {
        this.pricePlan = pricePlan;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getReservationNotifId() {
        return reservationNotifId;
    }
    
    public void setReservationNotifId(String reservationNotifId) {
        this.reservationNotifId = reservationNotifId;
    }
    
    public String getSuReservationId() {
        return suReservationId;
    }
    
    public void setSuReservationId(String suReservationId) {
        this.suReservationId = suReservationId;
    }
    
    public String getOtaRoomId() {
        return otaRoomId;
    }
    
    public void setOtaRoomId(String otaRoomId) {
        this.otaRoomId = otaRoomId;
    }
    
    public Long getOtaRoomTypeId() {
        return otaRoomTypeId;
    }
    
    public void setOtaRoomTypeId(Long otaRoomTypeId) {
        this.otaRoomTypeId = otaRoomTypeId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public java.math.BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(java.math.BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public java.math.BigDecimal getCurrentRoomPrice() {
        return currentRoomPrice;
    }
    
    public void setCurrentRoomPrice(java.math.BigDecimal currentRoomPrice) {
        this.currentRoomPrice = currentRoomPrice;
    }
}
