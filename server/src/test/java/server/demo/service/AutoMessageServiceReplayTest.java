package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.AutoMessage;
import server.demo.entity.Reservation;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.ReservationRepository;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 手动重放契约：
 * 1. 发送日志的"重置为等待重发"必须经 AutoMessageSendLogClaimService（独立短事务）完成；
 * 2. replayAutoMessage 自身不能有外层事务——否则外层 REPEATABLE_READ 快照读不到重置行
 *    （走插入冲突分支静默跳过），或外层持行锁让 REQUIRES_NEW 结果回写锁等待超时，
 *    最终导致重放无效或下一分钟被调度器补发第二次。
 */
class AutoMessageServiceReplayTest {

    private static final Long STORE_ID = 26L;
    private static final Long RESERVATION_ID = 800L;
    private static final Long TEMPLATE_ID = 13L;

    private AutoMessageRepository autoMessageRepository;
    private ReservationRepository reservationRepository;
    private AutoMessageSendLogClaimService sendLogClaimService;
    private SuBusinessAutoMessageService businessAutoMessageService;

    private AutoMessageService service;
    private Reservation reservation;
    private AutoMessage template;

    @BeforeEach
    void setUp() {
        autoMessageRepository = mock(AutoMessageRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        sendLogClaimService = mock(AutoMessageSendLogClaimService.class);
        businessAutoMessageService = mock(SuBusinessAutoMessageService.class);

        service = new AutoMessageService();
        ReflectionTestUtils.setField(service, "autoMessageRepository", autoMessageRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "autoMessageSendLogClaimService", sendLogClaimService);
        ReflectionTestUtils.setField(service, "suBusinessAutoMessageService", businessAutoMessageService);
        ReflectionTestUtils.setField(service, "clock", Clock.systemUTC());

        reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
        reservation.setStoreId(STORE_ID);

        template = new AutoMessage();
        template.setId(TEMPLATE_ID);
        template.setStoreId(STORE_ID);

        when(reservationRepository.findByStoreIdAndIdWithRoomType(STORE_ID, RESERVATION_ID))
                .thenReturn(Optional.of(reservation));
        when(autoMessageRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        StoreContextHolder.setContext(new StoreContext(1L, STORE_ID, "OWNER"));
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void replay_resetsLogViaClaimService_thenTriggersSend() {
        service.replayAutoMessage(RESERVATION_ID, TEMPLATE_ID);

        verify(sendLogClaimService).resetForResend(
                eq(STORE_ID),
                eq("AM:" + TEMPLATE_ID),
                eq("RESERVATION"),
                eq(RESERVATION_ID),
                eq(TEMPLATE_ID),
                argThat(msg -> msg != null && msg.startsWith("WAITING_MANUAL_REPLAY"))
        );
        verify(businessAutoMessageService).trySendForReservation(
                eq(STORE_ID), same(reservation), same(template), org.mockito.ArgumentMatchers.any(), eq(Duration.ZERO)
        );
    }

    @Test
    void replay_mustNotDeclareOuterTransaction() throws Exception {
        // 若重新加回 @Transactional，重置写入与 REQUIRES_NEW 结果回写会重新产生锁/快照冲突。
        Method method = AutoMessageService.class.getMethod("replayAutoMessage", Long.class, Long.class);
        assertNull(method.getAnnotation(Transactional.class),
                "replayAutoMessage 不能声明 @Transactional，见方法注释说明");
    }
}
