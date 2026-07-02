package server.demo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 经营指标统计DTO
 * 包含酒店核心经营指标、趋势数据和明细数据
 */
public class OperationalMetricsDTO {
    // 总房费
    private BigDecimal totalRoomFee;

    // 平均房价 ADR (Average Daily Rate)
    // 计算公式: 总房费收入 / 已售出的房间数
    private BigDecimal averageDailyRate;

    // 入住率 Occ (Occupancy Rate)
    // 计算公式: (已售出的房间数 / 可售房间总数) * 100%
    private BigDecimal occupancyRate;

    // 平均每房收益 RevPAR (Revenue Per Available Room)
    // 计算公式: 总房费收入 / 可售房间总数  或  ADR * Occ
    private BigDecimal revPAR;

    // 累计出售间夜数
    private Integer totalSoldRoomNights;

    // 可售房间总数 (总房间数 * 天数)
    private Integer totalAvailableRoomNights;

    // 总房间数
    private Integer totalRooms;

    // 统计天数
    private Integer days;

    // 收入口径元数据
    private RevenuePrecisionDTO revenuePrecision;

    // 每日趋势数据
    private List<DailyMetricsDTO> dailyTrends;

    // 房费明细数据
    private List<RoomDetailDTO> roomFeeDetails;

    // 间夜明细数据
    private List<RoomDetailDTO> roomNightsDetails;

    // 入住率明细数据
    private List<RoomDetailDTO> occupancyDetails;

    // RevPAR明细数据
    private List<RoomDetailDTO> revparDetails;

    // Constructors
    public OperationalMetricsDTO() {}

    public OperationalMetricsDTO(BigDecimal totalRoomFee, BigDecimal averageDailyRate,
                                BigDecimal occupancyRate, BigDecimal revPAR,
                                Integer totalSoldRoomNights, Integer totalAvailableRoomNights,
                                Integer totalRooms, Integer days) {
        this.totalRoomFee = totalRoomFee;
        this.averageDailyRate = averageDailyRate;
        this.occupancyRate = occupancyRate;
        this.revPAR = revPAR;
        this.totalSoldRoomNights = totalSoldRoomNights;
        this.totalAvailableRoomNights = totalAvailableRoomNights;
        this.totalRooms = totalRooms;
        this.days = days;
    }

    // Getters and Setters
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

    public BigDecimal getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(BigDecimal occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public BigDecimal getRevPAR() {
        return revPAR;
    }

    public void setRevPAR(BigDecimal revPAR) {
        this.revPAR = revPAR;
    }

    public Integer getTotalSoldRoomNights() {
        return totalSoldRoomNights;
    }

    public void setTotalSoldRoomNights(Integer totalSoldRoomNights) {
        this.totalSoldRoomNights = totalSoldRoomNights;
    }

    public Integer getTotalAvailableRoomNights() {
        return totalAvailableRoomNights;
    }

    public void setTotalAvailableRoomNights(Integer totalAvailableRoomNights) {
        this.totalAvailableRoomNights = totalAvailableRoomNights;
    }

    public Integer getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(Integer totalRooms) {
        this.totalRooms = totalRooms;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public RevenuePrecisionDTO getRevenuePrecision() {
        return revenuePrecision;
    }

    public void setRevenuePrecision(RevenuePrecisionDTO revenuePrecision) {
        this.revenuePrecision = revenuePrecision;
    }

    public List<DailyMetricsDTO> getDailyTrends() {
        return dailyTrends;
    }

    public void setDailyTrends(List<DailyMetricsDTO> dailyTrends) {
        this.dailyTrends = dailyTrends;
    }

    public List<RoomDetailDTO> getRoomFeeDetails() {
        return roomFeeDetails;
    }

    public void setRoomFeeDetails(List<RoomDetailDTO> roomFeeDetails) {
        this.roomFeeDetails = roomFeeDetails;
    }

    public List<RoomDetailDTO> getRoomNightsDetails() {
        return roomNightsDetails;
    }

    public void setRoomNightsDetails(List<RoomDetailDTO> roomNightsDetails) {
        this.roomNightsDetails = roomNightsDetails;
    }

    public List<RoomDetailDTO> getOccupancyDetails() {
        return occupancyDetails;
    }

    public void setOccupancyDetails(List<RoomDetailDTO> occupancyDetails) {
        this.occupancyDetails = occupancyDetails;
    }

    public List<RoomDetailDTO> getRevparDetails() {
        return revparDetails;
    }

    public void setRevparDetails(List<RoomDetailDTO> revparDetails) {
        this.revparDetails = revparDetails;
    }
}
