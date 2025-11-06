package server.demo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 营业汇总DTO
 */
public class BusinessSummaryDTO {
    private BigDecimal totalRevenue; // 总营业收入
    private Integer totalOrders; // 总订单数
    private Integer totalRoomNights; // 总间夜数
    private BigDecimal averageRoomRate; // 平均房价
    private BigDecimal occupancyRate; // 出租率

    // 按渠道统计
    private List<ChannelRevenue> revenueByChannel;

    // 按房型统计
    private List<RoomTypeRevenue> revenueByRoomType;

    // 按日期统计
    private List<DailyRevenue> revenueByDate;

    public BusinessSummaryDTO() {
    }

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Integer getTotalRoomNights() {
        return totalRoomNights;
    }

    public void setTotalRoomNights(Integer totalRoomNights) {
        this.totalRoomNights = totalRoomNights;
    }

    public BigDecimal getAverageRoomRate() {
        return averageRoomRate;
    }

    public void setAverageRoomRate(BigDecimal averageRoomRate) {
        this.averageRoomRate = averageRoomRate;
    }

    public BigDecimal getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(BigDecimal occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public List<ChannelRevenue> getRevenueByChannel() {
        return revenueByChannel;
    }

    public void setRevenueByChannel(List<ChannelRevenue> revenueByChannel) {
        this.revenueByChannel = revenueByChannel;
    }

    public List<RoomTypeRevenue> getRevenueByRoomType() {
        return revenueByRoomType;
    }

    public void setRevenueByRoomType(List<RoomTypeRevenue> revenueByRoomType) {
        this.revenueByRoomType = revenueByRoomType;
    }

    public List<DailyRevenue> getRevenueByDate() {
        return revenueByDate;
    }

    public void setRevenueByDate(List<DailyRevenue> revenueByDate) {
        this.revenueByDate = revenueByDate;
    }

    /**
     * 渠道收入统计
     */
    public static class ChannelRevenue {
        private String channelName;
        private BigDecimal revenue;
        private Integer orderCount;
        private Integer roomNights;

        public ChannelRevenue() {
        }

        public ChannelRevenue(String channelName, BigDecimal revenue, Integer orderCount, Integer roomNights) {
            this.channelName = channelName;
            this.revenue = revenue;
            this.orderCount = orderCount;
            this.roomNights = roomNights;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        public Integer getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(Integer orderCount) {
            this.orderCount = orderCount;
        }

        public Integer getRoomNights() {
            return roomNights;
        }

        public void setRoomNights(Integer roomNights) {
            this.roomNights = roomNights;
        }
    }

    /**
     * 房型收入统计
     */
    public static class RoomTypeRevenue {
        private String roomTypeName;
        private BigDecimal revenue;
        private Integer orderCount;
        private Integer roomNights;

        public RoomTypeRevenue() {
        }

        public RoomTypeRevenue(String roomTypeName, BigDecimal revenue, Integer orderCount, Integer roomNights) {
            this.roomTypeName = roomTypeName;
            this.revenue = revenue;
            this.orderCount = orderCount;
            this.roomNights = roomNights;
        }

        public String getRoomTypeName() {
            return roomTypeName;
        }

        public void setRoomTypeName(String roomTypeName) {
            this.roomTypeName = roomTypeName;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        public Integer getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(Integer orderCount) {
            this.orderCount = orderCount;
        }

        public Integer getRoomNights() {
            return roomNights;
        }

        public void setRoomNights(Integer roomNights) {
            this.roomNights = roomNights;
        }
    }

    /**
     * 每日收入统计
     */
    public static class DailyRevenue {
        private String date;
        private BigDecimal revenue;
        private Integer orderCount;
        private Integer roomNights;

        public DailyRevenue() {
        }

        public DailyRevenue(String date, BigDecimal revenue, Integer orderCount, Integer roomNights) {
            this.date = date;
            this.revenue = revenue;
            this.orderCount = orderCount;
            this.roomNights = roomNights;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        public Integer getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(Integer orderCount) {
            this.orderCount = orderCount;
        }

        public Integer getRoomNights() {
            return roomNights;
        }

        public void setRoomNights(Integer roomNights) {
            this.roomNights = roomNights;
        }
    }
}