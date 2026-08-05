package server.demo.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewChannelCodePolicyTest {

    @Test
    void acceptsOnlyStandardAndCurrentStoreLegacyCodes() {
        assertEquals(
                List.of("BOOKING", "BOOKING_10"),
                ReviewChannelCodePolicy.acceptedStoreCodes(10L, "BOOKING")
        );
        assertTrue(ReviewChannelCodePolicy.matchesStoreCode(10L, "BOOKING", "BOOKING"));
        assertTrue(ReviewChannelCodePolicy.matchesStoreCode(10L, "booking_10", "BOOKING"));
        assertTrue(ReviewChannelCodePolicy.matchesStoreCode(10L, "airbnb_10", "AIRBNB"));
    }

    @Test
    void rejectsApproximateOrOtherStoreCodes() {
        assertFalse(ReviewChannelCodePolicy.matchesStoreCode(10L, "BOOKING_COM", "BOOKING"));
        assertFalse(ReviewChannelCodePolicy.matchesStoreCode(10L, "BOOKING_11", "BOOKING"));
        assertFalse(ReviewChannelCodePolicy.matchesStoreCode(10L, "BOOKING_10_EXTRA", "BOOKING"));
        assertFalse(ReviewChannelCodePolicy.matchesStoreCode(10L, "AIRBNB_PARTNER", "AIRBNB"));
    }

    @Test
    void canonicalCode_shouldAllowBookingAirbnbExpedia() {
        assertEquals("BOOKING", ReviewChannelCodePolicy.canonicalCode(19));
        assertEquals("AIRBNB", ReviewChannelCodePolicy.canonicalCode(244));
        assertEquals("EXPEDIA", ReviewChannelCodePolicy.canonicalCode(9));
    }

    @Test
    void canonicalCode_shouldRejectTripAgodaAndUnknown() {
        // TRIP(339)/AGODA(189) Su 官方 Review API 不支持；VRBO(253) 未接入
        assertNull(ReviewChannelCodePolicy.canonicalCode(339));
        assertNull(ReviewChannelCodePolicy.canonicalCode(189));
        assertNull(ReviewChannelCodePolicy.canonicalCode(253));
        assertNull(ReviewChannelCodePolicy.canonicalCode(null));
        assertNull(ReviewChannelCodePolicy.canonicalCode(999));
    }

    @Test
    void acceptedStoreCodes_shouldSupportExpediaCanonicalAndStoreSuffix() {
        assertEquals(
                List.of("EXPEDIA", "EXPEDIA_10"),
                ReviewChannelCodePolicy.acceptedStoreCodes(10L, "EXPEDIA")
        );
        assertTrue(ReviewChannelCodePolicy.matchesStoreCode(10L, "EXPEDIA", "EXPEDIA"));
        assertTrue(ReviewChannelCodePolicy.matchesStoreCode(10L, "expedia_10", "EXPEDIA"));
        assertFalse(ReviewChannelCodePolicy.matchesStoreCode(10L, "EXPEDIA_11", "EXPEDIA"));
    }

    @Test
    void acceptedStoreCodes_shouldRejectTripAgodaCanonicalCodes() {
        assertEquals(List.of(), ReviewChannelCodePolicy.acceptedStoreCodes(10L, "TRIP"));
        assertEquals(List.of(), ReviewChannelCodePolicy.acceptedStoreCodes(10L, "AGODA"));
        assertEquals(List.of(), ReviewChannelCodePolicy.acceptedStoreCodes(10L, "BOOKING.COM"));
        assertEquals(List.of(), ReviewChannelCodePolicy.acceptedStoreCodes(null, "EXPEDIA"));
    }
}
