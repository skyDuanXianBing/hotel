package server.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SuBusinessAutoMessageServiceListingValidationTest {

    @Test
    void resolveTrustedListingId_rejectsShortNumericRoomIds() {
        assertNull(SuBusinessAutoMessageService.resolveTrustedListingId("50"));
        assertNull(SuBusinessAutoMessageService.resolveTrustedListingId(" 51 "));
    }

    @Test
    void resolveTrustedListingId_acceptsValidMessagingListingIds() {
        assertEquals("16016360", SuBusinessAutoMessageService.resolveTrustedListingId("16016360"));
        assertEquals("ABCD_1234", SuBusinessAutoMessageService.resolveTrustedListingId("ABCD_1234"));
    }
}

