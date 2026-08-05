package server.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三渠道（Expedia 9 / Trip.com 339 / Agoda 189）订单载荷解析聚焦测试。
 * <p>
 * 载荷形态与 channel-simulator/src/fixtures/reservations/{expedia,tripcom,agoda}-booking.json
 * 对齐（2026-08-05 核验：字段层级与 new-booking.json 完全一致，仅 affiliation/单号/客人差异），
 * 字段全集以真实 Su 载荷为准（官方文档无三渠道订单示例）。
 */
class SuReservationParserThreeChannelPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EXPEDIA_PAYLOAD = """
            {
              "reservations": [
                {
                  "reservation": {
                    "reservation_notif_id": "NOTIF_ID_6",
                    "id": "712345678901_HOTEL_ID",
                    "channel_booking_id": "712345678901",
                    "hotel_id": "HOTEL_ID",
                    "status": "new",
                    "booked_at": "2026-06-01",
                    "modified_at": "2026-06-01",
                    "currencycode": "JPY",
                    "paymenttype": "Hotel Collect",
                    "commissionamount": "2900.00",
                    "totalprice": "26336",
                    "affiliation": { "pos": "Expedia", "source": "expedia", "OTA_Code": "9" },
                    "customer": {
                      "first_name": "Emma",
                      "last_name": "Brown",
                      "telephone": "+1-415-555-0132",
                      "countrycode": "US",
                      "guest_lang": "en",
                      "remarks": "Expedia VIP guest"
                    },
                    "rooms": [
                      {
                        "id": "30",
                        "roomreservation_id": "177251687436100005",
                        "arrival_date": "2026-06-10",
                        "departure_date": "2026-06-12",
                        "roomstaystatus": "new",
                        "guest_name": "Emma Brown",
                        "numberofadults": "2",
                        "numberofchildren": "0",
                        "totalprice": "26336",
                        "specialrequest": "Non-smoking room",
                        "channel_room_id": "30",
                        "price": [
                          {
                            "date": "2026-06-10",
                            "rate_id": "BAR",
                            "mealplan_id": "15",
                            "mealplan": "Room only",
                            "tax": "1318",
                            "pricebeforetax": "11850",
                            "priceaftertax": "13168"
                          },
                          {
                            "date": "2026-06-11",
                            "rate_id": "BAR",
                            "mealplan_id": "15",
                            "mealplan": "Room only",
                            "tax": "1318",
                            "pricebeforetax": "11850",
                            "priceaftertax": "13168"
                          }
                        ]
                      }
                    ]
                  }
                }
              ]
            }
            """;

    private static final String TRIPCOM_PAYLOAD = """
            {
              "reservations": [
                {
                  "reservation": {
                    "reservation_notif_id": "NOTIF_ID_7",
                    "id": "TC-8899001122_HOTEL_ID",
                    "channel_booking_id": "TC-8899001122",
                    "hotel_id": "HOTEL_ID",
                    "status": "new",
                    "booked_at": "2026-06-01",
                    "modified_at": "2026-06-01",
                    "currencycode": "JPY",
                    "paymenttype": "Hotel Collect",
                    "commissionamount": "2900.00",
                    "totalprice": "26336",
                    "affiliation": { "pos": "Trip.com", "source": "trip.com", "OTA_Code": "339" },
                    "customer": {
                      "first_name": "Mei",
                      "last_name": "Chen",
                      "telephone": "+86-138-0000-0339",
                      "countrycode": "CN",
                      "guest_lang": "zh",
                      "remarks": "Trip.com guest"
                    },
                    "rooms": [
                      {
                        "id": "30",
                        "roomreservation_id": "177251687436100006",
                        "arrival_date": "2026-06-10",
                        "departure_date": "2026-06-12",
                        "roomstaystatus": "new",
                        "guest_name": "Mei Chen",
                        "numberofadults": "2",
                        "numberofchildren": "0",
                        "totalprice": "26336",
                        "specialrequest": "Non-smoking room",
                        "channel_room_id": "30",
                        "price": [
                          {
                            "date": "2026-06-10",
                            "rate_id": "BAR",
                            "mealplan_id": "15",
                            "mealplan": "Room only",
                            "tax": "1318",
                            "pricebeforetax": "11850",
                            "priceaftertax": "13168"
                          },
                          {
                            "date": "2026-06-11",
                            "rate_id": "BAR",
                            "mealplan_id": "15",
                            "mealplan": "Room only",
                            "tax": "1318",
                            "pricebeforetax": "11850",
                            "priceaftertax": "13168"
                          }
                        ]
                      }
                    ]
                  }
                }
              ]
            }
            """;

    private static final String AGODA_PAYLOAD = """
            {
              "reservations": [
                {
                  "reservation": {
                    "reservation_notif_id": "NOTIF_ID_8",
                    "id": "AG-5566778899_HOTEL_ID",
                    "channel_booking_id": "AG-5566778899",
                    "hotel_id": "HOTEL_ID",
                    "status": "new",
                    "booked_at": "2026-06-01",
                    "modified_at": "2026-06-01",
                    "currencycode": "JPY",
                    "paymenttype": "Hotel Collect",
                    "commissionamount": "2900.00",
                    "totalprice": "26336",
                    "affiliation": { "pos": "Agoda", "source": "agoda", "OTA_Code": "189" },
                    "customer": {
                      "first_name": "Ananya",
                      "last_name": "Phet",
                      "telephone": "+66-81-000-0189",
                      "countrycode": "TH",
                      "guest_lang": "en",
                      "remarks": "Agoda guest"
                    },
                    "rooms": [
                      {
                        "id": "30",
                        "roomreservation_id": "177251687436100007",
                        "arrival_date": "2026-06-10",
                        "departure_date": "2026-06-12",
                        "roomstaystatus": "new",
                        "guest_name": "Ananya Phet",
                        "numberofadults": "2",
                        "numberofchildren": "0",
                        "totalprice": "26336",
                        "specialrequest": "Non-smoking room",
                        "channel_room_id": "30",
                        "price": [
                          {
                            "date": "2026-06-10",
                            "rate_id": "BAR",
                            "mealplan_id": "15",
                            "mealplan": "Room only",
                            "tax": "1318",
                            "pricebeforetax": "11850",
                            "priceaftertax": "13168"
                          },
                          {
                            "date": "2026-06-11",
                            "rate_id": "BAR",
                            "mealplan_id": "15",
                            "mealplan": "Room only",
                            "tax": "1318",
                            "pricebeforetax": "11850",
                            "priceaftertax": "13168"
                          }
                        ]
                      }
                    ]
                  }
                }
              ]
            }
            """;

    @Test
    void expediaPayload_shouldParseAllOrderFieldsViaGenericPath() throws Exception {
        JsonNode reservation = firstReservation(EXPEDIA_PAYLOAD);
        JsonNode roomStay = SuReservationParser.extractRoomStays(reservation).get(0);

        assertEquals("NOTIF_ID_6", SuReservationParser.extractReservationNotifId(reservation));
        assertEquals("712345678901_HOTEL_ID", SuReservationParser.extractReservationId(reservation));
        assertEquals("712345678901", SuReservationParser.extractChannelBookingId(reservation));
        assertEquals("9", SuReservationParser.extractOtaCode(reservation));
        assertEquals("EXPEDIA", SuReservationParser.mapOtaChannelCode(SuReservationParser.extractOtaCode(reservation)));
        assertEquals("new", SuReservationParser.extractSuStatus(reservation));

        assertCommonOrderFields(reservation, roomStay);
        assertEquals("Emma Brown", SuReservationParser.extractGuestName(reservation, roomStay));
        assertEquals("+14155550132", SuReservationParser.extractGuestPhone(reservation, roomStay));
        assertEquals("US", SuReservationParser.extractGuestCountryCode(reservation));
        assertEquals("en", SuReservationParser.extractGuestLang(reservation));
        assertEquals("Expedia VIP guest", SuReservationParser.extractCustomerRemarks(reservation));
        assertEquals("177251687436100005", SuReservationParser.extractRoomReservationId(roomStay));
    }

    @Test
    void tripcomPayload_shouldParseAllOrderFieldsViaGenericPath() throws Exception {
        JsonNode reservation = firstReservation(TRIPCOM_PAYLOAD);
        JsonNode roomStay = SuReservationParser.extractRoomStays(reservation).get(0);

        assertEquals("NOTIF_ID_7", SuReservationParser.extractReservationNotifId(reservation));
        assertEquals("TC-8899001122_HOTEL_ID", SuReservationParser.extractReservationId(reservation));
        assertEquals("TC-8899001122", SuReservationParser.extractChannelBookingId(reservation));
        assertEquals("339", SuReservationParser.extractOtaCode(reservation));
        assertEquals("TRIP", SuReservationParser.mapOtaChannelCode(SuReservationParser.extractOtaCode(reservation)));

        assertCommonOrderFields(reservation, roomStay);
        assertEquals("Mei Chen", SuReservationParser.extractGuestName(reservation, roomStay));
        assertEquals("+8613800000339", SuReservationParser.extractGuestPhone(reservation, roomStay));
        assertEquals("CN", SuReservationParser.extractGuestCountryCode(reservation));
        assertEquals("zh", SuReservationParser.extractGuestLang(reservation));
        assertEquals("Trip.com guest", SuReservationParser.extractCustomerRemarks(reservation));
        assertEquals("177251687436100006", SuReservationParser.extractRoomReservationId(roomStay));
    }

    @Test
    void agodaPayload_shouldParseAllOrderFieldsViaGenericPath() throws Exception {
        JsonNode reservation = firstReservation(AGODA_PAYLOAD);
        JsonNode roomStay = SuReservationParser.extractRoomStays(reservation).get(0);

        assertEquals("NOTIF_ID_8", SuReservationParser.extractReservationNotifId(reservation));
        assertEquals("AG-5566778899_HOTEL_ID", SuReservationParser.extractReservationId(reservation));
        assertEquals("AG-5566778899", SuReservationParser.extractChannelBookingId(reservation));
        assertEquals("189", SuReservationParser.extractOtaCode(reservation));
        assertEquals("AGODA", SuReservationParser.mapOtaChannelCode(SuReservationParser.extractOtaCode(reservation)));

        assertCommonOrderFields(reservation, roomStay);
        assertEquals("Ananya Phet", SuReservationParser.extractGuestName(reservation, roomStay));
        assertEquals("+66810000189", SuReservationParser.extractGuestPhone(reservation, roomStay));
        assertEquals("TH", SuReservationParser.extractGuestCountryCode(reservation));
        assertEquals("en", SuReservationParser.extractGuestLang(reservation));
        assertEquals("Agoda guest", SuReservationParser.extractCustomerRemarks(reservation));
        assertEquals("177251687436100007", SuReservationParser.extractRoomReservationId(roomStay));
    }

    @Test
    void numericJsonNodes_shouldParseOtaCodeAndChannelBookingId() throws Exception {
        // 官方文档标注 affiliation.OTA_Code 为 number；channel_booking_id 同样兼容数字节点
        JsonNode reservation = objectMapper.readTree("""
                {
                  "reservation": {
                    "id": "712345678901_HOTEL_ID",
                    "channel_booking_id": 712345678901,
                    "totalprice": 26336,
                    "affiliation": { "OTA_Code": 339 }
                  }
                }
                """).get("reservation");

        assertEquals("339", SuReservationParser.extractOtaCode(reservation));
        assertEquals("TRIP", SuReservationParser.mapOtaChannelCode(SuReservationParser.extractOtaCode(reservation)));
        assertEquals("712345678901", SuReservationParser.extractChannelBookingId(reservation));
        assertEquals(new BigDecimal("26336"), SuReservationParser.extractTotalAmount(reservation, null));
    }

    private void assertCommonOrderFields(JsonNode reservation, JsonNode roomStay) {
        assertEquals(LocalDate.of(2026, 6, 10), SuReservationParser.extractArrivalDate(reservation, roomStay));
        assertEquals(LocalDate.of(2026, 6, 12), SuReservationParser.extractDepartureDate(reservation, roomStay));
        assertEquals(2, SuReservationParser.extractAdults(reservation, roomStay));
        assertEquals(0, SuReservationParser.extractChildren(reservation, roomStay));
        assertEquals(new BigDecimal("26336"), SuReservationParser.extractTotalAmount(reservation, roomStay));
        assertEquals("JPY", SuReservationParser.extractCurrencyCode(reservation));
        assertEquals("Hotel Collect", SuReservationParser.extractPaymentType(reservation));
        assertEquals(new BigDecimal("2900.00"), SuReservationParser.extractCommissionAmount(reservation));
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0), SuReservationParser.extractBookedAt(reservation));
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0), SuReservationParser.extractModifiedAt(reservation));
        assertEquals("new", SuReservationParser.extractRoomStayStatus(roomStay));
        assertEquals("Non-smoking room", SuReservationParser.extractRoomSpecialRequest(roomStay));
        assertEquals("30", SuReservationParser.extractRoomTypeId(roomStay));
        assertEquals("BAR", SuReservationParser.extractRatePlanId(reservation, roomStay));

        List<SuReservationParser.DailyRoomPrice> prices = SuReservationParser.extractDailyPrices(
                reservation,
                roomStay,
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 12)
        );
        assertEquals(2, prices.size());
        assertEquals(LocalDate.of(2026, 6, 10), prices.get(0).priceDate());
        assertEquals("BAR", prices.get(0).rateId());
        assertEquals(new BigDecimal("1318"), prices.get(0).taxAmount());
        assertEquals(new BigDecimal("11850"), prices.get(0).priceBeforeTax());
        assertEquals(new BigDecimal("13168"), prices.get(0).priceAfterTax());
        assertEquals(LocalDate.of(2026, 6, 11), prices.get(1).priceDate());
    }

    private JsonNode firstReservation(String payload) throws Exception {
        List<JsonNode> reservations = SuReservationParser.extractReservationNodes(objectMapper.readTree(payload));
        assertEquals(1, reservations.size());
        return reservations.get(0);
    }
}
