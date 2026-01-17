package server.demo.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AutoMessageTimingUtilTest {

    @Test
    void parseSendTiming_supported() {
        assertEquals(Duration.ZERO, AutoMessageTimingUtil.parseSendTiming("IMMEDIATELY"));
        assertEquals(Duration.ofMinutes(5), AutoMessageTimingUtil.parseSendTiming("5_MIN"));
        assertEquals(Duration.ofMinutes(10), AutoMessageTimingUtil.parseSendTiming("10_MIN"));
        assertEquals(Duration.ofMinutes(15), AutoMessageTimingUtil.parseSendTiming("15_MIN"));
        assertEquals(Duration.ofMinutes(30), AutoMessageTimingUtil.parseSendTiming("30_MIN"));
        assertEquals(Duration.ofHours(1), AutoMessageTimingUtil.parseSendTiming("1_HOUR"));
        assertEquals(Duration.ofHours(2), AutoMessageTimingUtil.parseSendTiming("2_HOUR"));
        assertEquals(Duration.ofHours(4), AutoMessageTimingUtil.parseSendTiming("4_HOUR"));
        assertEquals(Duration.ofHours(8), AutoMessageTimingUtil.parseSendTiming("8_HOUR"));
        assertEquals(Duration.ofHours(16), AutoMessageTimingUtil.parseSendTiming("16_HOUR"));
        assertEquals(Duration.ofHours(24), AutoMessageTimingUtil.parseSendTiming("24_HOUR"));
    }

    @Test
    void parseSendTiming_caseInsensitiveAndTrim() {
        assertEquals(Duration.ofHours(24), AutoMessageTimingUtil.parseSendTiming(" 24_hour "));
    }

    @Test
    void parseSendTiming_invalid_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> AutoMessageTimingUtil.parseSendTiming(null));
        assertThrows(IllegalArgumentException.class, () -> AutoMessageTimingUtil.parseSendTiming(""));
        assertThrows(IllegalArgumentException.class, () -> AutoMessageTimingUtil.parseSendTiming("3_HOUR"));
    }
}

