package server.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        assertEquals(LocalDate.of(2026, 1, 30), SuReservationParser.extractBookedAt(reservation));
        assertEquals(LocalDate.of(2026, 1, 30), SuReservationParser.extractModifiedAt(reservation));
        assertEquals("JPY", SuReservationParser.extractCurrencyCode(reservation));
        assertEquals("Hotel Collect", SuReservationParser.extractPaymentType(reservation));
        assertEquals(new BigDecimal("0.19"), SuReservationParser.extractCommissionAmount(reservation));

        assertEquals("Approximate time of arrival: between 17:00 and 18:00", SuReservationParser.extractCustomerRemarks(reservation));
        assertEquals("No Smoking", SuReservationParser.extractRoomSpecialRequest(roomStay));
    }
}

