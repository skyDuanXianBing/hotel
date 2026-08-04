package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.dto.ManagedOperationDtos;
import server.demo.enums.ManagedOperationFeeType;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.service.managedoperation.ManagedOperationImportRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import server.demo.i18n.ApiMessages;
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
                    ApiMessages.get("api.t.f495347d6acf") + row.sourceRowNumber() + ApiMessages.get("api.t.74d831ca6e61"));
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
            List<ManagedOperationDtos.FeeInput> fees) {
        validateRate(managementFeeRate, ApiMessages.get("api.t.b457f2525d4d"));
        validateRate(taxRate, ApiMessages.get("api.t.49afb4e7bdf8"));
        validateNonNegative(cleaningFeeGross, ApiMessages.get("api.t.ca2708ecfc3b"));
        validateNonNegative(registrationFeeNetUnit, ApiMessages.get("api.t.1fda0430b730"));
        if (selectedRoomCount < 0) throw new ManagedOperationValidationException(ApiMessages.get("api.t.c81ac87bdb78"));

        BigDecimal cleaningFeeNetUnit = cleaningFeeGross
                .divide(ONE.add(taxRate), 0, RoundingMode.HALF_UP);
        for (RowAmounts amount : included) {
            validateNonNegative(amount.receivedAmount(), ApiMessages.get("api.t.76e7365aab90"));
            validateNonNegative(amount.managementFee(), ApiMessages.get("api.t.e75002c3e5e5"));
            validateNonNegative(amount.scheduledTransfer(), ApiMessages.get("api.t.22d47fe26a04"));
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
        // 费用净额：扣款为正（减少转账），赠款为负（增加转账）
        BigDecimal otherDeductionsGross = BigDecimal.ZERO;
        BigDecimal feesNet = BigDecimal.ZERO;
        if (fees != null) {
            for (ManagedOperationDtos.FeeInput fee : fees) {
                ManagedOperationRunFieldsValidator.validateFee(fee);
                boolean credit = fee.feeType() == ManagedOperationFeeType.CREDIT;
                BigDecimal signedGross = credit ? fee.amountGross().negate() : fee.amountGross();
                otherDeductionsGross = otherDeductionsGross.add(signedGross);
                BigDecimal net = fee.amountGross().divide(ONE.add(taxRate), 0, RoundingMode.HALF_UP);
                feesNet = feesNet.add(credit ? net.negate() : net);
            }
        }
        BigDecimal finalTransfer = settlementSubtotal.subtract(registrationFeeGross).subtract(otherDeductionsGross);
        BigDecimal invoiceSubtotalNet = managementFeeNet.add(cleaningFeeNetTotal).add(registrationFeeNet).add(feesNet);
        BigDecimal invoiceTax = invoiceSubtotalNet.multiply(taxRate).setScale(0, RoundingMode.DOWN);
        BigDecimal invoiceTotalGross = invoiceSubtotalNet.add(invoiceTax);
        if (invoiceSubtotalNet.signum() < 0 || invoiceTotalGross.signum() < 0) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.49a35fdd7889"));
        }

        return new ManagedOperationDtos.PreviewSummary(
                included.size(), selectedRoomCount, yenExact(totalReceived), yenExact(managementFeeNet),
                cleaningFeeNetUnit, cleaningFeeNetTotal, cleaningTax, managementTax, settlementSubtotal,
                registrationFeeNet, registrationFeeGross, otherDeductionsGross, finalTransfer,
                invoiceSubtotalNet, invoiceTax, invoiceTotalGross);
    }

    public BigDecimal cleaningFeeNet(BigDecimal cleaningFeeGross, BigDecimal taxRate) {
        validateNonNegative(cleaningFeeGross, ApiMessages.get("api.t.ca2708ecfc3b"));
        validateRate(taxRate, ApiMessages.get("api.t.49afb4e7bdf8"));
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
            throw new ManagedOperationValidationException(field + ApiMessages.get("api.t.b1d5d5a3a6dc"));
        }
    }

    private static void validateNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_AMOUNT) > 0) {
            throw new ManagedOperationValidationException(field + ApiMessages.get("api.t.3e2ad38e6ee2"));
        }
        ManagedOperationMoneyRules.requireWholeYen(value, field);
    }

    public record RowAmounts(BigDecimal receivedAmount, BigDecimal managementFee, BigDecimal scheduledTransfer) {}
}
