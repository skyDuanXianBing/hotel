package server.demo.dto;

public class ReservationStatistics {
    private long todayCheckinCount;
    private long todayCheckoutCount;
    private long todayNewCount;
    private long unassignedCount;
    private long pendingCount;
    private long totalReservations;

    // Constructors
    public ReservationStatistics() {}

    public ReservationStatistics(long todayCheckinCount, long todayCheckoutCount, 
                               long todayNewCount, long unassignedCount, 
                               long pendingCount, long totalReservations) {
        this.todayCheckinCount = todayCheckinCount;
        this.todayCheckoutCount = todayCheckoutCount;
        this.todayNewCount = todayNewCount;
        this.unassignedCount = unassignedCount;
        this.pendingCount = pendingCount;
        this.totalReservations = totalReservations;
    }

    // Getters and Setters
    public long getTodayCheckinCount() {
        return todayCheckinCount;
    }

    public void setTodayCheckinCount(long todayCheckinCount) {
        this.todayCheckinCount = todayCheckinCount;
    }

    public long getTodayCheckoutCount() {
        return todayCheckoutCount;
    }

    public void setTodayCheckoutCount(long todayCheckoutCount) {
        this.todayCheckoutCount = todayCheckoutCount;
    }

    public long getTodayNewCount() {
        return todayNewCount;
    }

    public void setTodayNewCount(long todayNewCount) {
        this.todayNewCount = todayNewCount;
    }

    public long getUnassignedCount() {
        return unassignedCount;
    }

    public void setUnassignedCount(long unassignedCount) {
        this.unassignedCount = unassignedCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getTotalReservations() {
        return totalReservations;
    }

    public void setTotalReservations(long totalReservations) {
        this.totalReservations = totalReservations;
    }
}