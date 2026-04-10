package server.demo.util;

import org.junit.jupiter.api.Test;
import server.demo.entity.Store;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoreTimeZoneUtilTest {

    @Test
    void resolveZoneId_shouldUseStoreTimezone() {
        Store store = new Store();
        store.setTimezone("Asia/Tokyo");

        assertEquals(ZoneId.of("Asia/Tokyo"), StoreTimeZoneUtil.resolveZoneId(store));
    }

    @Test
    void resolveZoneId_shouldFallbackToDefaultTimezone() {
        assertEquals(ZoneId.of(StoreTimeZoneUtil.DEFAULT_TIMEZONE), StoreTimeZoneUtil.resolveZoneId("bad/timezone"));
    }

    @Test
    void nowAndConversionHelpers_shouldUseUtcBaseline() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-08T05:00:00Z"), ZoneOffset.UTC);
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");

        assertEquals(LocalDateTime.of(2026, 4, 8, 5, 0), StoreTimeZoneUtil.nowUtc(clock));
        assertEquals(
                LocalDateTime.of(2026, 4, 8, 14, 0),
                StoreTimeZoneUtil.toStoreLocalDateTime(LocalDateTime.of(2026, 4, 8, 5, 0), tokyo)
        );
        assertEquals(
                LocalDateTime.of(2026, 4, 8, 5, 0),
                StoreTimeZoneUtil.toUtcLocalDateTime(LocalDate.of(2026, 4, 8), LocalTime.of(14, 0), tokyo)
        );
    }
}
