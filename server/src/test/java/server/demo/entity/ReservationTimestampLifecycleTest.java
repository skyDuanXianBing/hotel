package server.demo.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReservationTimestampLifecycleTest {

    @BeforeEach
    void setUp() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("Asia/Shanghai"));
    }

    @AfterEach
    void tearDown() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(
                ZoneId.of(StoreTimeZoneUtil.DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE)
        );
    }

    @Test
    void onCreate_shouldWriteStorageZoneNaiveTimestampsWhenJvmDefaultZoneDiffers() {
        withJvmDefaultTimezone("Asia/Tokyo", () -> {
            Reservation reservation = new Reservation();

            LocalDateTime beforeStorage = storageNow("Asia/Shanghai");
            reservation.onCreate();
            LocalDateTime afterStorage = storageNow("Asia/Shanghai");

            assertBetweenInclusive(reservation.getCreatedAt(), beforeStorage, afterStorage);
            assertEquals(reservation.getCreatedAt(), reservation.getUpdatedAt());
        });
    }

    @Test
    void onCreate_shouldKeepPresetCreatedAtAndWriteUpdatedAtFromStorageZoneSource() {
        withJvmDefaultTimezone("Asia/Tokyo", () -> {
            Reservation reservation = new Reservation();
            LocalDateTime presetCreatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
            reservation.setCreatedAt(presetCreatedAt);

            LocalDateTime beforeStorage = storageNow("Asia/Shanghai");
            reservation.onCreate();
            LocalDateTime afterStorage = storageNow("Asia/Shanghai");

            assertEquals(presetCreatedAt, reservation.getCreatedAt());
            assertBetweenInclusive(reservation.getUpdatedAt(), beforeStorage, afterStorage);
        });
    }

    @Test
    void onUpdate_shouldWriteUpdatedAtFromStorageZoneSourceWhenJvmDefaultZoneDiffers() {
        withJvmDefaultTimezone("Asia/Tokyo", () -> {
            Reservation reservation = new Reservation();

            LocalDateTime beforeStorage = storageNow("Asia/Shanghai");
            reservation.onUpdate();
            LocalDateTime afterStorage = storageNow("Asia/Shanghai");

            assertBetweenInclusive(reservation.getUpdatedAt(), beforeStorage, afterStorage);
        });
    }

    @Test
    void onCreate_shouldSwitchToUtcNaiveWhenStorageZoneConfiguredAsUtc() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("UTC"));

        withJvmDefaultTimezone("Asia/Tokyo", () -> {
            Reservation reservation = new Reservation();

            LocalDateTime beforeUtc = storageNow("UTC");
            reservation.onCreate();
            LocalDateTime afterUtc = storageNow("UTC");

            assertBetweenInclusive(reservation.getCreatedAt(), beforeUtc, afterUtc);
            assertEquals(reservation.getCreatedAt(), reservation.getUpdatedAt());
        });
    }

    private static LocalDateTime storageNow(String timezone) {
        return LocalDateTime.now(Clock.system(ZoneId.of(timezone)));
    }

    private static void withJvmDefaultTimezone(String timezone, Runnable assertion) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(timezone));
            assertion.run();
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void assertBetweenInclusive(
            LocalDateTime actual,
            LocalDateTime startInclusive,
            LocalDateTime endInclusive
    ) {
        assertFalse(actual.isBefore(startInclusive), "timestamp must not be before expected lower bound");
        assertFalse(actual.isAfter(endInclusive), "timestamp must not be after expected upper bound");
    }
}
