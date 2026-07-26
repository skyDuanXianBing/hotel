package server.demo.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesGcmCryptoTest {

    private static final String KEY_BASE64 = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY_BASE64;

    static {
        byte[] other = new byte[32];
        other[0] = 1;
        OTHER_KEY_BASE64 = Base64.getEncoder().encodeToString(other);
    }

    @Test
    void encryptDecrypt_shouldRoundTrip() {
        AesGcmCrypto crypto = AesGcmCrypto.fromBase64Key(KEY_BASE64);

        String ciphertext = crypto.encrypt("sk_test_round_trip_123456");

        assertNotEquals("sk_test_round_trip_123456", ciphertext);
        assertTrue(ciphertext.startsWith("v1:"));
        assertEquals("sk_test_round_trip_123456", crypto.decrypt(ciphertext));
    }

    @Test
    void encrypt_shouldUseRandomNoncePerCall() {
        AesGcmCrypto crypto = AesGcmCrypto.fromBase64Key(KEY_BASE64);

        String first = crypto.encrypt("whsec_same_plaintext");
        String second = crypto.encrypt("whsec_same_plaintext");

        assertNotEquals(first, second);
        assertEquals("whsec_same_plaintext", crypto.decrypt(first));
        assertEquals("whsec_same_plaintext", crypto.decrypt(second));
    }

    @Test
    void decrypt_withWrongKey_shouldFail() {
        AesGcmCrypto crypto = AesGcmCrypto.fromBase64Key(KEY_BASE64);
        AesGcmCrypto other = AesGcmCrypto.fromBase64Key(OTHER_KEY_BASE64);
        String ciphertext = crypto.encrypt("sk_test_wrong_key");

        assertThrows(IllegalStateException.class, () -> other.decrypt(ciphertext));
    }

    @Test
    void decrypt_withTamperedCiphertext_shouldFail() {
        AesGcmCrypto crypto = AesGcmCrypto.fromBase64Key(KEY_BASE64);
        String ciphertext = crypto.encrypt("sk_test_tamper");
        String[] parts = ciphertext.split(":", 3);
        String tampered = parts[0] + ":" + parts[1] + ":"
                + Base64.getEncoder().encodeToString(new byte[32]);

        assertThrows(IllegalStateException.class, () -> crypto.decrypt(tampered));
    }

    @Test
    void decrypt_withUnsupportedFormat_shouldFail() {
        AesGcmCrypto crypto = AesGcmCrypto.fromBase64Key(KEY_BASE64);

        assertThrows(IllegalStateException.class, () -> crypto.decrypt("not-a-valid-payload"));
    }

    @Test
    void fromBase64Key_shouldRejectInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> AesGcmCrypto.fromBase64Key(null));
        assertThrows(IllegalArgumentException.class, () -> AesGcmCrypto.fromBase64Key("  "));
        assertThrows(IllegalArgumentException.class, () -> AesGcmCrypto.fromBase64Key("!!!not-base64!!!"));
        // Base64 合法但不是 32 字节
        assertThrows(
                IllegalArgumentException.class,
                () -> AesGcmCrypto.fromBase64Key(Base64.getEncoder().encodeToString(new byte[16]))
        );
    }
}
