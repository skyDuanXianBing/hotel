package server.demo.service.managedoperation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ManagedOperationImportRow(
        Platform platform,
        int sourceRowNumber,
        String bookingKey,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String guestName,
        String listingName,
        String currency,
        BigDecimal grossSales,
        BigDecimal otaServiceFee,
        BigDecimal payoutFee,
        LocalDate payoutDate,
        String payoutReference
) {
    public enum Platform { AIRBNB, BOOKING }
}
