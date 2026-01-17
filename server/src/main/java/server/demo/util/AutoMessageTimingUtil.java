package server.demo.util;

import java.time.Duration;

/**
 * 自动化消息发送时间工具：将 sendTiming（来自前端页面）转换为 Duration。
 */
public final class AutoMessageTimingUtil {

    private AutoMessageTimingUtil() {}

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
}

