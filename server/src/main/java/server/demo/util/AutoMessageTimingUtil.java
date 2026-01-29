package server.demo.util;

import java.time.Duration;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动化消息发送时间工具：将 sendTiming（来自前端页面）转换为 Duration。
 */
public final class AutoMessageTimingUtil {

    private AutoMessageTimingUtil() {}

    private static final Pattern DAY_TIME_PATTERN = Pattern.compile("^DAY_(-?\\d+)_([0-2]\\d):([0-5]\\d)$");

    public record DayTimeTiming(int dayOffset, LocalTime time) {}

    public static Duration parseSendTiming(String sendTiming) {
        if (sendTiming == null || sendTiming.isBlank()) {
            throw new IllegalArgumentException("sendTiming is required");
        }

        return switch (sendTiming.trim().toUpperCase()) {
            case "IMMEDIATELY" -> Duration.ZERO;
            case "5_MIN" -> Duration.ofMinutes(5);
            case "10_MIN" -> Duration.ofMinutes(10);
            case "15_MIN" -> Duration.ofMinutes(15);
            case "30_MIN" -> Duration.ofMinutes(30);
            case "1_HOUR" -> Duration.ofHours(1);
            case "2_HOUR" -> Duration.ofHours(2);
            case "4_HOUR" -> Duration.ofHours(4);
            case "8_HOUR" -> Duration.ofHours(8);
            case "16_HOUR" -> Duration.ofHours(16);
            case "24_HOUR" -> Duration.ofHours(24);
            default -> throw new IllegalArgumentException("Unsupported sendTiming: " + sendTiming);
        };
    }

    public static boolean isDayTimeTiming(String sendTiming) {
        if (sendTiming == null || sendTiming.isBlank()) {
            return false;
        }
        return DAY_TIME_PATTERN.matcher(sendTiming.trim().toUpperCase()).matches();
    }

    public static DayTimeTiming parseDayTimeTiming(String sendTiming) {
        if (sendTiming == null || sendTiming.isBlank()) {
            throw new IllegalArgumentException("sendTiming is required");
        }
        String normalized = sendTiming.trim().toUpperCase();
        Matcher m = DAY_TIME_PATTERN.matcher(normalized);
        if (!m.matches()) {
            throw new IllegalArgumentException("Unsupported day-time sendTiming: " + sendTiming);
        }

        int dayOffset;
        try {
            dayOffset = Integer.parseInt(m.group(1));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid dayOffset: " + sendTiming);
        }

        int hour = Integer.parseInt(m.group(2));
        int minute = Integer.parseInt(m.group(3));
        if (hour > 23) {
            throw new IllegalArgumentException("Invalid hour: " + sendTiming);
        }
        return new DayTimeTiming(dayOffset, LocalTime.of(hour, minute));
    }
}
