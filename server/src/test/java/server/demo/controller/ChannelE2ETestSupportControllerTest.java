package server.demo.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.service.ChannelE2ETestSupportService;
import server.demo.service.ChannelE2ETestSupportService.AutoMessageDispatchResponse;
import server.demo.service.ChannelE2ETestSupportService.AutoMessageSendLogLookupQuery;
import server.demo.service.ChannelE2ETestSupportService.AutoMessageSendLogLookupResponse;
import server.demo.service.ChannelE2ETestSupportService.ChannelE2EReadinessResponse;
import server.demo.service.ChannelE2ETestSupportService.MessagingThreadLookupQuery;
import server.demo.service.ChannelE2ETestSupportService.MessagingThreadLookupResponse;
import server.demo.service.ChannelE2ETestSupportService.ReservationLookupQuery;
import server.demo.service.ChannelE2ETestSupportService.ReservationLookupResponse;
import server.demo.service.ChannelE2ETestSupportService.TestSupportAccessException;
import server.demo.service.ChannelE2ETestSupportService.WebhookConfigSummary;
import server.demo.service.ChannelE2ETestSupportService.WebhookEventLookupQuery;
import server.demo.service.ChannelE2ETestSupportService.WebhookEventLookupResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelE2ETestSupportControllerTest {

    private static final Long STORE_ID = 77L;
    private static final String VALID_KEY = "valid-key";

    @Mock
    private ChannelE2ETestSupportService testSupportService;

    private ChannelE2ETestSupportController controller;

    @BeforeEach
    void setUp() {
        controller = new ChannelE2ETestSupportController(testSupportService);
        StoreContextHolder.setContext(new StoreContext(9L, STORE_ID, "owner"));
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @ParameterizedTest
    @EnumSource(ReadEndpoint.class)
    void readEndpoint_missingTestSupportKeyReturnsBadRequestAndSkipsBusinessRead(ReadEndpoint endpoint) {
        whenMissingKeyIsRejected();

        ResponseEntity<? extends ApiResponse<?>> response = callReadEndpoint(endpoint, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("缺少 X-Test-Support-Key", response.getBody().getMessage());
        verify(testSupportService).validateTestSupportAccess(null);
        verifyBusinessReadWasSkipped(endpoint);
    }

    @ParameterizedTest
    @EnumSource(ReadEndpoint.class)
    void readEndpoint_wrongTestSupportKeyReturnsForbiddenAndSkipsBusinessRead(ReadEndpoint endpoint) {
        whenWrongKeyIsRejected();

        ResponseEntity<? extends ApiResponse<?>> response = callReadEndpoint(endpoint, "wrong-key");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("X-Test-Support-Key 不匹配", response.getBody().getMessage());
        verify(testSupportService).validateTestSupportAccess("wrong-key");
        verifyBusinessReadWasSkipped(endpoint);
    }

    @ParameterizedTest
    @EnumSource(ReadEndpoint.class)
    void readEndpoint_validTestSupportKeyRunsBusinessRead(ReadEndpoint endpoint) {
        stubBusinessRead(endpoint);

        ResponseEntity<? extends ApiResponse<?>> response = callReadEndpoint(endpoint, VALID_KEY);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(testSupportService).validateTestSupportAccess(VALID_KEY);
        verifyBusinessReadWasCalled(endpoint);
    }

    @Test
    void dispatchAutoMessages_missingTestSupportKeyReturnsBadRequestAndSkipsDispatch() {
        whenMissingKeyIsRejected();

        ResponseEntity<? extends ApiResponse<?>> response = controller.dispatchAutoMessages(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("缺少 X-Test-Support-Key", response.getBody().getMessage());
        verify(testSupportService).validateTestSupportAccess(null);
        verify(testSupportService, never()).dispatchAutoMessages(anyLong());
    }

    @Test
    void dispatchAutoMessages_wrongTestSupportKeyReturnsForbiddenAndSkipsDispatch() {
        whenWrongKeyIsRejected();

        ResponseEntity<? extends ApiResponse<?>> response = controller.dispatchAutoMessages("wrong-key");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("X-Test-Support-Key 不匹配", response.getBody().getMessage());
        verify(testSupportService).validateTestSupportAccess("wrong-key");
        verify(testSupportService, never()).dispatchAutoMessages(anyLong());
    }

    @Test
    void dispatchAutoMessages_validTestSupportKeyRunsCurrentStoreDispatch() {
        when(testSupportService.dispatchAutoMessages(STORE_ID))
                .thenReturn(new AutoMessageDispatchResponse(STORE_ID, true));

        ResponseEntity<? extends ApiResponse<?>> response = controller.dispatchAutoMessages(VALID_KEY);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(testSupportService).validateTestSupportAccess(VALID_KEY);
        verify(testSupportService).dispatchAutoMessages(STORE_ID);
    }

    @Test
    void setupLocal_reservedCleanerIdentityConflictReturnsConflictEnvelope() {
        when(testSupportService.setupLocal(VALID_KEY))
                .thenThrow(new IllegalStateException("本地 E2E 保洁测试身份冲突：保留测试用户已停用"));

        ResponseEntity<? extends ApiResponse<?>> response = controller.setupLocal(VALID_KEY);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("保洁测试身份冲突"));
    }

    private void whenMissingKeyIsRejected() {
        TestSupportAccessException exception = new TestSupportAccessException(400, "缺少 X-Test-Support-Key");
        org.mockito.Mockito.doThrow(exception).when(testSupportService).validateTestSupportAccess(null);
    }

    private void whenWrongKeyIsRejected() {
        TestSupportAccessException exception = new TestSupportAccessException(403, "X-Test-Support-Key 不匹配");
        org.mockito.Mockito.doThrow(exception).when(testSupportService).validateTestSupportAccess("wrong-key");
    }

    private ResponseEntity<? extends ApiResponse<?>> callReadEndpoint(ReadEndpoint endpoint, String testSupportKey) {
        if (endpoint == ReadEndpoint.READINESS) {
            return controller.readiness(testSupportKey);
        }
        if (endpoint == ReadEndpoint.CONTEXT) {
            return controller.context(testSupportKey);
        }
        if (endpoint == ReadEndpoint.RESERVATIONS_LOOKUP) {
            return controller.lookupReservations(testSupportKey, null, null, null, null, null);
        }
        if (endpoint == ReadEndpoint.WEBHOOK_EVENTS) {
            return controller.lookupWebhookEvents(testSupportKey, null, null, null, null, null);
        }
        if (endpoint == ReadEndpoint.MESSAGING_THREADS) {
            return controller.lookupMessagingThreads(testSupportKey, null, null, null, null, null, null, null);
        }
        return controller.lookupAutoMessageSendLogs(testSupportKey, null, null, null, null);
    }

    private void stubBusinessRead(ReadEndpoint endpoint) {
        if (endpoint == ReadEndpoint.READINESS || endpoint == ReadEndpoint.CONTEXT) {
            when(testSupportService.getReadiness(STORE_ID)).thenReturn(buildReadinessResponse());
            return;
        }
        if (endpoint == ReadEndpoint.RESERVATIONS_LOOKUP) {
            when(testSupportService.lookupReservations(STORE_ID, null, null, null, null, null))
                    .thenReturn(buildReservationLookupResponse());
            return;
        }
        if (endpoint == ReadEndpoint.WEBHOOK_EVENTS) {
            when(testSupportService.lookupWebhookEvents(STORE_ID, null, null, null, null, null))
                    .thenReturn(buildWebhookEventLookupResponse());
            return;
        }
        if (endpoint == ReadEndpoint.MESSAGING_THREADS) {
            when(testSupportService.lookupMessagingThreads(STORE_ID, null, null, null, null, null, null, null))
                    .thenReturn(buildMessagingThreadLookupResponse());
            return;
        }
        when(testSupportService.lookupAutoMessageSendLogs(STORE_ID, null, null, null, null))
                .thenReturn(buildAutoMessageSendLogLookupResponse());
    }

    private void verifyBusinessReadWasSkipped(ReadEndpoint endpoint) {
        if (endpoint == ReadEndpoint.READINESS || endpoint == ReadEndpoint.CONTEXT) {
            verify(testSupportService, never()).getReadiness(anyLong());
            return;
        }
        if (endpoint == ReadEndpoint.RESERVATIONS_LOOKUP) {
            verify(testSupportService, never()).lookupReservations(
                    anyLong(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull()
            );
            return;
        }
        if (endpoint == ReadEndpoint.WEBHOOK_EVENTS) {
            verify(testSupportService, never()).lookupWebhookEvents(
                    anyLong(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull()
            );
            return;
        }
        if (endpoint == ReadEndpoint.MESSAGING_THREADS) {
            verify(testSupportService, never())
                    .lookupMessagingThreads(
                            anyLong(),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull(),
                            isNull()
                    );
            return;
        }
        verify(testSupportService, never()).lookupAutoMessageSendLogs(
                anyLong(),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        );
    }

    private void verifyBusinessReadWasCalled(ReadEndpoint endpoint) {
        if (endpoint == ReadEndpoint.READINESS || endpoint == ReadEndpoint.CONTEXT) {
            verify(testSupportService).getReadiness(STORE_ID);
            return;
        }
        if (endpoint == ReadEndpoint.RESERVATIONS_LOOKUP) {
            verify(testSupportService).lookupReservations(STORE_ID, null, null, null, null, null);
            return;
        }
        if (endpoint == ReadEndpoint.WEBHOOK_EVENTS) {
            verify(testSupportService).lookupWebhookEvents(STORE_ID, null, null, null, null, null);
            return;
        }
        if (endpoint == ReadEndpoint.MESSAGING_THREADS) {
            verify(testSupportService).lookupMessagingThreads(STORE_ID, null, null, null, null, null, null, null);
            return;
        }
        verify(testSupportService).lookupAutoMessageSendLogs(STORE_ID, null, null, null, null);
    }

    private ChannelE2EReadinessResponse buildReadinessResponse() {
        WebhookConfigSummary webhookConfig = new WebhookConfigSummary(
                true,
                "http://localhost:8092",
                "http://localhost:8092/api/v1/su/webhook/reservation-notif/TEST"
        );
        return new ChannelE2EReadinessResponse(
                STORE_ID,
                "Local Channel E2E Hotel",
                "Asia/Tokyo",
                "TEST",
                List.of("BOOKING", "AIRBNB"),
                true,
                List.of(),
                webhookConfig,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private ReservationLookupResponse buildReservationLookupResponse() {
        ReservationLookupQuery query = new ReservationLookupQuery(null, null, null, null, null);
        return new ReservationLookupResponse(query, 0, List.of());
    }

    private WebhookEventLookupResponse buildWebhookEventLookupResponse() {
        WebhookEventLookupQuery query = new WebhookEventLookupQuery(null, null, null, null, 50);
        return new WebhookEventLookupResponse(query, 0, List.of());
    }

    private MessagingThreadLookupResponse buildMessagingThreadLookupResponse() {
        MessagingThreadLookupQuery query = new MessagingThreadLookupQuery(null, null, null, null, null, 20, 20);
        return new MessagingThreadLookupResponse(query, 0, List.of());
    }

    private AutoMessageSendLogLookupResponse buildAutoMessageSendLogLookupResponse() {
        AutoMessageSendLogLookupQuery query = new AutoMessageSendLogLookupQuery(
                "RESERVATION",
                null,
                null,
                null,
                20
        );
        return new AutoMessageSendLogLookupResponse(query, 0, List.of());
    }

    private enum ReadEndpoint {
        READINESS,
        CONTEXT,
        RESERVATIONS_LOOKUP,
        WEBHOOK_EVENTS,
        MESSAGING_THREADS,
        AUTO_MESSAGE_SEND_LOGS
    }
}
