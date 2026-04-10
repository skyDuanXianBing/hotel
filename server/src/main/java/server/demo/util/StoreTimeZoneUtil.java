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

    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private static final Logger logger = LoggerFactory.getLogger(StoreTimeZoneUtil.class);
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(DEFAULT_TIMEZONE);

    private StoreTimeZoneUtil() {
    }

    /**
     * Resolve the business timezone for the given store.
     */
    public static ZoneId resolveZoneId(Store store) {
        if (store == null) {
            return DEFAULT_ZONE_ID;
        }
        return resolveZoneId(store.getTimezone(), store.getId());
    }

    /**
     * Resolve a timezone string to a valid {@link ZoneId}.
     */
    public static ZoneId resolveZoneId(String timezone) {
        return resolveZoneId(timezone, null);
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

    private static ZoneId resolveZoneId(String timezone, Long storeId) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_ZONE_ID;
        }

        String normalizedTimezone = timezone.trim();
        try {
            return ZoneId.of(normalizedTimezone);
        } catch (DateTimeException ex) {
            logger.warn(
                    "[StoreTimeZone] invalid timezone, fallback to default. storeId={}, timezone={}, fallback={}",
                    storeId,
                    normalizedTimezone,
                    DEFAULT_TIMEZONE
            );
            return DEFAULT_ZONE_ID;
        }
    }
}
