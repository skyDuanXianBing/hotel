package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.dto.RevenueSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
                new BigDecimal("149.00"),
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
        dailyRevenue.setTotalIncome(new BigDecimal("149.00"));
        dailyRevenue.setTotalExpense(new BigDecimal("22.00"));
        dailyRevenue.setNetIncome(new BigDecimal("127.00"));
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

        assertTrue(csv.contains("记一笔支出,退款"));
        assertTrue(csv.contains("2026-02-01,100.00,100.00,80.00,30.00,12.00,7.00,2.00,20.00,149.00,22.00,127.00,1,6"));
    }
}
