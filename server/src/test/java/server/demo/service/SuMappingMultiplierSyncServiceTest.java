package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.dto.ChannelMappingMultiplierSyncSummaryDTO;
import server.demo.entity.Channel;
import server.demo.entity.OtaIntegration;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.OtaIntegrationRepository;
import server.demo.repository.StoreRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuMappingMultiplierSyncServiceTest {

    private static final Long STORE_ID = 7L;
    private static final String HOTEL_ID = "HOTEL7";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OtaIntegrationRepository otaIntegrationRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private SuApiClient suApiClient;

    @Mock
    private SuAccessTokenService suAccessTokenService;

    @Test
    void syncForChannel_shouldPostBookingRatePlanMapWithPercentageMultiplier() throws Exception {
        SuMappingMultiplierSyncService service = createService();
        Channel channel = channel("BOOKING", PriceAdjustmentType.PERCENTAGE, new BigDecimal("10"));
        OtaIntegration integration = integration("BOOKING");
        JsonNode mappings = objectMapper.readTree("""
                {
                  "Status": "Success",
                  "19": [
                    {
                      "Status": "Active",
                      "ChannelHotelID": "BOOKING-HOTEL",
                      "RoomIDs": ["101"],
                      "Rateplans": [
                        {
                          "PMSRoomID": "101",
                          "PMSRateID": "BAR",
                          "ChannelRoomID": "B-ROOM",
                          "ChannelRateID": "B-RATE",
                          "MappingStatus": "Active",
                          "Pricing": {
                            "ApplicableNoOfGuest": "2",
                            "Multiplier": "1",
                            "Surcharge": "0"
                          }
                        }
                      ]
                    }
                  ]
                }
                """);
        JsonNode success = objectMapper.readTree("{\"Status\":\"Success\"}");

        when(otaIntegrationRepository.findByStoreIdAndCode(STORE_ID, "BOOKING"))
                .thenReturn(Optional.of(integration));
        when(suApiClient.getMappings("token", HOTEL_ID, "19")).thenReturn(mappings);
        when(suApiClient.postBookingRatePlanMap(eq("token"), any())).thenReturn(success);
        when(suApiClient.isSuSuccess(success)).thenReturn(true);
        mockTokenExecution();

        ChannelMappingMultiplierSyncSummaryDTO summary = service.syncForChannel(STORE_ID, channel);

        assertEquals("SUCCESS", summary.getStatus());
        assertEquals(1, summary.getSuccessCount());
        assertEquals(0, summary.getFailureCount());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postBookingRatePlanMap(eq("token"), payloadCaptor.capture());
        verify(suApiClient, never()).postAirbnbListingMap(anyString(), any());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals(HOTEL_ID, payload.get("hotelid"));
        assertEquals(19, payload.get("channelid"));
        assertEquals("BOOKING-HOTEL", payload.get("channelhotelid"));
        assertEquals("101", payload.get("roomid"));
        assertEquals("BAR", payload.get("rateid"));
        assertEquals("B-ROOM", payload.get("channelroomid"));
        assertEquals("B-RATE", payload.get("channelrateid"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pricing = (List<Map<String, Object>>) payload.get("pricing");
        assertEquals(2, pricing.get(0).get("applicablenoofguest"));
        assertEquals(new BigDecimal("1.1"), pricing.get(0).get("multiplier"));
        assertEquals(BigDecimal.ZERO, pricing.get(0).get("surcharge"));
    }

    @Test
    void syncForChannel_shouldReportPartialFailureWhenBookingMappingIdentifierIsMissing() throws Exception {
        SuMappingMultiplierSyncService service = createService();
        Channel channel = channel("BOOKING", PriceAdjustmentType.PERCENTAGE, new BigDecimal("10"));
        OtaIntegration integration = integration("BOOKING");
        JsonNode mappings = objectMapper.readTree("""
                {
                  "Status": "Success",
                  "19": [
                    {
                      "Status": "Active",
                      "ChannelHotelID": "BOOKING-HOTEL",
                      "RoomIDs": ["101"],
                      "Rateplans": [
                        {
                          "PMSRoomID": "101",
                          "PMSRateID": "BAR",
                          "ChannelRoomID": "B-ROOM",
                          "ChannelRateID": "B-RATE",
                          "MappingStatus": "Active",
                          "Pricing": { "ApplicableNoOfGuest": "2" }
                        },
                        {
                          "PMSRoomID": "101",
                          "PMSRateID": "FLEX",
                          "ChannelRoomID": "B-ROOM",
                          "MappingStatus": "Active",
                          "Pricing": { "ApplicableNoOfGuest": "2" }
                        }
                      ]
                    }
                  ]
                }
                """);
        JsonNode success = objectMapper.readTree("{\"Status\":\"Success\"}");

        when(otaIntegrationRepository.findByStoreIdAndCode(STORE_ID, "BOOKING"))
                .thenReturn(Optional.of(integration));
        when(suApiClient.getMappings("token", HOTEL_ID, "19")).thenReturn(mappings);
        when(suApiClient.postBookingRatePlanMap(eq("token"), any())).thenReturn(success);
        when(suApiClient.isSuSuccess(success)).thenReturn(true);
        mockTokenExecution();

        ChannelMappingMultiplierSyncSummaryDTO summary = service.syncForChannel(STORE_ID, channel);

        assertEquals("PARTIAL", summary.getStatus());
        assertEquals(1, summary.getSuccessCount());
        assertEquals(1, summary.getFailureCount());
        verify(suApiClient).postBookingRatePlanMap(eq("token"), any());
    }

    @Test
    void syncForChannel_shouldSkipAirbnbLegacySyncWithoutCallingListingMap() {
        SuMappingMultiplierSyncService service = createService();
        Channel channel = channel("AIRBNB", PriceAdjustmentType.FIXED, new BigDecimal("25"));

        ChannelMappingMultiplierSyncSummaryDTO summary = service.syncForChannel(STORE_ID, channel);

        assertEquals("SKIPPED", summary.getStatus());
        assertEquals("AIRBNB", summary.getChannelCode());
        assertEquals("244", summary.getSuChannelId());
        assertEquals(BigDecimal.ONE, summary.getRequestedMultiplier());
        assertEquals(new BigDecimal("25"), summary.getRequestedSurcharge());
        assertEquals("Airbnb 旧渠道级 Su 同步已停用；请在映射级价格设置中逐行保存后同步",
                summary.getMessage());
        verifyNoInteractions(suApiClient, suAccessTokenService);
    }

    @Test
    void syncForChannel_shouldSkipAirbnbLegacySyncBeforeReadingMappings() {
        SuMappingMultiplierSyncService service = createService();
        Channel channel = channel("AIRBNB", PriceAdjustmentType.PERCENTAGE, new BigDecimal("10"));

        ChannelMappingMultiplierSyncSummaryDTO summary = service.syncForChannel(STORE_ID, channel);

        assertEquals("SKIPPED", summary.getStatus());
        assertEquals(new BigDecimal("1.1"), summary.getRequestedMultiplier());
        assertEquals(BigDecimal.ZERO, summary.getRequestedSurcharge());
        verifyNoInteractions(otaIntegrationRepository, storeRepository, suApiClient, suAccessTokenService);
    }

    private SuMappingMultiplierSyncService createService() {
        return new SuMappingMultiplierSyncService(
                otaIntegrationRepository,
                storeRepository,
                suApiClient,
                suAccessTokenService
        );
    }

    private Channel channel(String code, PriceAdjustmentType type, BigDecimal value) {
        Channel channel = new Channel();
        channel.setStoreId(STORE_ID);
        channel.setCode(code);
        channel.setPriceAdjustmentType(type);
        channel.setPriceAdjustmentValue(value);
        return channel;
    }

    private OtaIntegration integration(String code) {
        OtaIntegration integration = new OtaIntegration();
        integration.setStoreId(STORE_ID);
        integration.setCode(code);
        integration.setSuPropertyId(HOTEL_ID);
        return integration;
    }

    private void mockTokenExecution() {
        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> action = (Function<String, Object>) invocation.getArgument(0);
            return action.apply("token");
        });
    }
}
