package server.demo.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
