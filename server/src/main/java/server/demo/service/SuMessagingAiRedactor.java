package server.demo.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class SuMessagingAiRedactor {
    private static final int MAX_TEXT_LENGTH = 1200;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://\\S+",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LONG_NUMBER_PATTERN = Pattern.compile("\\b\\d{6,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\w)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\w)"
    );

    public String redact(String value) {
        if (value == null) {
            return "";
        }

        String redacted = value;
        redacted = URL_PATTERN.matcher(redacted).replaceAll("[redacted-url]");
        redacted = EMAIL_PATTERN.matcher(redacted).replaceAll("[redacted-email]");
        redacted = PHONE_PATTERN.matcher(redacted).replaceAll("[redacted-phone]");
        redacted = LONG_NUMBER_PATTERN.matcher(redacted).replaceAll("[redacted-number]");
        redacted = redacted.replaceAll("[\\r\\n\\t]+", " ");
        redacted = redacted.replaceAll("\\s{2,}", " ").trim();
        if (redacted.length() > MAX_TEXT_LENGTH) {
            return redacted.substring(0, MAX_TEXT_LENGTH).trim();
        }
        return redacted;
    }
}
