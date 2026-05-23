package server.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.enums.SuWebhookEventStatus;
import server.demo.enums.SuWebhookEventType;
import server.demo.service.ChannelE2ETestSupportService;
import server.demo.service.ChannelE2ETestSupportService.ChannelE2EReadinessResponse;
import server.demo.service.ChannelE2ETestSupportService.MessagingThreadLookupResponse;
import server.demo.service.ChannelE2ETestSupportService.ReservationLookupResponse;
import server.demo.service.ChannelE2ETestSupportService.SetupLocalResponse;
import server.demo.service.ChannelE2ETestSupportService.TestSupportAccessException;
import server.demo.service.ChannelE2ETestSupportService.WebhookEventLookupResponse;

@RestController
@RequestMapping("/api/v1/test-support/channel-e2e")
public class ChannelE2ETestSupportController extends BaseStoreController {

    private static final String TEST_SUPPORT_KEY_HEADER = "X-Test-Support-Key";

    private final ChannelE2ETestSupportService testSupportService;

    public ChannelE2ETestSupportController(ChannelE2ETestSupportService testSupportService) {
        this.testSupportService = testSupportService;
    }

    @PostMapping("/setup-local")
    public ResponseEntity<ApiResponse<SetupLocalResponse>> setupLocal(
            @RequestHeader(value = TEST_SUPPORT_KEY_HEADER, required = false) String testSupportKey
    ) {
        try {
            SetupLocalResponse response = testSupportService.setupLocal(testSupportKey);
            return ResponseEntity.ok(ApiResponse.success("本地渠道E2E基础数据已就绪", response));
        } catch (TestSupportAccessException e) {
            return accessDenied(e);
        }
    }

    @GetMapping("/readiness")
    @StoreScoped
    public ResponseEntity<ApiResponse<ChannelE2EReadinessResponse>> readiness(
            @RequestHeader(value = TEST_SUPPORT_KEY_HEADER, required = false) String testSupportKey
    ) {
        try {
            testSupportService.validateTestSupportAccess(testSupportKey);
            ChannelE2EReadinessResponse response = testSupportService.getReadiness(currentStoreId());
            return ResponseEntity.ok(ApiResponse.success("获取本地渠道E2E上下文成功", response));
        } catch (TestSupportAccessException e) {
            return accessDenied(e);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/context")
    @StoreScoped
    public ResponseEntity<ApiResponse<ChannelE2EReadinessResponse>> context(
            @RequestHeader(value = TEST_SUPPORT_KEY_HEADER, required = false) String testSupportKey
    ) {
        return readiness(testSupportKey);
    }

    @GetMapping("/reservations/lookup")
    @StoreScoped
    public ResponseEntity<ApiResponse<ReservationLookupResponse>> lookupReservations(
            @RequestHeader(value = TEST_SUPPORT_KEY_HEADER, required = false) String testSupportKey,
            @RequestParam(required = false) String reservationNotifId,
            @RequestParam(required = false) String suReservationId,
            @RequestParam(required = false) String channelOrderNumber,
            @RequestParam(required = false) String externalBookingKey,
            @RequestParam(required = false) String orderNumber
    ) {
        try {
            testSupportService.validateTestSupportAccess(testSupportKey);
            ReservationLookupResponse response = testSupportService.lookupReservations(
                    currentStoreId(),
                    reservationNotifId,
                    suReservationId,
                    channelOrderNumber,
                    externalBookingKey,
                    orderNumber
            );
            return ResponseEntity.ok(ApiResponse.success("查询本地渠道E2E订单成功", response));
        } catch (TestSupportAccessException e) {
            return accessDenied(e);
        }
    }

    @GetMapping("/webhook-events")
    @StoreScoped
    public ResponseEntity<ApiResponse<WebhookEventLookupResponse>> lookupWebhookEvents(
            @RequestHeader(value = TEST_SUPPORT_KEY_HEADER, required = false) String testSupportKey,
            @RequestParam(required = false) String hotelId,
            @RequestParam(required = false) String reservationNotifId,
            @RequestParam(required = false) SuWebhookEventStatus status,
            @RequestParam(required = false) SuWebhookEventType eventType,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            testSupportService.validateTestSupportAccess(testSupportKey);
            WebhookEventLookupResponse response = testSupportService.lookupWebhookEvents(
                    currentStoreId(),
                    hotelId,
                    reservationNotifId,
                    status,
                    eventType,
                    limit
            );
            return ResponseEntity.ok(ApiResponse.success("查询本地渠道E2E webhook event成功", response));
        } catch (TestSupportAccessException e) {
            return accessDenied(e);
        }
    }

    @GetMapping("/messaging/threads")
    @StoreScoped
    public ResponseEntity<ApiResponse<MessagingThreadLookupResponse>> lookupMessagingThreads(
            @RequestHeader(value = TEST_SUPPORT_KEY_HEADER, required = false) String testSupportKey,
            @RequestParam(required = false) Integer channelId,
            @RequestParam(required = false) String threadId,
            @RequestParam(required = false) String bookingId,
            @RequestParam(required = false) String externalMessageId,
            @RequestParam(required = false) Long messageId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer messageLimit
    ) {
        try {
            testSupportService.validateTestSupportAccess(testSupportKey);
            MessagingThreadLookupResponse response = testSupportService.lookupMessagingThreads(
                    currentStoreId(),
                    channelId,
                    threadId,
                    bookingId,
                    externalMessageId,
                    messageId,
                    limit,
                    messageLimit
            );
            return ResponseEntity.ok(ApiResponse.success("查询本地渠道E2E messaging threads成功", response));
        } catch (TestSupportAccessException e) {
            return accessDenied(e);
        }
    }

    private <T> ResponseEntity<ApiResponse<T>> accessDenied(TestSupportAccessException e) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        if (e.getStatusCode() == 400) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(ApiResponse.error(e.getMessage()));
    }
}
