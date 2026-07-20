package server.demo.service;

import java.util.List;
import java.util.Locale;

/**
 * Review 模块允许的 PMS 渠道代码边界。
 * <p>
 * 历史初始化脚本会创建 {@code booking_<storeId>} / {@code airbnb_<storeId>}。
 * 只兼容当前门店的精确后缀，不接受近似名称或其他门店后缀。
 */
public final class ReviewChannelCodePolicy {

    public static final int CHANNEL_BOOKING = 19;
    public static final int CHANNEL_AIRBNB = 244;

    private static final String BOOKING = "BOOKING";
    private static final String AIRBNB = "AIRBNB";

    private ReviewChannelCodePolicy() {
    }

    public static String canonicalCode(Integer suChannelId) {
        if (suChannelId != null && suChannelId == CHANNEL_BOOKING) {
            return BOOKING;
        }
        if (suChannelId != null && suChannelId == CHANNEL_AIRBNB) {
            return AIRBNB;
        }
        return null;
    }

    public static List<String> acceptedStoreCodes(Long storeId, String canonicalCode) {
        String canonical = normalizeCanonical(canonicalCode);
        if (storeId == null || canonical == null) {
            return List.of();
        }
        return List.of(canonical, canonical + "_" + storeId);
    }

    public static boolean matchesStoreCode(Long storeId, String candidateCode, String canonicalCode) {
        if (candidateCode == null) {
            return false;
        }
        String normalizedCandidate = candidateCode.trim().toUpperCase(Locale.ROOT);
        return acceptedStoreCodes(storeId, canonicalCode).contains(normalizedCandidate);
    }

    private static String normalizeCanonical(String canonicalCode) {
        if (canonicalCode == null) {
            return null;
        }
        String normalized = canonicalCode.trim().toUpperCase(Locale.ROOT);
        return BOOKING.equals(normalized) || AIRBNB.equals(normalized) ? normalized : null;
    }
}
