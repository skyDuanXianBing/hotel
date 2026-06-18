package server.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageKnowledgeThreadOutputSanitizer {
    private static final int MIN_PRIVATE_VALUE_LENGTH = 4;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\w)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\w)"
    );
    private static final Pattern LONG_NUMBER_PATTERN = Pattern.compile("\\b\\d{6,}\\b");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_REFERENCE_PATTERN = Pattern.compile(
            "(?iu)(?:booking|reservation|confirmation|order|预订|預訂|订单|訂單|予約|確認)\\s*(?:id|no|number|编号|編號|番号)?\\s*[:：#-]?\\s*[A-Z0-9][A-Z0-9_-]{3,}"
    );
    private static final Pattern ONE_TIME_SECRET_PATTERN = Pattern.compile(
            "(?iu)(?:(?:one[- ]?time|temporary|verification|一次性|临时|臨時|验证码|驗證碼)\\s*(?:code|password|pin|密码|密碼|コード)?|(?:door|lock|keybox|smart lock|门锁|門鎖|钥匙盒|鑰匙盒)\\s*(?:code|password|pin|密码|密碼|コード))"
    );
    private static final Pattern PAYMENT_OR_ID_PATTERN = Pattern.compile(
            "(?iu)(?:passport|id card|identity|credit card|card number|payment card|cvv|护照|護照|身份证|身分證|信用卡|银行卡|銀行卡|支払|カード番号)"
    );
    private static final List<String> UNSAFE_URL_KEYS = List.of(
            "token",
            "signature",
            "session",
            "booking",
            "bookingid",
            "reservation",
            "reservationid",
            "order",
            "orderid",
            "guest",
            "guestid",
            "email",
            "phone",
            "code",
            "confirmation"
    );

    public MessageKnowledgeThreadSanitizedText sanitize(MessageKnowledgeThreadSanitizerRequest request) {
        if (request == null || request.text() == null) {
            return new MessageKnowledgeThreadSanitizedText("", true, false, List.of("blank_text"));
        }
        String normalized = normalizeText(request.text());
        if (normalized.isBlank()) {
            return new MessageKnowledgeThreadSanitizedText("", true, false, List.of("blank_text"));
        }

        List<String> reasonCodes = new ArrayList<>();
        boolean generalized = containsPlaceholder(normalized);
        addPatternReason(reasonCodes, EMAIL_PATTERN, normalized, "email");
        addPatternReason(reasonCodes, PHONE_PATTERN, normalized, "phone");
        addPatternReason(reasonCodes, ORDER_REFERENCE_PATTERN, stripPlaceholders(normalized), "order_reference");
        addPatternReason(reasonCodes, ONE_TIME_SECRET_PATTERN, normalized, "one_time_or_door_code");
        addPatternReason(reasonCodes, PAYMENT_OR_ID_PATTERN, normalized, "payment_or_identity");
        addLongNumberReason(reasonCodes, normalized);
        addPrivateContextReasons(reasonCodes, normalized, request.context());
        addUnsafeUrlReasons(reasonCodes, normalized);

        boolean rejected = !reasonCodes.isEmpty();
        return new MessageKnowledgeThreadSanitizedText(normalized, rejected, generalized, reasonCodes);
    }

    private static void addPatternReason(
            List<String> reasonCodes,
            Pattern pattern,
            String value,
            String reasonCode
    ) {
        if (pattern.matcher(value).find() && !reasonCodes.contains(reasonCode)) {
            reasonCodes.add(reasonCode);
        }
    }

    private static void addLongNumberReason(List<String> reasonCodes, String value) {
        Matcher matcher = LONG_NUMBER_PATTERN.matcher(value);
        while (matcher.find()) {
            String candidate = matcher.group();
            if (isLikelyDateOrTime(candidate)) {
                continue;
            }
            if (!reasonCodes.contains("long_number")) {
                reasonCodes.add("long_number");
            }
            return;
        }
    }

    private static void addPrivateContextReasons(
            List<String> reasonCodes,
            String value,
            SuMessagingThreadContext context
    ) {
        if (context == null) {
            return;
        }
        addPrivateValueReason(reasonCodes, value, context.getBookingKey(), "booking_key");
        addPrivateValueReason(reasonCodes, value, context.getGuestName(), "guest_name");
    }

    private static void addPrivateValueReason(
            List<String> reasonCodes,
            String value,
            String privateValue,
            String reasonCode
    ) {
        if (privateValue == null) {
            return;
        }
        String normalizedPrivateValue = privateValue.trim();
        if (normalizedPrivateValue.length() < MIN_PRIVATE_VALUE_LENGTH) {
            return;
        }
        if (containsPlaceholder(normalizedPrivateValue)) {
            return;
        }
        if (value.toLowerCase(Locale.ROOT).contains(normalizedPrivateValue.toLowerCase(Locale.ROOT))
                && !reasonCodes.contains(reasonCode)) {
            reasonCodes.add(reasonCode);
        }
    }

    private static void addUnsafeUrlReasons(List<String> reasonCodes, String value) {
        Matcher matcher = URL_PATTERN.matcher(value);
        while (matcher.find()) {
            String url = matcher.group();
            if (isUnsafeUrl(url)) {
                if (!reasonCodes.contains("private_url")) {
                    reasonCodes.add("private_url");
                }
                return;
            }
        }
    }

    private static boolean isUnsafeUrl(String url) {
        String normalizedUrl = trimTrailingUrlPunctuation(url);
        String placeholderStrippedUrl = stripPlaceholders(normalizedUrl);
        if (LONG_NUMBER_PATTERN.matcher(placeholderStrippedUrl).find()) {
            return true;
        }
        String lowerUrl = normalizedUrl.toLowerCase(Locale.ROOT);
        for (String key : UNSAFE_URL_KEYS) {
            if (hasUnsafeUrlParameter(lowerUrl, key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnsafeUrlParameter(String lowerUrl, String key) {
        Pattern pattern = Pattern.compile("[?&]" + Pattern.quote(key) + "=([^&#\\s]+)");
        Matcher matcher = pattern.matcher(lowerUrl);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (containsPlaceholder(value)) {
                continue;
            }
            if ("placeholder".equals(value)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static String stripPlaceholders(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\{[^}]+}", "placeholder");
    }

    private static boolean containsPlaceholder(String value) {
        return value != null && value.contains("{") && value.contains("}");
    }

    private static String trimTrailingUrlPunctuation(String value) {
        return value.replaceAll("[,，.;；。)）]+$", "");
    }

    private static boolean isLikelyDateOrTime(String value) {
        if (value == null) {
            return false;
        }
        return value.matches("20\\d{6}") || value.matches("\\d{8}");
    }

    private static String normalizeText(String value) {
        return value.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }
}
