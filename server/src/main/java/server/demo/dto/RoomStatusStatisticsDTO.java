package server.demo.dto;

import java.time.LocalDate;

public class RoomStatusStatisticsDTO {
    private LocalDate date;
    private Long todayArrivals;          // 今日预抵
    private Long todayDepartures;        // 今日预离
    private Long todayNewOrders;         // 今日新办
    private Long availableRooms;         // 今日可售
    private Long unassignedOrders;       // 未排房
    private Long pendingOrders;          // 待处理

    public RoomStatusStatisticsDTO() {}

    public RoomStatusStatisticsDTO(LocalDate date, Long todayArrivals, Long todayDepartures, 
                                  Long todayNewOrders, Long availableRooms, Long unassignedOrders, 
                                  Long pendingOrders) {
        this.date = date;
        this.todayArrivals = todayArrivals;
        this.todayDepartures = todayDepartures;
        this.todayNewOrders = todayNewOrders;
        this.availableRooms = availableRooms;
        this.unassignedOrders = unassignedOrders;
        this.pendingOrders = pendingOrders;
    }

    // Getters and setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getTodayArrivals() {
        return todayArrivals;
    }

    public void setTodayArrivals(Long todayArrivals) {
        this.todayArrivals = todayArrivals;
    }

    public Long getTodayDepartures() {
        return todayDepartures;
    }

    public void setTodayDepartures(Long todayDepartures) {
        this.todayDepartures = todayDepartures;
    }

    public Long getTodayNewOrders() {
        return todayNewOrders;
    }

    public void setTodayNewOrders(Long todayNewOrders) {
        this.todayNewOrders = todayNewOrders;
    }

    public Long getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(Long availableRooms) {
        this.availableRooms = availableRooms;
    }

    public Long getUnassignedOrders() {
        return unassignedOrders;
    }

    public void setUnassignedOrders(Long unassignedOrders) {
        this.unassignedOrders = unassignedOrders;
    }

    public Long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(Long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }
}