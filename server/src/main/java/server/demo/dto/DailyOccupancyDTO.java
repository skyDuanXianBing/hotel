package server.demo.dto;

import java.math.BigDecimal;

/**
 * 每日入住率DTO
 */
public class DailyOccupancyDTO {
    private String date;           // 日期
    private BigDecimal rate;       // 入住率（百分比）
    private Integer occupiedRooms; // 已入住房间数
    private Integer totalRooms;    // 总房间数

    public DailyOccupancyDTO() {
    }

    public DailyOccupancyDTO(String date, BigDecimal rate, Integer occupiedRooms, Integer totalRooms) {
        this.date = date;
        this.rate = rate;
        this.occupiedRooms = occupiedRooms;
        this.totalRooms = totalRooms;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public Integer getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(Integer occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public Integer getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(Integer totalRooms) {
        this.totalRooms = totalRooms;
    }
}