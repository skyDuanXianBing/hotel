package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomStatus;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuAvailabilitySyncServiceTest {

    @Test
    void syncRoomAvailability_shouldPushInventorycontrolWithOneZeroCalendar() throws Exception {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);

        SuAvailabilitySyncService service = new SuAvailabilitySyncService(
                roomRepository,
                reservationRepository,
                suApiClient,
                suAccessTokenService
        );

        Room r1 = new Room();
        r1.setId(1L);
        r1.setRoomNumber("101");
        RoomType rt1 = new RoomType();
        rt1.setId(1L);
        rt1.setName("RT1");
        r1.setRoomType(rt1);
        r1.setStatus(RoomStatus.AVAILABLE);

        Room r2 = new Room();
        r2.setId(2L);
        r2.setRoomNumber("102");
        r2.setRoomType(rt1);
        r2.setStatus(RoomStatus.MAINTENANCE);

        when(roomRepository.findByStoreIdWithRoomType(10L)).thenReturn(List.of(r1, r2));

        LocalDate today = LocalDate.now();
        Reservation reservation = new Reservation();
        reservation.setRoom(r1);
        reservation.setStatus(ReservationStatus.REQUESTED);
        reservation.setCheckInDate(today.plusDays(1));
        reservation.setCheckOutDate(today.plusDays(2));

        when(reservationRepository.findByStoreIdAndRoomIdInAndDateRangeAndStatuses(
                eq(10L),
                anyList(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(Set.class)
        )).thenReturn(List.of(reservation));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode ok = objectMapper.readTree("{\"Status\":\"Success\"}");

        when(suApiClient.postInvRateControl(anyString(), any())).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);

        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> fn = (Function<String, Object>) inv.getArgument(0);
            return fn.apply("token");
        });

        SuAvailabilitySyncService.SuAvailabilitySyncSummary summary = service.syncRoomAvailabilityForNextDays(
                10L,
                "HOTEL1",
                3
        );

        assertTrue(summary.availabilitySynced());
        assertEquals(2, summary.roomCount());
        assertEquals(3, summary.days());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postInvRateControl(eq("token"), payloadCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals("HOTEL1", payload.get("hotelid"));
        assertNotNull(payload.get("inventorycontrol"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inventory = (List<Map<String, Object>>) payload.get("inventorycontrol");
        assertEquals(2, inventory.size());

        Map<String, Object> inv101 = inventory.stream()
                .filter(it -> "1-101".equals(it.get("roomid")))
                .findFirst()
                .orElseThrow();
        Map<String, Object> inv102 = inventory.stream()
                .filter(it -> "1-102".equals(it.get("roomid")))
                .findFirst()
                .orElseThrow();

        // 101: day0=1, day1=0(REQUESTED), day2=1
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dates101 = (List<Map<String, Object>>) inv101.get("date");
        assertEquals(3, dates101.size());
        assertEquals(today.toString(), dates101.get(0).get("from"));
        assertEquals(today.toString(), dates101.get(0).get("to"));
        assertEquals("1", readRuleValue(dates101.get(0)));
        assertEquals(today.plusDays(1).toString(), dates101.get(1).get("from"));
        assertEquals(today.plusDays(1).toString(), dates101.get(1).get("to"));
        assertEquals("0", readRuleValue(dates101.get(1)));
        assertEquals(today.plusDays(2).toString(), dates101.get(2).get("from"));
        assertEquals(today.plusDays(2).toString(), dates101.get(2).get("to"));
        assertEquals("1", readRuleValue(dates101.get(2)));

        // 102: all 0 in one segment
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dates102 = (List<Map<String, Object>>) inv102.get("date");
        assertEquals(1, dates102.size());
        assertEquals(today.toString(), dates102.get(0).get("from"));
        assertEquals(today.plusDays(2).toString(), dates102.get(0).get("to"));
        assertEquals("0", readRuleValue(dates102.get(0)));
    }

    private static String readRuleValue(Map<String, Object> dateSegment) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> otaRule = (List<Map<String, Object>>) dateSegment.get("OTARule");
        @SuppressWarnings("unchecked")
        Map<String, Object> rule = (Map<String, Object>) otaRule.get(0).get("rule");
        return String.valueOf(rule.get("value"));
    }
}
