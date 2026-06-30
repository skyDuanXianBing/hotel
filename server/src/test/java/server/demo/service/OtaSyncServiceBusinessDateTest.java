package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.entity.Channel;
import server.demo.entity.ChannelPrice;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtaSyncServiceBusinessDateTest {

    @Mock
    private SuApiClient suApiClient;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelPriceRepository channelPriceRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ChannelPriceFallbackService channelPriceFallbackService;

    @Mock
    private SuAccessTokenService suAccessTokenService;

    @Test
    void syncStorePricesToSu_shouldUseStoreBusinessDate_whenStartDateIsNull() {
        Long storeId = 7L;
        LocalDate tokyoDate = LocalDate.of(2026, 4, 8);
        OtaSyncService service = createService("2026-04-07T15:30:00Z");

        mockStore(storeId, "Asia/Tokyo");
        mockNoOtaChannels(storeId);

        OtaSyncService.OtaSyncResult result = service.syncStorePricesToSu(storeId, null, 2);

        assertEquals(tokyoDate, result.startDate());
        assertEquals(tokyoDate.plusDays(1), result.endDate());
        verify(channelPriceFallbackService).generate(
                storeId,
                tokyoDate,
                2,
                List.of("AIRBNB", "BOOKING"),
                null
        );
    }

    @Test
    void syncStorePricesToSu_shouldKeepExplicitStartDate() {
        Long storeId = 7L;
        LocalDate explicitStartDate = LocalDate.of(2026, 3, 1);
        OtaSyncService service = createService("2026-04-07T15:30:00Z");

        mockStore(storeId, "Asia/Tokyo");
        mockNoOtaChannels(storeId);

        OtaSyncService.OtaSyncResult result = service.syncStorePricesToSu(storeId, explicitStartDate, 2);

        assertEquals(explicitStartDate, result.startDate());
        assertEquals(explicitStartDate.plusDays(1), result.endDate());
        verify(channelPriceFallbackService).generate(
                storeId,
                explicitStartDate,
                2,
                List.of("AIRBNB", "BOOKING"),
                null
        );
    }

    @Test
    void syncStorePricesToSu_shouldPushBasePriceToAvoidDoubleApplyingMappingMultiplier() throws Exception {
        Long storeId = 7L;
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        OtaSyncService service = createService("2026-04-07T15:30:00Z");

        mockStore(storeId, "Asia/Tokyo");

        Channel channel = new Channel();
        channel.setId(5L);
        channel.setStoreId(storeId);
        channel.setCode("AIRBNB");

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(20L);

        ChannelPrice channelPrice = new ChannelPrice();
        channelPrice.setId(88L);
        channelPrice.setStoreId(storeId);
        channelPrice.setChannel(channel);
        channelPrice.setRoomType(roomType);
        channelPrice.setPricePlan(pricePlan);
        channelPrice.setPriceDate(startDate);
        channelPrice.setBasePrice(new BigDecimal("100"));
        channelPrice.setChannelPrice(new BigDecimal("110"));

        JsonNode ok = new ObjectMapper().readTree("{\"Status\":\"Success\"}");
        when(channelRepository.findByStoreIdAndCode(storeId, "AIRBNB")).thenReturn(Optional.of(channel));
        when(channelRepository.findByStoreIdAndCode(storeId, "BOOKING")).thenReturn(Optional.empty());
        when(channelPriceRepository.findUnsyncedByStoreIdAndChannelIdAndDateRange(
                storeId,
                5L,
                startDate,
                startDate
        )).thenReturn(List.of(channelPrice));
        when(suApiClient.postInvRateControl(anyString(), any())).thenReturn(ok);
        when(channelPriceRepository.markAsSyncedToOta(List.of(88L))).thenReturn(1);
        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> action = (Function<String, Object>) invocation.getArgument(0);
            return action.apply("token");
        });

        service.syncStorePricesToSu(storeId, startDate, 1);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postInvRateControl(eq("token"), payloadCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ratecontrol = (List<Map<String, Object>>) payload.get("ratecontrol");
        assertNotNull(ratecontrol);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dates = (List<Map<String, Object>>) ratecontrol.get(0).get("date");
        assertEquals("100", readRuleValue(dates.get(0)));
    }

    @Test
    void syncStorePricesToSu_shouldReverseHistoricalAdjustedPriceWhenBasePriceIsMissing() throws Exception {
        Long storeId = 7L;
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        OtaSyncService service = createService("2026-04-07T15:30:00Z");

        mockStore(storeId, "Asia/Tokyo");

        Channel channel = new Channel();
        channel.setId(5L);
        channel.setStoreId(storeId);
        channel.setCode("AIRBNB");
        channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        channel.setPriceAdjustmentValue(new BigDecimal("10"));

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(20L);

        ChannelPrice channelPrice = new ChannelPrice();
        channelPrice.setId(89L);
        channelPrice.setStoreId(storeId);
        channelPrice.setChannel(channel);
        channelPrice.setRoomType(roomType);
        channelPrice.setPricePlan(pricePlan);
        channelPrice.setPriceDate(startDate);
        channelPrice.setBasePrice(null);
        channelPrice.setChannelPrice(new BigDecimal("110"));

        JsonNode ok = new ObjectMapper().readTree("{\"Status\":\"Success\"}");
        when(channelRepository.findByStoreIdAndCode(storeId, "AIRBNB")).thenReturn(Optional.of(channel));
        when(channelRepository.findByStoreIdAndCode(storeId, "BOOKING")).thenReturn(Optional.empty());
        when(channelPriceRepository.findUnsyncedByStoreIdAndChannelIdAndDateRange(
                storeId,
                5L,
                startDate,
                startDate
        )).thenReturn(List.of(channelPrice));
        when(suApiClient.postInvRateControl(anyString(), any())).thenReturn(ok);
        when(channelPriceRepository.markAsSyncedToOta(List.of(89L))).thenReturn(1);
        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> action = (Function<String, Object>) invocation.getArgument(0);
            return action.apply("token");
        });

        service.syncStorePricesToSu(storeId, startDate, 1);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postInvRateControl(eq("token"), payloadCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ratecontrol = (List<Map<String, Object>>) payload.get("ratecontrol");
        assertNotNull(ratecontrol);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dates = (List<Map<String, Object>>) ratecontrol.get(0).get("date");
        assertEquals("100", readRuleValue(dates.get(0)));
    }

    @Test
    void syncStorePricesToSu_shouldKeepHistoricalFixedPriceWhenFixedReverseWouldBeNonPositive() throws Exception {
        Long storeId = 7L;
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        OtaSyncService service = createService("2026-04-07T15:30:00Z");

        mockStore(storeId, "Asia/Tokyo");

        Channel channel = new Channel();
        channel.setId(5L);
        channel.setStoreId(storeId);
        channel.setCode("AIRBNB");
        channel.setPriceAdjustmentType(PriceAdjustmentType.FIXED);
        channel.setPriceAdjustmentValue(new BigDecimal("25"));

        RoomType roomType = new RoomType();
        roomType.setId(10L);
        PricePlan pricePlan = new PricePlan();
        pricePlan.setId(20L);

        ChannelPrice channelPrice = new ChannelPrice();
        channelPrice.setId(90L);
        channelPrice.setStoreId(storeId);
        channelPrice.setChannel(channel);
        channelPrice.setRoomType(roomType);
        channelPrice.setPricePlan(pricePlan);
        channelPrice.setPriceDate(startDate);
        channelPrice.setBasePrice(null);
        channelPrice.setChannelPrice(new BigDecimal("20"));

        ChannelPrice zeroChannelPrice = new ChannelPrice();
        zeroChannelPrice.setId(91L);
        zeroChannelPrice.setStoreId(storeId);
        zeroChannelPrice.setChannel(channel);
        zeroChannelPrice.setRoomType(roomType);
        zeroChannelPrice.setPricePlan(pricePlan);
        zeroChannelPrice.setPriceDate(startDate.plusDays(1));
        zeroChannelPrice.setBasePrice(null);
        zeroChannelPrice.setChannelPrice(BigDecimal.ZERO);

        JsonNode ok = new ObjectMapper().readTree("{\"Status\":\"Success\"}");
        when(channelRepository.findByStoreIdAndCode(storeId, "AIRBNB")).thenReturn(Optional.of(channel));
        when(channelRepository.findByStoreIdAndCode(storeId, "BOOKING")).thenReturn(Optional.empty());
        when(channelPriceRepository.findUnsyncedByStoreIdAndChannelIdAndDateRange(
                storeId,
                5L,
                startDate,
                startDate.plusDays(1)
        )).thenReturn(List.of(channelPrice, zeroChannelPrice));
        when(suApiClient.postInvRateControl(anyString(), any())).thenReturn(ok);
        when(channelPriceRepository.markAsSyncedToOta(List.of(90L))).thenReturn(1);
        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> action = (Function<String, Object>) invocation.getArgument(0);
            return action.apply("token");
        });

        service.syncStorePricesToSu(storeId, startDate, 2);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postInvRateControl(eq("token"), payloadCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ratecontrol = (List<Map<String, Object>>) payload.get("ratecontrol");
        assertNotNull(ratecontrol);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dates = (List<Map<String, Object>>) ratecontrol.get(0).get("date");
        assertEquals(1, dates.size());
        assertEquals("20", readRuleValue(dates.get(0)));
    }

    private OtaSyncService createService(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new OtaSyncService(
                suApiClient,
                channelRepository,
                channelPriceRepository,
                storeRepository,
                channelPriceFallbackService,
                suAccessTokenService,
                clock
        );
    }

    private void mockStore(Long storeId, String timezone) {
        Store store = new Store();
        store.setId(storeId);
        store.setTimezone(timezone);
        store.setSuHotelId("STORE" + storeId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
    }

    private void mockNoOtaChannels(Long storeId) {
        when(channelRepository.findByStoreIdAndCode(storeId, "AIRBNB")).thenReturn(Optional.empty());
        when(channelRepository.findByStoreIdAndCode(storeId, "BOOKING")).thenReturn(Optional.empty());
    }

    private static String readRuleValue(Map<String, Object> dateSegment) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> otaRule = (List<Map<String, Object>>) dateSegment.get("OTARule");
        @SuppressWarnings("unchecked")
        Map<String, Object> rule = (Map<String, Object>) otaRule.get(0).get("rule");
        return String.valueOf(rule.get("value"));
    }
}
