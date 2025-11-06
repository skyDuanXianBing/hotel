package server.demo.dto;

import java.math.BigDecimal;

/**
 * 消费记录DTO
 */
public class ConsumptionDTO {
    private Long id;
    private Long reservationId;
    private String item;
    private Integer quantity;
    private BigDecimal amount;
    private String date;
    private String remark;
    private String createdBy;
    private String createdAt;

    // Constructors
    public ConsumptionDTO() {
    }

    public ConsumptionDTO(Long id, Long reservationId, String item, Integer quantity,
                         BigDecimal amount, String date, String remark, String createdBy, String createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.item = item;
        this.quantity = quantity;
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

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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