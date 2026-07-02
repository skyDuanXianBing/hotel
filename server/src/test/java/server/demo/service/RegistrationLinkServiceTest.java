package server.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationLinkServiceTest {

    private static final String SECRET = "test-secret";
    private static final String BOOKING_KEY = "BOOK123";

    @Test
    void buildPublicBookingLink_shouldGenerateVerifiableToken() {
        RegistrationLinkService svc = new RegistrationLinkService(SECRET, 90);
        String url = svc.buildPublicBookingLink("https://example.com", 1L, BOOKING_KEY);
        assertTrue(url.startsWith("https://example.com/rb/BOOK123?t="));

        String token = url.substring(url.indexOf("?t=") + 3);
        RegistrationLinkService.Claims claims = svc.verifyToken(BOOKING_KEY, token);
        assertEquals(1L, claims.getStoreId());
        assertTrue(claims.getExpEpochSeconds() > 0);
    }

    @Test
    void buildPublicBookingLink_shouldEncodePathSegment() {
        RegistrationLinkService svc = new RegistrationLinkService(SECRET, 90);
        String url = svc.buildPublicBookingLink("https://example.com/", 9L, "B 1/2");
        assertTrue(url.startsWith("https://example.com/rb/"));
        assertTrue(url.contains("B%201%2F2"));
    }

    @Test
    void verifyToken_shouldAcceptExpiredLegacyTokenWhenSignatureMatches() {
        RegistrationLinkService svc = new RegistrationLinkService(SECRET, -1);
        String token = svc.generateToken(26L, BOOKING_KEY);

        RegistrationLinkService.Claims claims = svc.verifyToken(BOOKING_KEY, token);

        assertEquals(26L, claims.getStoreId());
        assertTrue(claims.getExpEpochSeconds() > 0);
    }

    @Test
    void verifyToken_shouldRejectTamperedExp() {
        RegistrationLinkService svc = new RegistrationLinkService(SECRET, -1);
        String token = svc.generateToken(26L, BOOKING_KEY);
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + (Long.parseLong(parts[1]) + 1L) + "." + parts[2];

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken(BOOKING_KEY, tamperedToken)
        );

        assertEquals("token无效", error.getMessage());
    }

    @Test
    void verifyToken_shouldRejectTamperedSignature() {
        RegistrationLinkService svc = new RegistrationLinkService(SECRET, -1);
        String token = svc.generateToken(26L, BOOKING_KEY);
        String tamperedToken = replaceLastCharacter(token);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken(BOOKING_KEY, tamperedToken)
        );

        assertEquals("token无效", error.getMessage());
    }

    @Test
    void verifyToken_shouldRejectTamperedStoreId() {
        RegistrationLinkService svc = new RegistrationLinkService(SECRET, -1);
        String token = svc.generateToken(26L, BOOKING_KEY);
        String[] parts = token.split("\\.");
        String tamperedToken = "27." + parts[1] + "." + parts[2];

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken(BOOKING_KEY, tamperedToken)
        );

        assertEquals("token无效", error.getMessage());
    }

    @Test
    void verifyToken_shouldRejectWrongSubject() {
        RegistrationLinkService svc = new RegistrationLinkService(SECRET, -1);
        String token = svc.generateToken(26L, BOOKING_KEY);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken("OTHER_BOOKING", token)
        );

        assertEquals("token无效", error.getMessage());
    }

    @Test
    void verifyToken_shouldRejectMalformedToken() {
        RegistrationLinkService svc = new RegistrationLinkService(SECRET, 90);

        assertEquals("缺少token", assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken(BOOKING_KEY, null)
        ).getMessage());
        assertEquals("缺少token", assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken(BOOKING_KEY, " ")
        ).getMessage());
        assertEquals("token格式错误", assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken(BOOKING_KEY, "1.2")
        ).getMessage());
        assertEquals("token格式错误", assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken(BOOKING_KEY, "store.2.signature")
        ).getMessage());
        assertEquals("token格式错误", assertThrows(
                IllegalArgumentException.class,
                () -> svc.verifyToken(BOOKING_KEY, "1.exp.signature")
        ).getMessage());
    }

    private String replaceLastCharacter(String value) {
        char replacement = '0';
        if (value.charAt(value.length() - 1) == replacement) {
            replacement = '1';
        }
        return value.substring(0, value.length() - 1) + replacement;
    }
}
