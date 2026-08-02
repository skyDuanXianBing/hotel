package server.demo.util;

import java.util.Map;

/**
 * 基于 Unicode 区间的客人消息语言粗检测。
 * 仅用于 AI 回复草稿等 prompt 的语言提示，不做精确识别。
 * 拉丁字母语言（英/德/法等）无法区分，检测不到有区分度的文字时返回 null，
 * 由调用方退回“使用客人最后一轮消息原文语言”的通用 prompt 指令。
 */
public final class GuestLanguageDetector {

    private static final Map<String, String> LANGUAGE_NAMES = Map.of(
            "zh", "Simplified Chinese",
            "ja", "Japanese",
            "ko", "Korean",
            "ar", "Arabic",
            "ru", "Russian",
            "th", "Thai"
    );

    private GuestLanguageDetector() {
    }

    /**
     * 检测有区分度文字（中/日/韩/阿拉伯/西里尔/泰文）对应的语言代码；无法判断时返回 null。
     */
    public static String detectDistinctiveLanguageCode(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (text.matches(".*[\\u3040-\\u30FF].*")) {
            return "ja";
        }
        if (text.matches(".*[\\uAC00-\\uD7AF].*")) {
            return "ko";
        }
        if (text.matches(".*[\\u0600-\\u06FF].*")) {
            return "ar";
        }
        if (text.matches(".*[\\u0400-\\u04FF].*")) {
            return "ru";
        }
        if (text.matches(".*[\\u0E00-\\u0E7F].*")) {
            return "th";
        }
        if (text.matches(".*[\\u4E00-\\u9FFF].*")) {
            return "zh";
        }
        return null;
    }

    /**
     * 返回用于 prompt 的英文语言名称；无法识别时返回 null。
     */
    public static String detectLanguageName(String text) {
        String code = detectDistinctiveLanguageCode(text);
        return code == null ? null : LANGUAGE_NAMES.get(code);
    }
}
