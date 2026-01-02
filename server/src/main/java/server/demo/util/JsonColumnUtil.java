package server.demo.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MySQL JSON 列写入辅助：
 * - 传入内容若已是合法 JSON（对象/数组/字符串/数字/布尔/null），原样（trim 后）保存
 * - 否则将其转换为合法 JSON 字符串（带引号并做转义）
 */
public final class JsonColumnUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonColumnUtil() {}

    public static String normalizeJsonText(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        if (isValidJson(trimmed)) {
            return trimmed;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(raw);
        } catch (Exception ignored) {
            // 极端情况下 Jackson 不可用时，兜底成一个最简单的 JSON 字符串
            return "\"" + escapeForJsonString(raw) + "\"";
        }
    }

    private static boolean isValidJson(String text) {
        try {
            OBJECT_MAPPER.readTree(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String escapeForJsonString(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}

