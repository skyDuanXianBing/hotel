package server.demo.util;

import server.demo.entity.Reservation;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 客人消息语言判定：
 * 1. guestLanguage（渠道订单 customer.guest_lang）为日语 → 日本客人
 * 2. guestCountry（渠道订单 customer.countrycode / 独立站无此字段）指向日本 → 日本客人
 * 3. guestPhone 任一号码带日本国际区号（+81 / 0081）→ 日本客人
 * 以上都不满足 → 非日本客人（消息统一发英文）。
 * 不带国家码的本地号码（如 090-1234-5678）不做猜测，避免误判。
 */
public final class GuestMessageLanguageUtil {

    private static final Pattern PHONE_SPLIT_PATTERN = Pattern.compile("[,，;；\\s]+");
    private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("[^0-9]");
    private static final Set<String> JAPANESE_LANGUAGE_CODES = Set.of("ja", "jpn", "japanese");

    private GuestMessageLanguageUtil() {
    }

    public static boolean isJapaneseGuest(Reservation reservation) {
        if (reservation == null) {
            return false;
        }
        return isJapanese(reservation.getGuestLanguage(), reservation.getGuestCountry(), reservation.getGuestPhone());
    }

    public static boolean isJapanese(String guestLanguage, String guestCountry, String guestPhone) {
        if (isJapaneseLanguage(guestLanguage)) {
            return true;
        }
        if (RegistrationLanguageMapper.isJapaneseCountry(guestCountry)) {
            return true;
        }
        return hasJapanesePhonePrefix(guestPhone);
    }

    static boolean isJapaneseLanguage(String guestLanguage) {
        if (guestLanguage == null) {
            return false;
        }
        String normalized = guestLanguage.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.startsWith("ja-") || normalized.startsWith("ja_")) {
            return true;
        }
        return JAPANESE_LANGUAGE_CODES.contains(normalized)
                || "日本語".equals(normalized)
                || "日语".equals(normalized)
                || "日語".equals(normalized);
    }

    static boolean hasJapanesePhonePrefix(String guestPhone) {
        if (guestPhone == null || guestPhone.isBlank()) {
            return false;
        }
        for (String segment : PHONE_SPLIT_PATTERN.split(guestPhone.trim())) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            String trimmed = segment.trim();
            boolean hasPlus = trimmed.startsWith("+");
            String digits = NON_DIGIT_PATTERN.matcher(trimmed).replaceAll("");
            if (digits.isEmpty()) {
                continue;
            }
            // +81... / 0081... → 日本；不带国家码的本地号码不猜测
            if (hasPlus && digits.startsWith("81")) {
                return true;
            }
            if (digits.startsWith("0081")) {
                return true;
            }
        }
        return false;
    }
}
