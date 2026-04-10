package server.demo.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class UtcTimeUtil {

    private static final Clock UTC_CLOCK = Clock.systemUTC();

    private UtcTimeUtil() {
    }

    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(UTC_CLOCK);
    }

    public static OffsetDateTime toUtcOffset(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
