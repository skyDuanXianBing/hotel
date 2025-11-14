package server.demo.dto;

import java.math.BigDecimal;

/**
 * 每日经营指标DTO
 * 用于趋势图展示
 */
public class DailyMetricsDTO {
    // 日期
    private String date;

    // 总房费
    private BigDecimal totalRoomFee;

    // 平均房价 ADR
    private BigDecimal averageDailyRate;

    // 平均每房收益 RevPAR
    private BigDecimal revPAR;

    // 间夜数
    private Integer roomNights;

    // 入住率
    private BigDecimal occupancyRate;

    // Constructors
    public DailyMetricsDTO() {}

    public DailyMetricsDTO(String date, BigDecimal totalRoomFee, BigDecimal averageDailyRate,
                          BigDecimal revPAR, Integer roomNights, BigDecimal occupancyRate) {
        this.date = date;
        this.totalRoomFee = totalRoomFee;
        this.averageDailyRate = averageDailyRate;
        this.revPAR = revPAR;
        this.roomNights = roomNights;
        this.occupancyRate = occupancyRate;
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getTotalRoomFee() {
        return totalRoomFee;
    }

    public void setTotalRoomFee(BigDecimal totalRoomFee) {
        this.totalRoomFee = totalRoomFee;
    }

    public BigDecimal getAverageDailyRate() {
        return averageDailyRate;
    }

    public void setAverageDailyRate(BigDecimal averageDailyRate) {
        this.averageDailyRate = averageDailyRate;
    }

    public BigDecimal getRevPAR() {
        return revPAR;
    }

    public void setRevPAR(BigDecimal revPAR) {
        this.revPAR = revPAR;
    }

    public Integer getRoomNights() {
        return roomNights;
    }

    public void setRoomNights(Integer roomNights) {
        this.roomNights = roomNights;
    }

    public BigDecimal getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(BigDecimal occupancyRate) {
        this.occupancyRate = occupancyRate;
    }
}
