package server.demo.dto;

import java.math.BigDecimal;

/**
 * 收款记录DTO
 */
public class PaymentDTO {
    private Long id;
    private Long reservationId;
    private String type;
    private String paymentMethod;
    private BigDecimal amount;
    private String date;
    private String remark;
    private String createdBy;
    private String createdAt;

    // Constructors
    public PaymentDTO() {
    }

    public PaymentDTO(Long id, Long reservationId, String type, String paymentMethod,
                     BigDecimal amount, String date, String remark, String createdBy, String createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.type = type;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.date = date;
        this.remark = remark;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}