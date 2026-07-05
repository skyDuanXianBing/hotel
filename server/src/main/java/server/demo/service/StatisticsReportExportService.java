package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.dto.RevenueSummaryDTO;
import server.demo.dto.SalesSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class StatisticsReportExportService {

    public static final String REPORT_ROOM_FEES = "room-fees";
    public static final String REPORT_TRANSACTION_SUMMARY = "transaction-summary";
    public static final String REPORT_DAILY = "daily";
    private static final int REPORT_PAGE_SIZE = 500;

    private final BusinessStatisticsService businessStatisticsService;

    public StatisticsReportExportService(BusinessStatisticsService businessStatisticsService) {
        this.businessStatisticsService = businessStatisticsService;
    }

    public boolean isSupported(String reportType) {
        return supportedTypes().contains(reportType);
    }

    public Set<String> supportedTypes() {
        return Set.of(REPORT_ROOM_FEES, REPORT_TRANSACTION_SUMMARY, REPORT_DAILY);
    }

    public String exportCsv(
            String reportType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Long channelId,
            String customer
    ) {
        if (REPORT_ROOM_FEES.equals(reportType)) {
            return exportRoomFees(startDate, endDate, keyword, channelId, customer);
        }
        if (REPORT_TRANSACTION_SUMMARY.equals(reportType)) {
            return exportTransactionSummary(startDate, endDate);
        }
        if (REPORT_DAILY.equals(reportType)) {
            return exportDaily(startDate, endDate);
        }
        throw new IllegalArgumentException("不支持的报表类型: " + reportType);
    }

    private String exportRoomFees(
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Long channelId,
            String customer
    ) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, List.of(
                "订单号",
                "渠道订单号",
                "渠道",
                "客户",
                "手机号",
                "房型",
                "入住日期",
                "退房日期",
                "统计范围税后房费",
                "整单总金额",
                "统计范围间夜数"
        ));

        int page = 1;
        int totalPages = 1;
        while (page <= totalPages) {
            SalesSummaryDTO summary = businessStatisticsService.getSalesSummary(
                    startDate,
                    endDate,
                    keyword,
                    channelId,
                    customer,
                    page,
                    REPORT_PAGE_SIZE
            );
            if (summary.getTotalPages() != null) {
                totalPages = summary.getTotalPages();
            }
            appendRoomFeeRows(csv, summary.getOrderDetails());
            page++;
        }
        return csv.toString();
    }

    private void appendRoomFeeRows(StringBuilder csv, List<SalesSummaryDTO.SalesOrderDetail> details) {
        if (details == null) {
            return;
        }
        for (SalesSummaryDTO.SalesOrderDetail detail : details) {
            appendRow(csv, List.of(
                    safe(detail.getOrderNumber()),
                    safe(detail.getChannelNumber()),
                    safe(detail.getChannelName()),
                    safe(detail.getCustomerName()),
                    safe(detail.getPhone()),
                    safe(detail.getRoomTypeName()),
                    safe(detail.getCheckInDate()),
                    safe(detail.getCheckOutDate()),
                    money(detail.getAllocatedAmount()),
                    money(detail.getTotalAmount()),
                    number(detail.getAllocatedRoomNights())
            ));
        }
    }

    private String exportTransactionSummary(LocalDate startDate, LocalDate endDate) {
        RevenueSummaryDTO summary = businessStatisticsService.getRevenueSummary(startDate, endDate);
        StringBuilder csv = new StringBuilder();
        appendRow(csv, List.of("指标", "金额", "来源", "备注"));
        appendMetric(csv, "总收入", summary.getTotalIncome(), "MIXED_STATISTICS", "税后房费+押金+客房消费+记一笔收入");
        appendMetric(csv, "总支出", summary.getTotalExpense(), "MIXED_STATISTICS", "退款+记一笔支出");
        appendMetric(csv, "净收入", summary.getNetIncome(), "MIXED_STATISTICS", "总收入-总支出");
        appendMetric(csv, "税后房费", summary.getRoomFee(), "RESERVATION_DAILY_PRICE_OR_RESIDUAL_AVERAGE", "入住日口径");
        appendMetric(csv, "分账款", summary.getSplitAccount(), "ROOM_FEE_CHANNEL_OTA", "渠道类型/名称识别 OTA 代收");
        appendMetric(csv, "实收款", summary.getActualReceived(), StatisticsFinancialAggregationService.SOURCE_PAYMENT_TYPE_TEXT, "Payment.type 文本归一化");
        appendMetric(csv, "押金", summary.getDeposit(), StatisticsFinancialAggregationService.SOURCE_PAYMENT_TYPE_TEXT, "Payment.type 文本归一化");
        appendMetric(csv, "客房消费", summary.getRoomServiceFee(), StatisticsFinancialAggregationService.SOURCE_CONSUMPTION_ABS_AMOUNT, "Consumption.amount 取绝对值");
        appendMetric(csv, "记一笔收入", summary.getNotesIncome(), StatisticsFinancialAggregationService.SOURCE_NOTE_TYPE, "Note.type=income");
        appendMetric(csv, "退款", summary.getPaymentRefund(), StatisticsFinancialAggregationService.SOURCE_PAYMENT_TYPE_TEXT, "Payment.type=refund/退款");
        appendMetric(csv, "记一笔支出", summary.getNotesExpense(), StatisticsFinancialAggregationService.SOURCE_NOTE_TYPE, "Note.type=expense");

        appendRow(csv, List.of(""));
        appendRow(csv, List.of("支付方式", "金额", "占比", "来源", "归一化类型", "交易笔数"));
        List<RevenueSummaryDTO.PaymentMethodStat> paymentStats = summary.getPaymentMethodStats();
        if (paymentStats != null) {
            for (RevenueSummaryDTO.PaymentMethodStat stat : paymentStats) {
                appendRow(csv, List.of(
                        safe(stat.getPaymentMethod()),
                        money(stat.getAmount()),
                        money(stat.getPercentage()),
                        safe(stat.getSourceType()),
                        safe(stat.getNormalizedType()),
                        number(stat.getTransactionCount())
                ));
            }
        }
        return csv.toString();
    }

    private String exportDaily(LocalDate startDate, LocalDate endDate) {
        RevenueSummaryDTO summary = businessStatisticsService.getRevenueSummary(startDate, endDate);
        StringBuilder csv = new StringBuilder();
        appendRow(csv, List.of(
                "日期",
                "税后房费",
                "分账款",
                "实收款",
                "押金",
                "客房消费",
                "记一笔收入",
                "记一笔支出",
                "退款",
                "总收入",
                "总支出",
                "净收入",
                "订单数",
                "交易笔数"
        ));
        List<RevenueSummaryDTO.DailyRevenue> dailyRevenues = summary.getDailyRevenues();
        if (dailyRevenues == null) {
            return csv.toString();
        }
        for (RevenueSummaryDTO.DailyRevenue daily : dailyRevenues) {
            appendRow(csv, List.of(
                    safe(daily.getDate()),
                    money(daily.getRoomFee()),
                    money(daily.getSplitAccount()),
                    money(daily.getActualReceived()),
                    money(daily.getDeposit()),
                    money(daily.getRoomServiceFee()),
                    money(daily.getNotesIncome()),
                    money(daily.getNotesExpense()),
                    money(daily.getPaymentRefund()),
                    money(daily.getTotalIncome()),
                    money(daily.getTotalExpense()),
                    money(daily.getNetIncome()),
                    number(daily.getOrderCount()),
                    number(daily.getTransactionCount())
            ));
        }
        return csv.toString();
    }

    private void appendMetric(StringBuilder csv, String name, BigDecimal amount, String source, String note) {
        appendRow(csv, List.of(name, money(amount), source, note));
    }

    private void appendRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append('\n');
    }

    private String escape(String value) {
        String safeValue = safe(value);
        boolean needsQuote = safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n");
        if (!needsQuote) {
            return safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.toPlainString();
    }

    private String number(Integer value) {
        if (value == null) {
            return "0";
        }
        return value.toString();
    }
}
