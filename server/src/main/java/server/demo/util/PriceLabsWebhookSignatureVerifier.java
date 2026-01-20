package server.demo.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * PriceLabs webhook signature verifier for X-PL-* headers.
 */
public class PriceLabsWebhookSignatureVerifier {

    private static final String SIGNED_HEADERS_STRING = "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID";

    private final String integrationToken;

    public PriceLabsWebhookSignatureVerifier(String integrationToken) {
        if (integrationToken == null || integrationToken.isBlank()) {
            throw new IllegalArgumentException("integrationToken is required");
        }
        this.integrationToken = integrationToken;
    }

    /**
     * Verify X-PL-SIGNED-HEADERS and X-PL-SIGNED-BODY.
     */
    public boolean verify(
            String signedHeadersValue,
            String signedBodyValue,
            String source,
            String timestamp,
            String requestId,
            String requestBody
    ) {
        if (isBlank(signedHeadersValue) || isBlank(signedBodyValue)) {
            return false;
        }
        if (isBlank(source) || isBlank(timestamp) || isBlank(requestId)) {
            return false;
        }

        ParsedSignature headersSig = parseSignedValue(signedHeadersValue);
        if (!isHeadersSignatureMatch(headersSig, source, timestamp, requestId)) {
            return false;
        }

        String bodyToSign = signedHeadersValue + (requestBody != null ? requestBody : "");
        return matchesSignatureWithOptionalPrefix(signedBodyValue, bodyToSign);
    }

    private boolean matchesSignatureWithOptionalPrefix(String signedValue, String dataToSign) {
        if (signedValue == null || dataToSign == null) {
            return false;
        }
        String trimmed = signedValue.trim();
        if (matchesSignature(dataToSign, trimmed)) {
            return true;
        }
        ParsedSignature parsed = parseSignedValue(trimmed);
        return matchesSignature(dataToSign, parsed.signature);
    }

    private static ParsedSignature parseSignedValue(String value) {
        if (value == null) {
            return new ParsedSignature(null, null);
        }
        String trimmed = value.trim();
        int dotIndex = trimmed.indexOf('.');
        if (dotIndex > 0 && dotIndex < trimmed.length() - 1) {
            return new ParsedSignature(trimmed.substring(0, dotIndex), trimmed.substring(dotIndex + 1));
        }
        return new ParsedSignature(null, trimmed);
    }

    private boolean isHeadersSignatureMatch(ParsedSignature signature, String source, String timestamp, String requestId) {
        if (signature == null || signature.signature == null) {
            return false;
        }
        String versionPrefix = signature.version;
        String[] baseHeadersToSign = buildHeadersToSignCandidates(source, timestamp, requestId);
        for (String headersToSign : baseHeadersToSign) {
            if (headersToSign == null) {
                continue;
            }
            String actual = versionPrefix != null ? versionPrefix + ":" + headersToSign : headersToSign;
            if (matchesSignature(actual, signature.signature)) {
                return true;
            }
        }
        return false;
    }

    private String[] buildHeadersToSignCandidates(String source, String timestamp, String requestId) {
        String s = source != null ? source : "";
        String t = timestamp != null ? timestamp : "";
        String r = requestId != null ? requestId : "";
        return new String[] {
                SIGNED_HEADERS_STRING,
                "X-SOURCE:" + s + ":X-PL-TIMESTAMP:" + t + ":X-PL-REQUESTID:" + r,
                s + ":" + t + ":" + r
        };
    }

    private boolean matchesSignature(String dataToSign, String signature) {
        if (dataToSign == null || signature == null) {
            return false;
        }
        String expectedBase64 = hmacSha256Base64(dataToSign);
        if (expectedBase64.equals(signature)) {
            return true;
        }
        String expectedHex = hmacSha256Hex(dataToSign);
        return expectedHex.equalsIgnoreCase(signature);
    }

    private String hmacSha256Base64(String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    integrationToken.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed: " + e.getMessage(), e);
        }
    }

    private String hmacSha256Hex(String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    integrationToken.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed: " + e.getMessage(), e);
        }
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ParsedSignature(String version, String signature) {}
}
