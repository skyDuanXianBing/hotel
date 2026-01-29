package server.demo.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtaReservationSyncServiceCalendarSyncRequestsTest {

    @Test
    void buildCalendarSyncRequests_newReservation_pushesNightsOnly() {
        List<OtaReservationSyncService.CalendarSyncRequest> reqs = OtaReservationSyncService.buildCalendarSyncRequests(
                null,
                null,
                null,
                12L,
                LocalDate.of(2026, 1, 28),
                LocalDate.of(2026, 1, 29)
        );

        assertEquals(1, reqs.size());
        assertEquals(12L, reqs.get(0).roomTypeId());
        assertEquals(LocalDate.of(2026, 1, 28), reqs.get(0).startDate());
        assertEquals(LocalDate.of(2026, 1, 28), reqs.get(0).endDate());
    }

    @Test
    void buildCalendarSyncRequests_modifiedDates_pushesOldAndNewRanges() {
        List<OtaReservationSyncService.CalendarSyncRequest> reqs = OtaReservationSyncService.buildCalendarSyncRequests(
                12L,
                LocalDate.of(2026, 1, 28),
                LocalDate.of(2026, 1, 29),
                12L,
                LocalDate.of(2026, 1, 29),
                LocalDate.of(2026, 1, 31)
        );

        assertEquals(2, reqs.size());
        assertEquals(LocalDate.of(2026, 1, 28), reqs.get(0).startDate());
        assertEquals(LocalDate.of(2026, 1, 28), reqs.get(0).endDate());
        assertEquals(LocalDate.of(2026, 1, 29), reqs.get(1).startDate());
        assertEquals(LocalDate.of(2026, 1, 30), reqs.get(1).endDate());
    }

    @Test
    void buildCalendarSyncRequests_sameRange_dedupes() {
        List<OtaReservationSyncService.CalendarSyncRequest> reqs = OtaReservationSyncService.buildCalendarSyncRequests(
                12L,
                LocalDate.of(2026, 1, 28),
                LocalDate.of(2026, 1, 29),
                12L,
                LocalDate.of(2026, 1, 28),
                LocalDate.of(2026, 1, 29)
        );

        assertEquals(1, reqs.size());
        assertEquals(12L, reqs.get(0).roomTypeId());
        assertEquals(LocalDate.of(2026, 1, 28), reqs.get(0).startDate());
        assertEquals(LocalDate.of(2026, 1, 28), reqs.get(0).endDate());
    }

    @Test
    void buildCalendarSyncRequests_sameDatesDifferentRoomType_pushesBoth() {
        List<OtaReservationSyncService.CalendarSyncRequest> reqs = OtaReservationSyncService.buildCalendarSyncRequests(
                11L,
                LocalDate.of(2026, 1, 28),
                LocalDate.of(2026, 1, 30),
                12L,
                LocalDate.of(2026, 1, 28),
                LocalDate.of(2026, 1, 30)
        );

        assertEquals(2, reqs.size());
        assertEquals(11L, reqs.get(0).roomTypeId());
        assertEquals(12L, reqs.get(1).roomTypeId());
    }
}

