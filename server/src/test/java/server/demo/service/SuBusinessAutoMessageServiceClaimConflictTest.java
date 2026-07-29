package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 并发抢占唯一键 (store_id, action, target_type, target_id) 冲突时：
 * - trySendForReservation 必须正常返回、不向外抛异常（否则会毒化外层 webhook 事务）；
 * - 重读为空（REPEATABLE_READ 快照读不到对方提交）时直接跳过，不再 save / 重试插入；
 * - 重读到 success=TRUE 的记录时跳过；
 * - 重读到 WAITING_ 状态的记录时继续走发送流程，
 *   发送结果经 sendLogClaimService.updateResult（独立短事务）按 id 回写，不经外层事务 save。
 */
@ExtendWith(MockitoExtension.class)
class SuBusinessAutoMessageServiceClaimConflictTest {

    private static final Long STORE_ID = 26L;
    private static final Long RESERVATION_ID = 132L;
    private static final Long TEMPLATE_ID = 14L;
    private static final String SEND_LOG_ACTION = "AM:14";

    @Mock
    private AutoMessageSendLogRepository sendLogRepository;

    @Mock
    private AutoMessageSendLogClaimService sendLogClaimService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private SuMessageThreadRepository threadRepository;

    private SuBusinessAutoMessageService service;
    private Reservation reservation;
    private AutoMessage template;

    @BeforeEach
    void setUp() {
        service = new SuBusinessAutoMessageService(
                sendLogRepository,
                sendLogClaimService,
                storeRepository,
                org.mockito.Mockito.mock(ReservationRepository.class),
                org.mockito.Mockito.mock(RoomTypeRepository.class),
                org.mockito.Mockito.mock(RoomRepository.class),
                org.mockito.Mockito.mock(RoomGroupMemberRepository.class),
                threadRepository,
                org.mockito.Mockito.mock(SuMessageRepository.class),
                org.mockito.Mockito.mock(SuReservationWebhookEventRepository.class),
                org.mockito.Mockito.mock(SuApiClient.class),
                org.mockito.Mockito.mock(SuAccessTokenService.class),
                org.mockito.Mockito.mock(SuMessagingRealtimeGateway.class),
                new ObjectMapper(),
                new RegistrationLinkService("test-secret", 90),
                "http://localhost:8091/",
                "Auto Message"
        );

        Channel channel = new Channel();
        channel.setId(33L);
        channel.setStoreId(STORE_ID);
        channel.setCode("AIRBNB");

        reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
        reservation.setStoreId(STORE_ID);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setChannel(channel);
        reservation.setChannelOrderNumber("AIR-BOOKING-132");

        template = new AutoMessage();
        template.setId(TEMPLATE_ID);
        template.setStoreId(STORE_ID);
        template.setAction("BOOKING_CONFIRM");
        template.setSendTiming("IMMEDIATELY");
        template.setChannels("[33]");
        template.setRoomSelectionType("ALL_LOCAL");
        template.setMessage("Welcome");

        when(sendLogClaimService.insertClaim(anyLong(), anyString(), anyString(), anyLong(), anyLong()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry '26-AM:14-RESERVATION-132'"));
    }

    @Test
    void claimConflict_rereadEmpty_returnsWithoutSaveOrRetry() {
        when(sendLogRepository.findByStoreIdAndActionAndTargetTypeAndTargetId(
                STORE_ID, SEND_LOG_ACTION, "RESERVATION", RESERVATION_ID
        )).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.trySendForReservation(
                STORE_ID, reservation, template, LocalDateTime.of(2026, 7, 28, 5, 0), Duration.ZERO
        ));

        verify(sendLogRepository, never()).save(any(AutoMessageSendLog.class));
        verify(storeRepository, never()).findById(any());
    }

    @Test
    void claimConflict_rereadAlreadySucceeded_skipsSending() {
        AutoMessageSendLog succeeded = new AutoMessageSendLog();
        succeeded.setStoreId(STORE_ID);
        succeeded.setSuccess(true);
        when(sendLogRepository.findByStoreIdAndActionAndTargetTypeAndTargetId(
                STORE_ID, SEND_LOG_ACTION, "RESERVATION", RESERVATION_ID
        )).thenReturn(Optional.empty(), Optional.of(succeeded));

        assertDoesNotThrow(() -> service.trySendForReservation(
                STORE_ID, reservation, template, LocalDateTime.of(2026, 7, 28, 5, 0), Duration.ZERO
        ));

        verify(sendLogRepository, never()).save(any(AutoMessageSendLog.class));
        verify(storeRepository, never()).findById(any());
    }

    @Test
    void claimConflict_rereadWaitingRecord_continuesSendFlowWithExistingLog() {
        AutoMessageSendLog waiting = new AutoMessageSendLog();
        waiting.setId(555L);
        waiting.setStoreId(STORE_ID);
        waiting.setSuccess(false);
        waiting.setErrorMessage("WAITING_THREAD: thread not found; wait for webhook sync");
        when(sendLogRepository.findByStoreIdAndActionAndTargetTypeAndTargetId(
                STORE_ID, SEND_LOG_ACTION, "RESERVATION", RESERVATION_ID
        )).thenReturn(Optional.empty(), Optional.of(waiting));

        Store store = new Store();
        store.setId(STORE_ID);
        store.setSuHotelId("STORE26");
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));
        // AIRBNB 渠道找不到 thread 时不会创建 fallback，会走 markWaiting 分支。
        when(threadRepository.findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(
                eq(STORE_ID), eq(SuMessagingService.CHANNEL_AIRBNB), anyString()
        )).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.trySendForReservation(
                STORE_ID, reservation, template, LocalDateTime.of(2026, 7, 28, 5, 0), Duration.ZERO
        ));

        // 继续走了发送流程（本例中到达 markWaiting），结果经独立短事务按 id 回写到重读出的那条记录，
        // 不经外层事务 save（外层 save 在 REPEATABLE_READ 快照下会抛 StaleObjectStateException 拖垮整个 tick）。
        verify(sendLogClaimService).updateResult(
                eq(555L), eq(false), argThat(msg -> msg != null && msg.startsWith("WAITING_THREAD")));
        verify(sendLogRepository, never()).save(any(AutoMessageSendLog.class));
    }
}
