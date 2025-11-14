package server.demo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 流水汇总统计DTO
 * 包括支付方式和款项分类两种维度的统计
 */
public class RevenueSummaryDTO {
    // 总流水
    private BigDecimal totalRevenue;

    // 分账款(OTA代收)
    private BigDecimal splitAccount;

    // 实收款(直接收款)
    private BigDecimal actualReceived;

    // 总收款
    private BigDecimal totalIncome;

    // 总支出
    private BigDecimal totalExpense;

    // 支付方式统计
    private List<PaymentMethodStat> paymentMethodStats;

    // 款项分类统计
    private List<CategoryStat> categoryStats;

    // 收款分布(饼图数据)
    private List<Distribution> incomeDistribution;

    // 支出分布(饼图数据)
    private List<Distribution> expenseDistribution;

    // 每日流水明细
    private List<DailyRevenue> dailyRevenues;

    // Constructors
    public RevenueSummaryDTO() {}

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getSplitAccount() {
        return splitAccount;
    }

    public void setSplitAccount(BigDecimal splitAccount) {
        this.splitAccount = splitAccount;
    }

    public BigDecimal getActualReceived() {
        return actualReceived;
    }

    public void setActualReceived(BigDecimal actualReceived) {
        this.actualReceived = actualReceived;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public List<PaymentMethodStat> getPaymentMethodStats() {
        return paymentMethodStats;
    }

    public void setPaymentMethodStats(List<PaymentMethodStat> paymentMethodStats) {
        this.paymentMethodStats = paymentMethodStats;
    }

    public List<CategoryStat> getCategoryStats() {
        return categoryStats;
    }

    public void setCategoryStats(List<CategoryStat> categoryStats) {
        this.categoryStats = categoryStats;
    }

    public List<Distribution> getIncomeDistribution() {
        return incomeDistribution;
    }

    public void setIncomeDistribution(List<Distribution> incomeDistribution) {
        this.incomeDistribution = incomeDistribution;
    }

    public List<Distribution> getExpenseDistribution() {
        return expenseDistribution;
    }

    public void setExpenseDistribution(List<Distribution> expenseDistribution) {
        this.expenseDistribution = expenseDistribution;
    }

    public List<DailyRevenue> getDailyRevenues() {
        return dailyRevenues;
    }

    public void setDailyRevenues(List<DailyRevenue> dailyRevenues) {
        this.dailyRevenues = dailyRevenues;
    }

    /**
     * 支付方式统计
     */
    public static class PaymentMethodStat {
        private String paymentMethod; // 支付方式名称 (Booking代收、Airbnb代收、微信、支付宝等)
        private BigDecimal amount; // 金额
        private BigDecimal percentage; // 百分比

        public PaymentMethodStat() {}

        public PaymentMethodStat(String paymentMethod, BigDecimal amount, BigDecimal percentage) {
            this.paymentMethod = paymentMethod;
            this.amount = amount;
            this.percentage = percentage;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
    }

    /**
     * 款项分类统计
     */
    public static class CategoryStat {
        private String category; // 分类名称 (常规流水、AR收错流水、记一笔流水)
        private BigDecimal amount; // 金额
        private BigDecimal percentage; // 百分比

        public CategoryStat() {}

        public CategoryStat(String category, BigDecimal amount, BigDecimal percentage) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
    }

    /**
     * 分布数据(饼图)
     */
    public static class Distribution {
        private String name; // 名称
        private BigDecimal value; // 金额
        private BigDecimal percentage; // 百分比

        public Distribution() {}

        public Distribution(String name, BigDecimal value, BigDecimal percentage) {
            this.name = name;
            this.value = value;
            this.percentage = percentage;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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
     * 每日流水
     */
    public static class DailyRevenue {
        private String date; // 日期
        private BigDecimal amount; // 金额
        private Integer orderCount; // 订单数

        public DailyRevenue() {}

        public DailyRevenue(String date, BigDecimal amount, Integer orderCount) {
            this.date = date;
            this.amount = amount;
            this.orderCount = orderCount;
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

        public Integer getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(Integer orderCount) {
            this.orderCount = orderCount;
        }
    }
}
