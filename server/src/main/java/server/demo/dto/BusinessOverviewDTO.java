package server.demo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 营业概况统计DTO
 */
public class BusinessOverviewDTO {
    // 住宿总营业额
    private BigDecimal totalRevenue;

    // 房费
    private BigDecimal roomFee;

    // 押金
    private BigDecimal deposit;

    // 退房金
    private BigDecimal checkoutFee;

    // 餐食/客房消费
    private BigDecimal roomServiceFee;

    // 消费分类分布(用于饼图)
    private List<CategoryDistribution> categoryDistribution;

    // 住宿消费趋势(用于柱状图 - 按日期)
    private List<DailyConsumption> consumptionTrend;

    // 住宿消费明细表格数据
    private List<ConsumptionDetail> consumptionDetails;

    // Constructors
    public BusinessOverviewDTO() {}

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getRoomFee() {
        return roomFee;
    }

    public void setRoomFee(BigDecimal roomFee) {
        this.roomFee = roomFee;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public void setDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    public BigDecimal getCheckoutFee() {
        return checkoutFee;
    }

    public void setCheckoutFee(BigDecimal checkoutFee) {
        this.checkoutFee = checkoutFee;
    }

    public BigDecimal getRoomServiceFee() {
        return roomServiceFee;
    }

    public void setRoomServiceFee(BigDecimal roomServiceFee) {
        this.roomServiceFee = roomServiceFee;
    }

    public List<CategoryDistribution> getCategoryDistribution() {
        return categoryDistribution;
    }

    public void setCategoryDistribution(List<CategoryDistribution> categoryDistribution) {
        this.categoryDistribution = categoryDistribution;
    }

    public List<DailyConsumption> getConsumptionTrend() {
        return consumptionTrend;
    }

    public void setConsumptionTrend(List<DailyConsumption> consumptionTrend) {
        this.consumptionTrend = consumptionTrend;
    }

    public List<ConsumptionDetail> getConsumptionDetails() {
        return consumptionDetails;
    }

    public void setConsumptionDetails(List<ConsumptionDetail> consumptionDetails) {
        this.consumptionDetails = consumptionDetails;
    }

    /**
     * 消费分类分布（饼图数据）
     */
    public static class CategoryDistribution {
        private String category; // 类别名称: 房费、押金、退房金、餐食/客房消费
        private BigDecimal value; // 金额
        private BigDecimal percentage; // 百分比

        public CategoryDistribution() {}

        public CategoryDistribution(String category, BigDecimal value, BigDecimal percentage) {
            this.category = category;
            this.value = value;
            this.percentage = percentage;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
    }

    /**
     * 每日消费趋势（柱状图数据）
     */
    public static class DailyConsumption {
        private String date; // 日期
        private BigDecimal roomFee; // 房费
        private BigDecimal deposit; // 押金
        private BigDecimal checkoutFee; // 退房金
        private BigDecimal roomServiceFee; // 餐食/客房消费

        public DailyConsumption() {}

        public DailyConsumption(String date, BigDecimal roomFee, BigDecimal deposit,
                               BigDecimal checkoutFee, BigDecimal roomServiceFee) {
            this.date = date;
            this.roomFee = roomFee;
            this.deposit = deposit;
            this.checkoutFee = checkoutFee;
            this.roomServiceFee = roomServiceFee;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getRoomFee() {
            return roomFee;
        }

        public void setRoomFee(BigDecimal roomFee) {
            this.roomFee = roomFee;
        }

        public BigDecimal getDeposit() {
            return deposit;
        }

        public void setDeposit(BigDecimal deposit) {
            this.deposit = deposit;
        }

        public BigDecimal getCheckoutFee() {
            return checkoutFee;
        }

        public void setCheckoutFee(BigDecimal checkoutFee) {
            this.checkoutFee = checkoutFee;
        }

        public BigDecimal getRoomServiceFee() {
            return roomServiceFee;
        }

        public void setRoomServiceFee(BigDecimal roomServiceFee) {
            this.roomServiceFee = roomServiceFee;
        }
    }

    /**
     * 住宿消费明细（表格数据）
     */
    public static class ConsumptionDetail {
        private String category; // 项目类别
        private BigDecimal total; // 总计
        private List<DailyAmount> dailyAmounts; // 每日金额列表

        public ConsumptionDetail() {}

        public ConsumptionDetail(String category, BigDecimal total, List<DailyAmount> dailyAmounts) {
            this.category = category;
            this.total = total;
            this.dailyAmounts = dailyAmounts;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }

        public List<DailyAmount> getDailyAmounts() {
            return dailyAmounts;
        }

        public void setDailyAmounts(List<DailyAmount> dailyAmounts) {
            this.dailyAmounts = dailyAmounts;
        }

        /**
         * 每日金额
         */
        public static class DailyAmount {
            private String date; // 日期
            private BigDecimal amount; // 金额

            public DailyAmount() {}

            public DailyAmount(String date, BigDecimal amount) {
                this.date = date;
                this.amount = amount;
            }

            public String getDate() {
                return date;
            }

            public void setDate(String date) {
                this.date = date;
            }

            public BigDecimal getAmount() {
                return amount;
            }

            public void setAmount(BigDecimal amount) {
                this.amount = amount;
            }
        }
    }
}
