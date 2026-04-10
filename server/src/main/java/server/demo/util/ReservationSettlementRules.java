package server.demo.util;

import server.demo.enums.ReservationStatus;

import java.math.BigDecimal;

public final class ReservationSettlementRules {

    private ReservationSettlementRules() {
    }

    public static boolean isSuReservation(String suReservationId) {
        return suReservationId != null && !suReservationId.trim().isEmpty();
    }

    public static boolean isCheckedInOrCheckedOut(ReservationStatus status) {
        return status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CHECKED_OUT;
    }

    public static boolean isFullyPaidByAmount(BigDecimal paidAmount, BigDecimal totalAmount) {
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        return total.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(total) >= 0;
    }

    public static boolean isSettled(
            Boolean manuallySettled,
            String suReservationId,
            ReservationStatus status,
            BigDecimal paidAmount,
            BigDecimal totalAmount
    ) {
        if (Boolean.TRUE.equals(manuallySettled)) {
            return true;
        }
        if (isSuReservation(suReservationId)) {
            return true;
        }
        if (isCheckedInOrCheckedOut(status)) {
            return true;
        }
        return isFullyPaidByAmount(paidAmount, totalAmount);
    }

    public static BigDecimal resolveDisplayPaidAmount(
            Boolean manuallySettled,
            String suReservationId,
            ReservationStatus status,
            BigDecimal paidAmount,
            BigDecimal totalAmount
    ) {
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        if (isSettled(manuallySettled, suReservationId, status, paidAmount, totalAmount)
                && total.compareTo(BigDecimal.ZERO) > 0
                && paid.compareTo(total) < 0) {
            return total;
        }
        return paid;
    }
}
