package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.ReservationStatus;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomGroupMemberRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.SuReservationWebhookEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 生产事故回归（2026-07-29，订单 HM8KD38ZDH 客人被同一条入住提醒刷屏 12 次）。
 *
 * 根因：发送成功的标记写在外层 tick 长事务里；REQUIRES_NEW 插入的抢占行对
 * REPEATABLE_READ 快照不可见，save detached 实体抛 StaleObjectStateException
 * 并拖垮整个 tick 事务，success 永远落不了库，导致每分钟重发。
 *
 * 修复契约：发送结果一律经 AutoMessageSendLogClaimService.updateResult（独立短事务）
 * 按 id 回写；外层事务对 auto_message_send_logs 只读不写。
 */
class SuBusinessAutoMessageServiceSendResultTest {

    private static final Long STORE_ID = 26L;
    private static final Long RESERVATION_ID = 800L;
    private static final Long TEMPLATE_ID = 13L;
    private static final Long SEND_LOG_ID = 4315L;
    private static final String SEND_LOG_ACTION = "AM:13";

    private AutoMessageSendLogRepository sendLogRepository;
    private AutoMessageSendLogClaimService sendLogClaimService;
    private StoreRepository storeRepository;
    private SuMessageThreadRepository threadRepository;
    private SuMessageRepository messageRepository;
    private SuApiClient suApiClient;
    private SuAccessTokenService suAccessTokenService;

    private SuBusinessAutoMessageService service;
    private Reservation reservation;
    private AutoMessage template;

    @BeforeEach
    void setUp() throws Exception {
        sendLogRepository = mock(AutoMessageSendLogRepository.class);
        sendLogClaimService = mock(AutoMessageSendLogClaimService.class);
        storeRepository = mock(StoreRepository.class);
        threadRepository = mock(SuMessageThreadRepository.class);
        messageRepository = mock(SuMessageRepository.class);
        suApiClient = mock(SuApiClient.class);
        suAccessTokenService = mock(SuAccessTokenService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        service = new SuBusinessAutoMessageService(
                sendLogRepository,
                sendLogClaimService,
                storeRepository,
                mock(ReservationRepository.class),
                mock(RoomTypeRepository.class),
                mock(RoomRepository.class),
                mock(RoomGroupMemberRepository.class),
                threadRepository,
                messageRepository,
                mock(SuReservationWebhookEventRepository.class),
                suApiClient,
                suAccessTokenService,
                mock(SuMessagingRealtimeGateway.class),
                objectMapper,
                new RegistrationLinkService("test-secret", 90),
                "http://localhost:8091/",
                "Auto Message"
        );

        Channel channel = new Channel();
        channel.setId(381L);
        channel.setStoreId(STORE_ID);
        channel.setCode("AIRBNB");

        reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
        reservation.setStoreId(STORE_ID);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setChannel(channel);
        reservation.setGuestName("Yuka Kamehama");
        reservation.setOrderNumber("ORDER-800");
        reservation.setChannelOrderNumber("HM8KD38ZDH");

        template = new AutoMessage();
        template.setId(TEMPLATE_ID);
        template.setStoreId(STORE_ID);
        template.setAction("CHECK_IN");
        template.setSendTiming("DAY_-3_11:00");
        template.setChannels("[381]");
        template.setRoomSelectionType("ALL_LOCAL");
        template.setMessage("Dear {{guest_name}}, check-in link: {{registration_link}}");

        Store store = new Store();
        store.setId(STORE_ID);
        store.setName("Local Hotel");

        SuMessageThread thread = new SuMessageThread();
        thread.setId(77L);
        thread.setStoreId(STORE_ID);
        thread.setSuHotelId("STORE26");
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setThreadKey("T77");
        thread.setThreadId("T77");
        thread.setGuestId("G77");
        thread.setBookingId("HM8KD38ZDH");
        thread.setListingId("LISTING77");
        thread.setClosed(false);

        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));
        when(threadRepository.findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(
                STORE_ID, SuMessagingService.CHANNEL_AIRBNB, "HM8KD38ZDH"
        )).thenReturn(Optional.of(thread));
        when(threadRepository.save(any(SuMessageThread.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.saveAndFlush(any(SuMessage.class))).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(404L);
            return message;
        });
        when(messageRepository.save(any(SuMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(suAccessTokenService.executeWithTokenRetry(any(), eq("messagingAB"))).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> action = (Function<String, Object>) inv.getArgument(0);
            return action.apply("token");
        });
        when(sendLogClaimService.insertClaim(anyLong(), anyString(), anyString(), anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    AutoMessageSendLog claimed = new AutoMessageSendLog();
                    claimed.setId(SEND_LOG_ID);
                    claimed.setStoreId(inv.getArgument(0));
                    claimed.setAction(inv.getArgument(1));
                    claimed.setTargetType(inv.getArgument(2));
                    claimed.setTargetId(inv.getArgument(3));
                    claimed.setAutoMessageId(inv.getArgument(4));
                    return claimed;
                });
    }

    @Test
    void sendSuccess_marksResultViaClaimService_andNeverSavesLogViaOuterRepository() throws Exception {
        stubNoExistingLog();
        stubSuSendSuccess();

        invoke();

        verify(suApiClient, times(1)).postMessagingAB(anyString(), any(Map.class));
        verify(sendLogClaimService).updateResult(SEND_LOG_ID, true, null);
        // 外层 tick 长事务不得再写发送日志表（否则会重演 StaleObjectStateException 拖垮整单事务）
        verify(sendLogRepository, never()).save(any(AutoMessageSendLog.class));
    }

