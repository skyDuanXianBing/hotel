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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoMessageTriggerServiceTest {

    private static final Long STORE_ID = 26L;
    private static final LocalDateTime EARLIEST_UTC = LocalDateTime.of(2026, 4, 1, 0, 0);

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AutoMessageRepository autoMessageRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SuBusinessAutoMessageService businessAutoMessageService;

    private Store store;
    private AutoMessage template;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("Asia/Shanghai"));

        store = new Store();
        store.setId(STORE_ID);

        template = new AutoMessage();
        template.setId(14L);
        template.setStoreId(STORE_ID);
        template.setAction("CHECK_IN");
        template.setSendTiming("DAY_-1_14:00");
        template.setEnabled(true);
        template.setCreatedAt(EARLIEST_UTC);

        reservation = new Reservation();
        reservation.setId(116L);
        reservation.setStoreId(STORE_ID);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCheckInDate(LocalDate.of(2026, 4, 9));

        when(autoMessageRepository.findByStoreIdAndEnabledTrue(STORE_ID)).thenReturn(List.of(template));
        lenient().when(reservationRepository.findByStoreIdAndCheckInDateBetween(eq(STORE_ID), any(), any()))
                .thenReturn(List.of(reservation));
        lenient().when(businessAutoMessageService.computeEarliestEventTime(eq(template), any(LocalDateTime.class)))
                .thenReturn(EARLIEST_UTC);
    }

    @AfterEach
    void tearDown() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(
                ZoneId.of(StoreTimeZoneUtil.DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE)
        );
    }

    @Test
    void tick_shouldDispatchDayTemplateAtTokyoLocalScheduledTime() {
        store.setTimezone("Asia/Tokyo");
        Clock clock = Clock.fixed(Instant.parse("2026-04-08T05:00:00Z"), ZoneOffset.UTC);
        AutoMessageTriggerService service = new AutoMessageTriggerService(
                storeRepository,
                autoMessageRepository,
                reservationRepository,
                businessAutoMessageService,
                clock
        );

        when(storeRepository.findAll()).thenReturn(List.of(store));

        service.tick();

        verify(businessAutoMessageService).trySendForReservation(
                eq(STORE_ID),
                same(reservation),
                same(template),
                eq(LocalDateTime.of(2026, 4, 8, 5, 0)),
                eq(Duration.ZERO)
        );
    }

    @Test
    void tick_shouldNotDispatchBeforeTokyoLocalScheduledTime() {
        store.setTimezone("Asia/Tokyo");
        Clock clock = Clock.fixed(Instant.parse("2026-04-08T04:59:00Z"), ZoneOffset.UTC);
        AutoMessageTriggerService service = new AutoMessageTriggerService(
                storeRepository,
                autoMessageRepository,
                reservationRepository,
                businessAutoMessageService,
                clock
        );

        when(storeRepository.findAll()).thenReturn(List.of(store));

        service.tick();

        verify(businessAutoMessageService, never()).trySendForReservation(
                eq(STORE_ID),
                same(reservation),
                same(template),
                any(LocalDateTime.class),
                eq(Duration.ZERO)
        );
    }

    @Test
    void tick_shouldRespectDifferentStoreTimezoneAtSameInstant() {
        store.setTimezone("Asia/Shanghai");
        Clock clock = Clock.fixed(Instant.parse("2026-04-08T05:00:00Z"), ZoneOffset.UTC);
        AutoMessageTriggerService service = new AutoMessageTriggerService(
                storeRepository,
                autoMessageRepository,
                reservationRepository,
                businessAutoMessageService,
                clock
        );

        when(storeRepository.findAll()).thenReturn(List.of(store));

        service.tick();

        verify(businessAutoMessageService, never()).trySendForReservation(
                eq(STORE_ID),
                same(reservation),
                same(template),
                any(LocalDateTime.class),
                eq(Duration.ZERO)
        );
    }

    @Test
    void tick_shouldFallbackToDefaultTimezoneWhenStoreTimezoneInvalid() {
        store.setTimezone("bad/timezone");
        Clock clock = Clock.fixed(Instant.parse("2026-04-08T06:00:00Z"), ZoneOffset.UTC);
        AutoMessageTriggerService service = new AutoMessageTriggerService(
                storeRepository,
                autoMessageRepository,
                reservationRepository,
                businessAutoMessageService,
                clock
        );

        when(storeRepository.findAll()).thenReturn(List.of(store));

        service.tick();

        verify(businessAutoMessageService).trySendForReservation(
                eq(STORE_ID),
                same(reservation),
                same(template),
                eq(LocalDateTime.of(2026, 4, 8, 6, 0)),
                eq(Duration.ZERO)
        );
    }

    @Test
    void tick_shouldQueryBookingConfirmCandidatesUsingReservationStorageWindow() {
        store.setTimezone("Asia/Tokyo");
        template.setAction("BOOKING_CONFIRM");
        template.setSendTiming("IMMEDIATELY");
        reservation.setCreatedAt(LocalDateTime.of(2026, 6, 1, 9, 58));
        Clock clock = Clock.fixed(Instant.parse("2026-06-01T02:00:00Z"), ZoneOffset.UTC);
        AutoMessageTriggerService service = new AutoMessageTriggerService(
                storeRepository,
                autoMessageRepository,
                reservationRepository,
                businessAutoMessageService,
                clock
        );

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(businessAutoMessageService.computeEarliestEventTime(eq(template), any(LocalDateTime.class)))
                .thenReturn(LocalDateTime.of(2026, 6, 1, 1, 55));
        when(reservationRepository.findByStoreIdAndCreatedAtBetween(
                STORE_ID,
                LocalDateTime.of(2026, 6, 1, 9, 55),
                LocalDateTime.of(2026, 6, 1, 10, 0)
        )).thenReturn(List.of(reservation));

        service.tick();

        verify(reservationRepository).findByStoreIdAndCreatedAtBetween(
                STORE_ID,
                LocalDateTime.of(2026, 6, 1, 9, 55),
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );
        verify(businessAutoMessageService).trySendForReservation(
                eq(STORE_ID),
                same(reservation),
                same(template),
                eq(LocalDateTime.of(2026, 6, 1, 2, 0)),
                eq(Duration.ZERO)
        );
    }

    @Test
    void tick_shouldUseTemplateCreatedAtAsReservationStorageTimeForBookingConfirmEarliest() {
        store.setTimezone("Asia/Tokyo");
        template.setAction("BOOKING_CONFIRM");
        template.setSendTiming("IMMEDIATELY");
        template.setCreatedAt(LocalDateTime.of(2026, 6, 1, 20, 40, 43, 937847000));
        reservation.setCreatedAt(LocalDateTime.of(2026, 6, 1, 20, 40, 44, 216989000));

        SuBusinessAutoMessageService realEarliestBusinessService = mock(SuBusinessAutoMessageService.class);
        when(realEarliestBusinessService.computeEarliestEventTime(eq(template), any(LocalDateTime.class)))
                .thenCallRealMethod();

        Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:41:00Z"), ZoneOffset.UTC);
        AutoMessageTriggerService service = new AutoMessageTriggerService(
                storeRepository,
                autoMessageRepository,
                reservationRepository,
                realEarliestBusinessService,
                clock
        );

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(reservationRepository.findByStoreIdAndCreatedAtBetween(
                STORE_ID,
                LocalDateTime.of(2026, 6, 1, 20, 40, 43, 937847000),
                LocalDateTime.of(2026, 6, 1, 20, 41)
        )).thenReturn(List.of(reservation));

        service.tick();

        verify(reservationRepository).findByStoreIdAndCreatedAtBetween(
                STORE_ID,
                LocalDateTime.of(2026, 6, 1, 20, 40, 43, 937847000),
                LocalDateTime.of(2026, 6, 1, 20, 41)
        );
        verify(realEarliestBusinessService).trySendForReservation(
                eq(STORE_ID),
                same(reservation),
                same(template),
                eq(LocalDateTime.of(2026, 6, 1, 12, 41)),
                eq(Duration.ZERO)
        );
    }

    @Test
    void tick_shouldQueryCheckInDelayCandidatesUsingReservationStorageWindow() {
        store.setTimezone("Asia/Tokyo");
        template.setAction("CHECK_IN");
        template.setSendTiming("5_MIN");
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setActualCheckIn(LocalDateTime.of(2026, 6, 1, 10, 3));
        Clock clock = Clock.fixed(Instant.parse("2026-06-01T02:10:00Z"), ZoneOffset.UTC);
        AutoMessageTriggerService service = new AutoMessageTriggerService(
                storeRepository,
                autoMessageRepository,
                reservationRepository,
                businessAutoMessageService,
                clock
        );

        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(businessAutoMessageService.computeEarliestEventTime(eq(template), any(LocalDateTime.class)))
                .thenReturn(LocalDateTime.of(2026, 6, 1, 2, 0));
        when(reservationRepository.findByStoreIdAndActualCheckInBetween(
                STORE_ID,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 1, 10, 5)
        )).thenReturn(List.of(reservation));

        service.tick();

        verify(reservationRepository).findByStoreIdAndActualCheckInBetween(
                STORE_ID,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 1, 10, 5)
        );
        verify(businessAutoMessageService).trySendForReservation(
                eq(STORE_ID),
                same(reservation),
                same(template),
                eq(LocalDateTime.of(2026, 6, 1, 2, 10)),
                eq(Duration.ofMinutes(5))
        );
    }
}
