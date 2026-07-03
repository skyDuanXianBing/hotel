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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationDailyPriceSyncServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void syncDailyPrices_shouldUpdateExistingRowsInsertNewRowsAndDeleteOnlyCurrentReservationStaleRows()
            throws Exception {
        ReservationDailyPriceRepository repository = mock(ReservationDailyPriceRepository.class);
        ReservationDailyPriceSyncService service = new ReservationDailyPriceSyncService(repository);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation reservation = buildReservation();
        ReservationDailyPrice existingRow = buildDailyPrice(
                reservation,
                LocalDate.of(2026, 2, 1),
                "old-rate",
                new BigDecimal("80"),
                new BigDecimal("8000"),
                new BigDecimal("8080")
        );
        ReservationDailyPrice staleRow = buildDailyPrice(
                reservation,
                LocalDate.of(2026, 2, 3),
                "stale-rate",
                new BigDecimal("70"),
                new BigDecimal("7000"),
                new BigDecimal("7070")
        );
        Reservation otherReservation = buildReservation(88L, 26L, "SU-200", "RR-2");
        ReservationDailyPrice otherReservationSameDate = buildDailyPrice(
                otherReservation,
                LocalDate.of(2026, 2, 3),
                "other-rate",
                new BigDecimal("60"),
                new BigDecimal("6000"),
                new BigDecimal("6060")
        );
        when(repository.findByStoreIdAndReservationIdOrderByPriceDateAsc(26L, 77L))
                .thenReturn(List.of(existingRow, staleRow, otherReservationSameDate));

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
                          "mealplan_id": "meal-b",
                          "mealplan": "Breakfast",
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
        verify(repository).findByStoreIdAndReservationIdOrderByPriceDateAsc(26L, 77L);
        verify(repository, never()).deleteByStoreIdAndReservationId(26L, 77L);
        verify(repository).deleteAll(List.of(staleRow));

        ArgumentCaptor<List<ReservationDailyPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ReservationDailyPrice> rows = captor.getValue();
        assertEquals(2, rows.size());

        ReservationDailyPrice updatedRow = rows.get(0);
        assertSame(existingRow, updatedRow);
        assertEquals(26L, updatedRow.getStoreId());
        assertEquals(77L, updatedRow.getReservation().getId());
        assertEquals("HOTEL26", updatedRow.getSuHotelId());
        assertEquals("SU-100", updatedRow.getSuReservationId());
        assertEquals("RR-1", updatedRow.getRoomReservationId());
        assertEquals(LocalDate.of(2026, 2, 1), updatedRow.getPriceDate());
        assertEquals("JPY", updatedRow.getCurrencyCode());
        assertEquals("rate-a", updatedRow.getRateId());
        assertEquals("meal-a", updatedRow.getMealplanId());
        assertEquals("Room Only", updatedRow.getMealplan());
        assertEquals(new BigDecimal("100"), updatedRow.getTaxAmount());
        assertEquals(new BigDecimal("10000"), updatedRow.getPriceBeforeTax());
        assertEquals(new BigDecimal("10100"), updatedRow.getPriceAfterTax());

        ReservationDailyPrice insertedRow = rows.get(1);
        assertNotSame(existingRow, insertedRow);
        assertNotSame(staleRow, insertedRow);
        assertNotSame(otherReservationSameDate, insertedRow);
        assertEquals(26L, insertedRow.getStoreId());
        assertEquals(77L, insertedRow.getReservation().getId());
        assertEquals("HOTEL26", insertedRow.getSuHotelId());
        assertEquals("SU-100", insertedRow.getSuReservationId());
        assertEquals("RR-1", insertedRow.getRoomReservationId());
        assertEquals(LocalDate.of(2026, 2, 2), insertedRow.getPriceDate());
        assertEquals("JPY", insertedRow.getCurrencyCode());
        assertEquals("rate-b", insertedRow.getRateId());
        assertEquals("meal-b", insertedRow.getMealplanId());
        assertEquals("Breakfast", insertedRow.getMealplan());
        assertEquals(new BigDecimal("120"), insertedRow.getTaxAmount());
        assertEquals(new BigDecimal("12000"), insertedRow.getPriceBeforeTax());
        assertEquals(new BigDecimal("12120"), insertedRow.getPriceAfterTax());

        assertEquals("other-rate", otherReservationSameDate.getRateId());
        assertEquals(new BigDecimal("6000"), otherReservationSameDate.getPriceBeforeTax());
    }

    @Test
    void syncDailyPrices_shouldInsertParsedDailyRowsWhenNoExistingRows() throws Exception {
        ReservationDailyPriceRepository repository = mock(ReservationDailyPriceRepository.class);
        ReservationDailyPriceSyncService service = new ReservationDailyPriceSyncService(repository);
        when(repository.findByStoreIdAndReservationIdOrderByPriceDateAsc(26L, 77L)).thenReturn(List.of());
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

        assertEquals(1, saved.size());
        verify(repository).findByStoreIdAndReservationIdOrderByPriceDateAsc(26L, 77L);
        verify(repository, never()).deleteByStoreIdAndReservationId(26L, 77L);
        verify(repository, never()).deleteAll(anyList());

        ArgumentCaptor<List<ReservationDailyPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ReservationDailyPrice> rows = captor.getValue();
        assertEquals(1, rows.size());
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
        verify(repository, never()).findByStoreIdAndReservationIdOrderByPriceDateAsc(26L, 77L);
        verify(repository, never()).deleteByStoreIdAndReservationId(26L, 77L);
        verify(repository, never()).deleteAll(anyList());
        verify(repository, never()).saveAll(anyList());
    }

    private Reservation buildReservation() {
        return buildReservation(77L, 26L, "SU-100", "RR-1");
    }

    private Reservation buildReservation(
            Long reservationId,
            Long storeId,
            String suReservationId,
            String roomReservationId
    ) {
        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setStoreId(storeId);
        reservation.setSuReservationId(suReservationId);
        reservation.setRoomReservationId(roomReservationId);
        reservation.setCurrencyCode("JPY");
        reservation.setCheckInDate(LocalDate.of(2026, 2, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 2, 3));
        return reservation;
    }

    private ReservationDailyPrice buildDailyPrice(
            Reservation reservation,
            LocalDate priceDate,
            String rateId,
            BigDecimal taxAmount,
            BigDecimal priceBeforeTax,
            BigDecimal priceAfterTax
    ) {
        ReservationDailyPrice row = new ReservationDailyPrice();
        row.setStoreId(reservation.getStoreId());
        row.setReservation(reservation);
        row.setSuHotelId("HOTEL26");
        row.setSuReservationId(reservation.getSuReservationId());
        row.setRoomReservationId(reservation.getRoomReservationId());
        row.setPriceDate(priceDate);
        row.setCurrencyCode(reservation.getCurrencyCode());
        row.setRateId(rateId);
        row.setTaxAmount(taxAmount);
        row.setPriceBeforeTax(priceBeforeTax);
        row.setPriceAfterTax(priceAfterTax);
        return row;
    }
}
