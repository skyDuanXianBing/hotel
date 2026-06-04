package server.demo.util;

import server.demo.enums.ResidenceType;
import server.demo.service.RegistrationTargetLanguage;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class RegistrationLanguageMapper {
    private static final String FALLBACK_REASON_UNRESOLVED = "UNRESOLVED_GUEST_LANGUAGE";

    private static final Set<String> JAPANESE_COUNTRIES = Set.of(
            "jp", "jpn", "japan", "japanese", "nihon", "nippon", "日本", "日本国", "日本人"
    );
    private static final Set<String> SIMPLIFIED_CHINESE_COUNTRIES = Set.of(
            "cn", "chn", "china", "chinese", "prc", "mainlandchina", "中国", "中國", "中国人", "中國人"
    );
    private static final Set<String> TRADITIONAL_CHINESE_COUNTRIES = Set.of(
            "tw", "twn", "taiwan", "taiwanese", "hk", "hongkong", "hksar", "mo", "macau",
            "macao", "台湾", "臺灣", "台湾人", "臺灣人", "香港", "澳门", "澳門"
    );
    private static final Set<String> KOREAN_COUNTRIES = Set.of(
            "kr", "kor", "korea", "southkorea", "korean", "republicofkorea", "韩国", "韓国",
            "대한민국", "한국"
    );
    private static final Set<String> ENGLISH_COUNTRIES = Set.of(
            "us", "usa", "unitedstates", "america", "american", "uk", "gb", "gbr", "unitedkingdom",
            "britain", "england", "australia", "australian", "canada", "canadian", "singapore",
            "singaporean", "newzealand", "ireland", "india", "philippines"
    );
    private static final Set<String> THAI_COUNTRIES = Set.of("th", "tha", "thailand", "thai", "泰国", "泰國", "タイ");
    private static final Set<String> VIETNAMESE_COUNTRIES = Set.of("vn", "vnm", "vietnam", "vietnamese", "越南");
    private static final Set<String> INDONESIAN_COUNTRIES = Set.of("id", "idn", "indonesia", "indonesian", "印度尼西亚", "印尼");
    private static final Set<String> FRENCH_COUNTRIES = Set.of("fr", "fra", "france", "french", "法国", "法國");
    private static final Set<String> SPANISH_COUNTRIES = Set.of("es", "esp", "spain", "spanish", "mexico", "mexican", "西班牙", "墨西哥");
    private static final Set<String> GERMAN_COUNTRIES = Set.of("de", "deu", "germany", "german", "德国", "德國");
    private static final Set<String> ITALIAN_COUNTRIES = Set.of("it", "ita", "italy", "italian", "意大利", "義大利");
    private static final Set<String> RUSSIAN_COUNTRIES = Set.of("ru", "rus", "russia", "russian", "俄罗斯", "俄羅斯");
    private static final Set<String> ARABIC_COUNTRIES = Set.of(
            "sa", "sau", "saudiarabia", "uae", "unitedarabemirates", "arabic", "عربي"
    );

    private RegistrationLanguageMapper() {
    }

    public static RegistrationTargetLanguage resolve(
            String nationality,
            String country,
            ResidenceType residenceType
    ) {
        RegistrationTargetLanguage byNationality = resolveText(nationality);
        if (byNationality != null) {
            return byNationality;
        }

        RegistrationTargetLanguage byCountry = resolveText(country);
        if (byCountry != null) {
            return byCountry;
        }

        if (residenceType == ResidenceType.JAPAN) {
            return RegistrationTargetLanguage.resolved("ja", "Japanese", "residenceType");
        }

        return RegistrationTargetLanguage.defaultEnglish(FALLBACK_REASON_UNRESOLVED);
    }

    private static RegistrationTargetLanguage resolveText(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return null;
        }

        if (JAPANESE_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("ja", "Japanese", value);
        }
        if (SIMPLIFIED_CHINESE_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("zh-CN", "Simplified Chinese", value);
        }
        if (TRADITIONAL_CHINESE_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("zh-TW", "Traditional Chinese", value);
        }
        if (KOREAN_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("ko", "Korean", value);
        }
        if (ENGLISH_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("en", "English", value);
        }
        if (THAI_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("th", "Thai", value);
        }
        if (VIETNAMESE_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("vi", "Vietnamese", value);
        }
        if (INDONESIAN_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("id", "Indonesian", value);
        }
        if (FRENCH_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("fr", "French", value);
        }
        if (SPANISH_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("es", "Spanish", value);
        }
        if (GERMAN_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("de", "German", value);
        }
        if (ITALIAN_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("it", "Italian", value);
        }
        if (RUSSIAN_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("ru", "Russian", value);
        }
        if (ARABIC_COUNTRIES.contains(normalized)) {
            return RegistrationTargetLanguage.resolved("ar", "Arabic", value);
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.getType(ch) == Character.NON_SPACING_MARK) {
                continue;
            }
            if (Character.isLetterOrDigit(ch)) {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
