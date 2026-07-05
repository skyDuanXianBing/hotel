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

    // 净收入
    private BigDecimal netIncome;

    // 税后房费
    private BigDecimal roomFee;

    // 押金
    private BigDecimal deposit;

    // 客房消费
    private BigDecimal roomServiceFee;

    // 记一笔收入
    private BigDecimal notesIncome;

    // 记一笔支出
    private BigDecimal notesExpense;

    // 退款
    private BigDecimal paymentRefund;

    // 收入口径元数据
    private RevenuePrecisionDTO revenuePrecision;

    // 数据来源元数据
    private List<StatisticsSourceMetadataDTO> sourceMetadata;

    // 当前模型无法稳定支撑的数据缺口
    private List<StatisticsDataGapDTO> dataGaps;

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

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public void setNetIncome(BigDecimal netIncome) {
        this.netIncome = netIncome;
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

    public BigDecimal getRoomServiceFee() {
        return roomServiceFee;
    }

    public void setRoomServiceFee(BigDecimal roomServiceFee) {
        this.roomServiceFee = roomServiceFee;
    }

    public BigDecimal getNotesIncome() {
        return notesIncome;
    }

    public void setNotesIncome(BigDecimal notesIncome) {
        this.notesIncome = notesIncome;
    }

    public BigDecimal getNotesExpense() {
        return notesExpense;
    }

    public void setNotesExpense(BigDecimal notesExpense) {
        this.notesExpense = notesExpense;
    }

    public BigDecimal getPaymentRefund() {
        return paymentRefund;
    }

    public void setPaymentRefund(BigDecimal paymentRefund) {
        this.paymentRefund = paymentRefund;
    }

    public RevenuePrecisionDTO getRevenuePrecision() {
        return revenuePrecision;
    }

    public void setRevenuePrecision(RevenuePrecisionDTO revenuePrecision) {
        this.revenuePrecision = revenuePrecision;
    }

    public List<StatisticsSourceMetadataDTO> getSourceMetadata() {
        return sourceMetadata;
    }

    public void setSourceMetadata(List<StatisticsSourceMetadataDTO> sourceMetadata) {
        this.sourceMetadata = sourceMetadata;
    }

    public List<StatisticsDataGapDTO> getDataGaps() {
        return dataGaps;
    }

    public void setDataGaps(List<StatisticsDataGapDTO> dataGaps) {
        this.dataGaps = dataGaps;
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
        private String sourceType; // 来源类型
        private String normalizedType; // 归一化款项类型
        private Integer transactionCount; // 交易笔数

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

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getNormalizedType() {
            return normalizedType;
        }

        public void setNormalizedType(String normalizedType) {
            this.normalizedType = normalizedType;
        }

        public Integer getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(Integer transactionCount) {
            this.transactionCount = transactionCount;
        }
    }

    /**
     * 款项分类统计
     */
    public static class CategoryStat {
        private String category; // 分类名称 (常规流水、AR收错流水、记一笔流水)
        private BigDecimal amount; // 金额
        private BigDecimal percentage; // 百分比
        private String sourceType; // 来源类型
        private Integer transactionCount; // 交易笔数

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

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public Integer getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(Integer transactionCount) {
            this.transactionCount = transactionCount;
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
        private BigDecimal roomFee; // 税后房费
        private BigDecimal splitAccount; // 分账款
        private BigDecimal actualReceived; // 实收款
        private BigDecimal deposit; // 押金
        private BigDecimal roomServiceFee; // 客房消费
        private BigDecimal notesIncome; // 记一笔收入
        private BigDecimal notesExpense; // 记一笔支出
        private BigDecimal paymentRefund; // 退款
        private BigDecimal totalIncome; // 当日总收入
        private BigDecimal totalExpense; // 当日总支出
        private BigDecimal netIncome; // 当日净收入
        private Integer transactionCount; // 交易笔数

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

        public BigDecimal getRoomFee() {
            return roomFee;
        }

        public void setRoomFee(BigDecimal roomFee) {
            this.roomFee = roomFee;
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

        public BigDecimal getDeposit() {
            return deposit;
        }

        public void setDeposit(BigDecimal deposit) {
            this.deposit = deposit;
        }

        public BigDecimal getRoomServiceFee() {
            return roomServiceFee;
        }

        public void setRoomServiceFee(BigDecimal roomServiceFee) {
            this.roomServiceFee = roomServiceFee;
        }

        public BigDecimal getNotesIncome() {
            return notesIncome;
        }

        public void setNotesIncome(BigDecimal notesIncome) {
            this.notesIncome = notesIncome;
        }

        public BigDecimal getNotesExpense() {
            return notesExpense;
        }

        public void setNotesExpense(BigDecimal notesExpense) {
            this.notesExpense = notesExpense;
        }

        public BigDecimal getPaymentRefund() {
            return paymentRefund;
        }

        public void setPaymentRefund(BigDecimal paymentRefund) {
            this.paymentRefund = paymentRefund;
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

        public BigDecimal getNetIncome() {
            return netIncome;
        }

        public void setNetIncome(BigDecimal netIncome) {
            this.netIncome = netIncome;
        }

        public Integer getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(Integer transactionCount) {
            this.transactionCount = transactionCount;
        }
    }
}
