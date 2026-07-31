package server.demo.util;

import java.util.Locale;

import server.demo.i18n.ApiMessages;
public final class PriceLabsCountryUtil {
    private PriceLabsCountryUtil() {}

    public static String normalizeToAlpha2(String rawCountry) {
        if (rawCountry == null) return null;
        String trimmed = rawCountry.trim();
        if (trimmed.isEmpty()) return null;

        String upper = trimmed.toUpperCase(Locale.ROOT);

        if ("CHN".equals(upper) || "CHINA".equals(upper) || "PRC".equals(upper) || ApiMessages.get("api.t.101806f57c32").equals(trimmed)) return "CN";
        if ("JPN".equals(upper) || "JAPAN".equals(upper) || ApiMessages.get("api.t.44da6bbcf285").equals(trimmed)) return "JP";
        if ("USA".equals(upper) || "UNITED STATES".equals(upper) || ApiMessages.get("api.t.2d1093550636").equals(trimmed) || "U.S.".equals(upper) || "US".equals(upper)) return "US";
        if ("GBR".equals(upper) || "UNITED KINGDOM".equals(upper) || "UK".equals(upper) || ApiMessages.get("api.t.ec90e84c8fa4").equals(trimmed) || "GB".equals(upper)) return "GB";

        if (upper.length() == 2 && upper.chars().allMatch(Character::isLetter)) return upper;

        return null;
    }

    public static String normalizeToAlpha3(String rawCountry) {
        if (rawCountry == null) return null;
        String trimmed = rawCountry.trim();
        if (trimmed.isEmpty()) return null;

        String upper = trimmed.toUpperCase(Locale.ROOT);

        if ("CN".equals(upper) || "CHN".equals(upper) || "CHINA".equals(upper) || "PRC".equals(upper) || ApiMessages.get("api.t.101806f57c32").equals(trimmed)) return "CHN";
        if ("JP".equals(upper) || "JPN".equals(upper) || "JAPAN".equals(upper) || ApiMessages.get("api.t.44da6bbcf285").equals(trimmed)) return "JPN";
        if ("US".equals(upper) || "USA".equals(upper) || "UNITED STATES".equals(upper) || ApiMessages.get("api.t.2d1093550636").equals(trimmed) || "U.S.".equals(upper)) return "USA";
        if ("GB".equals(upper) || "GBR".equals(upper) || "UNITED KINGDOM".equals(upper) || "UK".equals(upper) || ApiMessages.get("api.t.ec90e84c8fa4").equals(trimmed)) return "GBR";

        if (upper.length() == 3 && upper.chars().allMatch(Character::isLetter)) return upper;

        return null;
    }
}
