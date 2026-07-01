package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.MappingPriceSettingRowDTO;
import server.demo.dto.MappingPriceSettingRowSaveRequestDTO;
import server.demo.dto.MappingPriceSettingsResponseDTO;
import server.demo.dto.MappingPriceSettingsRetryRequestDTO;
import server.demo.dto.MappingPriceSettingsSaveRequestDTO;
import server.demo.dto.MappingPriceSettingsSaveResponseDTO;
import server.demo.entity.Channel;
import server.demo.entity.ChannelMappingPriceSetting;
import server.demo.entity.OtaIntegration;
import server.demo.enums.ChannelMappingPriceSyncStatus;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.ChannelMappingPriceSettingRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.OtaIntegrationRepository;
import server.demo.repository.StoreRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChannelMappingPriceSettingsServiceTest {

    private static final Long STORE_ID = 7L;
    private static final Long CHANNEL_ID = 11L;
    private static final Long INTEGRATION_ID = 21L;
    private static final String HOTEL_ID = "HOTEL7";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ChannelMappingPriceSetting> settings = new LinkedHashMap<>();
    private long nextSettingId = 1L;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private OtaIntegrationRepository otaIntegrationRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ChannelMappingPriceSettingRepository settingRepository;

    @Mock
    private SuApiClient suApiClient;

    @Mock
    private SuAccessTokenService suAccessTokenService;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(1L, STORE_ID, "OWNER"));
        mockSettingRepository();
        mockTokenExecution();
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void listMappingPriceSettings_shouldJoinBookingRowsWithPersistedStateAndDistinctGuestKeys() throws Exception {
        ChannelMappingPriceSettingsService service = createService();
        mockChannelAndIntegration("BOOKING");
        when(suApiClient.getMappings("token", HOTEL_ID, "19")).thenReturn(readJson(bookingMappingsWithTwoGuests()));

        MappingPriceSettingsResponseDTO firstList = service.listMappingPriceSettings(CHANNEL_ID);
        MappingPriceSettingRowDTO guestOne = firstList.getRows().get(0);
        MappingPriceSettingRowDTO guestTwo = firstList.getRows().get(1);
        assertNotEquals(guestOne.getRowKey(), guestTwo.getRowKey());

        ChannelMappingPriceSetting persisted = new ChannelMappingPriceSetting();
        persisted.setStoreId(STORE_ID);
        persisted.setChannelId(CHANNEL_ID);
        persisted.setOtaIntegrationId(INTEGRATION_ID);
        persisted.setChannelCode("BOOKING");
        persisted.setSuChannelId("19");
        persisted.setSuPropertyId(HOTEL_ID);
        persisted.setMappingKey(decodeRowKey(guestTwo.getRowKey()));
        persisted.setRowKey(guestTwo.getRowKey());
        persisted.setMultiplier(new BigDecimal("1.25"));
        persisted.setSurcharge(new BigDecimal("3.50"));
        persisted.setSyncStatus(ChannelMappingPriceSyncStatus.FAILED);
        persisted.setLastError("previous failure");
        persisted.setRetryCount(2);
        settingRepository.save(persisted);

        MappingPriceSettingsResponseDTO response = service.listMappingPriceSettings(CHANNEL_ID);

        assertEquals(2, response.getRows().size());
        assertEquals(1, response.getFailureCount());
        MappingPriceSettingRowDTO joined = findRowByGuest(response.getRows(), "2");
        assertEquals(new BigDecimal("1.25"), joined.getMultiplier());
        assertEquals(new BigDecimal("3.50"), joined.getSurcharge());
        assertEquals("FAILED", joined.getSyncStatus());
        assertEquals("previous failure", joined.getLastError());
        verify(suApiClient, never()).postBookingRatePlanMap(anyString(), any());
    }

    @Test
    void saveMappingPriceSettings_shouldPostAllBookingRowsAndNotWriteChannelAdjustmentFields() throws Exception {
        ChannelMappingPriceSettingsService service = createService();
        Channel channel = mockChannelAndIntegration("BOOKING");
        channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        channel.setPriceAdjustmentValue(new BigDecimal("8"));
        when(suApiClient.getMappings("token", HOTEL_ID, "19")).thenReturn(readJson(bookingMappingsWithTwoGuests()));
        JsonNode success = readJson("{\"Status\":\"Success\"}");
        when(suApiClient.postBookingRatePlanMap(eq("token"), any())).thenReturn(success);
        when(suApiClient.isSuSuccess(success)).thenReturn(true);

        MappingPriceSettingsResponseDTO list = service.listMappingPriceSettings(CHANNEL_ID);
        MappingPriceSettingsSaveResponseDTO response = service.saveMappingPriceSettings(
                CHANNEL_ID,
                saveRequest(List.of(
                        row(list.getRows().get(0).getRowKey(), "1.10", "0"),
                        row(list.getRows().get(1).getRowKey(), "1.20", "5.00")
                ))
        );

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(2, response.getSuccessCount());
        verify(channelRepository, never()).save(any());
        assertEquals(PriceAdjustmentType.PERCENTAGE, channel.getPriceAdjustmentType());
        assertEquals(new BigDecimal("8"), channel.getPriceAdjustmentValue());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient, times(2)).postBookingRatePlanMap(eq("token"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> firstPayload = (Map<String, Object>) payloadCaptor.getAllValues().get(0);
        assertEquals("setup", firstPayload.get("action"));
        assertTrue(firstPayload.containsKey("derivedrateids"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pricing = (List<Map<String, Object>>) firstPayload.get("pricing");
        assertEquals(2, pricing.size());
        assertEquals(1, pricing.get(0).get("applicablenoofguest"));
        assertEquals(2, pricing.get(1).get("applicablenoofguest"));
        assertEquals(new BigDecimal("1.1"), pricing.get(0).get("multiplier"));
        assertEquals(new BigDecimal("1"), pricing.get(1).get("multiplier"));
        @SuppressWarnings("unchecked")
        Map<String, Object> secondPayload = (Map<String, Object>) payloadCaptor.getAllValues().get(1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> secondPricing = (List<Map<String, Object>>) secondPayload.get("pricing");
        assertEquals(new BigDecimal("1.1"), secondPricing.get(0).get("multiplier"));
        assertEquals(new BigDecimal("1.2"), secondPricing.get(1).get("multiplier"));

        List<ChannelMappingPriceSetting> saved = new ArrayList<>(settings.values());
        assertEquals(2, saved.size());
        assertEquals(ChannelMappingPriceSyncStatus.SUCCESS, saved.get(0).getSyncStatus());
        assertEquals(ChannelMappingPriceSyncStatus.SUCCESS, saved.get(1).getSyncStatus());
    }

    @Test
    void saveMappingPriceSettings_shouldPersistPartialFailureWithoutRollingBackSuccessRows() throws Exception {
        ChannelMappingPriceSettingsService service = createService();
        mockChannelAndIntegration("BOOKING");
        when(suApiClient.getMappings("token", HOTEL_ID, "19")).thenReturn(readJson(bookingMappingsWithTwoGuests()));
        JsonNode success = readJson("{\"Status\":\"Success\"}");
        JsonNode failure = readJson("{\"Status\":\"Fail\",\"Errors\":{\"ShortText\":\"rate rejected\"}}");
        when(suApiClient.postBookingRatePlanMap(eq("token"), any())).thenReturn(success).thenReturn(failure);
        when(suApiClient.isSuSuccess(success)).thenReturn(true);
        when(suApiClient.isSuSuccess(failure)).thenReturn(false);
        when(suApiClient.extractSuErrorMessage(failure)).thenReturn("rate rejected");

        MappingPriceSettingsResponseDTO list = service.listMappingPriceSettings(CHANNEL_ID);
        MappingPriceSettingsSaveResponseDTO response = service.saveMappingPriceSettings(
                CHANNEL_ID,
                saveRequest(List.of(
                        row(list.getRows().get(0).getRowKey(), "1.10", "0"),
                        row(list.getRows().get(1).getRowKey(), "1.20", "5.00")
                ))
        );

        assertEquals("PARTIAL", response.getStatus());
        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        assertEquals(ChannelMappingPriceSyncStatus.SUCCESS, settings.get(list.getRows().get(0).getRowKey()).getSyncStatus());
        assertEquals(ChannelMappingPriceSyncStatus.FAILED, settings.get(list.getRows().get(1).getRowKey()).getSyncStatus());
        assertEquals("rate rejected", settings.get(list.getRows().get(1).getRowKey()).getLastError());
    }

    @Test
    void retryMappingPriceSettings_shouldUsePersistedDesiredValues() throws Exception {
        ChannelMappingPriceSettingsService service = createService();
        mockChannelAndIntegration("BOOKING");
        when(suApiClient.getMappings("token", HOTEL_ID, "19")).thenReturn(readJson(bookingMappingsWithTwoGuests()));
        JsonNode failure = readJson("{\"Status\":\"Fail\",\"Errors\":{\"ShortText\":\"temporary\"}}");
        JsonNode success = readJson("{\"Status\":\"Success\"}");
        when(suApiClient.postBookingRatePlanMap(eq("token"), any())).thenReturn(failure).thenReturn(success);
        when(suApiClient.isSuSuccess(failure)).thenReturn(false);
        when(suApiClient.isSuSuccess(success)).thenReturn(true);
        when(suApiClient.extractSuErrorMessage(failure)).thenReturn("temporary");

        MappingPriceSettingsResponseDTO list = service.listMappingPriceSettings(CHANNEL_ID);
        String failedRowKey = list.getRows().get(0).getRowKey();
        service.saveMappingPriceSettings(CHANNEL_ID, saveRequest(List.of(row(failedRowKey, "1.33", "7.00"))));
        clearInvocations(suApiClient);

        MappingPriceSettingsSaveResponseDTO retryResponse =
                service.retryMappingPriceSettings(CHANNEL_ID, new MappingPriceSettingsRetryRequestDTO());

        assertEquals("SUCCESS", retryResponse.getStatus());
        ChannelMappingPriceSetting retried = settings.get(failedRowKey);
        assertEquals(ChannelMappingPriceSyncStatus.SUCCESS, retried.getSyncStatus());
        assertEquals(1, retried.getRetryCount());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postBookingRatePlanMap(eq("token"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pricing = (List<Map<String, Object>>) payload.get("pricing");
        assertEquals(new BigDecimal("1.33"), pricing.get(0).get("multiplier"));
        assertEquals(new BigDecimal("7"), pricing.get(0).get("surcharge"));
    }

    @Test
    void saveMappingPriceSettings_shouldMarkMissingIdentifiersStaleAndSkipSuWrite() throws Exception {
        ChannelMappingPriceSettingsService service = createService();
        mockChannelAndIntegration("BOOKING");
        when(suApiClient.getMappings("token", HOTEL_ID, "19")).thenReturn(readJson(bookingMappingMissingChannelRateId()));

        MappingPriceSettingsResponseDTO list = service.listMappingPriceSettings(CHANNEL_ID);
        MappingPriceSettingsSaveResponseDTO response = service.saveMappingPriceSettings(
                CHANNEL_ID,
                saveRequest(List.of(row(list.getRows().get(0).getRowKey(), "1.15", "0")))
        );

        assertEquals("FAILED", response.getStatus());
        assertEquals(1, response.getStaleCount());
        assertEquals("STALE_MAPPING", response.getRows().get(0).getSyncStatus());
        assertTrue(response.getRows().get(0).getLastError().contains("channelrateid"));
        verify(suApiClient, never()).postBookingRatePlanMap(anyString(), any());
        assertEquals(ChannelMappingPriceSyncStatus.STALE_MAPPING, settings.get(list.getRows().get(0).getRowKey()).getSyncStatus());
    }

    @Test
    void airbnbRows_shouldUseOccupancyInDistinctRowKeysAndListingUpdatePayload() throws Exception {
        ChannelMappingPriceSettingsService service = createService();
        mockChannelAndIntegration("AIRBNB");
        when(suApiClient.getMappings("token", HOTEL_ID, "244")).thenReturn(readJson(airbnbMappingsWithTwoOccupancies()));
        JsonNode retrieve = readJson(airbnbRetrieveListing("A-LISTING", "Airbnb Listing"));
        JsonNode success = readJson("{\"Status\":\"Success\"}");
        when(suApiClient.retrieveAirbnbListing(eq("token"), any())).thenReturn(retrieve);
        when(suApiClient.postAirbnbListingUpdate(eq("token"), any())).thenReturn(success);
        when(suApiClient.isSuSuccess(success)).thenReturn(true);

        MappingPriceSettingsResponseDTO list = service.listMappingPriceSettings(CHANNEL_ID);
        assertEquals(2, list.getRows().size());
        assertNotEquals(list.getRows().get(0).getRowKey(), list.getRows().get(1).getRowKey());
        assertEquals("1", list.getRows().get(0).getOccupancy());
        assertEquals("2", list.getRows().get(1).getOccupancy());

        MappingPriceSettingsSaveResponseDTO response = service.saveMappingPriceSettings(
                CHANNEL_ID,
                saveRequest(List.of(
                        row(list.getRows().get(0).getRowKey(), "1.05", "0"),
                        row(list.getRows().get(1).getRowKey(), "1.15", "2.00")
                ))
        );

        assertEquals("SUCCESS", response.getStatus());
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient, times(2)).retrieveAirbnbListing(eq("token"), any());
        verify(suApiClient, times(2)).postAirbnbListingUpdate(eq("token"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> firstPayload = (Map<String, Object>) payloadCaptor.getAllValues().get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> secondPayload = (Map<String, Object>) payloadCaptor.getAllValues().get(1);
        assertEquals("Airbnb Listing", firstPayload.get("name"));
        assertEquals(1, firstPayload.get("occupancy"));
        assertEquals(2, secondPayload.get("occupancy"));
        verify(suApiClient, never()).postAirbnbListingMap(anyString(), any());
        verify(suApiClient, never()).postBookingRatePlanMap(anyString(), any());
    }

    @Test
    void airbnbSave_shouldFailSafelyWhenListingDetailsAreMissingAndNotFallbackToMap() throws Exception {
        ChannelMappingPriceSettingsService service = createService();
        mockChannelAndIntegration("AIRBNB");
        when(suApiClient.getMappings("token", HOTEL_ID, "244")).thenReturn(readJson(airbnbMappingsWithTwoOccupancies()));
        JsonNode retrieveWithoutName = readJson("""
                {
                  "Status": "Success",
                  "Data": {
                    "listing": {
                      "listingid": "A-LISTING"
                    }
                  }
                }
                """);
        when(suApiClient.retrieveAirbnbListing(eq("token"), any())).thenReturn(retrieveWithoutName);

        MappingPriceSettingsResponseDTO list = service.listMappingPriceSettings(CHANNEL_ID);
        MappingPriceSettingsSaveResponseDTO response = service.saveMappingPriceSettings(
                CHANNEL_ID,
                saveRequest(List.of(row(list.getRows().get(0).getRowKey(), "1.05", "0")))
        );

        assertEquals("FAILED", response.getStatus());
        assertEquals(1, response.getFailureCount());
        assertTrue(response.getRows().get(0).getLastError()
                .contains("Airbnb listing update requires listing details"));
        verify(suApiClient).retrieveAirbnbListing(eq("token"), any());
        verify(suApiClient, never()).postAirbnbListingUpdate(anyString(), any());
        verify(suApiClient, never()).postAirbnbListingMap(anyString(), any());
    }

    @Test
    void airbnbSave_shouldKeepDuplicateMappingErrorAsRowFailure() throws Exception {
        ChannelMappingPriceSettingsService service = createService();
        mockChannelAndIntegration("AIRBNB");
        when(suApiClient.getMappings("token", HOTEL_ID, "244")).thenReturn(readJson(airbnbMappingsWithTwoOccupancies()));
        JsonNode retrieve = readJson(airbnbRetrieveListing("A-LISTING", "Airbnb Listing"));
        JsonNode duplicateFailure = readJson("""
                {
                  "Status": "Fail",
                  "Errors": {
                    "ShortText": "Room and rateplan combination already mapped"
                  }
                }
                """);
        when(suApiClient.retrieveAirbnbListing(eq("token"), any())).thenReturn(retrieve);
        when(suApiClient.postAirbnbListingUpdate(eq("token"), any())).thenReturn(duplicateFailure);
        when(suApiClient.isSuSuccess(duplicateFailure)).thenReturn(false);
        when(suApiClient.extractSuErrorMessage(duplicateFailure))
                .thenReturn("Room and rateplan combination already mapped");

        MappingPriceSettingsResponseDTO list = service.listMappingPriceSettings(CHANNEL_ID);
        MappingPriceSettingsSaveResponseDTO response = service.saveMappingPriceSettings(
                CHANNEL_ID,
                saveRequest(List.of(row(list.getRows().get(0).getRowKey(), "1.05", "0")))
        );

        assertEquals("FAILED", response.getStatus());
        assertEquals(1, response.getFailureCount());
        assertEquals(
                "Room and rateplan combination already mapped",
                response.getRows().get(0).getLastError()
        );
        verify(suApiClient, never()).postAirbnbListingMap(anyString(), any());
    }

    private ChannelMappingPriceSettingsService createService() {
        return new ChannelMappingPriceSettingsService(
                channelRepository,
                otaIntegrationRepository,
                storeRepository,
                settingRepository,
                suApiClient,
                suAccessTokenService
        );
    }

    private Channel mockChannelAndIntegration(String channelCode) {
        Channel channel = new Channel();
        channel.setId(CHANNEL_ID);
        channel.setStoreId(STORE_ID);
        channel.setCode(channelCode);
        channel.setName(channelCode);
        channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
        channel.setPriceAdjustmentValue(BigDecimal.ZERO);
        OtaIntegration integration = new OtaIntegration();
        integration.setId(INTEGRATION_ID);
        integration.setStoreId(STORE_ID);
        integration.setCode(channelCode);
        integration.setSuPropertyId(HOTEL_ID);

        when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channel));
        when(otaIntegrationRepository.findByStoreIdAndCode(STORE_ID, channelCode)).thenReturn(Optional.of(integration));
        return channel;
    }

    private void mockTokenExecution() {
        lenient().when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> action = (Function<String, Object>) invocation.getArgument(0);
            return action.apply("token");
        });
    }

    private void mockSettingRepository() {
        lenient().when(settingRepository.findByStoreIdAndChannelIdAndSuPropertyId(any(), any(), anyString()))
                .thenAnswer(invocation -> filterSettings(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2), null, null));
        lenient().when(settingRepository.findByStoreIdAndChannelIdAndMappingKey(any(), any(), anyString()))
                .thenAnswer(invocation -> {
                    List<ChannelMappingPriceSetting> found = filterSettings(
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            null,
                            invocation.getArgument(2),
                            null
                    );
                    if (found.isEmpty()) {
                        return Optional.empty();
                    }
                    return Optional.of(found.get(0));
                });
        lenient().when(settingRepository.findByStoreIdAndChannelIdAndRowKey(any(), any(), anyString()))
                .thenAnswer(invocation -> {
                    ChannelMappingPriceSetting setting = settings.get(invocation.getArgument(2));
                    if (setting == null) {
                        return Optional.empty();
                    }
                    Long storeId = invocation.getArgument(0);
                    Long channelId = invocation.getArgument(1);
                    if (!storeId.equals(setting.getStoreId()) || !channelId.equals(setting.getChannelId())) {
                        return Optional.empty();
                    }
                    return Optional.of(setting);
                });
        lenient().when(settingRepository.findByStoreIdAndChannelIdAndRowKeyIn(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Collection<String> rowKeys = invocation.getArgument(2);
                    List<ChannelMappingPriceSetting> result = new ArrayList<>();
                    for (String rowKey : rowKeys) {
                        ChannelMappingPriceSetting setting = settings.get(rowKey);
                        if (setting != null) {
                            result.add(setting);
                        }
                    }
                    return result;
                });
        lenient().when(settingRepository.findByStoreIdAndChannelIdAndSyncStatusIn(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Collection<ChannelMappingPriceSyncStatus> statuses = invocation.getArgument(2);
                    List<ChannelMappingPriceSetting> result = new ArrayList<>();
                    for (ChannelMappingPriceSetting setting : settings.values()) {
                        if (statuses.contains(setting.getSyncStatus())) {
                            result.add(setting);
                        }
                    }
                    return result;
                });
        lenient().when(settingRepository.save(any(ChannelMappingPriceSetting.class))).thenAnswer(invocation -> {
            ChannelMappingPriceSetting setting = invocation.getArgument(0);
            if (setting.getId() == null) {
                setting.setId(nextSettingId);
                nextSettingId++;
            }
            settings.put(setting.getRowKey(), setting);
            return setting;
        });
    }

    private List<ChannelMappingPriceSetting> filterSettings(
            Long storeId,
            Long channelId,
            String suPropertyId,
            String mappingKey,
            String rowKey
    ) {
        List<ChannelMappingPriceSetting> result = new ArrayList<>();
        for (ChannelMappingPriceSetting setting : settings.values()) {
            if (!storeId.equals(setting.getStoreId())) {
                continue;
            }
            if (!channelId.equals(setting.getChannelId())) {
                continue;
            }
            if (suPropertyId != null && !suPropertyId.equals(setting.getSuPropertyId())) {
                continue;
            }
            if (mappingKey != null && !mappingKey.equals(setting.getMappingKey())) {
                continue;
            }
            if (rowKey != null && !rowKey.equals(setting.getRowKey())) {
                continue;
            }
            result.add(setting);
        }
        return result;
    }

    private MappingPriceSettingsSaveRequestDTO saveRequest(List<MappingPriceSettingRowSaveRequestDTO> rows) {
        MappingPriceSettingsSaveRequestDTO request = new MappingPriceSettingsSaveRequestDTO();
        request.setRows(rows);
        return request;
    }

    private MappingPriceSettingRowSaveRequestDTO row(String rowKey, String multiplier, String surcharge) {
        MappingPriceSettingRowSaveRequestDTO row = new MappingPriceSettingRowSaveRequestDTO();
        row.setRowKey(rowKey);
        row.setMultiplier(new BigDecimal(multiplier));
        row.setSurcharge(new BigDecimal(surcharge));
        return row;
    }

    private MappingPriceSettingRowDTO findRowByGuest(List<MappingPriceSettingRowDTO> rows, String guest) {
        for (MappingPriceSettingRowDTO row : rows) {
            if (guest.equals(row.getApplicableNoOfGuest())) {
                return row;
            }
        }
        throw new AssertionError("row not found for guest " + guest);
    }

    private JsonNode readJson(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private String decodeRowKey(String rowKey) {
        byte[] decoded = java.util.Base64.getUrlDecoder().decode(rowKey);
        return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String bookingMappingsWithTwoGuests() {
        return """
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
                          "Pricing": [
                            { "ApplicableNoOfGuest": "1", "Multiplier": "1.00", "Surcharge": "0" },
                            { "ApplicableNoOfGuest": "2", "Multiplier": "1.00", "Surcharge": "0" }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String bookingMappingMissingChannelRateId() {
        return """
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
                          "MappingStatus": "Active",
                          "Pricing": { "ApplicableNoOfGuest": "2" }
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String airbnbMappingsWithTwoOccupancies() {
        return """
                {
                  "Status": "Success",
                  "244": [
                    {
                      "Status": "Active",
                      "ChannelHotelID": "AIRBNB-HOTEL",
                      "RoomIDs": ["101"],
                      "Rateplans": [
                        {
                          "PMSRoomID": "101",
                          "PMSRateID": "BAR",
                          "ListingID": "A-LISTING",
                          "MappingStatus": "Active",
                          "Pricing": [
                            { "Occupancy": "1", "Multiplier": "1.00", "Surcharge": "0" },
                            { "Occupancy": "2", "Multiplier": "1.00", "Surcharge": "0" }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String airbnbRetrieveListing(String listingId, String name) {
        return """
                {
                  "Status": "Success",
                  "Data": {
                    "listing": {
                      "listingid": "%s",
                      "name": "%s",
                      "person_capacity": 2,
                      "room_type_category": "entire_home"
                    }
                  }
                }
                """.formatted(listingId, name);
    }
}
