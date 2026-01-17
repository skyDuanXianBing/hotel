package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuPropertyServiceTest {

    @Test
    void upsertStoreProperty_whenAlreadyExists_shouldFailAndSuggestChangeHotelId() throws Exception {
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuPropertyService service = new SuPropertyService(storeRepository, suApiClient, suAccessTokenService);

        Store store = new Store();
        store.setId(8L);
        store.setName("Store 8");
        store.setCity("Shanghai");
        store.setCountry("China");
        store.setAddress("Addr");
        store.setPhone("13800000000");
        store.setTimezone("Asia/Shanghai");
        store.setCurrency("CNY");
        store.setSuHotelId(null);

        when(storeRepository.findById(8L)).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storeRepository.findBySuHotelId(anyString())).thenReturn(Optional.empty());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode existsResp = mapper.readTree("{\"Status\":\"Fail\",\"Errors\":[{\"ShortText\":\"Property already exists\"}]}");

        when(suApiClient.isSuSuccess(existsResp)).thenReturn(false);
        when(suApiClient.extractSuErrorMessage(existsResp)).thenReturn("Property already exists");
        when(suApiClient.upsertProperty(anyString(), any())).thenReturn(existsResp);

        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> fn = (Function<String, Object>) inv.getArgument(0);
            return fn.apply("token");
        });

        SuPropertyService.UpsertResult result = service.upsertStoreProperty(8L);
        assertTrue(result.attempted());
        assertFalse(result.success());
        assertNotNull(result.hotelId());
        assertNotNull(result.message());
        assertTrue(result.message().contains("请更换"));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient, Mockito.times(1)).upsertProperty(eq("token"), payloadCaptor.capture());

        Object firstPayload = payloadCaptor.getAllValues().get(0);
        assertEquals("New", extractNotifType(firstPayload));
    }

    @SuppressWarnings("unchecked")
    private static String extractNotifType(Object payloadObj) {
        Map<String, Object> payload = (Map<String, Object>) payloadObj;
        Map<String, Object> contents = (Map<String, Object>) payload.get("HotelDescriptiveContents");
        Map<String, Object> content = (Map<String, Object>) contents.get("HotelDescriptiveContent");
        Object v = content.get("HotelDescriptiveContentNotifType");
        return v != null ? v.toString() : null;
    }
}
