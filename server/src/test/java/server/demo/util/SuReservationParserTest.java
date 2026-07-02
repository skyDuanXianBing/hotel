package server.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuReservationParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapOtaChannelCode_supportsAirbnbAndBooking() {
        assertEquals("AIRBNB", SuReservationParser.mapOtaChannelCode("244"));
        assertEquals("BOOKING", SuReservationParser.mapOtaChannelCode("19"));
        assertNull(SuReservationParser.mapOtaChannelCode("999"));
        assertNull(SuReservationParser.mapOtaChannelCode(null));
    }

    @Test
    void extractReservationNodes_andRoomStays_basicHappyPath() throws Exception {
        String json = """
                {
                  "Status": "Success",
                  "reservations": [
                    {
                      "reservation": {
                        "reservation_notif_id": "notif_1",
                        "id": "res_1",
                        "channel_booking_id": "cb_1",
                        "status": "new",
                        "affiliation": { "OTA_Code": "244" },
                        "rooms": [
                          {
                            "roomreservation_id": "rr_1",
                            "arrival_date": "2025-12-20",
                            "departure_date": "2025-12-22",
                            "roomstaystatus": "cancelled",
                            "guest_name": "张三",
                            "numberofadults": "2",
                            "numberofchildren": "1",
                            "totalprice": "1234.56"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        JsonNode root = objectMapper.readTree(json);
        List<JsonNode> reservations = SuReservationParser.extractReservationNodes(root);
        assertEquals(1, reservations.size());

        JsonNode reservation = reservations.get(0);
        assertEquals("notif_1", SuReservationParser.extractReservationNotifId(reservation));
        assertEquals("res_1", SuReservationParser.extractReservationId(reservation));
        assertEquals("cb_1", SuReservationParser.extractChannelBookingId(reservation));
        assertEquals("244", SuReservationParser.extractOtaCode(reservation));
        assertEquals("AIRBNB", SuReservationParser.mapOtaChannelCode(SuReservationParser.extractOtaCode(reservation)));

        List<JsonNode> roomStays = SuReservationParser.extractRoomStays(reservation);
        assertEquals(1, roomStays.size());

        JsonNode roomStay = roomStays.get(0);
        assertEquals("rr_1", SuReservationParser.extractRoomReservationId(roomStay));
        assertEquals("cancelled", SuReservationParser.extractRoomStayStatus(roomStay));
        assertEquals(LocalDate.of(2025, 12, 20), SuReservationParser.extractArrivalDate(reservation, roomStay));
        assertEquals(LocalDate.of(2025, 12, 22), SuReservationParser.extractDepartureDate(reservation, roomStay));
        assertEquals("张三", SuReservationParser.extractGuestName(reservation, roomStay));
        assertEquals(2, SuReservationParser.extractAdults(reservation, roomStay));
        assertEquals(1, SuReservationParser.extractChildren(reservation, roomStay));
        assertEquals(new BigDecimal("1234.56"), SuReservationParser.extractTotalAmount(reservation, roomStay));
    }

    @Test
    void buildOrderNumber_isStableAndWithinLimit() {
        String orderNumber = SuReservationParser.buildOrderNumber(5L, "res_very_long_id_abcdefghijklmnopqrstuvwxyz_1234567890", "rr_very_long_id_abcdefghijklmnopqrstuvwxyz_1234567890");
        assertNotNull(orderNumber);
        assertTrue(orderNumber.length() <= 50, "orderNumber length should be <= 50");
    }

    @Test
    void extractBookedAtAndChannelFields_andSpecialRequests() throws Exception {
        String json = """
                {
                  "reservations": [
                    {
                      "reservation": {
                        "reservation_notif_id": "176973668024670264210",
                        "id": "468944348_S15KQEKHXP",
                        "channel_booking_id": "468944348",
                        "hotel_id": "S15KQEKHXP",
                        "booked_at": "2026-01-30",
                        "modified_at": "2026-01-30",
                        "currencycode": "JPY",
                        "paymenttype": "Hotel Collect",
                        "commissionamount": "0.19",
                        "customer": { "remarks": "Approximate time of arrival: between 17:00 and 18:00" },
                        "rooms": [
                          {
                            "roomreservation_id": "1769736696564",
                            "arrival_date": "2026-01-31",
                            "departure_date": "2026-02-01",
                            "specialrequest": "No Smoking"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        JsonNode root = objectMapper.readTree(json);
        JsonNode reservation = SuReservationParser.extractReservationNodes(root).get(0);
        JsonNode roomStay = SuReservationParser.extractRoomStays(reservation).get(0);

        assertEquals(LocalDateTime.of(2026, 1, 30, 0, 0), SuReservationParser.extractBookedAt(reservation));
        assertEquals(LocalDateTime.of(2026, 1, 30, 0, 0), SuReservationParser.extractModifiedAt(reservation));
        assertEquals("JPY", SuReservationParser.extractCurrencyCode(reservation));
        assertEquals("Hotel Collect", SuReservationParser.extractPaymentType(reservation));
        assertEquals(new BigDecimal("0.19"), SuReservationParser.extractCommissionAmount(reservation));

        assertEquals("Approximate time of arrival: between 17:00 and 18:00", SuReservationParser.extractCustomerRemarks(reservation));
        assertEquals("No Smoking", SuReservationParser.extractRoomSpecialRequest(roomStay));
    }

    @Test
    void extractDailyPrices_shouldReadRoomPriceArrayWithinStayDates() throws Exception {
        String json = """
                {
                  "reservation": {
                    "currencycode": "JPY",
                    "rooms": [
                      {
                        "arrival_date": "2026-02-01",
                        "departure_date": "2026-02-03",
                        "price": [
                          {
                            "date": "2026-02-01",
                            "rate_id": "rate-a",
                            "mealplan_id": "meal-a",
                            "mealplan": "Room Only",
                            "tax": "100",
                            "pricebeforetax": "10000",
                            "priceaftertax": "10100"
                          },
                          {
                            "date": "2026-02-02",
                            "rateId": "rate-b",
                            "mealPlanId": "meal-b",
                            "mealPlan": "Breakfast",
                            "taxAmount": "120",
                            "priceBeforeTax": "12000",
                            "priceAfterTax": "12120"
                          },
                          {
                            "date": "2026-02-03",
                            "priceaftertax": "13000"
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        JsonNode roomStay = SuReservationParser.extractRoomStays(reservation).get(0);

        List<SuReservationParser.DailyRoomPrice> prices = SuReservationParser.extractDailyPrices(
                reservation,
                roomStay,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3)
        );

        assertEquals(2, prices.size());
        assertEquals(LocalDate.of(2026, 2, 1), prices.get(0).priceDate());
        assertEquals("rate-a", prices.get(0).rateId());
        assertEquals("meal-a", prices.get(0).mealplanId());
        assertEquals("Room Only", prices.get(0).mealplan());
        assertEquals(new BigDecimal("100"), prices.get(0).taxAmount());
        assertEquals(new BigDecimal("10000"), prices.get(0).priceBeforeTax());
        assertEquals(new BigDecimal("10100"), prices.get(0).priceAfterTax());
        assertEquals(LocalDate.of(2026, 2, 2), prices.get(1).priceDate());
        assertEquals("rate-b", prices.get(1).rateId());
        assertEquals("meal-b", prices.get(1).mealplanId());
        assertEquals("Breakfast", prices.get(1).mealplan());
        assertEquals(new BigDecimal("120"), prices.get(1).taxAmount());
        assertEquals(new BigDecimal("12000"), prices.get(1).priceBeforeTax());
        assertEquals(new BigDecimal("12120"), prices.get(1).priceAfterTax());
    }

    @Test
    void extractDailyPrices_shouldIgnoreInvalidAndOutOfStayPriceRows() throws Exception {
        String json = """
                {
                  "reservation": {
                    "currencycode": "JPY",
                    "rooms": [
                      {
                        "arrival_date": "2026-02-01",
                        "departure_date": "2026-02-04",
                        "price": [
                          { "date": "2026-01-31", "priceaftertax": "90" },
                          { "date": "invalid-date", "priceaftertax": "100" },
                          { "date": "2026-02-02", "priceaftertax": "120" },
                          { "date": "2026-02-04", "priceaftertax": "130" }
                        ]
                      }
                    ]
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        JsonNode roomStay = SuReservationParser.extractRoomStays(reservation).get(0);

        List<SuReservationParser.DailyRoomPrice> prices = SuReservationParser.extractDailyPrices(
                reservation,
                roomStay,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 4)
        );

        assertEquals(1, prices.size());
        assertEquals(LocalDate.of(2026, 2, 2), prices.get(0).priceDate());
        assertEquals(new BigDecimal("120"), prices.get(0).priceAfterTax());
    }

    @Test
    void extractBookedAtAndModifiedAt_preserveSuDateTimePrecision() throws Exception {
        String json = """
                {
                  "reservation": {
                    "booked_at": "2026-01-30 14:35:11",
                    "modified_at": "2026-01-31 08:09:10"
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");

        assertEquals(LocalDateTime.of(2026, 1, 30, 14, 35, 11), SuReservationParser.extractBookedAt(reservation));
        assertEquals(LocalDateTime.of(2026, 1, 31, 8, 9, 10), SuReservationParser.extractModifiedAt(reservation));
    }

    @Test
    void extractBookedAtAndModifiedAt_keepDateOnlyAndInvalidCompatibility() throws Exception {
        String json = """
                {
                  "reservation": {
                    "booked_at": "2026-01-30",
                    "modified_at": "0000-00-00"
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");

        assertEquals(LocalDateTime.of(2026, 1, 30, 0, 0), SuReservationParser.extractBookedAt(reservation));
        assertNull(SuReservationParser.extractModifiedAt(reservation));
    }

    @Test
    void extractMessagingListingId_prefersExplicitListingFieldOnRoomStay() throws Exception {
        String json = """
                {
                  "reservation": {
                    "listingid": "52",
                    "booking_details": { "property_id": "16016360" },
                    "rooms": [
                      { "listing_id": "16016361" }
                    ]
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        JsonNode roomStay = SuReservationParser.extractRoomStays(reservation).get(0);

        assertEquals("16016361", SuReservationParser.extractMessagingListingId(reservation, roomStay));
    }

    @Test
    void extractMessagingListingId_prefersRoomChannelRoomId() throws Exception {
        String json = """
                {
                  "reservation": {
                    "channel_room_id": "1157718387975828173",
                    "rooms": [
                      {
                        "channel_room_id": "1157718387975828174",
                        "listing_id": "16016361"
                      }
                    ]
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        JsonNode roomStay = SuReservationParser.extractRoomStays(reservation).get(0);

        SuReservationParser.MessagingListingResolution resolved =
                SuReservationParser.extractMessagingListingIdWithSource(reservation, roomStay);
        assertNotNull(resolved);
        assertEquals("1157718387975828174", resolved.listingId());
        assertEquals("room_channel_room_id", resolved.source());
    }

    @Test
    void extractMessagingListingId_usesReservationChannelRoomIdWhenRoomMissing() throws Exception {
        String json = """
                {
                  "reservation": {
                    "channel_room_id": "1157718387975828174",
                    "booking_details": {
                      "property_id": "16016360"
                    }
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");

        SuReservationParser.MessagingListingResolution resolved =
                SuReservationParser.extractMessagingListingIdWithSource(reservation, null);
        assertNotNull(resolved);
        assertEquals("1157718387975828174", resolved.listingId());
        assertEquals("reservation_channel_room_id", resolved.source());
    }

    @Test
    void extractMessagingListingId_usesBookingPayloadHintWhenExplicitMissing() throws Exception {
        String json = """
                {
                  "reservation": {
                    "booking_details": { "property_id": "16016360" },
                    "rooms": [
                      { "id": "50" }
                    ]
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        assertEquals("16016360", SuReservationParser.extractMessagingListingId(reservation, null));
    }

    @Test
    void normalizeMessagingListingId_rejectsShortNumericRoomId() {
        assertNull(SuReservationParser.normalizeMessagingListingId("52"));
        assertNull(SuReservationParser.normalizeMessagingListingId("  51 "));
        assertEquals("16016360", SuReservationParser.normalizeMessagingListingId("16016360"));
    }

    @Test
    void extractMessagingListingId_usesRemarksHotelIdWhenStructuredFieldsMissing() throws Exception {
        String json = """
                {
                  "reservation": {
                    "channel_booking_id": "5842688289",
                    "customer": {
                      "remarks": "BOOKING NOTE | url: https://admin.booking.com/hotel/hoteladmin/extranet_ng/manage/booking.html?res_id=5842688289&hotel_id=16016360&lang=en |"
                    },
                    "rooms": [
                      { "id": "50" }
                    ]
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        assertEquals("16016360", SuReservationParser.extractMessagingListingId(reservation, null));
    }

    @Test
    void extractMessagingListingId_prefersRemarksHotelIdWithMatchingResId() throws Exception {
        String json = """
                {
                  "reservation": {
                    "channel_booking_id": "5236589933",
                    "customer": {
                      "remarks": "url:https://admin.booking.com/x?res_id=111111&hotel_id=99999999 | url:https://admin.booking.com/y?res_id=5236589933&hotel_id=16016360&lang=en |"
                    }
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        assertEquals("16016360", SuReservationParser.extractMessagingListingId(reservation, null));
    }

    @Test
    void extractMessagingListingIdWithSource_marksRemarksHotelIdSource() throws Exception {
        String json = """
                {
                  "reservation": {
                    "channel_booking_id": "5236589933",
                    "customer": {
                      "remarks_en": "https://admin.booking.com/y?res_id=5236589933&hotel_id=16016360"
                    }
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        SuReservationParser.MessagingListingResolution resolved =
                SuReservationParser.extractMessagingListingIdWithSource(reservation, null);
        assertNotNull(resolved);
        assertEquals("16016360", resolved.listingId());
        assertEquals("remarks_hotel_id", resolved.source());
    }

    @Test
    void extractGuestPhone_keepsMultiPhonesWithSeparatorAndDeduplicates() throws Exception {
        String json = """
                {
                  "reservation": {
                    "customer": {
                      "telephone": " 17032204754,17032204797，17032204754 ; +8613800138000 "
                    }
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");

        String parsed = SuReservationParser.extractGuestPhone(reservation, null);

        assertEquals("17032204754,17032204797,+8613800138000", parsed);
    }

    @Test
    void extractGuestPhone_truncatesToColumnLimit() throws Exception {
        StringBuilder phones = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            if (i > 0) {
                phones.append(',');
            }
            phones.append("1703220").append(String.format("%04d", i));
        }

        String json = """
                {
                  "reservation": {
                    "customer": {
                      "telephone": "%s"
                    }
                  }
                }
                """.formatted(phones);

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        String parsed = SuReservationParser.extractGuestPhone(reservation, null);

        assertNotNull(parsed);
        assertTrue(parsed.length() <= 255);
        assertTrue(parsed.startsWith("17032200000"));
    }

    @Test
    void normalizeBookingReservationId_supportsWebhookAndOrderNumberFormats() {
        assertEquals("5003249282", SuReservationParser.normalizeBookingReservationId("5003249282"));
        assertEquals("5003249282", SuReservationParser.normalizeBookingReservationId("5003249282_W39FVCQYSN"));
        assertEquals("5003249282", SuReservationParser.normalizeBookingReservationId("SU26-5003249282_W39FVCQYSN-1774939615039"));
        assertNull(SuReservationParser.normalizeBookingReservationId("SU26-HMFWJRDAX5_W39FVCQYSN-1775037944902"));
    }

    @Test
    void extractBookingReservationId_prefersRemarksResIdWhenChannelMissing() throws Exception {
        String json = """
                {
                  "reservation": {
                    "id": "5003249282_W39FVCQYSN",
                    "customer": {
                      "remarks": "BOOKING NOTE | url: https://admin.booking.com/manage/booking.html?res_id=5842688289&hotel_id=16016360&lang=en |"
                    }
                  }
                }
                """;

        JsonNode reservation = objectMapper.readTree(json).get("reservation");
        assertEquals("5842688289", SuReservationParser.extractBookingReservationId(reservation));
    }

    @Test
    void extractBookingReservationId_usesOrderNumberFallbackWhenPayloadMissing() throws Exception {
        JsonNode reservation = objectMapper.readTree("{\"reservation\":{}}")
                .get("reservation");

        assertEquals(
                "5003249282",
                SuReservationParser.extractBookingReservationId(
                        reservation,
                        "SU26-5003249282_W39FVCQYSN-1774939615039"
                )
        );
    }
}
