package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.dto.ManagedOperationDtos;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.service.managedoperation.ManagedOperationImportRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ManagedOperationCalculationService {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    public RowAmounts calculateRow(
            ManagedOperationImportRow row,
            BigDecimal cleaningFeeNet,
            BigDecimal managementFeeRate) {
        BigDecimal received = switch (row.platform()) {
            case BOOKING -> row.grossSales()
                    .subtract(row.otaServiceFee().abs())
                    .subtract(row.payoutFee().abs())
                    .subtract(cleaningFeeNet);
            case AIRBNB -> row.grossSales()
                    .subtract(row.otaServiceFee().abs())
                    .subtract(cleaningFeeNet);
        };
        if (received.signum() < 0) {
            throw new ManagedOperationValidationException(
                    "第 " + row.sourceRowNumber() + " 行受取金为负数，请检查销售额和手续费");
        }
        BigDecimal managementFee = yen(received.multiply(managementFeeRate));
        BigDecimal scheduledTransfer = yen(received.multiply(ONE.subtract(managementFeeRate)));
        return new RowAmounts(received, managementFee, scheduledTransfer);
    }

    public ManagedOperationDtos.PreviewSummary summarize(
            List<RowAmounts> included,
            int selectedRoomCount,
            BigDecimal cleaningFeeGross,
            BigDecimal managementFeeRate,
            BigDecimal taxRate,
            BigDecimal registrationFeeNetUnit,
            List<ManagedOperationDtos.DeductionInput> deductions) {
        validateRate(managementFeeRate, "管理费率");
        validateRate(taxRate, "消费税率");
        validateNonNegative(cleaningFeeGross, "含税清洁费");
        validateNonNegative(registrationFeeNetUnit, "名簿制作费");
        if (selectedRoomCount < 0) throw new ManagedOperationValidationException("房间数量不合法");

        BigDecimal cleaningFeeNetUnit = cleaningFeeGross
                .divide(ONE.add(taxRate), 0, RoundingMode.HALF_UP);
        for (RowAmounts amount : included) {
            validateNonNegative(amount.receivedAmount(), "受取金");
            validateNonNegative(amount.managementFee(), "管理费");
            validateNonNegative(amount.scheduledTransfer(), "振込予定金额");
        }
        BigDecimal totalReceived = included.stream().map(RowAmounts::receivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal managementFeeNet = included.stream().map(RowAmounts::managementFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cleaningFeeNetTotal = cleaningFeeNetUnit.multiply(BigDecimal.valueOf(included.size()));
        BigDecimal cleaningTax = yen(cleaningFeeNetTotal.multiply(taxRate));
        BigDecimal managementTax = yen(managementFeeNet.multiply(taxRate));
        BigDecimal settlementSubtotal = totalReceived.subtract(managementFeeNet)
                .subtract(cleaningTax).subtract(managementTax);
        BigDecimal registrationFeeNet = registrationFeeNetUnit.multiply(BigDecimal.valueOf(selectedRoomCount));
        BigDecimal registrationFeeGross = yen(registrationFeeNet.multiply(ONE.add(taxRate)));
        BigDecimal otherDeductionsGross = deductions == null ? BigDecimal.ZERO : deductions.stream()
                .map(deduction -> {
                    validateDeduction(deduction);
                    return deduction.amountGross();
                }).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalTransfer = settlementSubtotal.subtract(registrationFeeGross).subtract(otherDeductionsGross);
        BigDecimal invoiceSubtotalNet = managementFeeNet.add(cleaningFeeNetTotal).add(registrationFeeNet);
        BigDecimal invoiceTax = invoiceSubtotalNet.multiply(taxRate).setScale(0, RoundingMode.DOWN);
        BigDecimal invoiceTotalGross = invoiceSubtotalNet.add(invoiceTax);
        if (invoiceSubtotalNet.signum() < 0 || invoiceTotalGross.signum() < 0) {
            throw new ManagedOperationValidationException("请款书金额不能为负数");
        }

        return new ManagedOperationDtos.PreviewSummary(
                included.size(), selectedRoomCount, yenExact(totalReceived), yenExact(managementFeeNet),
                cleaningFeeNetUnit, cleaningFeeNetTotal, cleaningTax, managementTax, settlementSubtotal,
                registrationFeeNet, registrationFeeGross, otherDeductionsGross, finalTransfer,
                invoiceSubtotalNet, invoiceTax, invoiceTotalGross);
    }

    public BigDecimal cleaningFeeNet(BigDecimal cleaningFeeGross, BigDecimal taxRate) {
        validateNonNegative(cleaningFeeGross, "含税清洁费");
        validateRate(taxRate, "消费税率");
        return cleaningFeeGross.divide(ONE.add(taxRate), 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal yen(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal yenExact(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value;
    }

    private static void validateRate(BigDecimal value, String field) {
        if (value == null || value.signum() < 0 || value.compareTo(ONE) > 0) {
            throw new ManagedOperationValidationException(field + "必须在 0 到 1 之间");
        }
    }

    private static void validateNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_AMOUNT) > 0) {
            throw new ManagedOperationValidationException(field + "不合法");
        }
        ManagedOperationMoneyRules.requireWholeYen(value, field);
    }

    private static void validateDeduction(ManagedOperationDtos.DeductionInput deduction) {
        if (deduction == null || deduction.description() == null || deduction.description().isBlank()
                || deduction.description().strip().length() > 200) {
            throw new ManagedOperationValidationException("扣款项目名称不能为空且不能超过 200 字符");
        }
        validateNonNegative(deduction.amountGross(), "扣款金额");
    }

    public record RowAmounts(BigDecimal receivedAmount, BigDecimal managementFee, BigDecimal scheduledTransfer) {}
}
