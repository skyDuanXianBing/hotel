package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.enums.ReservationStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationSettlementRulesTest {

    @Test
    void isSettled_returnsTrueWhenSuReservation() {
        boolean settled = ReservationSettlementRules.isSettled(
                false,
                "SU26-123",
                ReservationStatus.CONFIRMED,
                BigDecimal.ZERO,
                new BigDecimal("1000")
        );
        assertTrue(settled);
    }

    @Test
    void isSettled_returnsTrueWhenCheckedIn() {
        boolean settled = ReservationSettlementRules.isSettled(
                false,
                null,
                ReservationStatus.CHECKED_IN,
                BigDecimal.ZERO,
                new BigDecimal("1000")
        );
        assertTrue(settled);
    }

    @Test
    void isSettled_returnsTrueWhenManuallySettled() {
        boolean settled = ReservationSettlementRules.isSettled(
                true,
                null,
                ReservationStatus.CONFIRMED,
                BigDecimal.ZERO,
                new BigDecimal("1000")
        );
        assertTrue(settled);
    }

    @Test
    void isSettled_returnsFalseForManualConfirmedWithoutPayment() {
        boolean settled = ReservationSettlementRules.isSettled(
                false,
                null,
                ReservationStatus.CONFIRMED,
                BigDecimal.ZERO,
                new BigDecimal("1000")
        );
        assertFalse(settled);
    }

    @Test
    void resolveDisplayPaidAmount_returnsTotalAmountWhenSettledByRules() {
        BigDecimal displayPaidAmount = ReservationSettlementRules.resolveDisplayPaidAmount(
                false,
                "SU26-123",
                ReservationStatus.CONFIRMED,
                BigDecimal.ZERO,
                new BigDecimal("1000")
        );
        assertEquals(new BigDecimal("1000"), displayPaidAmount);
    }
}
