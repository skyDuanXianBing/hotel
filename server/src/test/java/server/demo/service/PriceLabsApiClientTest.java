package server.demo.service;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PriceLabsApiClientTest {

    @Test
    void extractFailureMessages_empty_returnsEmpty() {
        assertTrue(PriceLabsApiClient.extractFailureMessages(null).isEmpty());
        assertTrue(PriceLabsApiClient.extractFailureMessages(Map.of()).isEmpty());
    }

    @Test
    void extractFailureMessages_failureStringList_returnsMessages() {
        Map<String, Object> response = Map.of("failure", List.of("bad request", "invalid key"));
        List<String> failures = PriceLabsApiClient.extractFailureMessages(response);
        assertEquals(2, failures.size());
        assertEquals("bad request", failures.get(0));
    }

    @Test
    void extractFailureMessages_errorsAsMap_formatsUsefulSummary() {
        Map<String, Object> response = Map.of(
            "errors",
            List.of(Map.of("listing_id", "rt_13", "rate_plan_id", "pl_8", "error", "not found"))
        );
        List<String> failures = PriceLabsApiClient.extractFailureMessages(response);
        assertEquals(1, failures.size());
        assertTrue(failures.get(0).contains("listing_id=rt_13"));
        assertTrue(failures.get(0).contains("rate_plan_id=pl_8"));
        assertTrue(failures.get(0).contains("error=not found"));
    }

    @Test
    void extractFailureMessages_successFalse_usesMessage() {
        Map<String, Object> response = Map.of("success", false, "message", "signature invalid");
        List<String> failures = PriceLabsApiClient.extractFailureMessages(response);
        assertEquals(List.of("signature invalid"), failures);
    }

    @Test
    void extractNewToken_supportsTopLevelAndNested() {
        assertEquals("abc", PriceLabsApiClient.extractNewToken(Map.of("new_token", "abc")));
        assertEquals("xyz", PriceLabsApiClient.extractNewToken(Map.of("integration_token", "xyz")));
        assertEquals(
            "nested",
            PriceLabsApiClient.extractNewToken(Map.of("data", Map.of("new_token", "nested")))
        );
    }

    @Test
    void priceLabsResponse_failedField_isTreatedAsFailure() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"success\":[\"ok\"],\"failed\":[{\"error\":\"x\"}],\"extra\":\"ignored\"}";
        PriceLabsApiClient.PriceLabsResponse res = mapper.readValue(json, PriceLabsApiClient.PriceLabsResponse.class);
        assertNotNull(res.getSuccess());
        assertEquals(1, res.getSuccess().size());
        assertNotNull(res.getFailure());
        assertEquals(1, res.getFailure().size());
    }

    @Test
    void buildRatePlansPayload_groupsByListingIdAndUsesDataItems() {
        PriceLabsApiClient.RatePlanData p1 = new PriceLabsApiClient.RatePlanData();
        p1.setListingId("store_2_room_type_13");
        p1.setRatePlanId("plan_8");
        p1.setName("BAR");
        p1.setIsDefault(true);

        PriceLabsApiClient.RatePlanData p2 = new PriceLabsApiClient.RatePlanData();
        p2.setListingId("store_2_room_type_13");
        p2.setRatePlanId("plan_9");
        p2.setName("NONREF");
        p2.setIsDefault(false);

        List<PriceLabsApiClient.RatePlansByListing> payload = PriceLabsApiClient.buildRatePlansPayload(List.of(p1, p2));
        assertEquals(1, payload.size());
        assertEquals("store_2_room_type_13", payload.get(0).getListingId());
        assertEquals(2, payload.get(0).getData().size());
        assertEquals("plan_8", payload.get(0).getData().get(0).getId());
        assertEquals("true", payload.get(0).getData().get(0).getDefaultFlag());
    }

    @Test
    void calendarData_currency_isSerialized() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        PriceLabsApiClient.CalendarData cal = new PriceLabsApiClient.CalendarData();
        cal.setListingId("store_2_room_type_13");
        cal.setRatePlanId("plan_8");
        cal.setCurrency("CNY");
        cal.setCalendar(List.of());
        String json = mapper.writeValueAsString(cal);
        assertTrue(json.contains("\"currency\":\"CNY\""));
        assertTrue(json.contains("\"data\":[]"));
        assertFalse(json.contains("\"calendar\""));
    }

    @Test
    void calendarEntry_doesNotSerializeLegacyFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        PriceLabsApiClient.CalendarEntry e = new PriceLabsApiClient.CalendarEntry();
        e.setDate("2025-12-16");
        e.setEndDate("2025-12-16");
        e.setAvailable(true);
        e.setMinStay(1);
        String json = mapper.writeValueAsString(e);
        assertFalse(json.contains("\"available\""));
        assertFalse(json.contains("\"minStay\""));
    }

    @Test
    void listingData_serializesOnlySwaggerListingFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        PriceLabsApiClient.Location loc = new PriceLabsApiClient.Location();
        loc.setCity("Tokyo");
        loc.setCountry("JPN");
        loc.setLatitude(35.6762);
        loc.setLongitude(139.6503);

        PriceLabsApiClient.ListingData listing = new PriceLabsApiClient.ListingData();
        listing.setListingId("store_1_room_type_1");
        listing.setUserToken("test@test.com");
        listing.setName("Test Listing");
        listing.setStatus("available");
        listing.setBedrooms(1);
        listing.setLocation(loc);

        String json = mapper.writeValueAsString(listing);
        assertTrue(json.contains("\"listing_id\":\"store_1_room_type_1\""));
        assertTrue(json.contains("\"user_token\":\"test@test.com\""));
        assertTrue(json.contains("\"name\":\"Test Listing\""));
        assertTrue(json.contains("\"status\":\"available\""));
        assertTrue(json.contains("\"number_of_bedrooms\":1"));
        assertTrue(json.contains("\"location\""));

        // ensure removed/unsupported fields are not present in /listings payload
        assertFalse(json.contains("\"timezone\""));
        assertFalse(json.contains("\"bathroom\""));
        assertFalse(json.contains("\"bathrooms\""));
        assertFalse(json.contains("\"number_of_bathrooms\""));
        assertFalse(json.contains("\"accommodates\""));
        assertFalse(json.contains("\"property_type\""));
        assertFalse(json.contains("\"currency\""));
        assertFalse(json.contains("\"base_price\""));
        assertFalse(json.contains("\"min_price\""));
        assertFalse(json.contains("\"max_price\""));
        assertFalse(json.contains("\"multi_unit\""));
        assertFalse(json.contains("\"multi_unit_count\""));
        assertFalse(json.contains("\"address\""));
        assertFalse(json.contains("\"state\""));
    }
}
