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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 映射倍率链路渠道解析聚焦测试：两处 resolveSuChannelId 查目录 + payload channelid 参数化。
 */
@ExtendWith(MockitoExtension.class)
class SuMappingChannelIdResolutionTest {

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
    void multiplierSyncResolveSuChannelId_shouldCoverAllFiveChannelsAndAliases() {
        assertEquals("19", SuMappingMultiplierSyncService.resolveSuChannelId("BOOKING"));
        assertEquals("19", SuMappingMultiplierSyncService.resolveSuChannelId("BOOKING.COM"));
        assertEquals("244", SuMappingMultiplierSyncService.resolveSuChannelId("AIRBNB"));
        assertEquals("9", SuMappingMultiplierSyncService.resolveSuChannelId("EXPEDIA"));
        assertEquals("339", SuMappingMultiplierSyncService.resolveSuChannelId("TRIP"));
        assertEquals("339", SuMappingMultiplierSyncService.resolveSuChannelId("CTRIP"));
        assertEquals("189", SuMappingMultiplierSyncService.resolveSuChannelId("AGODA"));
        assertNull(SuMappingMultiplierSyncService.resolveSuChannelId("UNKNOWN"));
        assertNull(SuMappingMultiplierSyncService.resolveSuChannelId(null));
    }

    @Test
    void mappingPriceSettingsResolveSuChannelId_shouldCoverAllFiveChannelsAndAliases() {
        assertEquals("19", ChannelMappingPriceSettingsService.resolveSuChannelId("BOOKING"));
        assertEquals("19", ChannelMappingPriceSettingsService.resolveSuChannelId("BOOKING.COM"));
        assertEquals("244", ChannelMappingPriceSettingsService.resolveSuChannelId("AIRBNB"));
        assertEquals("9", ChannelMappingPriceSettingsService.resolveSuChannelId("EXPEDIA"));
        assertEquals("339", ChannelMappingPriceSettingsService.resolveSuChannelId("TRIP"));
        assertEquals("339", ChannelMappingPriceSettingsService.resolveSuChannelId("CTRIP"));
        assertEquals("189", ChannelMappingPriceSettingsService.resolveSuChannelId("AGODA"));
        assertNull(ChannelMappingPriceSettingsService.resolveSuChannelId("UNKNOWN"));
        assertNull(ChannelMappingPriceSettingsService.resolveSuChannelId(null));
    }

    @Test
    void otaSyncDefaultChannelCodes_shouldIncludeAllFiveChannels() {
        OtaSyncService service = new OtaSyncService(null, null, null, null, null, null, null);
        assertEquals(
                List.of("BOOKING", "AIRBNB", "EXPEDIA", "TRIP", "AGODA"),
                service.getDefaultOtaChannelCodes()
        );
    }

    @Test
    void syncForChannel_shouldPostTripRatePlanMapWithTripSuChannelId() throws Exception {
        SuMappingMultiplierSyncService service = new SuMappingMultiplierSyncService(
                otaIntegrationRepository,
                storeRepository,
                suApiClient,
                suAccessTokenService
        );
        Channel channel = channel("TRIP");
        OtaIntegration integration = integration("TRIP");
        JsonNode mappings = objectMapper.readTree("""
                {
                  "Status": "Success",
                  "339": [
                    {
                      "Status": "Active",
                      "ChannelHotelID": "TRIP-HOTEL",
                      "RoomIDs": ["101"],
                      "Rateplans": [
                        {
                          "PMSRoomID": "101",
                          "PMSRateID": "BAR",
                          "ChannelRoomID": "T-ROOM",
                          "ChannelRateID": "T-RATE",
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

        when(otaIntegrationRepository.findByStoreIdAndCode(STORE_ID, "TRIP"))
                .thenReturn(Optional.of(integration));
        when(suApiClient.getMappings("token", HOTEL_ID, "339")).thenReturn(mappings);
        when(suApiClient.postBookingRatePlanMap(eq("token"), any())).thenReturn(success);
        when(suApiClient.isSuSuccess(success)).thenReturn(true);
        mockTokenExecution();

        ChannelMappingMultiplierSyncSummaryDTO summary = service.syncForChannel(STORE_ID, channel);

        assertEquals("SUCCESS", summary.getStatus());
        assertEquals("339", summary.getSuChannelId());

        // channelid 必须带 Trip.com 的 Su channel id 339，避免误推到 Booking(19)
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postBookingRatePlanMap(eq("token"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals(339, payload.get("channelid"));
        assertEquals(HOTEL_ID, payload.get("hotelid"));
        assertEquals("TRIP-HOTEL", payload.get("channelhotelid"));
    }

    private Channel channel(String code) {
        Channel channel = new Channel();
        channel.setStoreId(STORE_ID);
        channel.setCode(code);
        channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        channel.setPriceAdjustmentValue(new BigDecimal("10"));
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
