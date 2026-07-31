package server.demo.i18n;

import java.util.Locale;
import java.util.Locale.LanguageRange;

/**
 * Supported API locales and header/tag normalization.
 */
public final class AppLocale {

    public static final String HEADER_APP_LOCALE = "X-App-Locale";
    public static final Locale DEFAULT = Locale.forLanguageTag("zh-CN");

    private AppLocale() {
    }

    public static Locale resolve(String appLocaleHeader, String acceptLanguageHeader) {
        Locale fromApp = fromTag(appLocaleHeader);
        if (fromApp != null) {
            return fromApp;
        }
        Locale fromAccept = fromAcceptLanguage(acceptLanguageHeader);
        if (fromAccept != null) {
            return fromAccept;
        }
        return DEFAULT;
    }

    public static Locale fromTag(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String tag = raw.trim().replace('_', '-');
        // Take first token if a list sneaks in
        int comma = tag.indexOf(',');
        if (comma >= 0) {
            tag = tag.substring(0, comma).trim();
        }
        int q = tag.indexOf(';');
        if (q >= 0) {
            tag = tag.substring(0, q).trim();
        }
        if (tag.isEmpty()) {
            return null;
        }
        return normalize(Locale.forLanguageTag(tag));
    }

    public static Locale fromAcceptLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }
        try {
            for (LanguageRange range : LanguageRange.parse(acceptLanguage)) {
                Locale mapped = normalize(Locale.forLanguageTag(range.getRange()));
                if (mapped != null) {
                    return mapped;
                }
            }
        } catch (IllegalArgumentException ignored) {
            Locale direct = fromTag(acceptLanguage);
            if (direct != null) {
                return direct;
            }
        }
        return null;
    }

    /**
     * Map: zh-TW/zh-HK → zh-TW; zh/zh-CN → zh-CN; ja → ja; en → en; else null.
     */
    public static Locale normalize(Locale locale) {
        if (locale == null) {
            return null;
        }
        String language = locale.getLanguage();
        if (language == null || language.isBlank()) {
            return null;
        }
        String languageLower = language.toLowerCase(Locale.ROOT);
        String script = locale.getScript() == null ? "" : locale.getScript();
        String country = locale.getCountry() == null ? "" : locale.getCountry().toUpperCase(Locale.ROOT);

        if ("zh".equals(languageLower)) {
            if ("Hant".equalsIgnoreCase(script)
                    || "TW".equals(country)
                    || "HK".equals(country)
                    || "MO".equals(country)) {
                return Locale.forLanguageTag("zh-TW");
            }
            return Locale.forLanguageTag("zh-CN");
        }
        if ("ja".equals(languageLower)) {
            return Locale.forLanguageTag("ja");
        }
        if ("en".equals(languageLower)) {
            return Locale.forLanguageTag("en");
        }
        return null;
    }

    public static String toTag(Locale locale) {
        Locale normalized = normalize(locale);
        if (normalized == null) {
            return DEFAULT.toLanguageTag();
        }
        return normalized.toLanguageTag();
    }
}
