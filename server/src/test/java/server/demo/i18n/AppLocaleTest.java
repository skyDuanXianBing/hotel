package server.demo.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppLocaleTest {

    @Test
    void resolve_prefersAppLocaleHeader() {
        Locale locale = AppLocale.resolve("en", "zh-CN,zh;q=0.9");
        assertEquals("en", locale.toLanguageTag());
    }

    @Test
    void resolve_usesAcceptLanguageWhenAppLocaleMissing() {
        Locale locale = AppLocale.resolve(null, "ja-JP,ja;q=0.9");
        assertEquals("ja", locale.toLanguageTag());
    }

    @Test
    void resolve_fallsBackToZhCn() {
        Locale locale = AppLocale.resolve(null, null);
        assertEquals("zh-CN", locale.toLanguageTag());
    }

    @Test
    void normalize_mapsTraditionalChineseVariants() {
        assertEquals("zh-TW", AppLocale.normalize(Locale.forLanguageTag("zh-HK")).toLanguageTag());
        assertEquals("zh-TW", AppLocale.normalize(Locale.forLanguageTag("zh-TW")).toLanguageTag());
        assertEquals("zh-CN", AppLocale.normalize(Locale.forLanguageTag("zh")).toLanguageTag());
        assertEquals("zh-CN", AppLocale.normalize(Locale.forLanguageTag("zh-CN")).toLanguageTag());
    }
}
