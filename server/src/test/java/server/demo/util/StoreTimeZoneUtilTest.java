package server.demo.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoreTimeZoneUtilTest {

    @BeforeEach
    void setUp() {
        StoreTimeZoneUtil.setBusinessDefaultZoneId(ZoneId.of(StoreTimeZoneUtil.DEFAULT_TIMEZONE));
    }

    @AfterEach
    void tearDown() {
        StoreTimeZoneUtil.setBusinessDefaultZoneId(ZoneId.of(StoreTimeZoneUtil.DEFAULT_TIMEZONE));
    }

    @Test
    void resolveZoneId_shouldUseStoreTimezone() {
        Store store = new Store();
        store.setTimezone("Asia/Tokyo");

        assertEquals(ZoneId.of("Asia/Tokyo"), StoreTimeZoneUtil.resolveZoneId(store));
    }

    @Test
    void resolveZoneId_shouldUseConfiguredDefaultTimezone() {
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        StoreTimeZoneUtil.setBusinessDefaultZoneId(shanghai);

        assertEquals(shanghai, StoreTimeZoneUtil.resolveZoneId("bad/timezone"));
        assertEquals(shanghai, StoreTimeZoneUtil.resolveZoneId(" "));
        assertEquals(shanghai, StoreTimeZoneUtil.resolveZoneId((Store) null));
    }

    @Test
    void resolveDefaultZoneId_shouldFallbackToTokyoForBlankOrInvalidConfig() {
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");

        assertEquals(tokyo, StoreTimeZoneUtil.resolveDefaultZoneId(null));
        assertEquals(tokyo, StoreTimeZoneUtil.resolveDefaultZoneId(" "));
        assertEquals(tokyo, StoreTimeZoneUtil.resolveDefaultZoneId("bad/timezone"));
    }

    @Test
    void normalizeOrNull_shouldTrimBlankToNullAndRejectInvalidTimezone() {
        assertEquals("Asia/Tokyo", StoreTimeZoneUtil.normalizeOrNull(" Asia/Tokyo "));
        assertNull(StoreTimeZoneUtil.normalizeOrNull(null));
        assertNull(StoreTimeZoneUtil.normalizeOrNull(" "));
        assertThrows(IllegalArgumentException.class, () -> StoreTimeZoneUtil.normalizeOrNull("bad/timezone"));
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
