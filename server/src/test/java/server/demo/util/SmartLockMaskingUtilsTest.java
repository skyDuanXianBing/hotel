package server.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartLockMaskingUtilsTest {
    @Test
    void redactSensitiveMessage_shouldCoverProviderCredentialFields() {
        String raw = "provider rejected token=tok_live_123 secret=sec_live_456 "
                + "clientSecret=client_secret_789 password=plain_password passcode=123456 "
                + "accessToken=access_live refresh_token=refresh_live "
                + "authorization: Bearer bearer_live device token=device_token_123 "
                + "apiKey=api_key_123 key=raw_key_123 status=400";

        String redacted = SmartLockMaskingUtils.redactSensitiveMessage(raw);

        assertFalse(redacted.contains("tok_live_123"));
        assertFalse(redacted.contains("sec_live_456"));
        assertFalse(redacted.contains("client_secret_789"));
        assertFalse(redacted.contains("plain_password"));
        assertFalse(redacted.contains("123456"));
        assertFalse(redacted.contains("access_live"));
        assertFalse(redacted.contains("refresh_live"));
        assertFalse(redacted.contains("bearer_live"));
        assertFalse(redacted.contains("device_token_123"));
        assertFalse(redacted.contains("api_key_123"));
        assertFalse(redacted.contains("raw_key_123"));
        assertTrue(redacted.contains("provider rejected"));
        assertTrue(redacted.contains("status=400"));
        assertTrue(redacted.contains("[REDACTED]"));
    }
}
