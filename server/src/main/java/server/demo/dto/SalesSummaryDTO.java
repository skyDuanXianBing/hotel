package server.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售汇总统计DTO
 */
public class SalesSummaryDTO {
    // 总销售额
    private BigDecimal totalSales;

    // 总订单数
    private Integer totalOrders;

    // 每日销售额趋势(折线图)
    private List<DailySales> dailySalesTrend;

    // 销售订单明细(表格数据)
    private List<SalesOrderDetail> orderDetails;

    // 收入口径元数据
    private RevenuePrecisionDTO revenuePrecision;

    // Constructors
    public SalesSummaryDTO() {}

    // Getters and Setters
    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public List<DailySales> getDailySalesTrend() {
        return dailySalesTrend;
    }

    public void setDailySalesTrend(List<DailySales> dailySalesTrend) {
        this.dailySalesTrend = dailySalesTrend;
    }

    public List<SalesOrderDetail> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(List<SalesOrderDetail> orderDetails) {
        this.orderDetails = orderDetails;
    }

    public RevenuePrecisionDTO getRevenuePrecision() {
        return revenuePrecision;
    }

    public void setRevenuePrecision(RevenuePrecisionDTO revenuePrecision) {
        this.revenuePrecision = revenuePrecision;
    }

    /**
     * 每日销售额
     */
    public static class DailySales {
        private String date; // 日期
        private BigDecimal sales; // 销售额
        private Integer orderCount; // 订单数

        public DailySales() {}

        public DailySales(String date, BigDecimal sales, Integer orderCount) {
            this.date = date;
            this.sales = sales;
            this.orderCount = orderCount;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getSales() {
            return sales;
        }

        public void setSales(BigDecimal sales) {
            this.sales = sales;
        }

        public Integer getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(Integer orderCount) {
            this.orderCount = orderCount;
        }
    }

    /**
     * 销售订单明细
     */
    public static class SalesOrderDetail {
        private Long id; // 订单ID
        private String orderNumber; // 订单号
        private String channelNumber; // 渠道号
        private LocalDateTime createdAt; // 创建时间
        private String guestName; // 操作人
        private String channelName; // 渠道
        private String customerName; // 客户名
        private String phone; // 手机号
        private BigDecimal amount; // 订单总金额
        private String roomTypeName; // 房型名称
        private String checkInDate; // 入住日期
        private String checkOutDate; // 退房日期

        public SalesOrderDetail() {}

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public String getChannelNumber() {
            return channelNumber;
        }

        public void setChannelNumber(String channelNumber) {
            this.channelNumber = channelNumber;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public String getGuestName() {
            return guestName;
        }

        public void setGuestName(String guestName) {
            this.guestName = guestName;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getRoomTypeName() {
            return roomTypeName;
        }

        public void setRoomTypeName(String roomTypeName) {
            this.roomTypeName = roomTypeName;
        }

        public String getCheckInDate() {
            return checkInDate;
        }

        public void setCheckInDate(String checkInDate) {
            this.checkInDate = checkInDate;
        }

        public String getCheckOutDate() {
            return checkOutDate;
        }

        public void setCheckOutDate(String checkOutDate) {
            this.checkOutDate = checkOutDate;
        }
    }
}
