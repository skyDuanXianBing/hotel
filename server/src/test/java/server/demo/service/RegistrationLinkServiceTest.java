package server.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationLinkServiceTest {

    @Test
    void buildPublicBookingLink_shouldGenerateVerifiableToken() {
        RegistrationLinkService svc = new RegistrationLinkService("test-secret", 90);
        String url = svc.buildPublicBookingLink("https://example.com", 1L, "BOOK123");
        assertTrue(url.startsWith("https://example.com/rb/BOOK123?t="));

        String token = url.substring(url.indexOf("?t=") + 3);
        RegistrationLinkService.Claims claims = svc.verifyToken("BOOK123", token);
        assertEquals(1L, claims.getStoreId());
        assertTrue(claims.getExpEpochSeconds() > 0);
    }

    @Test
    void buildPublicBookingLink_shouldEncodePathSegment() {
        RegistrationLinkService svc = new RegistrationLinkService("test-secret", 90);
        String url = svc.buildPublicBookingLink("https://example.com/", 9L, "B 1/2");
        assertTrue(url.startsWith("https://example.com/rb/"));
        assertTrue(url.contains("B%201%2F2"));
    }
}

