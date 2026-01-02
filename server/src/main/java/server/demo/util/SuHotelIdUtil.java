package server.demo.util;

import java.util.Locale;

/**
 * Su Channel Manager 的 hotelid / HotelCode 工具类。
 */
public final class SuHotelIdUtil {

    // Su 错误码 853：HotelCode 最大 15 个字符
    private static final int MAX_LENGTH = 15;
    private static final String VALID_PATTERN = "^[A-Z0-9]{1,15}$";

    private SuHotelIdUtil() {}

    public static String buildDefault(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        String hotelId = "STORE" + storeId;
        if (hotelId.length() <= MAX_LENGTH) {
            return hotelId;
        }
        hotelId = "S" + storeId;
        if (hotelId.length() <= MAX_LENGTH) {
            return hotelId;
        }
        // 最后兜底：S + base36(storeId)，确保长度 <= 15
        return ("S" + Long.toString(storeId, 36)).toUpperCase(Locale.ROOT);
    }

    public static String normalize(String hotelId) {
        if (hotelId == null) {
            return null;
        }
        String trimmed = hotelId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String hotelId) {
        String normalized = normalize(hotelId);
        if (normalized == null) {
            return false;
        }
        if (normalized.length() > MAX_LENGTH) {
            return false;
        }
        return normalized.matches(VALID_PATTERN);
    }
}
