package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.dto.ManagedOperationDtos;
import server.demo.service.managedoperation.ManagedOperationImportRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedOperationCalculationServiceTest {
    private final ManagedOperationCalculationService service = new ManagedOperationCalculationService();

    @Test
    void bookingFirstRow_shouldMatchEvidenceFormula() {
        ManagedOperationImportRow row = new ManagedOperationImportRow(
                ManagedOperationImportRow.Platform.BOOKING, 2, "5425310803",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), "Guest", "", "JPY",
                new BigDecimal("14601"), new BigDecimal("2628"), new BigDecimal("336"), null, "");

        ManagedOperationCalculationService.RowAmounts amounts = service.calculateRow(
                row, new BigDecimal("7273"), new BigDecimal("0.10"));

        assertEquals(new BigDecimal("4364"), amounts.receivedAmount());
        assertEquals(new BigDecimal("436"), amounts.managementFee());
        assertEquals(new BigDecimal("3928"), amounts.scheduledTransfer());
    }

    @Test
    void summary_shouldMatchThirtyThreeReservationBaseline() {
        List<ManagedOperationCalculationService.RowAmounts> rows = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            rows.add(new ManagedOperationCalculationService.RowAmounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        rows.add(new ManagedOperationCalculationService.RowAmounts(
                new BigDecimal("1252063"), new BigDecimal("125206"), BigDecimal.ZERO));
        List<ManagedOperationDtos.FeeInput> fees = List.of(
                new ManagedOperationDtos.FeeInput(
                        server.demo.enums.ManagedOperationFeeType.DEDUCTION, "その他費用", new BigDecimal("35495")));

        ManagedOperationDtos.PreviewSummary summary = service.summarize(
                rows, 6, new BigDecimal("8000"), new BigDecimal("0.10"), new BigDecimal("0.10"),
                new BigDecimal("2000"), fees);

        assertEquals(new BigDecimal("7273"), summary.cleaningFeeNetUnit());
        assertEquals(new BigDecimal("240009"), summary.cleaningFeeNetTotal());
        assertEquals(new BigDecimal("24001"), summary.cleaningTax());
        assertEquals(new BigDecimal("12521"), summary.managementTax());
        assertEquals(new BigDecimal("1090335"), summary.settlementSubtotal());
        assertEquals(new BigDecimal("13200"), summary.registrationFeeGross());
        assertEquals(new BigDecimal("35495"), summary.otherDeductionsGross());
        assertEquals(new BigDecimal("1041640"), summary.finalTransfer());
        // 费用进入请款书：含税 35495 ÷ 1.1 = 税前 32268 计入小計
        assertEquals(new BigDecimal("409483"), summary.invoiceSubtotalNet());
        assertEquals(new BigDecimal("40948"), summary.invoiceTax());
        assertEquals(new BigDecimal("450431"), summary.invoiceTotalGross());
    }

    @Test
    void creditFees_shouldIncreaseTransferAndReduceInvoiceTotal() {
        List<ManagedOperationCalculationService.RowAmounts> rows = List.of(
                new ManagedOperationCalculationService.RowAmounts(
                        new BigDecimal("10000"), new BigDecimal("1000"), new BigDecimal("9000")));
        List<ManagedOperationDtos.FeeInput> fees = List.of(
                new ManagedOperationDtos.FeeInput(
                        server.demo.enums.ManagedOperationFeeType.DEDUCTION, "修理費", new BigDecimal("550")),
                new ManagedOperationDtos.FeeInput(
                        server.demo.enums.ManagedOperationFeeType.CREDIT, "祝い金", new BigDecimal("1100")));

        ManagedOperationDtos.PreviewSummary summary = service.summarize(
                rows, 1, new BigDecimal("8000"), new BigDecimal("0.10"), new BigDecimal("0.10"),
                new BigDecimal("2000"), fees);

        // 净费用 = 550 - 1100 = -550（赠款提高转账金额）
        assertEquals(new BigDecimal("-550"), summary.otherDeductionsGross());
        assertEquals(new BigDecimal("8173"), summary.settlementSubtotal());
        assertEquals(new BigDecimal("6523"), summary.finalTransfer());
        // 请款书费用净额 = 500 - 1000 = -500
        assertEquals(new BigDecimal("9773"), summary.invoiceSubtotalNet());
        assertEquals(new BigDecimal("977"), summary.invoiceTax());
        assertEquals(new BigDecimal("10750"), summary.invoiceTotalGross());
    }

    @Test
    void negativeReceivedAmount_shouldBeRejected() {
        ManagedOperationImportRow row = new ManagedOperationImportRow(
                ManagedOperationImportRow.Platform.BOOKING, 9, "NEGATIVE",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), "Guest", "", "JPY",
                new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50"), null, "");
        assertThrows(server.demo.exception.ManagedOperationValidationException.class,
                () -> service.calculateRow(row, new BigDecimal("7273"), new BigDecimal("0.10")));
    }

    @Test
    void fractionalDeduction_shouldBeRejectedButZeroFractionAllowed() {
        List<ManagedOperationCalculationService.RowAmounts> rows = List.of(
                new ManagedOperationCalculationService.RowAmounts(
                        new BigDecimal("10000"), new BigDecimal("1000"), new BigDecimal("9000")));
        assertDoesNotThrow(() -> service.summarize(
                rows, 1, new BigDecimal("8000.00"), new BigDecimal("0.10"), new BigDecimal("0.10"),
                new BigDecimal("2000.00"), List.of(
                        new ManagedOperationDtos.FeeInput(
                                server.demo.enums.ManagedOperationFeeType.DEDUCTION,
                                "耗材", new BigDecimal("300.00")))));
        assertThrows(server.demo.exception.ManagedOperationValidationException.class, () -> service.summarize(
                rows, 1, new BigDecimal("8000"), new BigDecimal("0.10"), new BigDecimal("0.10"),
                new BigDecimal("2000"), List.of(
                        new ManagedOperationDtos.FeeInput(
                                server.demo.enums.ManagedOperationFeeType.DEDUCTION,
                                "耗材", new BigDecimal("300.50")))));
    }
}
