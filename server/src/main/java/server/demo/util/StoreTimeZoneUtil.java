package server.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.demo.entity.Store;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class StoreTimeZoneUtil {

    public static final String DEFAULT_TIMEZONE = "Asia/Tokyo";
    public static final String DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE = "Asia/Shanghai";

    private static final Logger logger = LoggerFactory.getLogger(StoreTimeZoneUtil.class);
    private static final ZoneId HARD_FALLBACK_ZONE_ID = ZoneId.of(DEFAULT_TIMEZONE);
    private static final ZoneId HARD_FALLBACK_RESERVATION_TIMESTAMP_STORAGE_ZONE_ID =
            ZoneId.of(DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE);
    private static volatile ZoneId businessDefaultZoneId = HARD_FALLBACK_ZONE_ID;
    private static volatile ZoneId reservationTimestampStorageZoneId =
            HARD_FALLBACK_RESERVATION_TIMESTAMP_STORAGE_ZONE_ID;

    private StoreTimeZoneUtil() {
    }

    /**
     * Resolve the business timezone for the given store.
     */
    public static ZoneId resolveZoneId(Store store) {
        return resolveZoneId(store, businessDefaultZoneId);
    }

    /**
     * Resolve the business timezone for the given store with an explicit default zone.
     */
    public static ZoneId resolveZoneId(Store store, ZoneId defaultZoneId) {
        if (store == null) {
            return normalizeDefaultZoneId(defaultZoneId);
        }
        return resolveZoneId(store.getTimezone(), store.getId(), defaultZoneId);
    }

    /**
     * Resolve a timezone string to a valid {@link ZoneId}.
     */
    public static ZoneId resolveZoneId(String timezone) {
        return resolveZoneId(timezone, businessDefaultZoneId);
    }

    /**
     * Resolve a timezone string to a valid {@link ZoneId} with an explicit default zone.
     */
    public static ZoneId resolveZoneId(String timezone, ZoneId defaultZoneId) {
        return resolveZoneId(timezone, null, defaultZoneId);
    }

    /**
     * Resolve the configured business default timezone, falling back to Asia/Tokyo.
     */
    public static ZoneId resolveDefaultZoneId(String defaultTimezone) {
        if (defaultTimezone == null || defaultTimezone.isBlank()) {
            return HARD_FALLBACK_ZONE_ID;
        }

        String normalizedTimezone = defaultTimezone.trim();
        try {
            return ZoneId.of(normalizedTimezone);
        } catch (DateTimeException ex) {
            logger.warn(
                    "[StoreTimeZone] invalid business default timezone, fallback to hard default. timezone={}, fallback={}",
                    normalizedTimezone,
                    DEFAULT_TIMEZONE
            );
            return HARD_FALLBACK_ZONE_ID;
        }
    }

    /**
     * Resolve the configured reservation timestamp storage zone, falling back to Asia/Shanghai.
     */
    public static ZoneId resolveReservationTimestampStorageZoneId(String storageTimezone) {
        if (storageTimezone == null || storageTimezone.isBlank()) {
            return HARD_FALLBACK_RESERVATION_TIMESTAMP_STORAGE_ZONE_ID;
        }

        String normalizedTimezone = storageTimezone.trim();
        try {
            return ZoneId.of(normalizedTimezone);
        } catch (DateTimeException ex) {
            logger.warn(
                    "[StoreTimeZone] invalid reservation timestamp storage timezone, fallback to hard default. timezone={}, fallback={}",
                    normalizedTimezone,
                    DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE
            );
            return HARD_FALLBACK_RESERVATION_TIMESTAMP_STORAGE_ZONE_ID;
        }
    }

    /**
     * Update the process-level business default used by legacy static callers.
     */
    public static void setBusinessDefaultZoneId(ZoneId defaultZoneId) {
        businessDefaultZoneId = normalizeDefaultZoneId(defaultZoneId);
    }

    public static ZoneId getBusinessDefaultZoneId() {
        return businessDefaultZoneId;
    }

    /**
     * Update the process-level reservation timestamp storage zone used by entity lifecycle callbacks.
     */
    public static void setReservationTimestampStorageZoneId(ZoneId storageZoneId) {
        reservationTimestampStorageZoneId = normalizeReservationTimestampStorageZoneId(storageZoneId);
    }

    public static ZoneId getReservationTimestampStorageZoneId() {
        return reservationTimestampStorageZoneId;
    }

    /**
     * Normalize a store timezone for persistence. Blank values become null; invalid values are rejected.
     */
    public static String normalizeOrNull(String timezone) {
        if (timezone == null) {
            return null;
        }

        String normalizedTimezone = timezone.trim();
        if (normalizedTimezone.isEmpty()) {
            return null;
        }

        try {
            return ZoneId.of(normalizedTimezone).getId();
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("门店时区必须是有效的 IANA 时区: " + normalizedTimezone, ex);
        }
    }

    /**
     * Return the current UTC-based wall clock time used by auto-message services.
     */
    public static LocalDateTime nowUtc(Clock clock) {
        return LocalDateTime.now(clock);
    }

    /**
     * Convert an instant to the store's local wall clock time.
     */
    public static LocalDateTime toStoreLocalDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null || zoneId == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    /**
     * Convert a UTC-based {@link LocalDateTime} to the store's local wall clock time.
     */
    public static LocalDateTime toStoreLocalDateTime(LocalDateTime utcLocalDateTime, ZoneId zoneId) {
        if (utcLocalDateTime == null || zoneId == null) {
            return null;
        }
        return utcLocalDateTime.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(zoneId)
                .toLocalDateTime();
    }

    /**
     * Convert a store-local scheduled date/time to the UTC-based {@link LocalDateTime} used in persistence.
     */
    public static LocalDateTime toUtcLocalDateTime(LocalDate date, LocalTime time, ZoneId zoneId) {
        if (date == null || time == null || zoneId == null) {
            return null;
        }
        return ZonedDateTime.of(date, time, zoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    /**
     * Return the current wall clock time in the configured reservation timestamp storage zone.
     */
    public static LocalDateTime nowReservationTimestampLocalDateTime() {
        return LocalDateTime.now(reservationTimestampStorageZoneId);
    }

    /**
     * Convert a store-local business date/time to the configured reservation timestamp storage zone.
     */
    public static LocalDateTime toReservationTimestampStorageLocalDateTime(
            LocalDate date,
            LocalTime time,
            ZoneId businessZoneId
    ) {
        if (date == null || time == null || businessZoneId == null) {
            return null;
        }
        return ZonedDateTime.of(date, time, businessZoneId)
                .withZoneSameInstant(reservationTimestampStorageZoneId)
                .toLocalDateTime();
    }

    private static ZoneId resolveZoneId(String timezone, Long storeId, ZoneId defaultZoneId) {
        ZoneId fallbackZoneId = normalizeDefaultZoneId(defaultZoneId);
        if (timezone == null || timezone.isBlank()) {
            return fallbackZoneId;
        }

        String normalizedTimezone = timezone.trim();
        try {
            return ZoneId.of(normalizedTimezone);
        } catch (DateTimeException ex) {
            logger.warn(
                    "[StoreTimeZone] invalid timezone, fallback to default. storeId={}, timezone={}, fallback={}",
                    storeId,
                    normalizedTimezone,
                    fallbackZoneId.getId()
            );
            return fallbackZoneId;
        }
    }

    private static ZoneId normalizeDefaultZoneId(ZoneId defaultZoneId) {
        return defaultZoneId != null ? defaultZoneId : HARD_FALLBACK_ZONE_ID;
    }

    private static ZoneId normalizeReservationTimestampStorageZoneId(ZoneId storageZoneId) {
        return storageZoneId != null ? storageZoneId : HARD_FALLBACK_RESERVATION_TIMESTAMP_STORAGE_ZONE_ID;
    }
}
