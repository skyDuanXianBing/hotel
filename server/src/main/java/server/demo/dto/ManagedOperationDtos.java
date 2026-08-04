package server.demo.dto;

import server.demo.enums.ManagedOperationFeeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ManagedOperationDtos {
    private ManagedOperationDtos() {}

    public record RoomOption(Long id, String roomNumber, String roomTypeName) {}

    public record Settings(
            Long id,
            String propertyName,
            List<Long> selectedRoomIds,
            BigDecimal managementFeeRate,
            BigDecimal taxRate,
            BigDecimal cleaningFeeGross,
            BigDecimal registrationFeeNet,
            Integer invoiceIssueDay,
            Integer receiptIssueDay,
            String ownerCompanyName,
            String ownerContactName,
            String ownerPostalCode,
            String ownerAddress,
            String issuerCompanyName,
            String issuerPostalCode,
            String issuerAddress,
            String issuerRegistrationNumber,
            String issuerPhone,
            String issuerEmail,
            String bankName,
            String bankBranch,
            String bankAccountType,
            String bankAccountNumber,
            String bankAccountHolder,
            boolean hasStamp
    ) {}

    public record SettingsResponse(Settings settings, List<RoomOption> availableRooms, boolean persisted) {}

    public record SettingsRequest(
            String propertyName,
            List<Long> selectedRoomIds,
            BigDecimal managementFeeRate,
            BigDecimal taxRate,
            BigDecimal cleaningFeeGross,
            BigDecimal registrationFeeNet,
            Integer invoiceIssueDay,
            Integer receiptIssueDay,
            String ownerCompanyName,
            String ownerContactName,
            String ownerPostalCode,
            String ownerAddress,
            String issuerCompanyName,
            String issuerPostalCode,
            String issuerAddress,
            String issuerRegistrationNumber,
            String issuerPhone,
            String issuerEmail,
            String bankName,
            String bankBranch,
            String bankAccountType,
            String bankAccountNumber,
            String bankAccountHolder
    ) {}

    public record PropertySummary(
            Long id,
            String propertyName,
            int roomCount,
            boolean hasStamp,
            LocalDateTime updatedAt
    ) {}

    public record CreatePropertyRequest(String propertyName) {}

    public record IssueDayRequest(Integer invoiceIssueDay, Integer receiptIssueDay) {}

    public record FeeInput(ManagedOperationFeeType feeType, String description, BigDecimal amountGross) {}

    public record RunRequest(
            String settlementMonth,
            List<FeeInput> fees,
            String invoiceNumber,
            LocalDate invoiceDate,
            LocalDate paymentDueDate,
            String receiptNumber,
            LocalDate receiptDate,
            String note
    ) {}

    public record MonthlyDataRequest(
            String settlementMonth,
            List<FeeInput> fees,
            String invoiceNumber,
            LocalDate invoiceDate,
            LocalDate paymentDueDate,
            String receiptNumber,
            LocalDate receiptDate,
            String note
    ) {}

    public record MonthlyDataResponse(
            String settlementMonth,
            List<FeeInput> fees,
            String invoiceNumber,
            LocalDate invoiceDate,
            LocalDate paymentDueDate,
            String receiptNumber,
            LocalDate receiptDate,
            String note,
            String airbnbFileName,
            String bookingFileName,
            boolean persisted
    ) {}

    public record DocumentNumberSuggestion(
            String invoiceNumber,
            String receiptNumber,
            LocalDate invoiceDate,
            LocalDate receiptDate,
            int invoiceIssueDay,
            int receiptIssueDay
    ) {}

    public enum LineStatus {
        INCLUDED,
        PERIOD_EXCLUDED,
        UNMATCHED,
        AMBIGUOUS,
        ROOM_EXCLUDED,
        CANCELLED
    }

    public record PreviewLine(
            String platform,
            int sourceRowNumber,
            String bookingKey,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String guestName,
            String roomNumber,
            String currency,
            BigDecimal grossSales,
            BigDecimal otaServiceFee,
            BigDecimal payoutFee,
            BigDecimal cleaningFeeNet,
            BigDecimal receivedAmount,
            BigDecimal managementFee,
            BigDecimal scheduledTransfer,
            LocalDate payoutDate,
            String payoutReference,
            LineStatus status,
            List<String> warnings
    ) {}

    public record PreviewStats(
            int airbnbRows,
            int bookingRows,
            Map<LineStatus, Integer> statusCounts
    ) {}

    public record PreviewSummary(
            int includedReservationCount,
            int selectedRoomCount,
            BigDecimal totalReceived,
            BigDecimal managementFeeNet,
            BigDecimal cleaningFeeNetUnit,
            BigDecimal cleaningFeeNetTotal,
            BigDecimal cleaningTax,
            BigDecimal managementTax,
            BigDecimal settlementSubtotal,
            BigDecimal registrationFeeNet,
            BigDecimal registrationFeeGross,
            BigDecimal otherDeductionsGross,
            BigDecimal finalTransfer,
            BigDecimal invoiceSubtotalNet,
            BigDecimal invoiceTax,
            BigDecimal invoiceTotalGross
    ) {}

    public record PreviewResponse(
            List<PreviewLine> lines,
            PreviewStats stats,
            PreviewSummary summary,
            boolean exportAllowed,
            List<String> blockingReasons
    ) {}

    public record StampResponse(boolean hasStamp) {}
}
