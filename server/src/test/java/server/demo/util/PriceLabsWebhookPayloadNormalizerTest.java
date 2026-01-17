package server.demo.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.dto.PriceLabsWebhookRequest;

import static org.junit.jupiter.api.Assertions.*;

class PriceLabsWebhookPayloadNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalizeCompactSyncPayload_shouldWork() {
        String raw = """
                {
                  "listing_id": "store_11_room_type_2",
                  "data": [
                    { "date": "2026-01-15", "price": 12384 },
                    { "date": "2026-01-16", "price": 14734, "min_stay": 2 }
                  ]
                }
                """;

        PriceLabsWebhookPayloadNormalizer.NormalizedPayload normalized =
                PriceLabsWebhookPayloadNormalizer.normalizeSyncPayload(objectMapper, raw);

        assertEquals("compact", normalized.format());

        PriceLabsWebhookRequest req = normalized.request();
        assertNotNull(req);
        assertEquals("price_update", req.getType());
        assertNotNull(req.getData());
        assertEquals(1, req.getData().size());

        PriceLabsWebhookRequest.ListingData listing = req.getData().get(0);
        assertEquals("store_11_room_type_2", listing.getListingId());
        assertNull(listing.getRatePlanId());
        assertNotNull(listing.getCalendar());
        assertEquals(2, listing.getCalendar().size());
        assertEquals("2026-01-15", listing.getCalendar().get(0).getDate());
        assertEquals(0, listing.getCalendar().get(0).getPrice().compareTo(new java.math.BigDecimal("12384")));
        assertEquals(2, listing.getCalendar().get(1).getMinStay());
    }

    @Test
    void normalizeStandardSyncPayload_shouldWork() {
        String raw = """
                {
                  "type": "price_update",
                  "data": [
                    {
                      "listing_id": "store_11_room_type_2",
                      "rate_plan_id": "plan_2",
                      "calendar": [
                        { "date": "2026-01-15", "price": 12384 }
                      ]
                    }
                  ]
                }
                """;

        PriceLabsWebhookPayloadNormalizer.NormalizedPayload normalized =
                PriceLabsWebhookPayloadNormalizer.normalizeSyncPayload(objectMapper, raw);

        assertEquals("standard", normalized.format());
        assertNotNull(normalized.request());
        assertEquals("price_update", normalized.request().getType());
        assertEquals("plan_2", normalized.request().getData().get(0).getRatePlanId());
    }
}

