package server.demo.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceLabsWebhookRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeSnakeCasePayload() throws Exception {
        String json = """
                {
                  "type": "price_update",
                  "timestamp": "2026-01-14T00:00:00Z",
                  "data": [
                    {
                      "listing_id": "store_11_room_type_2",
                      "rate_plan_id": "plan_2",
                      "calendar": [
                        {
                          "date": "2026-01-16",
                          "price": 900,
                          "min_stay": 2,
                          "max_stay": 7,
                          "closed_to_arrival": false,
                          "closed_to_departure": true
                        }
                      ]
                    }
                  ]
                }
                """;

        PriceLabsWebhookRequest req = objectMapper.readValue(json, PriceLabsWebhookRequest.class);
        assertEquals("price_update", req.getType());
        assertNotNull(req.getData());
        assertEquals(1, req.getData().size());

        PriceLabsWebhookRequest.ListingData listing = req.getData().get(0);
        assertEquals("store_11_room_type_2", listing.getListingId());
        assertEquals("plan_2", listing.getRatePlanId());
        assertNotNull(listing.getCalendar());
        assertEquals(1, listing.getCalendar().size());

        PriceLabsWebhookRequest.CalendarData cal = listing.getCalendar().get(0);
        assertEquals("2026-01-16", cal.getDate());
        assertNotNull(cal.getPrice());
        assertEquals(0, cal.getPrice().compareTo(new java.math.BigDecimal("900")));
        assertEquals(2, cal.getMinStay());
        assertEquals(7, cal.getMaxStay());
        assertEquals(false, cal.getClosedToArrival());
        assertEquals(true, cal.getClosedToDeparture());
    }

    @Test
    void shouldDeserializeCamelCasePayload() throws Exception {
        String json = """
                {
                  "type": "price_update",
                  "data": [
                    {
                      "listingId": "store_11_room_type_2",
                      "ratePlanId": "plan_2",
                      "calendar": [
                        {
                          "date": "2026-01-17",
                          "price": 1000,
                          "minStay": 1
                        }
                      ]
                    }
                  ]
                }
                """;

        PriceLabsWebhookRequest req = objectMapper.readValue(json, PriceLabsWebhookRequest.class);
        assertNotNull(req.getData());
        assertEquals(1, req.getData().size());

        PriceLabsWebhookRequest.ListingData listing = req.getData().get(0);
        assertEquals("store_11_room_type_2", listing.getListingId());
        assertEquals("plan_2", listing.getRatePlanId());

        PriceLabsWebhookRequest.CalendarData cal = listing.getCalendar().get(0);
        assertEquals("2026-01-17", cal.getDate());
        assertEquals(1, cal.getMinStay());
        assertNull(cal.getMaxStay());
    }
}

