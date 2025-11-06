package server.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public class NotesStatisticsDTO {
    private BigDecimal netIncome; // 净收入
    private BigDecimal totalIncome; // 总收入
    private BigDecimal totalExpense; // 总支出
    private List<CategoryStatistic> incomeByProject; // 收入按项目分类
    private List<CategoryStatistic> expenseByProject; // 支出按项目分类
    private List<PaymentStatistic> incomeByPayment; // 收入按支付方式分类
    private List<PaymentStatistic> expenseByPayment; // 支出按支付方式分类

    // Constructors
    public NotesStatisticsDTO() {
    }

    // Getters and Setters
    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public void setNetIncome(BigDecimal netIncome) {
        this.netIncome = netIncome;
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

    public List<CategoryStatistic> getIncomeByProject() {
        return incomeByProject;
    }

    public void setIncomeByProject(List<CategoryStatistic> incomeByProject) {
        this.incomeByProject = incomeByProject;
    }

    public List<CategoryStatistic> getExpenseByProject() {
        return expenseByProject;
    }

    public void setExpenseByProject(List<CategoryStatistic> expenseByProject) {
        this.expenseByProject = expenseByProject;
    }

    public List<PaymentStatistic> getIncomeByPayment() {
        return incomeByPayment;
    }

    public void setIncomeByPayment(List<PaymentStatistic> incomeByPayment) {
        this.incomeByPayment = incomeByPayment;
    }

    public List<PaymentStatistic> getExpenseByPayment() {
        return expenseByPayment;
    }

    public void setExpenseByPayment(List<PaymentStatistic> expenseByPayment) {
        this.expenseByPayment = expenseByPayment;
    }

    // 内部类：分类统计
    public static class CategoryStatistic {
        private String name;
        private BigDecimal value;

        public CategoryStatistic() {
        }

        public CategoryStatistic(String name, BigDecimal value) {
            this.name = name;
            this.value = value;
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
    }

    // 内部类：支付方式统计
    public static class PaymentStatistic {
        private String name;
        private BigDecimal value;

        public PaymentStatistic() {
        }

        public PaymentStatistic(String name, BigDecimal value) {
            this.name = name;
            this.value = value;
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
    }
}