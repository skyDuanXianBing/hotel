package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.PricePlan;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypePricePlanRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuAriSyncServiceTest {

    @Test
    void syncBaseAri_shouldPostRoomstosellAndRatesPayloads() throws Exception {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        RoomTypePricePlanRepository rtppRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);

        SuAriSyncService service = new SuAriSyncService(
                roomRepository,
                reservationRepository,
                rtppRepository,
                roomPriceRepository,
                suApiClient,
                suAccessTokenService
        );

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setDefaultPrice(new BigDecimal("200.00"));

        Room room = new Room();
        room.setId(11L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);

        PricePlan plan = new PricePlan();
        plan.setId(10L);
        plan.setName("hhh");

        RoomTypePricePlan rtpp = new RoomTypePricePlan();
        rtpp.setRoomType(roomType);
        rtpp.setPricePlan(plan);

        when(roomRepository.findByStoreIdWithRoomType(5L)).thenReturn(List.of(room));
        when(rtppRepository.findByStoreIdWithRoomTypeAndPricePlan(5L)).thenReturn(List.of(rtpp));

        LocalDate today = LocalDate.now();
        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(today.plusDays(1));
        reservation.setCheckOutDate(today.plusDays(2));

        when(reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                eq(5L),
                anyList(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(Set.class)
        )).thenReturn(List.of(reservation));

        RoomPrice d0 = new RoomPrice(roomType, plan, today, new BigDecimal("100.00"));
        RoomPrice d1 = new RoomPrice(roomType, plan, today.plusDays(1), new BigDecimal("100.00"));
        RoomPrice d2 = new RoomPrice(roomType, plan, today.plusDays(2), new BigDecimal("120.00"));

        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                eq(5L),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(d0, d1, d2));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode ok = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.postAvailability(anyString(), any())).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);

        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> fn = (Function<String, Object>) inv.getArgument(0);
            return fn.apply("token");
        });

        SuAriSyncService.SuAriSyncSummary summary = service.syncBaseAriForNextDays(5L, "STORE5", 3);
        assertTrue(summary.availabilityPushed());
        assertTrue(summary.ratesPushed());
        assertEquals(1, summary.roomCount());
        assertEquals(1, summary.ratePlanCount());
        assertEquals(2, summary.requestsPosted());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient, Mockito.times(2)).postAvailability(eq("token"), payloadCaptor.capture());
        List<Object> payloads = payloadCaptor.getAllValues();
        assertEquals(2, payloads.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> availabilityPayload = (Map<String, Object>) payloads.get(0);
        assertEquals("STORE5", availabilityPayload.get("hotelid"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> roomsNode1 = (List<Map<String, Object>>) availabilityPayload.get("room");
        assertEquals(1, roomsNode1.size());
        assertEquals("1-101", roomsNode1.get(0).get("roomid"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dates1 = (List<Map<String, Object>>) roomsNode1.get(0).get("date");
        assertEquals(3, dates1.size());
        assertEquals(today.toString(), dates1.get(0).get("from"));
        assertEquals(today.toString(), dates1.get(0).get("to"));
        assertEquals("1", dates1.get(0).get("roomstosell"));
        assertEquals(today.plusDays(1).toString(), dates1.get(1).get("from"));
        assertEquals(today.plusDays(1).toString(), dates1.get(1).get("to"));
        assertEquals("0", dates1.get(1).get("roomstosell"));

        @SuppressWarnings("unchecked")
        Map<String, Object> ratesPayload = (Map<String, Object>) payloads.get(1);
        assertEquals("STORE5", ratesPayload.get("hotelid"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> roomsNode2 = (List<Map<String, Object>>) ratesPayload.get("room");
        assertEquals(1, roomsNode2.size());
        assertEquals("1-101", roomsNode2.get(0).get("roomid"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dates2 = (List<Map<String, Object>>) roomsNode2.get(0).get("date");
        assertEquals(2, dates2.size(), "100 merged for day0-1, 120 for day2");
        assertEquals(today.toString(), dates2.get(0).get("from"));
        assertEquals(today.plusDays(1).toString(), dates2.get(0).get("to"));
        assertNull(dates2.get(0).get("roomstosell"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rate = (List<Map<String, Object>>) dates2.get(0).get("rate");
        assertEquals("10", rate.get(0).get("rateplanid"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> price = (List<Map<String, Object>>) dates2.get(0).get("price");
        assertEquals("2", price.get(0).get("NumberOfGuests"));
        assertEquals("100.00", price.get(0).get("value"));
    }
}
