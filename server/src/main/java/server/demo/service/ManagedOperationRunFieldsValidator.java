package server.demo.service;

import server.demo.dto.ManagedOperationDtos;
import server.demo.exception.ManagedOperationValidationException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import server.demo.i18n.ApiMessages;

/**
 * 代运营「本月原始数据 / 预览导出请求」公共字段校验。
 */
public final class ManagedOperationRunFieldsValidator {
    private static final int MAX_FEES = 100;
    private static final int MAX_NOTE_LENGTH = 1000;
    private static final int MAX_DOCUMENT_NUMBER_LENGTH = 100;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    private ManagedOperationRunFieldsValidator() {}

    public static YearMonth requireMonth(String value) {
        if (value == null || value.isBlank()) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.7e604be81e55"));
        }
        try {
            return YearMonth.parse(value.strip());
        } catch (DateTimeParseException ex) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.19b040f67f48"));
        }
    }

    public static void validateFees(List<ManagedOperationDtos.FeeInput> fees) {
        if (fees == null) return;
        if (fees.size() > MAX_FEES) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.04e6bbd990ff"));
        }
        for (ManagedOperationDtos.FeeInput fee : fees) {
            validateFee(fee);
        }
    }

    public static void validateFee(ManagedOperationDtos.FeeInput fee) {
        if (fee == null || fee.feeType() == null) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.ab8844b40fb4"));
        }
        if (fee.description() == null || fee.description().isBlank()
                || fee.description().strip().length() > 200) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.1269ec40efd9"));
        }
        BigDecimal amount = fee.amountGross();
        if (amount == null || amount.signum() < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.9b5c1b5cbd69"));
        }
        ManagedOperationMoneyRules.requireWholeYen(amount, ApiMessages.get("api.t.9b5c1b5cbd69"));
    }

    public static void validateDocumentNumbers(String invoiceNumber, String receiptNumber) {
        for (String value : List.of(
                invoiceNumber == null ? "" : invoiceNumber,
                receiptNumber == null ? "" : receiptNumber)) {
            if (value.length() > MAX_DOCUMENT_NUMBER_LENGTH) {
                throw new ManagedOperationValidationException(ApiMessages.get("api.t.2cd27799cc05"));
            }
        }
    }

    public static void validateNote(String note) {
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new ManagedOperationValidationException(ApiMessages.get("api.t.aa5c0fd385c7"));
        }
    }
}
