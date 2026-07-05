package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.dto.RevenueSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatisticsReportExportServiceTest {

    @Test
    void exportCsv_dailyReportShouldSeparateNoteExpenseAndPaymentRefund() {
        BusinessStatisticsService businessStatisticsService = mock(BusinessStatisticsService.class);
        StatisticsReportExportService service = new StatisticsReportExportService(businessStatisticsService);
        LocalDate date = LocalDate.of(2026, 2, 1);

        RevenueSummaryDTO.DailyRevenue dailyRevenue = new RevenueSummaryDTO.DailyRevenue(
                date.toString(),
                new BigDecimal("112.00"),
                1
        );
        dailyRevenue.setRoomFee(new BigDecimal("100.00"));
        dailyRevenue.setSplitAccount(new BigDecimal("100.00"));
        dailyRevenue.setActualReceived(new BigDecimal("80.00"));
        dailyRevenue.setDeposit(new BigDecimal("30.00"));
        dailyRevenue.setRoomServiceFee(new BigDecimal("12.00"));
        dailyRevenue.setNotesIncome(new BigDecimal("7.00"));
        dailyRevenue.setNotesExpense(new BigDecimal("2.00"));
        dailyRevenue.setPaymentRefund(new BigDecimal("20.00"));
        dailyRevenue.setTotalIncome(new BigDecimal("112.00"));
        dailyRevenue.setTotalExpense(new BigDecimal("22.00"));
        dailyRevenue.setNetIncome(new BigDecimal("90.00"));
        dailyRevenue.setTransactionCount(6);

        RevenueSummaryDTO summary = new RevenueSummaryDTO();
        summary.setDailyRevenues(List.of(dailyRevenue));
        when(businessStatisticsService.getRevenueSummary(date, date)).thenReturn(summary);

        String csv = service.exportCsv(
                StatisticsReportExportService.REPORT_DAILY,
                date,
                date,
                null,
                null,
                null
        );

        assertTrue(csv.contains("记一笔支出,退款/退押金"));
        assertTrue(csv.contains("2026-02-01,100.00,100.00,80.00,30.00,12.00,7.00,2.00,20.00,112.00,22.00,90.00,1,6"));
    }

    @Test
    void exportCsv_transactionSummaryShouldNotExposeInternalSourceColumns() {
        BusinessStatisticsService businessStatisticsService = mock(BusinessStatisticsService.class);
        StatisticsReportExportService service = new StatisticsReportExportService(businessStatisticsService);
        LocalDate date = LocalDate.of(2026, 2, 1);

        RevenueSummaryDTO.PaymentMethodStat paymentMethodStat =
                new RevenueSummaryDTO.PaymentMethodStat("微信", new BigDecimal("80.00"), new BigDecimal("100.00"));
        paymentMethodStat.setSourceType("PAYMENT_TYPE_TEXT_NORMALIZED");
        paymentMethodStat.setNormalizedType("ROOM_PAYMENT");
        paymentMethodStat.setTransactionCount(1);

        RevenueSummaryDTO summary = new RevenueSummaryDTO();
        summary.setTotalIncome(new BigDecimal("112.00"));
        summary.setTotalExpense(new BigDecimal("22.00"));
        summary.setNetIncome(new BigDecimal("90.00"));
        summary.setRoomFee(new BigDecimal("100.00"));
        summary.setSplitAccount(new BigDecimal("100.00"));
        summary.setActualReceived(new BigDecimal("80.00"));
        summary.setDeposit(new BigDecimal("30.00"));
        summary.setRoomServiceFee(new BigDecimal("12.00"));
        summary.setNotesIncome(new BigDecimal("7.00"));
        summary.setPaymentRefund(new BigDecimal("20.00"));
        summary.setNotesExpense(new BigDecimal("2.00"));
        summary.setPaymentMethodStats(List.of(paymentMethodStat));
        when(businessStatisticsService.getRevenueSummary(date, date)).thenReturn(summary);

        String csv = service.exportCsv(
                StatisticsReportExportService.REPORT_TRANSACTION_SUMMARY,
                date,
                date,
                null,
                null,
                null
        );

        assertTrue(csv.contains("项目,金额"));
        assertTrue(csv.contains("住宿营业额,112.00"));
        assertTrue(csv.contains("OTA代收款,100.00"));
        assertFalse(csv.contains("来源"));
        assertFalse(csv.contains("备注"));
        assertFalse(csv.contains("归一化类型"));
        assertFalse(csv.contains("MIXED_STATISTICS"));
        assertFalse(csv.contains("PAYMENT_TYPE_TEXT_NORMALIZED"));
        assertFalse(csv.contains("CONSUMPTION_ABS_AMOUNT"));
        assertFalse(csv.contains("NOTE_TYPE"));
        assertFalse(csv.contains("Payment.type 文本归一化"));
        assertFalse(csv.contains("分账款"));
    }
}
