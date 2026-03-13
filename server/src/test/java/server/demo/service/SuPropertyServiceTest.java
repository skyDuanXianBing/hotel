package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.Store;
import server.demo.entity.StorePolicy;
import server.demo.repository.StoreRepository;
import server.demo.repository.StorePolicyRepository;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuPropertyServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void upsertStoreProperty_whenAlreadyExists_shouldFailAndSuggestChangeHotelId() throws Exception {
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StorePolicyRepository storePolicyRepository = Mockito.mock(StorePolicyRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuPropertyService service = new SuPropertyService(storeRepository, storePolicyRepository, suApiClient, suAccessTokenService);

        Store store = buildStore(8L);
        store.setSuHotelId(null);

        when(storeRepository.findById(8L)).thenReturn(Optional.of(store));
        when(storePolicyRepository.findByStoreId(8L)).thenReturn(Optional.empty());
        when(storeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findBySuHotelId(anyString())).thenReturn(Optional.empty());

        JsonNode existsResponse = objectMapper.readTree("{\"Status\":\"Fail\",\"Errors\":[{\"ShortText\":\"Property already exists\"}]}");
        when(suApiClient.upsertProperty(anyString(), any())).thenReturn(existsResponse);
        when(suApiClient.isSuSuccess(existsResponse)).thenReturn(false);
        when(suApiClient.extractSuErrorMessage(existsResponse)).thenReturn("Property already exists");
        mockTokenExecution(suAccessTokenService);

        SuPropertyService.UpsertResult result = service.upsertStoreProperty(8L);

        assertTrue(result.attempted());
        assertFalse(result.success());
        assertNotNull(result.hotelId());
        assertNotNull(result.message());
        assertTrue(result.message().contains("该酒店ID可能已被占用"));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient, times(1)).upsertProperty(eq("token"), payloadCaptor.capture());
        assertEquals("New", extractNotifType(payloadCaptor.getValue()));
    }

    @Test
    void updateStoreProperty_whenPropertyExists_shouldUseOverlayAndSucceed() throws Exception {
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StorePolicyRepository storePolicyRepository = Mockito.mock(StorePolicyRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuPropertyService service = new SuPropertyService(storeRepository, storePolicyRepository, suApiClient, suAccessTokenService);

        Store store = buildStore(9L);
        store.setType("2");
        store.setLanguage("ja");
        store.setPhoneTechType("5");
        StorePolicy storePolicy = new StorePolicy();
        storePolicy.setCheckinTime("15:00");
        storePolicy.setCheckoutTime("11:00");
        when(storeRepository.findById(9L)).thenReturn(Optional.of(store));
        when(storePolicyRepository.findByStoreId(9L)).thenReturn(Optional.of(storePolicy));

        JsonNode successResponse = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.upsertProperty(anyString(), any())).thenReturn(successResponse);
        when(suApiClient.isSuSuccess(successResponse)).thenReturn(true);
        mockTokenExecution(suAccessTokenService);

        SuPropertyService.UpsertResult result = service.updateStoreProperty(9L);

        assertTrue(result.success());
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient, times(1)).upsertProperty(eq("token"), payloadCaptor.capture());
        assertEquals("Overlay", extractNotifType(payloadCaptor.getValue()));
        assertEquals("2", extractContentValue(payloadCaptor.getValue(), "HotelType"));
        assertEquals("ja", extractContentValue(payloadCaptor.getValue(), "LanguageCode"));
        assertEquals("15:00", extractContentValue(payloadCaptor.getValue(), "OfficialCheckinTime"));
        assertEquals("11:00", extractContentValue(payloadCaptor.getValue(), "OfficialCheckoutTime"));
        assertEquals("5", extractPhoneTechType(payloadCaptor.getValue()));
    }

    @Test
    void updateStoreProperty_whenOverlaySaysInvalidHotelCode_shouldFallbackToNew() throws Exception {
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StorePolicyRepository storePolicyRepository = Mockito.mock(StorePolicyRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuPropertyService service = new SuPropertyService(storeRepository, storePolicyRepository, suApiClient, suAccessTokenService);

        Store store = buildStore(10L);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(storePolicyRepository.findByStoreId(10L)).thenReturn(Optional.empty());

        JsonNode overlayFailResponse = objectMapper.readTree("{\"Status\":\"Fail\",\"Errors\":[{\"ShortText\":\"Invalid HotelCode\"}]}");
        JsonNode newSuccessResponse = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.upsertProperty(anyString(), any())).thenReturn(overlayFailResponse, newSuccessResponse);
        when(suApiClient.isSuSuccess(overlayFailResponse)).thenReturn(false);
        when(suApiClient.isSuSuccess(newSuccessResponse)).thenReturn(true);
        when(suApiClient.extractSuErrorMessage(overlayFailResponse)).thenReturn("Invalid HotelCode");
        mockTokenExecution(suAccessTokenService);

        SuPropertyService.UpsertResult result = service.updateStoreProperty(10L);

        assertTrue(result.success());
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient, times(2)).upsertProperty(eq("token"), payloadCaptor.capture());
        assertEquals("Overlay", extractNotifType(payloadCaptor.getAllValues().get(0)));
        assertEquals("New", extractNotifType(payloadCaptor.getAllValues().get(1)));
    }

    @Test
    void updateStoreProperty_whenOverlayFailsWithOtherError_shouldNotFallbackToNew() throws Exception {
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        StorePolicyRepository storePolicyRepository = Mockito.mock(StorePolicyRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuPropertyService service = new SuPropertyService(storeRepository, storePolicyRepository, suApiClient, suAccessTokenService);

        Store store = buildStore(11L);
        when(storeRepository.findById(11L)).thenReturn(Optional.of(store));
        when(storePolicyRepository.findByStoreId(11L)).thenReturn(Optional.empty());

        JsonNode overlayFailResponse = objectMapper.readTree("{\"Status\":\"Fail\",\"Errors\":[{\"ShortText\":\"Authorization Required\"}]}");
        when(suApiClient.upsertProperty(anyString(), any())).thenReturn(overlayFailResponse);
        when(suApiClient.isSuSuccess(overlayFailResponse)).thenReturn(false);
        when(suApiClient.extractSuErrorMessage(overlayFailResponse)).thenReturn("Authorization Required");
        mockTokenExecution(suAccessTokenService);

        SuPropertyService.UpsertResult result = service.updateStoreProperty(11L);

        assertFalse(result.success());
        assertEquals("Authorization Required", result.message());
        verify(suApiClient, times(1)).upsertProperty(eq("token"), any());
    }

    private Store buildStore(Long storeId) {
        Store store = new Store();
        store.setId(storeId);
        store.setName("Store " + storeId);
        store.setCity("Shanghai");
        store.setCountry("China");
        store.setAddress("Addr");
        store.setPhone("13800000000");
        store.setTimezone("Asia/Shanghai");
        store.setCurrency("CNY");
        store.setSuHotelId("HOTEL1234567890");
        store.setType("1");
        store.setLanguage("en");
        store.setPhoneTechType("1");
        return store;
    }

    private static void mockTokenExecution(SuAccessTokenService suAccessTokenService) {
        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> fn = (Function<String, Object>) invocation.getArgument(0);
            return fn.apply("token");
        });
    }

    @SuppressWarnings("unchecked")
    private static String extractNotifType(Object payloadObject) {
        return extractContentValue(payloadObject, "HotelDescriptiveContentNotifType");
    }

    @SuppressWarnings("unchecked")
    private static String extractContentValue(Object payloadObject, String key) {
        Map<String, Object> payload = (Map<String, Object>) payloadObject;
        Map<String, Object> contents = (Map<String, Object>) payload.get("HotelDescriptiveContents");
        Map<String, Object> content = (Map<String, Object>) contents.get("HotelDescriptiveContent");
        Object value = content.get(key);
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static String extractPhoneTechType(Object payloadObject) {
        Map<String, Object> payload = (Map<String, Object>) payloadObject;
        Map<String, Object> contents = (Map<String, Object>) payload.get("HotelDescriptiveContents");
        Map<String, Object> content = (Map<String, Object>) contents.get("HotelDescriptiveContent");
        Map<String, Object> contactInfos = (Map<String, Object>) content.get("ContactInfos");
        java.util.List<Map<String, Object>> contactInfoList = (java.util.List<Map<String, Object>>) contactInfos.get("ContactInfo");
        Map<String, Object> availabilityContact = contactInfoList.get(1);
        Map<String, Object> phones = (Map<String, Object>) availabilityContact.get("Phones");
        java.util.List<Map<String, Object>> phoneList = (java.util.List<Map<String, Object>>) phones.get("Phone");
        Object value = phoneList.get(0).get("PhoneTechType");
        return value != null ? value.toString() : null;
    }
}
