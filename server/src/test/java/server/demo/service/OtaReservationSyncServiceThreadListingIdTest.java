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
                "BOOKING",
                reservation,
                roomStay,
                "52"
        );

        assertEquals("16016361", resolved);
    }

                @Test
                void resolveThreadListingId_prefersChannelRoomIdForAirbnb() throws Exception {
                                JsonNode reservation = objectMapper.readTree("""
                                                                {
                                                                        "booking_details": { "property_id": "16016360" },
                                                                        "rooms": [
                                                                                { "channel_room_id": "1157718387975828174", "listingid": "16016361" }
                                                                        ]
                                                                }
                                                                """);
                                JsonNode roomStay = reservation.get("rooms").get(0);

                                String resolved = OtaReservationSyncService.resolveThreadListingIdForReservation(
                                                                "AIRBNB",
                                                                reservation,
                                                                roomStay,
                                                                "53"
                                );

                                assertEquals("1157718387975828174", resolved);
                }

    @Test
    void resolveThreadListingId_bookingDoesNotUseOtaRoomFallback() throws Exception {
        JsonNode reservation = objectMapper.readTree("""
                {
                  "booking_details": {}
                }
                """);

        assertNull(
                OtaReservationSyncService.resolveThreadListingIdForReservation("BOOKING", reservation, null, "16016360")
        );
        assertNull(
                OtaReservationSyncService.resolveThreadListingIdForReservation("BOOKING", reservation, null, "51")
        );
    }

    @Test
    void resolveThreadListingId_nonBookingCanUseValidFallback() throws Exception {
        JsonNode reservation = objectMapper.readTree("""
                {
                  "booking_details": {}
                }
                """);

        assertEquals(
                "16016360",
                OtaReservationSyncService.resolveThreadListingIdForReservation("AIRBNB", reservation, null, "16016360")
        );
    }
}
