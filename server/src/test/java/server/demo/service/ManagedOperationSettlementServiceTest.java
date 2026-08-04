package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.Channel;
import server.demo.service.managedoperation.ManagedOperationImportRow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedOperationSettlementServiceTest {

    @Test
    void isPlatform_shouldMatchOnlyExplicitAirbnbCode() {
        assertTrue(ManagedOperationSettlementService.isPlatform(
                channel("AIRBNB", "Any name"), ManagedOperationImportRow.Platform.AIRBNB));
        assertTrue(ManagedOperationSettlementService.isPlatform(
                channel(" airbnb ", "Any name"), ManagedOperationImportRow.Platform.AIRBNB));

        assertFalse(ManagedOperationSettlementService.isPlatform(
                channel("AIRBNB_PARTNER", "Airbnb"), ManagedOperationImportRow.Platform.AIRBNB));
        assertFalse(ManagedOperationSettlementService.isPlatform(
                channel("DIRECT", "Airbnb"), ManagedOperationImportRow.Platform.AIRBNB));
        assertFalse(ManagedOperationSettlementService.isPlatform(
                channel(null, "Airbnb"), ManagedOperationImportRow.Platform.AIRBNB));
    }

    @Test
    void isPlatform_shouldMatchOnlyExplicitBookingCodesAndNeverUseNameFallback() {
        assertTrue(ManagedOperationSettlementService.isPlatform(
                channel("BOOKING", "Any name"), ManagedOperationImportRow.Platform.BOOKING));
        assertTrue(ManagedOperationSettlementService.isPlatform(
                channel("booking.com", "Any name"), ManagedOperationImportRow.Platform.BOOKING));
        assertTrue(ManagedOperationSettlementService.isPlatform(
                channel("BOOKING_COM", "Any name"), ManagedOperationImportRow.Platform.BOOKING));

        assertFalse(ManagedOperationSettlementService.isPlatform(
                channel("BOOKING_ENGINE", "Booking.com"), ManagedOperationImportRow.Platform.BOOKING));
        assertFalse(ManagedOperationSettlementService.isPlatform(
                channel("FASTBOOKING", "Booking.com"), ManagedOperationImportRow.Platform.BOOKING));
        assertFalse(ManagedOperationSettlementService.isPlatform(
                channel("DIRECT", "Booking.com"), ManagedOperationImportRow.Platform.BOOKING));
        assertFalse(ManagedOperationSettlementService.isPlatform(
                null, ManagedOperationImportRow.Platform.BOOKING));
    }

    @Test
    void isRelevantForMonth_airbnbFollowsPayoutDateNotCheckOut() {
        java.time.YearMonth month = java.time.YearMonth.of(2026, 8);
        // 7 月退房、8 月入金 → 属于 8 月结算
        assertTrue(ManagedOperationSettlementService.isRelevantForMonth(
                airbnbRow(java.time.LocalDate.of(2026, 7, 30), java.time.LocalDate.of(2026, 8, 4)), month));
        // 8 月退房、6 月 30 日入金 → 不属于 8 月结算（多导出的行被截取掉）
        assertFalse(ManagedOperationSettlementService.isRelevantForMonth(
                airbnbRow(java.time.LocalDate.of(2026, 8, 28), java.time.LocalDate.of(2026, 6, 30)), month));
        // 没有入金日 → 无法归属
        assertFalse(ManagedOperationSettlementService.isRelevantForMonth(
                airbnbRow(java.time.LocalDate.of(2026, 8, 28), null), month));
    }

    @Test
    void isRelevantForMonth_bookingRowsAreAlwaysRelevant() {
        java.time.YearMonth month = java.time.YearMonth.of(2026, 8);
        assertTrue(ManagedOperationSettlementService.isRelevantForMonth(
                bookingRow(java.time.LocalDate.of(2026, 6, 30), null), month));
        assertTrue(ManagedOperationSettlementService.isRelevantForMonth(
                bookingRow(java.time.LocalDate.of(2026, 8, 31), java.time.LocalDate.of(2026, 9, 5)), month));
    }

    private static ManagedOperationImportRow airbnbRow(java.time.LocalDate checkOut, java.time.LocalDate payoutDate) {
        return new ManagedOperationImportRow(
                ManagedOperationImportRow.Platform.AIRBNB, 2, "HMAAAAAAAA",
                checkOut.minusDays(2), checkOut, "Guest", "", "JPY",
                new java.math.BigDecimal("10000"), new java.math.BigDecimal("1000"),
                java.math.BigDecimal.ZERO, payoutDate, "");
    }

    private static ManagedOperationImportRow bookingRow(java.time.LocalDate checkOut, java.time.LocalDate payoutDate) {
        return new ManagedOperationImportRow(
                ManagedOperationImportRow.Platform.BOOKING, 2, "5425310803",
                checkOut.minusDays(2), checkOut, "Guest", "", "JPY",
                new java.math.BigDecimal("10000"), new java.math.BigDecimal("1000"),
                new java.math.BigDecimal("100"), payoutDate, "");
    }

    private static Channel channel(String code, String name) {
        Channel channel = new Channel();
        channel.setCode(code);
        channel.setName(name);
        return channel;
    }
}
