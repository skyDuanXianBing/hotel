package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.PricePlan;
import server.demo.entity.Room;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuRateSyncServiceTest {

    @Test
    void syncRoomRates_shouldPushRatecontrolWithMergedSegments() throws Exception {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomTypePricePlanRepository rtppRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);

        SuRateSyncService service = new SuRateSyncService(
                roomRepository,
                rtppRepository,
                roomPriceRepository,
                suApiClient,
                suAccessTokenService
        );

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setDefaultPrice(new BigDecimal("150"));

        Room room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);

        PricePlan plan = new PricePlan();
        plan.setId(5L);
        plan.setName("BAR");

        RoomTypePricePlan rtpp = new RoomTypePricePlan();
        rtpp.setId(99L);
        rtpp.setRoomType(roomType);
        rtpp.setPricePlan(plan);

        when(roomRepository.findByStoreIdWithRoomType(7L)).thenReturn(List.of(room));
        when(rtppRepository.findByStoreIdWithRoomTypeAndPricePlan(7L)).thenReturn(List.of(rtpp));

        LocalDate today = LocalDate.now();
        RoomPrice d0 = new RoomPrice(roomType, plan, today, new BigDecimal("100"));
        RoomPrice d1 = new RoomPrice(roomType, plan, today.plusDays(1), new BigDecimal("200"));
        RoomPrice d2 = new RoomPrice(roomType, plan, today.plusDays(2), new BigDecimal("200"));

        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                eq(7L),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(d0, d1, d2));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode ok = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.postInvRateControl(anyString(), any())).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);

        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> fn = (Function<String, Object>) inv.getArgument(0);
            return fn.apply("token");
        });

        SuRateSyncService.SuRateSyncSummary summary = service.syncRoomRatesForNextDays(
                7L,
                "STORE7",
                3
        );

        assertTrue(summary.rateSynced());
        assertEquals(1, summary.roomCount());
        assertEquals(1, summary.ratePlanCount());
        assertEquals(1, summary.combinationsPushed());
        assertEquals(0, summary.combinationsSkippedNoPrice());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postInvRateControl(eq("token"), payloadCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals("STORE7", payload.get("hotelid"));
        assertNotNull(payload.get("ratecontrol"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ratecontrol = (List<Map<String, Object>>) payload.get("ratecontrol");
        assertEquals(1, ratecontrol.size());

        Map<String, Object> item = ratecontrol.get(0);
        assertEquals("1-101", item.get("roomid"));
        assertEquals("5", item.get("rateid"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dates = (List<Map<String, Object>>) item.get("date");
        assertEquals(2, dates.size(), "100 for day0, 200 merged for day1-2");
        assertEquals(today.toString(), dates.get(0).get("from"));
        assertEquals(today.toString(), dates.get(0).get("to"));
        assertEquals("100", readRuleValue(dates.get(0)));
        assertEquals(today.plusDays(1).toString(), dates.get(1).get("from"));
        assertEquals(today.plusDays(2).toString(), dates.get(1).get("to"));
        assertEquals("200", readRuleValue(dates.get(1)));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> otaRule = (List<Map<String, Object>>) dates.get(0).get("OTARule");
        @SuppressWarnings("unchecked")
        List<Integer> otaCodes = (List<Integer>) otaRule.get(0).get("OTACode");
        assertEquals(Set.of(19, 244), Set.copyOf(otaCodes));
    }

    @Test
    void syncRoomRates_shouldSkipWhenNoPriceForAllDays() throws Exception {
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        RoomTypePricePlanRepository rtppRepository = Mockito.mock(RoomTypePricePlanRepository.class);
        RoomPriceRepository roomPriceRepository = Mockito.mock(RoomPriceRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);

        SuRateSyncService service = new SuRateSyncService(
                roomRepository,
                rtppRepository,
                roomPriceRepository,
                suApiClient,
                suAccessTokenService
        );

        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setDefaultPrice(null);

        Room room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);

        PricePlan plan = new PricePlan();
        plan.setId(5L);
        plan.setName("BAR");

        RoomTypePricePlan rtpp = new RoomTypePricePlan();
        rtpp.setId(99L);
        rtpp.setRoomType(roomType);
        rtpp.setPricePlan(plan);

        when(roomRepository.findByStoreIdWithRoomType(7L)).thenReturn(List.of(room));
        when(rtppRepository.findByStoreIdWithRoomTypeAndPricePlan(7L)).thenReturn(List.of(rtpp));
        when(roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                eq(7L),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of());

        SuRateSyncService.SuRateSyncSummary summary = service.syncRoomRatesForNextDays(
                7L,
                "STORE7",
                3
        );

        assertTrue(summary.rateSynced());
        assertEquals(1, summary.roomCount());
        assertEquals(1, summary.ratePlanCount());
        assertEquals(0, summary.combinationsPushed());
        assertEquals(1, summary.combinationsSkippedNoPrice());
        assertEquals(1, summary.missingRates().size());

        verify(suApiClient, never()).postInvRateControl(anyString(), any());
        verify(suAccessTokenService, never()).executeWithTokenRetry(any(), anyString());
    }

    private static String readRuleValue(Map<String, Object> dateSegment) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> otaRule = (List<Map<String, Object>>) dateSegment.get("OTARule");
        @SuppressWarnings("unchecked")
        Map<String, Object> rule = (Map<String, Object>) otaRule.get(0).get("rule");
        return String.valueOf(rule.get("value"));
    }
}
