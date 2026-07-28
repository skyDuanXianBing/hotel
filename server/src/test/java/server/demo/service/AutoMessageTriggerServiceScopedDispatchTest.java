package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.entity.AutoMessage;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * dispatchStoreOnce(storeId, onlyReservationIds)：
 * - 传入预订 id 集合时只对集合内预订发送（webhook 入库后不再全门店扫描）；
 * - 传 null 保持原全量语义（定时 tick 与 E2E 用）。
 */
@ExtendWith(MockitoExtension.class)
class AutoMessageTriggerServiceScopedDispatchTest {

    private static final Long STORE_ID = 26L;
    private static final LocalDateTime EARLIEST_UTC = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AutoMessageRepository autoMessageRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SuBusinessAutoMessageService businessAutoMessageService;

    private AutoMessageTriggerService service;
    private Store store;
    private AutoMessage template;
    private Reservation inScopeReservation;
    private Reservation outOfScopeReservation;

    @BeforeEach
    void setUp() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("Asia/Shanghai"));

        store = new Store();
        store.setId(STORE_ID);

        template = new AutoMessage();
        template.setId(14L);
        template.setStoreId(STORE_ID);
        template.setAction("BOOKING_CONFIRM");
        template.setSendTiming("IMMEDIATELY");
        template.setEnabled(true);
        template.setCreatedAt(EARLIEST_UTC);

        inScopeReservation = new Reservation();
        inScopeReservation.setId(1594L);
        inScopeReservation.setStoreId(STORE_ID);
        inScopeReservation.setStatus(ReservationStatus.CONFIRMED);

        outOfScopeReservation = new Reservation();
        outOfScopeReservation.setId(123L);
        outOfScopeReservation.setStoreId(STORE_ID);
        outOfScopeReservation.setStatus(ReservationStatus.CONFIRMED);

        Clock clock = Clock.fixed(Instant.parse("2026-07-28T05:10:00Z"), ZoneOffset.UTC);
        service = new AutoMessageTriggerService(
                storeRepository,
                autoMessageRepository,
                reservationRepository,
                businessAutoMessageService,
                clock
        );

        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));
        when(autoMessageRepository.findByStoreIdAndEnabledTrue(STORE_ID)).thenReturn(List.of(template));
        lenient().when(businessAutoMessageService.computeEarliestEventTime(eq(template), any(LocalDateTime.class)))
                .thenReturn(EARLIEST_UTC);
        lenient().when(reservationRepository.findByStoreIdAndCreatedAtBetween(eq(STORE_ID), any(), any()))
                .thenReturn(List.of(outOfScopeReservation, inScopeReservation));
    }

    @AfterEach
    void tearDown() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(
                ZoneId.of(StoreTimeZoneUtil.DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE)
        );
    }

    @Test
    void dispatchStoreOnce_withScope_onlySendsForReservationsInScope() {
        service.dispatchStoreOnce(STORE_ID, Set.of(1594L));

        verify(businessAutoMessageService).trySendForReservation(
                eq(STORE_ID),
                same(inScopeReservation),
                same(template),
                any(LocalDateTime.class),
                eq(Duration.ZERO)
        );
        verify(businessAutoMessageService, never()).trySendForReservation(
                eq(STORE_ID),
                same(outOfScopeReservation),
                same(template),
                any(LocalDateTime.class),
                any(Duration.class)
        );
    }

    @Test
    void dispatchStoreOnce_withNullScope_keepsFullStoreDispatch() {
        service.dispatchStoreOnce(STORE_ID, null);

        verify(businessAutoMessageService).trySendForReservation(
                eq(STORE_ID),
                same(inScopeReservation),
                same(template),
                any(LocalDateTime.class),
                eq(Duration.ZERO)
        );
        verify(businessAutoMessageService).trySendForReservation(
                eq(STORE_ID),
                same(outOfScopeReservation),
                same(template),
                any(LocalDateTime.class),
                eq(Duration.ZERO)
        );
    }
}
