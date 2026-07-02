package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.Reservation;
import server.demo.entity.ReservationDailyPrice;
import server.demo.repository.ReservationDailyPriceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationRevenueAllocationServiceTest {

    @Test
    void allocateRevenue_shouldPreferExactDailyRowsWithDifferentPrices() {
        ReservationDailyPriceRepository repository = mock(ReservationDailyPriceRepository.class);
        ReservationRevenueAllocationService service = new ReservationRevenueAllocationService(repository);
        Reservation reservation = buildReservation(10L, "300.00");

        ReservationDailyPrice first = buildDailyPrice(reservation, LocalDate.of(2026, 2, 1), "100.00");
        ReservationDailyPrice second = buildDailyPrice(reservation, LocalDate.of(2026, 2, 2), "200.00");
        when(repository.findByStoreIdAndReservationIdInAndPriceDateBetween(
                26L,
                List.of(10L),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 2)
        )).thenReturn(List.of(first, second));

        ReservationRevenueAllocationService.AllocationResult result = service.allocateRevenue(
                26L,
                List.of(reservation),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 2)
        );

        assertEquals(2, result.allocations().size());
        assertEquals(new BigDecimal("300.00"), result.totalRevenue());
        assertEquals(new BigDecimal("100.00"), result.allocations().get(0).amount());
        assertEquals(new BigDecimal("200.00"), result.allocations().get(1).amount());
        assertEquals(2, result.revenuePrecision().getExactRoomNights());
        assertEquals(0, result.revenuePrecision().getAveragedRoomNights());
        assertEquals(ReservationRevenueAllocationService.PRICE_BASIS_SU_DAILY_PRICE,
                result.revenuePrecision().getPriceBasis());
    }

    @Test
    void allocateRevenue_shouldAverageTotalAmountWhenDailyRowsMissing() {
        ReservationDailyPriceRepository repository = mock(ReservationDailyPriceRepository.class);
        ReservationRevenueAllocationService service = new ReservationRevenueAllocationService(repository);
        Reservation reservation = buildReservation(11L, "303.00");

        when(repository.findByStoreIdAndReservationIdInAndPriceDateBetween(
                26L,
                List.of(11L),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3)
        )).thenReturn(List.of());

        ReservationRevenueAllocationService.AllocationResult result = service.allocateRevenue(
                26L,
                List.of(reservation),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3)
        );

        assertEquals(3, result.allocations().size());
        assertEquals(new BigDecimal("303.00"), result.totalRevenue());
        assertEquals(new BigDecimal("101.00"), result.allocations().get(0).amount());
        assertEquals(new BigDecimal("101.00"), result.allocations().get(1).amount());
        assertEquals(new BigDecimal("101.00"), result.allocations().get(2).amount());
        assertEquals(0, result.revenuePrecision().getExactRoomNights());
        assertEquals(3, result.revenuePrecision().getAveragedRoomNights());
        assertEquals(ReservationRevenueAllocationService.PRICE_BASIS_AVERAGED_TOTAL_AMOUNT,
                result.revenuePrecision().getPriceBasis());
    }

    @Test
    void allocateRevenue_shouldMixExactDailyRowsAndAverageOnlyMissingDates() {
        ReservationDailyPriceRepository repository = mock(ReservationDailyPriceRepository.class);
        ReservationRevenueAllocationService service = new ReservationRevenueAllocationService(repository);
        Reservation reservation = buildReservation(12L, "300.00");

        ReservationDailyPrice exactMiddleNight =
                buildDailyPrice(reservation, LocalDate.of(2026, 2, 2), "150.00");
        when(repository.findByStoreIdAndReservationIdInAndPriceDateBetween(
                26L,
                List.of(12L),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3)
        )).thenReturn(List.of(exactMiddleNight));

        ReservationRevenueAllocationService.AllocationResult result = service.allocateRevenue(
                26L,
                List.of(reservation),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3)
        );

        assertEquals(3, result.allocations().size());
        assertEquals(LocalDate.of(2026, 2, 1), result.allocations().get(0).date());
        assertEquals(new BigDecimal("100.00"), result.allocations().get(0).amount());
        assertFalse(result.allocations().get(0).exactDailyPrice());
        assertEquals(LocalDate.of(2026, 2, 2), result.allocations().get(1).date());
        assertEquals(new BigDecimal("150.00"), result.allocations().get(1).amount());
        assertTrue(result.allocations().get(1).exactDailyPrice());
        assertEquals(LocalDate.of(2026, 2, 3), result.allocations().get(2).date());
        assertEquals(new BigDecimal("100.00"), result.allocations().get(2).amount());
        assertFalse(result.allocations().get(2).exactDailyPrice());
        assertEquals(new BigDecimal("350.00"), result.totalRevenue());
        assertEquals(1, result.revenuePrecision().getExactRoomNights());
        assertEquals(2, result.revenuePrecision().getAveragedRoomNights());
        assertEquals(3, result.revenuePrecision().getTotalRoomNights());
        assertEquals(new BigDecimal("33.33"), result.revenuePrecision().getCoverageRate());
        assertEquals(ReservationRevenueAllocationService.PRICE_BASIS_MIXED,
                result.revenuePrecision().getPriceBasis());
    }

    @Test
    void allocateRevenue_shouldReportMixedCurrencyWhenExactAndAveragedAllocationsUseDifferentCurrencies() {
        ReservationDailyPriceRepository repository = mock(ReservationDailyPriceRepository.class);
        ReservationRevenueAllocationService service = new ReservationRevenueAllocationService(repository);
        Reservation exactReservation = buildReservation(13L, "100.00", "JPY");
        exactReservation.setCheckOutDate(LocalDate.of(2026, 2, 2));
        Reservation averagedReservation = buildReservation(14L, "80.00", "USD");
        averagedReservation.setCheckOutDate(LocalDate.of(2026, 2, 2));

        ReservationDailyPrice exactDailyPrice =
                buildDailyPrice(exactReservation, LocalDate.of(2026, 2, 1), "100.00", "JPY");
        when(repository.findByStoreIdAndReservationIdInAndPriceDateBetween(
                26L,
                List.of(13L, 14L),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 1)
        )).thenReturn(List.of(exactDailyPrice));

        ReservationRevenueAllocationService.AllocationResult result = service.allocateRevenue(
                26L,
                List.of(exactReservation, averagedReservation),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 1)
        );

        assertEquals(2, result.allocations().size());
        assertEquals(new BigDecimal("180.00"), result.totalRevenue());
        assertEquals(1, result.revenuePrecision().getExactRoomNights());
        assertEquals(1, result.revenuePrecision().getAveragedRoomNights());
        assertEquals(ReservationRevenueAllocationService.CURRENCY_MIXED,
                result.revenuePrecision().getCurrencyCode());
        assertEquals(ReservationRevenueAllocationService.PRICE_BASIS_MIXED,
                result.revenuePrecision().getPriceBasis());
    }

    private Reservation buildReservation(Long id, String totalAmount) {
        return buildReservation(id, totalAmount, "JPY");
    }

    private Reservation buildReservation(Long id, String totalAmount, String currencyCode) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStoreId(26L);
        reservation.setCurrencyCode(currencyCode);
        reservation.setCheckInDate(LocalDate.of(2026, 2, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 2, 4));
        reservation.setTotalAmount(new BigDecimal(totalAmount));
        return reservation;
    }

    private ReservationDailyPrice buildDailyPrice(Reservation reservation, LocalDate date, String amount) {
        return buildDailyPrice(reservation, date, amount, "JPY");
    }

    private ReservationDailyPrice buildDailyPrice(
            Reservation reservation,
            LocalDate date,
            String amount,
            String currencyCode
    ) {
        ReservationDailyPrice dailyPrice = new ReservationDailyPrice();
        dailyPrice.setStoreId(26L);
        dailyPrice.setReservation(reservation);
        dailyPrice.setPriceDate(date);
        dailyPrice.setCurrencyCode(currencyCode);
        dailyPrice.setPriceAfterTax(new BigDecimal(amount));
        return dailyPrice;
    }
}
