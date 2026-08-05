package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.util.SuChannelCatalog;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SuApiClient.getEncryptedChannelCode 键名规整聚焦测试（B4：TRIP 为主键名，CTRIP 保留别名）。
 */
class SuApiClientEncryptedChannelCodeTest {

    private final SuApiClient suApiClient = new SuApiClient();

    @Test
    void shouldResolveAllFiveCatalogChannels() {
        assertEquals(SuChannelCatalog.BOOKING.encryptedCode(), suApiClient.getEncryptedChannelCode("BOOKING"));
        assertEquals(SuChannelCatalog.AIRBNB.encryptedCode(), suApiClient.getEncryptedChannelCode("AIRBNB"));
        assertEquals(SuChannelCatalog.EXPEDIA.encryptedCode(), suApiClient.getEncryptedChannelCode("EXPEDIA"));
        assertEquals(SuChannelCatalog.TRIP.encryptedCode(), suApiClient.getEncryptedChannelCode("TRIP"));
        assertEquals(SuChannelCatalog.AGODA.encryptedCode(), suApiClient.getEncryptedChannelCode("AGODA"));
    }

    @Test
    void tripAndCtripAlias_shouldShareTheSameEncryptedCode() {
        String trip = suApiClient.getEncryptedChannelCode("TRIP");
        String ctrip = suApiClient.getEncryptedChannelCode("CTRIP");
        assertEquals("mvYVz5x5ExxioyfyMo3jUUpNVZVbMyC6SUExMG9iaIY", trip);
        assertEquals(trip, ctrip);
    }

    @Test
    void shouldKeepVrboPresetCode() {
        assertEquals("6w9fCl2fQYkSXlG4pJXMFegJVyWDk7K0IHzqmjm2egI", suApiClient.getEncryptedChannelCode("VRBO"));
    }

    @Test
    void shouldReturnEmptyForUnknownOrNull() {
        assertEquals("", suApiClient.getEncryptedChannelCode("UNKNOWN"));
        assertEquals("", suApiClient.getEncryptedChannelCode(null));
    }
}
