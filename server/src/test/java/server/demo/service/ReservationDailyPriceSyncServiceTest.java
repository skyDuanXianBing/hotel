package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import server.demo.entity.Reservation;
import server.demo.entity.ReservationDailyPrice;
import server.demo.repository.ReservationDailyPriceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationDailyPriceSyncServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void syncDailyPrices_shouldDeleteOldRowsAndInsertParsedDailyRows() throws Exception {
        ReservationDailyPriceRepository repository = mock(ReservationDailyPriceRepository.class);
        ReservationDailyPriceSyncService service = new ReservationDailyPriceSyncService(repository);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation reservation = buildReservation();
        JsonNode reservationNode = OBJECT_MAPPER.readTree("""
                {
                  "rooms": [
                    {
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
                          "rate_id": "rate-b",
                          "tax": "120",
                          "pricebeforetax": "12000",
                          "priceaftertax": "12120"
                        }
                      ]
                    }
                  ]
                }
                """);
        JsonNode roomStay = reservationNode.get("rooms").get(0);

        List<ReservationDailyPrice> saved = service.syncDailyPrices(
                26L,
                "HOTEL26",
                reservation,
                reservationNode,
                roomStay
        );

        assertEquals(2, saved.size());
        verify(repository).deleteByStoreIdAndReservationId(26L, 77L);

        ArgumentCaptor<List<ReservationDailyPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ReservationDailyPrice> rows = captor.getValue();
        assertEquals(2, rows.size());
        assertEquals(26L, rows.get(0).getStoreId());
        assertEquals(77L, rows.get(0).getReservation().getId());
        assertEquals("HOTEL26", rows.get(0).getSuHotelId());
        assertEquals("SU-100", rows.get(0).getSuReservationId());
        assertEquals("RR-1", rows.get(0).getRoomReservationId());
        assertEquals(LocalDate.of(2026, 2, 1), rows.get(0).getPriceDate());
        assertEquals("JPY", rows.get(0).getCurrencyCode());
        assertEquals("rate-a", rows.get(0).getRateId());
        assertEquals("meal-a", rows.get(0).getMealplanId());
        assertEquals(new BigDecimal("100"), rows.get(0).getTaxAmount());
        assertEquals(new BigDecimal("10000"), rows.get(0).getPriceBeforeTax());
        assertEquals(new BigDecimal("10100"), rows.get(0).getPriceAfterTax());
    }

    @Test
    void syncDailyPrices_shouldKeepExistingRowsWhenPayloadHasNoDailyPrice() throws Exception {
        ReservationDailyPriceRepository repository = mock(ReservationDailyPriceRepository.class);
        ReservationDailyPriceSyncService service = new ReservationDailyPriceSyncService(repository);

        Reservation reservation = buildReservation();
        JsonNode reservationNode = OBJECT_MAPPER.readTree("""
                {
                  "rooms": [
                    {
                      "totalprice": "22220"
                    }
                  ]
                }
                """);
        JsonNode roomStay = reservationNode.get("rooms").get(0);

        List<ReservationDailyPrice> saved = service.syncDailyPrices(
                26L,
                "HOTEL26",
                reservation,
                reservationNode,
                roomStay
        );

        assertEquals(0, saved.size());
        verify(repository, never()).deleteByStoreIdAndReservationId(26L, 77L);
        verify(repository, never()).saveAll(anyList());
    }

    private Reservation buildReservation() {
        Reservation reservation = new Reservation();
        reservation.setId(77L);
        reservation.setStoreId(26L);
        reservation.setSuReservationId("SU-100");
        reservation.setRoomReservationId("RR-1");
        reservation.setCurrencyCode("JPY");
        reservation.setCheckInDate(LocalDate.of(2026, 2, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 2, 3));
        return reservation;
    }
}
