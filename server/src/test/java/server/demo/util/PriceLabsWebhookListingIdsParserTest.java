package server.demo.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceLabsWebhookListingIdsParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseListingIdsOnlyPayload() {
        String json = """
                {
                  "listing_ids": ["store_1_room_type_2", "store_1_room_type_3"]
                }
                """;

        PriceLabsWebhookListingIdsParser.ListingIdsPayload payload =
                PriceLabsWebhookListingIdsParser.parse(objectMapper, json);

        assertEquals(List.of("store_1_room_type_2", "store_1_room_type_3"), payload.listingIds());
        assertFalse(payload.hasPriceData());
    }

    @Test
    void parsePriceDataPayloadDetectsCalendar() {
        String json = """
                {
                  "listing_id": "store_1_room_type_2",
                  "data": [
                    {
                      "date": "2026-01-10",
                      "price": 100
                    }
                  ]
                }
                """;

        PriceLabsWebhookListingIdsParser.ListingIdsPayload payload =
                PriceLabsWebhookListingIdsParser.parse(objectMapper, json);

        assertTrue(payload.hasPriceData());
        assertEquals(List.of("store_1_room_type_2"), payload.listingIds());
    }
}
