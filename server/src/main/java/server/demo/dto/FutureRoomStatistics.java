package server.demo.dto;

/**
 * 远期房情底部统计数据DTO
 */
public class FutureRoomStatistics {
    private String date; // 日期
    private int effectiveRooms; // 有效客房数
    private double expectedOccupancyRate; // 入住率（预期）%
    private double expectedRoomRevenue; // 客房收入（预期）
    private double expectedTotalRoomFee; // 总房费（预期）
    private double averageRoomRevenue; // 平均客房收益（预期）

    // 构造函数
    public FutureRoomStatistics() {}

    public FutureRoomStatistics(String date, int effectiveRooms, double expectedOccupancyRate,
                               double expectedRoomRevenue, double expectedTotalRoomFee, double averageRoomRevenue) {
        this.date = date;
        this.effectiveRooms = effectiveRooms;
        this.expectedOccupancyRate = expectedOccupancyRate;
        this.expectedRoomRevenue = expectedRoomRevenue;
        this.expectedTotalRoomFee = expectedTotalRoomFee;
        this.averageRoomRevenue = averageRoomRevenue;
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getEffectiveRooms() {
        return effectiveRooms;
    }

    public void setEffectiveRooms(int effectiveRooms) {
        this.effectiveRooms = effectiveRooms;
    }

    public double getExpectedOccupancyRate() {
        return expectedOccupancyRate;
    }

    public void setExpectedOccupancyRate(double expectedOccupancyRate) {
        this.expectedOccupancyRate = expectedOccupancyRate;
    }

    public double getExpectedRoomRevenue() {
        return expectedRoomRevenue;
    }

    public void setExpectedRoomRevenue(double expectedRoomRevenue) {
        this.expectedRoomRevenue = expectedRoomRevenue;
    }

    public double getExpectedTotalRoomFee() {
        return expectedTotalRoomFee;
    }

    public void setExpectedTotalRoomFee(double expectedTotalRoomFee) {
        this.expectedTotalRoomFee = expectedTotalRoomFee;
    }

    public double getAverageRoomRevenue() {
        return averageRoomRevenue;
    }

    public void setAverageRoomRevenue(double averageRoomRevenue) {
        this.averageRoomRevenue = averageRoomRevenue;
    }
}