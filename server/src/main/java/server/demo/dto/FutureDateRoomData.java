package server.demo.dto;

/**
 * 远期房情日期数据DTO
 */
public class FutureDateRoomData {
    private String date; // 日期 (YYYY-MM-DD)
    private String dayOfWeek; // 星期几
    private int available; // 可售
    private int occupied; // 占用
    private int unavailable; // 不可售
    private double availableRate; // 可售比例 (%)
    private double occupiedRate; // 占用比例 (%)
    private double unavailableRate; // 不可售比例 (%)

    // 构造函数
    public FutureDateRoomData() {}

    public FutureDateRoomData(String date, String dayOfWeek, int available, int occupied, int unavailable,
                             double availableRate, double occupiedRate, double unavailableRate) {
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.available = available;
        this.occupied = occupied;
        this.unavailable = unavailable;
        this.availableRate = availableRate;
        this.occupiedRate = occupiedRate;
        this.unavailableRate = unavailableRate;
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getOccupied() {
        return occupied;
    }

    public void setOccupied(int occupied) {
        this.occupied = occupied;
    }

    public int getUnavailable() {
        return unavailable;
    }

    public void setUnavailable(int unavailable) {
        this.unavailable = unavailable;
    }

    public double getAvailableRate() {
        return availableRate;
    }

    public void setAvailableRate(double availableRate) {
        this.availableRate = availableRate;
    }

    public double getOccupiedRate() {
        return occupiedRate;
    }

    public void setOccupiedRate(double occupiedRate) {
        this.occupiedRate = occupiedRate;
    }

    public double getUnavailableRate() {
        return unavailableRate;
    }

    public void setUnavailableRate(double unavailableRate) {
        this.unavailableRate = unavailableRate;
    }
}