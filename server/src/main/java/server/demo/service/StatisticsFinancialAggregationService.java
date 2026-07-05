package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.entity.Consumption;
import server.demo.entity.Note;
import server.demo.entity.Payment;
import server.demo.repository.ConsumptionRepository;
import server.demo.repository.NoteRepository;
import server.demo.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StatisticsFinancialAggregationService {

    public static final String SOURCE_PAYMENT_TYPE_TEXT = "PAYMENT_TYPE_TEXT_NORMALIZED";
    public static final String SOURCE_PAYMENT_METHOD_TEXT = "PAYMENT_METHOD_TEXT";
    public static final String SOURCE_CONSUMPTION_ABS_AMOUNT = "CONSUMPTION_ABS_AMOUNT";
    public static final String SOURCE_NOTE_TYPE = "NOTE_TYPE";
    public static final String PAYMENT_TYPE_ROOM_PAYMENT = "ROOM_PAYMENT";
    public static final String PAYMENT_TYPE_DEPOSIT = "DEPOSIT";
    public static final String PAYMENT_TYPE_REFUND = "REFUND";
    public static final String PAYMENT_TYPE_UNKNOWN_RECEIPT = "UNKNOWN_TEXT_DEFAULT_RECEIPT";

    private static final String DEFAULT_PAYMENT_METHOD = "未填写支付方式";
    private static final String DEFAULT_CONSUMPTION_ITEM = "未填写消费项目";
    private static final String DEFAULT_NOTE_CATEGORY = "未填写记账分类";

    private final PaymentRepository paymentRepository;
    private final ConsumptionRepository consumptionRepository;
    private final NoteRepository noteRepository;

    public StatisticsFinancialAggregationService(
            PaymentRepository paymentRepository,
            ConsumptionRepository consumptionRepository,
            NoteRepository noteRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.consumptionRepository = consumptionRepository;
        this.noteRepository = noteRepository;
    }

    public FinancialAggregation aggregate(Long storeId, LocalDate startDate, LocalDate endDate) {
        FinancialAggregation result = new FinancialAggregation(startDate, endDate);

        List<Payment> payments = paymentRepository.findActiveReservationPaymentsByStoreIdAndDateRange(
                storeId,
                startDate,
                endDate
        );
        aggregatePayments(result, payments);

        List<Consumption> consumptions = consumptionRepository.findActiveReservationConsumptionsByStoreIdAndDateRange(
                storeId,
                startDate,
                endDate
        );
        aggregateConsumptions(result, consumptions);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        List<Note> notes = noteRepository.findByStoreIdAndDateRange(storeId, startDateTime, endDateTime);
        aggregateNotes(result, notes);

        return result;
    }

    private void aggregatePayments(FinancialAggregation result, List<Payment> payments) {
        if (payments == null) {
            return;
        }

        for (Payment payment : payments) {
            if (payment == null || payment.getDate() == null) {
                continue;
            }
            BigDecimal amount = positiveAmount(payment.getAmount());
            PaymentTypeInfo typeInfo = normalizePaymentType(payment.getType());
            String paymentMethod = normalizePaymentMethod(payment.getPaymentMethod());
            DailyFinancialAmount daily = result.getDaily(payment.getDate());
            if (daily == null) {
                continue;
            }

            if (PAYMENT_TYPE_REFUND.equals(typeInfo.normalizedType)) {
                result.paymentRefund = result.paymentRefund.add(amount);
                daily.paymentRefund = daily.paymentRefund.add(amount);
                result.addExpensePaymentMethod(paymentMethod, amount, typeInfo.normalizedType, SOURCE_PAYMENT_TYPE_TEXT);
                result.addCategory("退款", amount, SOURCE_PAYMENT_TYPE_TEXT);
            } else if (PAYMENT_TYPE_DEPOSIT.equals(typeInfo.normalizedType)) {
                result.deposit = result.deposit.add(amount);
                daily.deposit = daily.deposit.add(amount);
                result.addIncomePaymentMethod(paymentMethod, amount, typeInfo.normalizedType, SOURCE_PAYMENT_TYPE_TEXT);
                result.addCategory("押金", amount, SOURCE_PAYMENT_TYPE_TEXT);
            } else {
                if (isSplitAccountPaymentMethod(paymentMethod)) {
                    result.splitAccountPayment = result.splitAccountPayment.add(amount);
                    daily.splitAccountPayment = daily.splitAccountPayment.add(amount);
                } else {
                    result.actualReceived = result.actualReceived.add(amount);
                    daily.actualReceived = daily.actualReceived.add(amount);
                }
                result.addIncomePaymentMethod(paymentMethod, amount, typeInfo.normalizedType, SOURCE_PAYMENT_TYPE_TEXT);
                result.addCategory("实收款", amount, SOURCE_PAYMENT_TYPE_TEXT);
            }
            daily.transactionCount++;
            result.paymentTypeSources.add(typeInfo.sourceLabel);
        }
    }

    private void aggregateConsumptions(FinancialAggregation result, List<Consumption> consumptions) {
        if (consumptions == null) {
            return;
        }

        for (Consumption consumption : consumptions) {
            if (consumption == null || consumption.getDate() == null) {
                continue;
            }
            BigDecimal amount = positiveAmount(consumption.getAmount());
            DailyFinancialAmount daily = result.getDaily(consumption.getDate());
            if (daily == null) {
                continue;
            }
            result.roomServiceFee = result.roomServiceFee.add(amount);
            daily.roomServiceFee = daily.roomServiceFee.add(amount);
            daily.transactionCount++;
            result.addCategory(normalizeBlank(consumption.getItem(), DEFAULT_CONSUMPTION_ITEM),
                    amount,
                    SOURCE_CONSUMPTION_ABS_AMOUNT);
        }
    }

    private void aggregateNotes(FinancialAggregation result, List<Note> notes) {
        if (notes == null) {
            return;
        }

        for (Note note : notes) {
            if (note == null || note.getDatetime() == null) {
                continue;
            }
            LocalDate date = note.getDatetime().toLocalDate();
            DailyFinancialAmount daily = result.getDaily(date);
            if (daily == null) {
                continue;
            }

            BigDecimal amount = positiveAmount(note.getAmount());
            String paymentMethod = normalizePaymentMethod(note.getPaymentMethod());
            String category = normalizeBlank(note.getCategory(), DEFAULT_NOTE_CATEGORY);
            String type = normalizeBlank(note.getType(), "");
            if ("income".equalsIgnoreCase(type)) {
                result.notesIncome = result.notesIncome.add(amount);
                daily.notesIncome = daily.notesIncome.add(amount);
                result.addIncomePaymentMethod(paymentMethod, amount, "NOTE_INCOME", SOURCE_NOTE_TYPE);
                result.addCategory("记一笔收入:" + category, amount, SOURCE_NOTE_TYPE);
                daily.transactionCount++;
            } else if ("expense".equalsIgnoreCase(type)) {
                result.notesExpense = result.notesExpense.add(amount);
                daily.notesExpense = daily.notesExpense.add(amount);
                result.addExpensePaymentMethod(paymentMethod, amount, "NOTE_EXPENSE", SOURCE_NOTE_TYPE);
                result.addCategory("记一笔支出:" + category, amount, SOURCE_NOTE_TYPE);
                daily.transactionCount++;
            }
        }
    }

    private PaymentTypeInfo normalizePaymentType(String type) {
        String normalized = normalizeBlank(type, "");
        if (normalized.isEmpty()) {
            return new PaymentTypeInfo(PAYMENT_TYPE_UNKNOWN_RECEIPT, "empty->receipt");
        }

        String lower = normalized.toLowerCase();
        if (lower.contains("refund") || normalized.contains("退款") || normalized.contains("退押")) {
            return new PaymentTypeInfo(PAYMENT_TYPE_REFUND, normalized);
        }
        if (lower.contains("deposit") || normalized.contains("押金")) {
            return new PaymentTypeInfo(PAYMENT_TYPE_DEPOSIT, normalized);
        }
        if (lower.contains("payment") || lower.contains("pay") || normalized.contains("收款")
                || normalized.contains("房费")) {
            return new PaymentTypeInfo(PAYMENT_TYPE_ROOM_PAYMENT, normalized);
        }
        return new PaymentTypeInfo(PAYMENT_TYPE_UNKNOWN_RECEIPT, normalized + "->receipt");
    }

    private boolean isSplitAccountPaymentMethod(String paymentMethod) {
        String method = normalizeBlank(paymentMethod, "");
        if (method.isEmpty()) {
            return false;
        }
        String lower = method.toLowerCase();
        return method.contains("代收")
                || lower.contains("ota")
                || lower.contains("booking")
                || lower.contains("airbnb")
                || lower.contains("agoda")
                || lower.contains("expedia")
                || lower.contains("ctrip")
                || method.contains("携程")
                || method.contains("美团");
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = normalizeBlank(paymentMethod, DEFAULT_PAYMENT_METHOD);
        String lower = normalized.toLowerCase();
        if ("wechat".equals(lower)) {
            return "微信";
        }
        if ("alipay".equals(lower)) {
            return "支付宝";
        }
        if ("cash".equals(lower)) {
            return "现金";
        }
        return normalized;
    }

    private BigDecimal positiveAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.abs();
    }

    private String normalizeBlank(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        return trimmed;
    }

    private record PaymentTypeInfo(String normalizedType, String sourceLabel) {}

    public static class FinancialAggregation {
        private final Map<LocalDate, DailyFinancialAmount> dailyAmounts = new LinkedHashMap<>();
        private final Map<String, MethodAmount> incomeByPaymentMethod = new LinkedHashMap<>();
        private final Map<String, MethodAmount> expenseByPaymentMethod = new LinkedHashMap<>();
        private final Map<String, CategoryAmount> categoryAmounts = new LinkedHashMap<>();
        private final Set<String> paymentTypeSources = new LinkedHashSet<>();
        private BigDecimal actualReceived = BigDecimal.ZERO;
        private BigDecimal splitAccountPayment = BigDecimal.ZERO;
        private BigDecimal deposit = BigDecimal.ZERO;
        private BigDecimal paymentRefund = BigDecimal.ZERO;
        private BigDecimal roomServiceFee = BigDecimal.ZERO;
        private BigDecimal notesIncome = BigDecimal.ZERO;
        private BigDecimal notesExpense = BigDecimal.ZERO;

        FinancialAggregation(LocalDate startDate, LocalDate endDate) {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                dailyAmounts.put(currentDate, new DailyFinancialAmount(currentDate));
                currentDate = currentDate.plusDays(1);
            }
        }

        DailyFinancialAmount getDaily(LocalDate date) {
            return dailyAmounts.get(date);
        }

        void addIncomePaymentMethod(String name, BigDecimal amount, String normalizedType, String sourceType) {
            MethodAmount methodAmount = incomeByPaymentMethod.computeIfAbsent(name, MethodAmount::new);
            methodAmount.add(amount, normalizedType, sourceType);
        }

        void addExpensePaymentMethod(String name, BigDecimal amount, String normalizedType, String sourceType) {
            MethodAmount methodAmount = expenseByPaymentMethod.computeIfAbsent(name, MethodAmount::new);
            methodAmount.add(amount, normalizedType, sourceType);
        }

        void addCategory(String name, BigDecimal amount, String sourceType) {
            CategoryAmount categoryAmount = categoryAmounts.computeIfAbsent(name, CategoryAmount::new);
            categoryAmount.add(amount, sourceType);
        }

        public BigDecimal getActualReceived() {
            return actualReceived;
        }

        public BigDecimal getSplitAccountPayment() {
            return splitAccountPayment;
        }

        public BigDecimal getDeposit() {
            return deposit;
        }

        public BigDecimal getPaymentRefund() {
            return paymentRefund;
        }

        public BigDecimal getRoomServiceFee() {
            return roomServiceFee;
        }

        public BigDecimal getNotesIncome() {
            return notesIncome;
        }

        public BigDecimal getNotesExpense() {
            return notesExpense;
        }

        public List<DailyFinancialAmount> getDailyAmounts() {
            return new ArrayList<>(dailyAmounts.values());
        }

        public Map<LocalDate, DailyFinancialAmount> getDailyAmountMap() {
            return dailyAmounts;
        }

        public List<MethodAmount> getIncomeByPaymentMethod() {
            return new ArrayList<>(incomeByPaymentMethod.values());
        }

        public List<MethodAmount> getExpenseByPaymentMethod() {
            return new ArrayList<>(expenseByPaymentMethod.values());
        }

        public List<CategoryAmount> getCategoryAmounts() {
            return new ArrayList<>(categoryAmounts.values());
        }

        public Set<String> getPaymentTypeSources() {
            return paymentTypeSources;
        }
    }

    public static class DailyFinancialAmount {
        private final LocalDate date;
        private BigDecimal actualReceived = BigDecimal.ZERO;
        private BigDecimal splitAccountPayment = BigDecimal.ZERO;
        private BigDecimal deposit = BigDecimal.ZERO;
        private BigDecimal paymentRefund = BigDecimal.ZERO;
        private BigDecimal roomServiceFee = BigDecimal.ZERO;
        private BigDecimal notesIncome = BigDecimal.ZERO;
        private BigDecimal notesExpense = BigDecimal.ZERO;
        private int transactionCount = 0;

        DailyFinancialAmount(LocalDate date) {
            this.date = date;
        }

        public LocalDate getDate() {
            return date;
        }

        public BigDecimal getActualReceived() {
            return actualReceived;
        }

        public BigDecimal getSplitAccountPayment() {
            return splitAccountPayment;
        }

        public BigDecimal getDeposit() {
            return deposit;
        }

        public BigDecimal getPaymentRefund() {
            return paymentRefund;
        }

        public BigDecimal getRoomServiceFee() {
            return roomServiceFee;
        }

        public BigDecimal getNotesIncome() {
            return notesIncome;
        }

        public BigDecimal getNotesExpense() {
            return notesExpense;
        }

        public int getTransactionCount() {
            return transactionCount;
        }
    }

    public static class MethodAmount {
        private final String name;
        private BigDecimal amount = BigDecimal.ZERO;
        private int transactionCount = 0;
        private final Set<String> normalizedTypes = new LinkedHashSet<>();
        private final Set<String> sourceTypes = new LinkedHashSet<>();

        MethodAmount(String name) {
            this.name = name;
        }

        void add(BigDecimal value, String normalizedType, String sourceType) {
            if (value != null) {
                amount = amount.add(value);
            }
            transactionCount++;
            normalizedTypes.add(normalizedType);
            sourceTypes.add(sourceType);
        }

        public String getName() {
            return name;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public String getNormalizedType() {
            if (normalizedTypes.size() == 1) {
                return normalizedTypes.iterator().next();
            }
            return "MIXED";
        }

        public String getSourceType() {
            if (sourceTypes.size() == 1) {
                return sourceTypes.iterator().next();
            }
            return "MIXED";
        }
    }

    public static class CategoryAmount {
        private final String name;
        private BigDecimal amount = BigDecimal.ZERO;
        private int transactionCount = 0;
        private final Set<String> sourceTypes = new LinkedHashSet<>();

        CategoryAmount(String name) {
            this.name = name;
        }

        void add(BigDecimal value, String sourceType) {
            if (value != null) {
                amount = amount.add(value);
            }
            transactionCount++;
            sourceTypes.add(sourceType);
        }

        public String getName() {
            return name;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public String getSourceType() {
            if (sourceTypes.size() == 1) {
                return sourceTypes.iterator().next();
            }
            return "MIXED";
        }
    }
}
