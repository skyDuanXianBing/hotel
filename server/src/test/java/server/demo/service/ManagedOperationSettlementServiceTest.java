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

    private static Channel channel(String code, String name) {
        Channel channel = new Channel();
        channel.setCode(code);
        channel.setName(name);
        return channel;
    }
}
