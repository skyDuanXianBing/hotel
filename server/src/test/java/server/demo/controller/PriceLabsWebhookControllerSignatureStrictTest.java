package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.config.PriceLabsConfig;
import server.demo.service.PriceLabsWebhookAsyncProcessor;
import server.demo.service.PriceLabsWebhookAsyncService;
import server.demo.service.PriceLabsWebhookTaskTracker;
import server.demo.service.PriceLabsSyncService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceLabsWebhookControllerSignatureStrictTest {

    private static final String TOKEN = "secret-token";
    private static final String SOURCE = "pms";
    private static final String TIMESTAMP = "1700000000";
    private static final String REQUEST_ID = "req-1";

    private PriceLabsWebhookController controller;
    private PriceLabsSyncService priceLabsSyncService;
    private PriceLabsWebhookAsyncProcessor asyncProcessor;
    private PriceLabsWebhookAsyncService asyncService;
    private PriceLabsWebhookTaskTracker taskTracker;

    @BeforeEach
    void setUp() {
        PriceLabsConfig config = Mockito.mock(PriceLabsConfig.class);
        Mockito.when(config.getIntegrationToken()).thenReturn(TOKEN);
        Mockito.when(config.isDebug()).thenReturn(false);

        priceLabsSyncService = Mockito.mock(PriceLabsSyncService.class);
        asyncProcessor = Mockito.mock(PriceLabsWebhookAsyncProcessor.class);
        asyncService = Mockito.mock(PriceLabsWebhookAsyncService.class);
        taskTracker = Mockito.mock(PriceLabsWebhookTaskTracker.class);

        controller = new PriceLabsWebhookController();
        ReflectionTestUtils.setField(controller, "config", config);
        ReflectionTestUtils.setField(controller, "priceLabsSyncService", priceLabsSyncService);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(controller, "priceLabsWebhookAsyncProcessor", asyncProcessor);
        ReflectionTestUtils.setField(controller, "priceLabsWebhookAsyncService", asyncService);
        ReflectionTestUtils.setField(controller, "priceLabsWebhookTaskTracker", taskTracker);
    }

    @Test
    void hook_missingSignature_returns401() {
        ResponseEntity<Map<String, Object>> response = controller.handleHook(
                null,
                null,
                null,
                null,
                null,
                null,
                "{}",
                null
        );
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void calendarTrigger_missingSignature_returns401() {
        ResponseEntity<Map<String, Object>> response = controller.handleCalendarTrigger(
                null,
                null,
                null,
                null,
                null,
                null,
                "{}",
                null
        );
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void hook_validSignature_returns200() {
        String body = "{\"API_FAILED_SYNC\":[\"store_1_room_type_1\"]}";
        String signedHeaders = signBase64(TOKEN, "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String signedBody = signBase64(TOKEN, signedHeaders + body);

        ResponseEntity<Map<String, Object>> response = controller.handleHook(
                null,
                signedHeaders,
                signedBody,
                SOURCE,
                TIMESTAMP,
                REQUEST_ID,
                body,
                null
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void calendarTrigger_withListingId_callsListingSync() {
        String body = "{\"start_date\":\"2026-01-20\",\"end_date\":\"2026-01-25\",\"listing_id\":\"store_6_room_type_12\"}";
        String signedHeaders = signBase64(TOKEN, "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String signedBody = signBase64(TOKEN, signedHeaders + body);

        ResponseEntity<Map<String, Object>> response = controller.handleCalendarTrigger(
                null,
                signedHeaders,
                signedBody,
                SOURCE,
                TIMESTAMP,
                REQUEST_ID,
                body,
                null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Mockito.verify(priceLabsSyncService).syncCalendarForListingIds(
                Mockito.eq(List.of("store_6_room_type_12")),
                Mockito.any(),
                Mockito.any()
        );
    }

    @Test
    void sync_validSignature_returns200AndEnqueueAsyncJob() {
        String body = "{\"listing_ids\":[\"store_6_room_type_12\"]}";
        String signedHeaders = signBase64(TOKEN, "X-SOURCE:X-PL-TIMESTAMP:X-PL-REQUESTID");
        String signedBody = signBase64(TOKEN, signedHeaders + body);

        ResponseEntity<Map<String, Object>> response = controller.handlePriceSync(
                null,
                signedHeaders,
                signedBody,
                SOURCE,
                TIMESTAMP,
                REQUEST_ID,
                body,
                null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Mockito.verify(taskTracker).markPending(Mockito.anyString(), Mockito.eq("sync"));
        Mockito.verify(asyncProcessor).submit(Mockito.startsWith("sync-"), Mockito.any(Runnable.class));
    }

    private static String signBase64(String token, String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
