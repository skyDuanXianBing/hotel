package server.demo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmartLockMaskingUtils {
    private static final String REDACTED = "[REDACTED]";
    private static final String SENSITIVE_KEY_PATTERN = String.join(
            "|",
            "access[-_\\s]*token",
            "refresh[-_\\s]*token",
            "client[-_\\s]*secret",
            "device[-_\\s]*token",
            "api[-_\\s]*key",
            "secret[-_\\s]*key",
            "encryption[-_\\s]*key",
            "authorization",
            "keyboardPwd",
            "passcode",
            "password",
            "secret",
            "token",
            "pwd",
            "key"
    );
    private static final Pattern SENSITIVE_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)([\"']?\\b(?:" + SENSITIVE_KEY_PATTERN + ")\\b[\"']?\\s*[:=]\\s*)"
                    + "(\"[^\"]*\"|'[^']*'|[^\\s,;{}\\]\\)&]+)"
    );
    private static final Pattern AUTH_SCHEME_PATTERN = Pattern.compile(
            "(?i)\\b(Bearer|Basic)\\s+([A-Za-z0-9._~+/=-]+)"
    );
    private static final Pattern PASSCODE_NUMBER_PATTERN = Pattern.compile(
            "(?i)((?:\\b(?:passcode|password|keyboardPwd|pwd)\\b|密码)\\s*(?:is|为|是|[:=])?\\s*)(\\d{4,12})"
    );

    private SmartLockMaskingUtils() {
    }

    public static String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    public static String maskPasscode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 2) {
            return "**";
        }
        int visible = Math.min(2, trimmed.length());
        return "****" + trimmed.substring(trimmed.length() - visible);
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    public static String safeExceptionMessage(Throwable error) {
        if (error == null) {
            return "未知错误";
        }
        String message = redactSensitiveMessage(error.getMessage());
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message;
    }

    public static String redactSensitiveMessage(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        String redacted = redactAuthSchemes(message);
        redacted = redactKeyValues(redacted);
        return redactPasscodeNumbers(redacted);
    }

    private static String redactAuthSchemes(String message) {
        Matcher matcher = AUTH_SCHEME_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + " " + REDACTED;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String redactKeyValues(String message) {
        Matcher matcher = SENSITIVE_KEY_VALUE_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + redactedValue(matcher.group(2));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String redactPasscodeNumbers(String message) {
        Matcher matcher = PASSCODE_NUMBER_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + REDACTED;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String redactedValue(String value) {
        if (value == null || value.length() < 2) {
            return REDACTED;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return first + REDACTED + String.valueOf(last);
        }
        return REDACTED;
    }
}
