package server.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import server.demo.config.SuMessagingWebhookAuthConfig;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.service.SuMessagingService;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingWebhookControllerTest {

    private StoreRepository storeRepository;
    private SuMessagingService suMessagingService;
    private SuMessagingWebhookController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        storeRepository = Mockito.mock(StoreRepository.class);
        suMessagingService = Mockito.mock(SuMessagingService.class);
        controller = new SuMessagingWebhookController(
                new ObjectMapper(),
                storeRepository,
                suMessagingService,
                new SuMessagingWebhookAuthConfig()
        );
        request = Mockito.mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Store store = new Store();
        store.setId(10L);
        store.setSuHotelId("STORE10");
        when(storeRepository.findBySuHotelId("STORE10")).thenReturn(Optional.of(store));
    }

    @Test
    void retriesOnceAfterDeadlockAndSucceedsWithExactlyTwoServiceCalls() {
        doThrow(new DeadlockLoserDataAccessException("deadlock loser", null))
                .doNothing()
                .when(suMessagingService)
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());

        ResponseEntity<Map<String, String>> response =
                controller.handleMessagingWebhook(request, "{\"hotelid\":\"STORE10\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody().get("status"));
        verify(suMessagingService, times(2))
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());
    }

    @Test
    void retriesUniqueKeyRaceAndSucceeds() {
        doThrow(new DataIntegrityViolationException("duplicate entry"))
                .doNothing()
                .when(suMessagingService)
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());

        ResponseEntity<Map<String, String>> response =
                controller.handleMessagingWebhook(request, "{\"hotelid\":\"STORE10\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody().get("status"));
        verify(suMessagingService, times(2))
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());
    }

    @Test
    void persistentDeadlockStopsAtMaxAttemptsAndStillReturnsSuccess() {
        doThrow(new DeadlockLoserDataAccessException("always deadlocked", null))
                .when(suMessagingService)
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());

        ResponseEntity<Map<String, String>> response =
                controller.handleMessagingWebhook(request, "{\"hotelid\":\"STORE10\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody().get("status"));
        verify(suMessagingService, times(3))
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());
    }

    @Test
    void successfulInboundDoesNotTriggerRetry() {
        doNothing()
                .when(suMessagingService)
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());

        ResponseEntity<Map<String, String>> response =
                controller.handleMessagingWebhook(request, "{\"hotelid\":\"STORE10\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody().get("status"));
        verify(suMessagingService, times(1))
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());
    }

    @Test
    void nonRetryableFailureCallsServiceOnceAndStillReturnsSuccess() {
        doThrow(new IllegalStateException("not a concurrency failure"))
                .when(suMessagingService)
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());

        ResponseEntity<Map<String, String>> response =
                controller.handleMessagingWebhook(request, "{\"hotelid\":\"STORE10\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody().get("status"));
        verify(suMessagingService, times(1))
                .handleInboundMessage(eq(10L), eq("STORE10"), any(JsonNode.class), anyString());
    }
}
