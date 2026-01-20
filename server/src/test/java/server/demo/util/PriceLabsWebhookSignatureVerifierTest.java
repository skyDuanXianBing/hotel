package server.demo.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceLabsWebhookSignatureVerifierTest {

    @Test
    void verify_withVersionPrefix_passes() {
        String token = "secret";
        String body = "{\"listing_id\":\"abc\",\"data\":[]}";
        String headersValue = "v1." + sign(token, "v1:X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String bodyValue = sign(token, headersValue + body);

        PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(token);
        assertTrue(verifier.verify(headersValue, bodyValue, "pms", "1700000000", "req-1", body));
    }

    @Test
    void verify_withoutVersionPrefix_passes() {
        String token = "secret";
        String body = "{\"listing_id\":\"abc\",\"data\":[]}";
        String headersValue = sign(token, "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String bodyValue = sign(token, headersValue + body);

        PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(token);
        assertTrue(verifier.verify(headersValue, bodyValue, "pms", "1700000000", "req-1", body));
    }

    @Test
    void verify_bodySignatureWithPrefix_passes() {
        String token = "secret";
        String body = "{\"listing_id\":\"abc\",\"data\":[]}";
        String headersValue = sign(token, "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String bodyValue = "v1." + sign(token, headersValue + body);

        PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(token);
        assertTrue(verifier.verify(headersValue, bodyValue, "pms", "1700000000", "req-1", body));
    }

    @Test
    void verify_missingHeaders_fails() {
        String token = "secret";
        String body = "{\"listing_id\":\"abc\",\"data\":[]}";
        String headersValue = sign(token, "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String bodyValue = sign(token, headersValue + body);

        PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(token);
        assertFalse(verifier.verify(headersValue, bodyValue, null, "1700000000", "req-1", body));
    }

    @Test
    void verify_signatureMismatch_fails() {
        String token = "secret";
        String body = "{\"listing_id\":\"abc\",\"data\":[]}";
        String headersValue = sign(token, "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String bodyValue = sign("other", headersValue + body);

        PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(token);
        assertFalse(verifier.verify(headersValue, bodyValue, "pms", "1700000000", "req-1", body));
    }

    @Test
    void verify_headerValuesSignature_passes() {
        String token = "secret";
        String body = "{\"listing_id\":\"abc\",\"data\":[]}";
        String source = "pms";
        String timestamp = "1700000000";
        String requestId = "req-1";
        String headersValue = sign(token, "X-SOURCE:" + source + ":X-PL-TIMESTAMP:" + timestamp + ":X-PL-REQUESTID:" + requestId);
        String bodyValue = sign(token, headersValue + body);

        PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(token);
        assertTrue(verifier.verify(headersValue, bodyValue, source, timestamp, requestId, body));
    }

    @Test
    void verify_plainValuesSignature_passes() {
        String token = "secret";
        String body = "{\"listing_id\":\"abc\",\"data\":[]}";
        String source = "pms";
        String timestamp = "1700000000";
        String requestId = "req-1";
        String headersValue = sign(token, source + ":" + timestamp + ":" + requestId);
        String bodyValue = sign(token, headersValue + body);

        PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(token);
        assertTrue(verifier.verify(headersValue, bodyValue, source, timestamp, requestId, body));
    }

    @Test
    void verify_hexSignature_passes() {
        String token = "secret";
        String body = "{\"listing_id\":\"abc\",\"data\":[]}";
        String headersValue = signHex(token, "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String bodyValue = signHex(token, headersValue + body);

        PriceLabsWebhookSignatureVerifier verifier = new PriceLabsWebhookSignatureVerifier(token);
        assertTrue(verifier.verify(headersValue, bodyValue, "pms", "1700000000", "req-1", body));
    }

    private static String sign(String token, String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String signHex(String token, String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