    @Test
    void sendSuccess_whenClaimRowCommittedButResultLost_sendsOnceAndMarksReliably() throws Exception {
        // 生产事故场景：抢占行已被 REQUIRES_NEW 提交，但外层事务回滚导致 success 丢失（NULL），
        // 重发守卫对 NULL 放行会再发一次；这次发送的结果必须经独立短事务可靠落库，循环才能终止。
        stubExistingLog(SEND_LOG_ID, null, null);
        stubSuSendSuccess();

        invoke();

        verify(sendLogClaimService, never()).insertClaim(anyLong(), anyString(), anyString(), anyLong(), anyLong());
        verify(suApiClient, times(1)).postMessagingAB(anyString(), any(Map.class));
        verify(sendLogClaimService).updateResult(SEND_LOG_ID, true, null);
        verify(sendLogRepository, never()).save(any(AutoMessageSendLog.class));
    }

    @Test
    void existingSucceeded_skipsSend() {
        stubExistingLog(SEND_LOG_ID, true, null);

        invoke();

        verify(suApiClient, never()).postMessagingAB(anyString(), any(Map.class));
        verify(sendLogClaimService, never()).insertClaim(anyLong(), anyString(), anyString(), anyLong(), anyLong());
        verify(sendLogClaimService, never()).updateResult(any(), any(), any());
    }

    @Test
    void existingFailedNonWaiting_skipsSend() {
        stubExistingLog(SEND_LOG_ID, false, "SU_ERR[code=UNKNOWN]: Su message send failed");

        invoke();

        verify(suApiClient, never()).postMessagingAB(anyString(), any(Map.class));
        verify(sendLogClaimService, never()).updateResult(any(), any(), any());
    }

    @Test
    void existingWaiting_resendsAndMarksSuccess() throws Exception {
        stubExistingLog(SEND_LOG_ID, false, "WAITING_THREAD: thread not found; wait for webhook sync");
        stubSuSendSuccess();

        invoke();

        verify(suApiClient, times(1)).postMessagingAB(anyString(), any(Map.class));
        verify(sendLogClaimService).updateResult(SEND_LOG_ID, true, null);
    }

    @Test
    void sendFailsRecoverable_marksWaitingViaClaimService() throws Exception {
        stubNoExistingLog();
        stubSuSendFailure("invalid bookingid");

        assertDoesNotThrow(this::invoke);

        verify(sendLogClaimService).updateResult(
                eq(SEND_LOG_ID), eq(false), argThat(msg -> msg != null && msg.startsWith("WAITING_THREAD_FIELDS")));
        verify(sendLogRepository, never()).save(any(AutoMessageSendLog.class));
    }

    @Test
    void sendFailsNonRecoverable_marksFailedViaClaimService() throws Exception {
        stubNoExistingLog();
        stubSuSendFailure("Su message send failed");

        assertDoesNotThrow(this::invoke);

        verify(sendLogClaimService).updateResult(
                eq(SEND_LOG_ID), eq(false), argThat(msg -> msg != null && msg.startsWith("SU_ERR")));
        verify(sendLogRepository, never()).save(any(AutoMessageSendLog.class));
    }

    @Test
    void resultWriteFailure_doesNotInterruptDispatch() throws Exception {
        stubNoExistingLog();
        stubSuSendSuccess();
        doThrow(new RuntimeException("db down"))
                .when(sendLogClaimService).updateResult(any(), any(), any());

        assertDoesNotThrow(this::invoke);

        verify(suApiClient, times(1)).postMessagingAB(anyString(), any(Map.class));
    }

    private void invoke() {
        service.trySendForReservation(
                STORE_ID, reservation, template,
                LocalDateTime.of(2026, 7, 29, 11, 1), Duration.ZERO
        );
    }

    private void stubNoExistingLog() {
        when(sendLogRepository.findByStoreIdAndActionAndTargetTypeAndTargetId(
                STORE_ID, SEND_LOG_ACTION, "RESERVATION", RESERVATION_ID
        )).thenReturn(Optional.empty());
    }

    private void stubExistingLog(Long id, Boolean success, String errorMessage) {
        AutoMessageSendLog log = new AutoMessageSendLog();
        log.setId(id);
        log.setStoreId(STORE_ID);
        log.setAction(SEND_LOG_ACTION);
        log.setTargetType("RESERVATION");
        log.setTargetId(RESERVATION_ID);
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        when(sendLogRepository.findByStoreIdAndActionAndTargetTypeAndTargetId(
                STORE_ID, SEND_LOG_ACTION, "RESERVATION", RESERVATION_ID
        )).thenReturn(Optional.of(log));
    }

    private void stubSuSendSuccess() throws Exception {
        JsonNode ok = new ObjectMapper().readTree("{\"Status\":\"Success\"}");
        when(suApiClient.postMessagingAB(anyString(), any(Map.class))).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);
    }

    private void stubSuSendFailure(String suErrorMessage) throws Exception {
        JsonNode fail = new ObjectMapper().readTree("{\"Status\":\"Fail\"}");
        when(suApiClient.postMessagingAB(anyString(), any(Map.class))).thenReturn(fail);
        when(suApiClient.isSuSuccess(fail)).thenReturn(false);
        when(suApiClient.extractSuErrorMessage(fail)).thenReturn(suErrorMessage);
        when(suApiClient.extractSuErrorCode(fail)).thenReturn(null);
    }
}
