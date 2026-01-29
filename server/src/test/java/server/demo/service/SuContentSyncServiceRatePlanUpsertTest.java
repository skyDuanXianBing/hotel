package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.PricePlan;
import server.demo.repository.PricePlanRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuContentSyncServiceRatePlanUpsertTest {

    @Test
    void syncRatePlansForWidget_shouldUpsertIndividually_withOverlayFallback() throws Exception {
        RoomTypeRepository roomTypeRepository = Mockito.mock(RoomTypeRepository.class);
        RoomRepository roomRepository = Mockito.mock(RoomRepository.class);
        PricePlanRepository pricePlanRepository = Mockito.mock(PricePlanRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);

        ObjectMapper mapper = new ObjectMapper();

        PricePlan plan3 = new PricePlan();
        plan3.setId(3L);
        plan3.setName("p3");

        PricePlan plan4 = new PricePlan();
        plan4.setId(4L);
        plan4.setName("p4");

        when(pricePlanRepository.findByStoreIdOrderByName(7L)).thenReturn(List.of(plan4, plan3));

        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, JsonNode> action = (Function<String, JsonNode>) invocation.getArgument(0);
            return action.apply("token");
        });

        JsonNode failAlreadyExists = mapper.readTree("""
                {"Errors":[{"Code":"896","ShortText":"Requested rate does not qualify. Rate with id 3 already exists"}],"Status":"Fail"}
                """);
        JsonNode success = mapper.readTree("""
                {"Status":"Success"}
                """);

        when(suApiClient.postOtaHotelRatePlan(anyString(), any())).thenAnswer(invocation -> {
            Object payload = invocation.getArgument(1);
            JsonNode json = mapper.valueToTree(payload);
            JsonNode ratePlanNode = json.at("/RatePlans/RatePlan");
            assertTrue(ratePlanNode.isArray(), "RatePlans.RatePlan should be array");
            assertEquals(1, ratePlanNode.size(), "每次请求必须只包含 1 条 RatePlan");

            JsonNode one = ratePlanNode.get(0);
            String rateplanid = one.path("rateplanid").asText();
            String notifType = one.path("RatePlanNotifType").asText();

            if ("3".equals(rateplanid) && "New".equalsIgnoreCase(notifType)) {
                return failAlreadyExists;
            }
            return success;
        });

        when(suApiClient.isSuSuccess(failAlreadyExists)).thenReturn(false);
        when(suApiClient.isSuSuccess(success)).thenReturn(true);

        when(suApiClient.extractSuErrorMessage(failAlreadyExists)).thenReturn("Requested rate does not qualify. Rate with id 3 already exists");
        when(suApiClient.extractSuErrorMessage(success)).thenReturn(null);

        SuContentSyncService service = new SuContentSyncService(
                roomTypeRepository,
                roomRepository,
                pricePlanRepository,
                suApiClient,
                suAccessTokenService
        );

        SuContentSyncService.SuRatePlanSyncSummary summary = service.syncRatePlansForWidget(7L, "SXN2NBLE42");
        assertNotNull(summary);
        assertTrue(summary.ratePlansSynced(), "rate plans should be synced");
        assertEquals(2, summary.pricePlanCount());

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient, Mockito.atLeast(1)).postOtaHotelRatePlan(anyString(), payloadCaptor.capture());

        // 至少 3 次：plan4 New + plan3 New(fail) + plan3 Overlay
        assertTrue(payloadCaptor.getAllValues().size() >= 3);
    }
}

