package server.demo.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 房情表统计数据DTO
 */
public class RoomTableStatisticsDTO {
    private String roomTypeName; // 房型名称
    private Integer totalRooms; // 总房数
    private Integer availableForSale; // 可售房
    private Integer availableRooms; // 可用房
    private Integer occupiedRooms; // 在住
    private Integer occupiedWithoutDeparture; // 在住（不含预离）
    private Integer scheduledDeparture; // 预离
    private Integer scheduledArrival; // 预抵
    private Integer reservedRooms; // 保留房（关房）
    private Integer maintenanceRooms; // 维修房（关房）
    private Integer outOfOrderRooms; // 停用房（关房）
    private Integer linkedClosedRooms; // 链接关房（关房）
    private Integer cleanRooms; // 净房
    private Integer dirtyRooms; // 脏房
    private BigDecimal expectedOccupancyRate; // 预计入住率（百分比）
    private Integer dailyCancelledRooms; // 当日取消房

    public RoomTableStatisticsDTO() {}

    public RoomTableStatisticsDTO(String roomTypeName, Integer totalRooms) {
        this.roomTypeName = roomTypeName;
        this.totalRooms = totalRooms;
        // 初始化其他字段为0
        this.availableForSale = 0;
        this.availableRooms = 0;
        this.occupiedRooms = 0;
        this.occupiedWithoutDeparture = 0;
        this.scheduledDeparture = 0;
        this.scheduledArrival = 0;
        this.reservedRooms = 0;
        this.maintenanceRooms = 0;
        this.outOfOrderRooms = 0;
        this.linkedClosedRooms = 0;
        this.cleanRooms = 0;
        this.dirtyRooms = 0;
        this.expectedOccupancyRate = BigDecimal.ZERO;
        this.dailyCancelledRooms = 0;
    }

    /**
     * 计算可售房数量
     * 可售房 = 总房数 - 预抵 - 在住(不含预离) - 关房
     */
    public void calculateAvailableForSale() {
        int closedRooms = (reservedRooms != null ? reservedRooms : 0) + 
                         (maintenanceRooms != null ? maintenanceRooms : 0) + 
                         (outOfOrderRooms != null ? outOfOrderRooms : 0) + 
                         (linkedClosedRooms != null ? linkedClosedRooms : 0);
        
        this.availableForSale = (totalRooms != null ? totalRooms : 0) - 
                               (scheduledArrival != null ? scheduledArrival : 0) - 
                               (occupiedWithoutDeparture != null ? occupiedWithoutDeparture : 0) - 
                               closedRooms;
        
        if (this.availableForSale < 0) {
            this.availableForSale = 0;
        }
    }

    /**
     * 计算可用房数量
     * 可用房 = 总房数 - 预离 - 在住(不含预离) - 关房
     */
    public void calculateAvailableRooms() {
        int closedRooms = (reservedRooms != null ? reservedRooms : 0) + 
                         (maintenanceRooms != null ? maintenanceRooms : 0) + 
                         (outOfOrderRooms != null ? outOfOrderRooms : 0) + 
                         (linkedClosedRooms != null ? linkedClosedRooms : 0);
        
        this.availableRooms = (totalRooms != null ? totalRooms : 0) - 
                             (scheduledDeparture != null ? scheduledDeparture : 0) - 
                             (occupiedWithoutDeparture != null ? occupiedWithoutDeparture : 0) - 
                             closedRooms;
        
        if (this.availableRooms < 0) {
            this.availableRooms = 0;
        }
    }

    /**
     * 计算在住房数量
     * 在住 = 在住(不含预离) + 预离
     */
    public void calculateOccupiedRooms() {
        this.occupiedRooms = (occupiedWithoutDeparture != null ? occupiedWithoutDeparture : 0) + 
                            (scheduledDeparture != null ? scheduledDeparture : 0);
    }

    /**
     * 计算预计入住率
     * 预计入住率 = (在住房数 + 预抵房数) / 总房数 * 100
     */
    public void calculateExpectedOccupancyRate() {
        if (totalRooms == null || totalRooms == 0) {
            this.expectedOccupancyRate = BigDecimal.ZERO;
            return;
        }
        
        int occupiedTotal = (occupiedWithoutDeparture != null ? occupiedWithoutDeparture : 0) + 
                           (scheduledArrival != null ? scheduledArrival : 0);
        
        BigDecimal rate = BigDecimal.valueOf(occupiedTotal)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalRooms), 1, RoundingMode.HALF_UP);
        
        this.expectedOccupancyRate = rate;
    }

    // Getters and Setters
    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Integer getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(Integer totalRooms) {
        this.totalRooms = totalRooms;
    }

    public Integer getAvailableForSale() {
        return availableForSale;
    }

    public void setAvailableForSale(Integer availableForSale) {
        this.availableForSale = availableForSale;
    }

    public Integer getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(Integer availableRooms) {
        this.availableRooms = availableRooms;
    }

    public Integer getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(Integer occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public Integer getOccupiedWithoutDeparture() {
        return occupiedWithoutDeparture;
    }

    public void setOccupiedWithoutDeparture(Integer occupiedWithoutDeparture) {
        this.occupiedWithoutDeparture = occupiedWithoutDeparture;
    }

    public Integer getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(Integer scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public Integer getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(Integer scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public Integer getReservedRooms() {
        return reservedRooms;
    }

    public void setReservedRooms(Integer reservedRooms) {
        this.reservedRooms = reservedRooms;
    }

    public Integer getMaintenanceRooms() {
        return maintenanceRooms;
    }

    public void setMaintenanceRooms(Integer maintenanceRooms) {
        this.maintenanceRooms = maintenanceRooms;
    }

    public Integer getOutOfOrderRooms() {
        return outOfOrderRooms;
    }

    public void setOutOfOrderRooms(Integer outOfOrderRooms) {
        this.outOfOrderRooms = outOfOrderRooms;
    }

    public Integer getLinkedClosedRooms() {
        return linkedClosedRooms;
    }

    public void setLinkedClosedRooms(Integer linkedClosedRooms) {
        this.linkedClosedRooms = linkedClosedRooms;
    }

    public Integer getCleanRooms() {
        return cleanRooms;
    }

    public void setCleanRooms(Integer cleanRooms) {
        this.cleanRooms = cleanRooms;
    }

    public Integer getDirtyRooms() {
        return dirtyRooms;
    }

    public void setDirtyRooms(Integer dirtyRooms) {
        this.dirtyRooms = dirtyRooms;
    }

    public BigDecimal getExpectedOccupancyRate() {
        return expectedOccupancyRate;
    }

    public void setExpectedOccupancyRate(BigDecimal expectedOccupancyRate) {
        this.expectedOccupancyRate = expectedOccupancyRate;
    }

    public Integer getDailyCancelledRooms() {
        return dailyCancelledRooms;
    }

    public void setDailyCancelledRooms(Integer dailyCancelledRooms) {
        this.dailyCancelledRooms = dailyCancelledRooms;
    }
}