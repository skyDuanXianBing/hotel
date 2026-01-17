package server.demo.util;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Su Channel Manager 的 hotelid / HotelCode 工具类。
 */
public final class SuHotelIdUtil {

    // Su 错误码 853：HotelCode 最大 15 个字符
    private static final int MAX_LENGTH = 15;
    private static final String VALID_PATTERN = "^[A-Z0-9]{1,15}$";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

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
        return ("S" + Long.toString(storeId, 36)).toUpperCase(Locale.ROOT);
    }

    /**
     * 生成随机 hotelid（仅 A-Z/0-9，长度 <= 15），用于避免不同账号/环境之间的 hotelid 冲突。
     */
    public static String generateRandom() {
        return generateRandom(10);
    }

    public static String generateRandom(int length) {
        if (length < 1 || length > MAX_LENGTH) {
            throw new IllegalArgumentException("length must be between 1 and " + MAX_LENGTH);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);
        }
        return sb.toString();
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
