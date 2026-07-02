package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.dto.RevenuePrecisionDTO;
import server.demo.entity.Reservation;
import server.demo.entity.ReservationDailyPrice;
import server.demo.repository.ReservationDailyPriceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReservationRevenueAllocationService {

    public static final String PRICE_BASIS_SU_DAILY_PRICE = "SU_DAILY_PRICE";
    public static final String PRICE_BASIS_AVERAGED_TOTAL_AMOUNT = "AVERAGED_TOTAL_AMOUNT";
    public static final String PRICE_BASIS_MIXED = "MIXED_SU_DAILY_PRICE_AND_AVERAGED_TOTAL";
    public static final String PRICE_BASIS_NO_REVENUE = "NO_REVENUE";
    public static final String DATE_BASIS_STAY_DATE = "STAY_DATE";
    public static final String TAX_MODE_PRICE_AFTER_TAX = "PRICE_AFTER_TAX";
    public static final String CURRENCY_MIXED = "MIXED";

    private final ReservationDailyPriceRepository dailyPriceRepository;

    public ReservationRevenueAllocationService(ReservationDailyPriceRepository dailyPriceRepository) {
        this.dailyPriceRepository = dailyPriceRepository;
    }

    public record Allocation(
            Reservation reservation,
            LocalDate date,
            BigDecimal amount,
            boolean exactDailyPrice
    ) {}

    public record AllocationResult(
            List<Allocation> allocations,
            RevenuePrecisionDTO revenuePrecision
    ) {
        public BigDecimal totalRevenue() {
            BigDecimal total = BigDecimal.ZERO;
            for (Allocation allocation : allocations) {
                total = total.add(allocation.amount());
            }
            return total;
        }

        public int totalRoomNights() {
            return allocations.size();
        }
    }

    public AllocationResult allocateRevenue(
            Long storeId,
            List<Reservation> reservations,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<Reservation> sourceReservations = reservations != null ? reservations : List.of();
        List<Reservation> scopedReservations = new ArrayList<>();
        for (Reservation reservation : sourceReservations) {
            if (reservation == null || reservation.getId() == null) {
                continue;
            }
            if (reservation.getCheckInDate() == null || reservation.getCheckOutDate() == null) {
                continue;
            }
            if (!isReservationOverlapping(reservation, startDate, endDate)) {
                continue;
            }
            scopedReservations.add(reservation);
        }

        Map<Long, Map<LocalDate, ReservationDailyPrice>> dailyPricesByReservation =
                loadDailyPrices(storeId, scopedReservations, startDate, endDate);

        List<Allocation> allocations = new ArrayList<>();
        Set<String> currencyCodes = new LinkedHashSet<>();
        int exactRoomNights = 0;
        int averagedRoomNights = 0;

        for (Reservation reservation : scopedReservations) {
            Map<LocalDate, ReservationDailyPrice> dailyPrices =
                    dailyPricesByReservation.getOrDefault(reservation.getId(), Map.of());
            BigDecimal averagedAmount = calculateAveragedDailyAmount(reservation);

            LocalDate currentDate = maxDate(reservation.getCheckInDate(), startDate);
            LocalDate endExclusive = minDate(reservation.getCheckOutDate(), endDate.plusDays(1));

            while (currentDate.isBefore(endExclusive)) {
                ReservationDailyPrice dailyPrice = dailyPrices.get(currentDate);
                if (dailyPrice != null) {
                    BigDecimal amount = resolveDailyPriceAmount(dailyPrice);
                    allocations.add(new Allocation(reservation, currentDate, amount, true));
                    addCurrency(currencyCodes, dailyPrice.getCurrencyCode());
                    exactRoomNights++;
                } else {
                    allocations.add(new Allocation(reservation, currentDate, averagedAmount, false));
                    addCurrency(currencyCodes, reservation.getCurrencyCode());
                    averagedRoomNights++;
                }
                currentDate = currentDate.plusDays(1);
            }
        }

        RevenuePrecisionDTO precision = buildPrecision(currencyCodes, exactRoomNights, averagedRoomNights);
        return new AllocationResult(allocations, precision);
    }

    public BigDecimal resolveDailyPriceAmount(ReservationDailyPrice dailyPrice) {
        if (dailyPrice == null) {
            return BigDecimal.ZERO;
        }
        if (dailyPrice.getPriceAfterTax() != null) {
            return dailyPrice.getPriceAfterTax();
        }
        if (dailyPrice.getPriceBeforeTax() != null && dailyPrice.getTaxAmount() != null) {
            return dailyPrice.getPriceBeforeTax().add(dailyPrice.getTaxAmount());
        }
        if (dailyPrice.getPriceBeforeTax() != null) {
            return dailyPrice.getPriceBeforeTax();
        }
        return BigDecimal.ZERO;
    }

    private Map<Long, Map<LocalDate, ReservationDailyPrice>> loadDailyPrices(
            Long storeId,
            Collection<Reservation> reservations,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (storeId == null || reservations == null || reservations.isEmpty()) {
            return Map.of();
        }

        List<Long> reservationIds = new ArrayList<>();
        for (Reservation reservation : reservations) {
            reservationIds.add(reservation.getId());
        }

        List<ReservationDailyPrice> rows = dailyPriceRepository
                .findByStoreIdAndReservationIdInAndPriceDateBetween(
                        storeId,
                        reservationIds,
                        startDate,
                        endDate
                );

        Map<Long, Map<LocalDate, ReservationDailyPrice>> result = new HashMap<>();
        for (ReservationDailyPrice row : rows) {
            if (row == null || row.getReservation() == null || row.getReservation().getId() == null) {
                continue;
            }
            if (row.getPriceDate() == null) {
                continue;
            }
            Long reservationId = row.getReservation().getId();
            result.computeIfAbsent(reservationId, ignored -> new HashMap<>())
                    .put(row.getPriceDate(), row);
        }
        return result;
    }

    private RevenuePrecisionDTO buildPrecision(
            Set<String> currencyCodes,
            int exactRoomNights,
            int averagedRoomNights
    ) {
        int totalRoomNights = exactRoomNights + averagedRoomNights;
        RevenuePrecisionDTO precision = new RevenuePrecisionDTO();
        precision.setDateBasis(DATE_BASIS_STAY_DATE);
        precision.setTaxMode(TAX_MODE_PRICE_AFTER_TAX);
        precision.setCurrencyCode(resolveCurrencyCode(currencyCodes));
        precision.setExactRoomNights(exactRoomNights);
        precision.setAveragedRoomNights(averagedRoomNights);
        precision.setTotalRoomNights(totalRoomNights);

        if (totalRoomNights == 0) {
            precision.setPriceBasis(PRICE_BASIS_NO_REVENUE);
            precision.setCoverageRate(BigDecimal.ZERO);
            return precision;
        }

        if (exactRoomNights > 0 && averagedRoomNights > 0) {
            precision.setPriceBasis(PRICE_BASIS_MIXED);
        } else if (exactRoomNights > 0) {
            precision.setPriceBasis(PRICE_BASIS_SU_DAILY_PRICE);
        } else {
            precision.setPriceBasis(PRICE_BASIS_AVERAGED_TOTAL_AMOUNT);
        }

        BigDecimal coverageRate = BigDecimal.valueOf(exactRoomNights)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalRoomNights), 2, RoundingMode.HALF_UP);
        precision.setCoverageRate(coverageRate);
        return precision;
    }

    private BigDecimal calculateAveragedDailyAmount(Reservation reservation) {
        BigDecimal totalAmount = reservation.getTotalAmount() != null
                ? reservation.getTotalAmount()
                : BigDecimal.ZERO;
        long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
        if (nights <= 0) {
            nights = 1;
        }
        return totalAmount.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
    }

    private boolean isReservationOverlapping(Reservation reservation, LocalDate startDate, LocalDate endDate) {
        return reservation.getCheckInDate().isBefore(endDate.plusDays(1))
                && reservation.getCheckOutDate().isAfter(startDate);
    }

    private LocalDate maxDate(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate minDate(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private void addCurrency(Set<String> currencyCodes, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return;
        }
        currencyCodes.add(currencyCode.trim().toUpperCase());
    }

    private String resolveCurrencyCode(Set<String> currencyCodes) {
        if (currencyCodes == null || currencyCodes.isEmpty()) {
            return null;
        }
        if (currencyCodes.size() == 1) {
            return currencyCodes.iterator().next();
        }
        return CURRENCY_MIXED;
    }
}
