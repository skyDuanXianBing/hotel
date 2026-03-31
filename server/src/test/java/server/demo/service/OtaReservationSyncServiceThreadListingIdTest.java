package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OtaReservationSyncServiceThreadListingIdTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolveThreadListingId_prefersWebhookListingOverFallback() throws Exception {
        JsonNode reservation = objectMapper.readTree("""
                {
                  "booking_details": { "property_id": "16016360" }
                }
                """);
        JsonNode roomStay = objectMapper.readTree("""
                {
                  "listingid": "16016361"
                }
                """);

        String resolved = OtaReservationSyncService.resolveThreadListingIdForReservation(
                reservation,
                roomStay,
                "52"
        );

        assertEquals("16016361", resolved);
    }

    @Test
    void resolveThreadListingId_usesFallbackOnlyWhenValid() throws Exception {
        JsonNode reservation = objectMapper.readTree("""
                {
                  "booking_details": {}
                }
                """);

        assertEquals(
                "16016360",
                OtaReservationSyncService.resolveThreadListingIdForReservation(reservation, null, "16016360")
        );
        assertNull(
                OtaReservationSyncService.resolveThreadListingIdForReservation(reservation, null, "51")
        );
    }
}

