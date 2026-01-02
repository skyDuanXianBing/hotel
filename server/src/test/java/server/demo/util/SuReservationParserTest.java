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
}

