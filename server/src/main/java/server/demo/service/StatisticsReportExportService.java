package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.dto.RevenueSummaryDTO;
import server.demo.dto.SalesSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import server.demo.i18n.ApiMessages;
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
        throw new IllegalArgumentException(ApiMessages.get("api.t.4f07e7903fe6") + reportType);
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
                ApiMessages.get("api.t.459868e5cb99"),
                ApiMessages.get("api.t.0c195543e2e2"),
                ApiMessages.get("api.t.c152be9f5040"),
                ApiMessages.get("api.t.f20687060126"),
                ApiMessages.get("api.t.5a9cc5e89148"),
                ApiMessages.get("api.t.ad9e95e81c53"),
                ApiMessages.get("api.t.0b47d70496d9"),
                ApiMessages.get("api.t.0ee8509fbe56"),
                ApiMessages.get("api.t.ecd65aabf99e"),
                ApiMessages.get("api.t.b3bf359fc58a"),
                ApiMessages.get("api.t.81d2ef5e0f7b")
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
        appendRow(csv, List.of(ApiMessages.get("api.t.22336e6b892f"), ApiMessages.get("api.t.34943c40c9af")));
        appendMetric(csv, ApiMessages.get("api.t.1991cc37e575"), summary.getTotalIncome());
        appendMetric(csv, ApiMessages.get("api.t.be4351de8b3b"), summary.getTotalExpense());
        appendMetric(csv, ApiMessages.get("api.t.7fc777e5e4fe"), summary.getNetIncome());
        appendMetric(csv, ApiMessages.get("api.t.ecd65aabf99e"), summary.getRoomFee());
        appendMetric(csv, ApiMessages.get("api.t.28b409749796"), summary.getSplitAccount());
        appendMetric(csv, ApiMessages.get("api.t.85bc71828473"), summary.getActualReceived());
        appendMetric(csv, ApiMessages.get("api.t.07c29bcb7c86"), summary.getDeposit());
        appendMetric(csv, ApiMessages.get("api.t.4e98a770002d"), summary.getRoomServiceFee());
        appendMetric(csv, ApiMessages.get("api.t.1ecca135d105"), summary.getNotesIncome());
        appendMetric(csv, ApiMessages.get("api.t.725202313f83"), summary.getPaymentRefund());
        appendMetric(csv, ApiMessages.get("api.t.1967c77045d8"), summary.getNotesExpense());

        appendRow(csv, List.of(""));
        appendRow(csv, List.of(ApiMessages.get("api.t.a870784d0b6e"), ApiMessages.get("api.t.34943c40c9af"), ApiMessages.get("api.t.380a2fed3ffe"), ApiMessages.get("api.t.f860b90c3016")));
        List<RevenueSummaryDTO.PaymentMethodStat> paymentStats = summary.getPaymentMethodStats();
        if (paymentStats != null) {
            for (RevenueSummaryDTO.PaymentMethodStat stat : paymentStats) {
                appendRow(csv, List.of(
                        safe(stat.getPaymentMethod()),
                        money(stat.getAmount()),
                        money(stat.getPercentage()),
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
                ApiMessages.get("api.t.b6fed9af8313"),
                ApiMessages.get("api.t.ecd65aabf99e"),
                ApiMessages.get("api.t.28b409749796"),
                ApiMessages.get("api.t.85bc71828473"),
                ApiMessages.get("api.t.07c29bcb7c86"),
                ApiMessages.get("api.t.4e98a770002d"),
                ApiMessages.get("api.t.1ecca135d105"),
                ApiMessages.get("api.t.1967c77045d8"),
                ApiMessages.get("api.t.725202313f83"),
                ApiMessages.get("api.t.1991cc37e575"),
                ApiMessages.get("api.t.be4351de8b3b"),
                ApiMessages.get("api.t.7fc777e5e4fe"),
                ApiMessages.get("api.t.084389cc97c4"),
                ApiMessages.get("api.t.f860b90c3016")
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

    private void appendMetric(StringBuilder csv, String name, BigDecimal amount) {
        appendRow(csv, List.of(name, money(amount)));
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
