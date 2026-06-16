package server.demo.service.helper.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.demo.enums.ReservationStatus;
import server.demo.util.StoreTimeZoneUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationOccupancyProjectionTest {

    private static final Set<ReservationStatus> OCCUPANCY_STATUSES = Set.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT
    );
    private ZoneId previousStorageZoneId;

    @BeforeEach
    void setUp() {
        previousStorageZoneId = StoreTimeZoneUtil.getReservationTimestampStorageZoneId();
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("Asia/Shanghai"));
    }

    @AfterEach
    void tearDown() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(previousStorageZoneId);
    }

    @Test
    void checkedOutActualJune16_shouldKeepJune15OccupiedAndReleaseJune16() {
        LocalDate checkIn = LocalDate.of(2026, 6, 15);
        LocalDate checkOut = LocalDate.of(2026, 6, 18);
        LocalDateTime actualCheckOut = LocalDateTime.of(2026, 6, 16, 10, 0);
        ZoneId storeZoneId = ZoneId.of("Asia/Shanghai");

        LocalDate effectiveEnd = ReservationOccupancyProjection.resolveEffectiveCheckOutDate(
                checkIn,
                checkOut,
                ReservationStatus.CHECKED_OUT,
                actualCheckOut,
                storeZoneId
        );

        assertEquals(LocalDate.of(2026, 6, 16), effectiveEnd);
        assertTrue(occupies(checkIn, checkOut, actualCheckOut, LocalDate.of(2026, 6, 15), storeZoneId));
        assertFalse(occupies(checkIn, checkOut, actualCheckOut, LocalDate.of(2026, 6, 16), storeZoneId));
        assertFalse(occupies(checkIn, checkOut, actualCheckOut, LocalDate.of(2026, 6, 17), storeZoneId));
    }

    @Test
    void checkedOutActualJune17_shouldReleaseJune17() {
        LocalDate checkIn = LocalDate.of(2026, 6, 15);
        LocalDate checkOut = LocalDate.of(2026, 6, 18);
        LocalDateTime actualCheckOut = LocalDateTime.of(2026, 6, 17, 9, 30);
        ZoneId storeZoneId = ZoneId.of("Asia/Shanghai");

        assertTrue(occupies(checkIn, checkOut, actualCheckOut, LocalDate.of(2026, 6, 15), storeZoneId));
        assertTrue(occupies(checkIn, checkOut, actualCheckOut, LocalDate.of(2026, 6, 16), storeZoneId));
        assertFalse(occupies(checkIn, checkOut, actualCheckOut, LocalDate.of(2026, 6, 17), storeZoneId));
    }

    @Test
    void checkedOutWithoutActualCheckout_shouldFallbackToOriginalCheckoutDate() {
        LocalDate checkIn = LocalDate.of(2026, 6, 15);
        LocalDate checkOut = LocalDate.of(2026, 6, 18);
        ZoneId storeZoneId = ZoneId.of("Asia/Shanghai");

        LocalDate effectiveEnd = ReservationOccupancyProjection.resolveEffectiveCheckOutDate(
                checkIn,
                checkOut,
                ReservationStatus.CHECKED_OUT,
                null,
                storeZoneId
        );

        assertEquals(checkOut, effectiveEnd);
        assertTrue(ReservationOccupancyProjection.occupiesDate(
                checkIn,
                checkOut,
                ReservationStatus.CHECKED_OUT,
                null,
                LocalDate.of(2026, 6, 17),
                OCCUPANCY_STATUSES,
                storeZoneId
        ));
        assertFalse(ReservationOccupancyProjection.occupiesDate(
                checkIn,
                checkOut,
                ReservationStatus.CHECKED_OUT,
                null,
                LocalDate.of(2026, 6, 18),
                OCCUPANCY_STATUSES,
                storeZoneId
        ));
    }

    @Test
    void checkedOutActualCheckoutStorageTime_shouldUseStoreLocalDate() {
        LocalDate checkIn = LocalDate.of(2026, 6, 15);
        LocalDate checkOut = LocalDate.of(2026, 6, 18);
        LocalDateTime storageActualCheckOut = LocalDateTime.of(2026, 6, 16, 0, 30);
        ZoneId losAngeles = ZoneId.of("America/Los_Angeles");

        LocalDate effectiveEnd = ReservationOccupancyProjection.resolveEffectiveCheckOutDate(
                checkIn,
                checkOut,
                ReservationStatus.CHECKED_OUT,
                storageActualCheckOut,
                losAngeles
        );

        assertEquals(LocalDate.of(2026, 6, 15), effectiveEnd);
        assertFalse(ReservationOccupancyProjection.occupiesDate(
                checkIn,
                checkOut,
                ReservationStatus.CHECKED_OUT,
                storageActualCheckOut,
                LocalDate.of(2026, 6, 15),
                OCCUPANCY_STATUSES,
                losAngeles
        ));
    }

    @Test
    void cancelledAndNoShow_shouldNotOccupy() {
        LocalDate checkIn = LocalDate.of(2026, 6, 15);
        LocalDate checkOut = LocalDate.of(2026, 6, 18);
        ZoneId storeZoneId = ZoneId.of("Asia/Shanghai");

        assertFalse(ReservationOccupancyProjection.occupiesDate(
                checkIn,
                checkOut,
                ReservationStatus.CANCELLED,
                null,
                LocalDate.of(2026, 6, 15),
                OCCUPANCY_STATUSES,
                storeZoneId
        ));
        assertFalse(ReservationOccupancyProjection.occupiesDate(
                checkIn,
                checkOut,
                ReservationStatus.NO_SHOW,
                null,
                LocalDate.of(2026, 6, 15),
                OCCUPANCY_STATUSES,
                storeZoneId
        ));
    }

    private boolean occupies(
            LocalDate checkIn,
            LocalDate checkOut,
            LocalDateTime actualCheckOut,
            LocalDate targetDate,
            ZoneId storeZoneId
    ) {
        return ReservationOccupancyProjection.occupiesDate(
                checkIn,
                checkOut,
                ReservationStatus.CHECKED_OUT,
                actualCheckOut,
                targetDate,
                OCCUPANCY_STATUSES,
                storeZoneId
        );
    }
}
