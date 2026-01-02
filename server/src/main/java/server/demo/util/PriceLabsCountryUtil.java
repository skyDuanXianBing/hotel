package server.demo.util;

import java.util.Locale;

public final class PriceLabsCountryUtil {
    private PriceLabsCountryUtil() {}

    public static String normalizeToAlpha2(String rawCountry) {
        if (rawCountry == null) return null;
        String trimmed = rawCountry.trim();
        if (trimmed.isEmpty()) return null;

        String upper = trimmed.toUpperCase(Locale.ROOT);

        if ("CHN".equals(upper) || "CHINA".equals(upper) || "PRC".equals(upper) || "中国".equals(trimmed)) return "CN";
        if ("JPN".equals(upper) || "JAPAN".equals(upper) || "日本".equals(trimmed)) return "JP";
        if ("USA".equals(upper) || "UNITED STATES".equals(upper) || "美国".equals(trimmed) || "U.S.".equals(upper) || "US".equals(upper)) return "US";
        if ("GBR".equals(upper) || "UNITED KINGDOM".equals(upper) || "UK".equals(upper) || "英国".equals(trimmed) || "GB".equals(upper)) return "GB";

        if (upper.length() == 2 && upper.chars().allMatch(Character::isLetter)) return upper;

        return null;
    }

    public static String normalizeToAlpha3(String rawCountry) {
        if (rawCountry == null) return null;
        String trimmed = rawCountry.trim();
        if (trimmed.isEmpty()) return null;

        String upper = trimmed.toUpperCase(Locale.ROOT);

        if ("CN".equals(upper) || "CHN".equals(upper) || "CHINA".equals(upper) || "PRC".equals(upper) || "中国".equals(trimmed)) return "CHN";
        if ("JP".equals(upper) || "JPN".equals(upper) || "JAPAN".equals(upper) || "日本".equals(trimmed)) return "JPN";
        if ("US".equals(upper) || "USA".equals(upper) || "UNITED STATES".equals(upper) || "美国".equals(trimmed) || "U.S.".equals(upper)) return "USA";
        if ("GB".equals(upper) || "GBR".equals(upper) || "UNITED KINGDOM".equals(upper) || "UK".equals(upper) || "英国".equals(trimmed)) return "GBR";

        if (upper.length() == 3 && upper.chars().allMatch(Character::isLetter)) return upper;

        return null;
    }
}
