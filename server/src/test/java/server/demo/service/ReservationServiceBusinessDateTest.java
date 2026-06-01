package server.demo.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.repository.ChannelRepository;
import server.demo.repository.OrderBoxRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.UserRepository;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceBusinessDateTest {

    private static final Long USER_ID = 7L;
    private static final Long STORE_ID = 26L;

    @Mock private ReservationRepository reservationRepository;
    @Mock private OrderBoxRepository orderBoxRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private AutoMessageTriggerService autoMessageTriggerService;
    @Mock private PriceLabsReservationSyncService priceLabsReservationSyncService;
    @Mock private CleaningTaskAutoService cleaningTaskAutoService;
    @Mock private OperationLogService operationLogService;
    @Mock private OrderNotificationDispatchService orderNotificationDispatchService;

    @InjectMocks
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("Asia/Shanghai"));
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(
                ZoneId.of(StoreTimeZoneUtil.DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE)
        );
    }

    @Test
    void statistics_shouldUseTokyoBusinessDateAndShanghaiStorageCreatedAtWindow() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        ReflectionTestUtils.setField(
                reservationService,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Tokyo")));

        reservationService.getReservationStatistics();

        verify(reservationRepository).countTodayArrivalsByStoreId(STORE_ID, LocalDate.of(2026, 4, 8));
        verify(reservationRepository).countByStoreIdAndCheckOutDate(STORE_ID, LocalDate.of(2026, 4, 8));
        verify(reservationRepository).countTodayNewOrdersByStoreId(
                STORE_ID,
                LocalDateTime.of(2026, 4, 7, 23, 0),
                LocalDateTime.of(2026, 4, 8, 23, 0)
        );
        verify(reservationRepository).countUnassignedOrUnmappedByStoreId(STORE_ID, LocalDate.of(2026, 4, 8));
    }

    @Test
    void todayCheckIns_shouldUseShanghaiBusinessDateForChinaStore() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        ReflectionTestUtils.setField(
                reservationService,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Shanghai")));
        when(reservationRepository.findByStoreIdAndCheckInDateBetween(
                STORE_ID,
                LocalDate.of(2026, 4, 7),
                LocalDate.of(2026, 4, 7)
        )).thenReturn(List.of());

        reservationService.getTodayCheckIns();

        verify(reservationRepository).findByStoreIdAndCheckInDateBetween(
                STORE_ID,
                LocalDate.of(2026, 4, 7),
                LocalDate.of(2026, 4, 7)
        );
    }

    @Test
    void todayNewReservations_shouldQueryCreatedAtBusinessWindowInShanghaiStorageZone() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        ReflectionTestUtils.setField(
                reservationService,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Tokyo")));
        when(reservationRepository.findByStoreIdAndCreatedAtBetween(
                STORE_ID,
                LocalDateTime.of(2026, 4, 7, 23, 0),
                LocalDateTime.of(2026, 4, 8, 23, 0)
        )).thenReturn(List.of());

        reservationService.getTodayNewReservations();

        verify(reservationRepository).findByStoreIdAndCreatedAtBetween(
                STORE_ID,
                LocalDateTime.of(2026, 4, 7, 23, 0),
                LocalDateTime.of(2026, 4, 8, 23, 0)
        );
    }

    @Test
    void todayNewReservations_shouldSwitchCreatedAtWindowToUtcStorageZone() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("UTC"));
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        ReflectionTestUtils.setField(
                reservationService,
                "clock",
                Clock.fixed(Instant.parse("2026-04-07T15:30:00Z"), ZoneOffset.UTC)
        );
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Tokyo")));
        when(reservationRepository.findByStoreIdAndCreatedAtBetween(
                STORE_ID,
                LocalDateTime.of(2026, 4, 7, 15, 0),
                LocalDateTime.of(2026, 4, 8, 15, 0)
        )).thenReturn(List.of());

        reservationService.getTodayNewReservations();

        verify(reservationRepository).findByStoreIdAndCreatedAtBetween(
                STORE_ID,
                LocalDateTime.of(2026, 4, 7, 15, 0),
                LocalDateTime.of(2026, 4, 8, 15, 0)
        );
    }

    @Test
    void getReservationById_shouldExposeDefaultShanghaiReservationTimestampStorageZone() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        Reservation reservation = reservation(101L);
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));

        Optional<server.demo.dto.ReservationDTO> result = reservationService.getReservationById(101L);

        assertTrue(result.isPresent());
        assertEquals("Asia/Shanghai", result.get().getReservationTimestampStorageZone());
    }

    @Test
    void getReservationById_shouldExposeUtcReservationTimestampStorageZoneWhenConfigured() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("UTC"));
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        Reservation reservation = reservation(102L);
        when(reservationRepository.findById(102L)).thenReturn(Optional.of(reservation));

        Optional<server.demo.dto.ReservationDTO> result = reservationService.getReservationById(102L);

        assertTrue(result.isPresent());
        assertEquals("UTC", result.get().getReservationTimestampStorageZone());
    }

    @Test
    void getReservationsByTypeTodayNew_shouldUseShanghaiStorageCreatedAtWindow() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        ReflectionTestUtils.setField(
                reservationService,
                "clock",
                Clock.fixed(Instant.parse("2026-06-01T02:00:00Z"), ZoneOffset.UTC)
        );
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Tokyo")));
        when(reservationRepository.findTodayNewOrdersByStoreId(
                STORE_ID,
                LocalDateTime.of(2026, 5, 31, 23, 0),
                LocalDateTime.of(2026, 6, 1, 23, 0)
        )).thenReturn(List.of());

        reservationService.getReservationsByType("today-new");

        verify(reservationRepository).findTodayNewOrdersByStoreId(
                STORE_ID,
                LocalDateTime.of(2026, 5, 31, 23, 0),
                LocalDateTime.of(2026, 6, 1, 23, 0)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReservationsWithFiltersTodayNewOrderType_shouldUseShanghaiStorageCreatedAtWindow() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        ReflectionTestUtils.setField(
                reservationService,
                "clock",
                Clock.fixed(Instant.parse("2026-06-01T02:00:00Z"), ZoneOffset.UTC)
        );
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Tokyo")));
        when(reservationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Specification<Reservation> specification = invocation.getArgument(0);
                    Root<Reservation> root = mock(Root.class);
                    CriteriaQuery<?> query = mock(CriteriaQuery.class);
                    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
                    Path<Long> storeIdPath = mock(Path.class);
                    Path<LocalDateTime> createdAtPath = mock(Path.class);
                    Predicate predicate = mock(Predicate.class);
                    LocalDateTime expectedStart = LocalDateTime.of(2026, 5, 31, 23, 0);
                    LocalDateTime expectedEnd = LocalDateTime.of(2026, 6, 1, 23, 0);

                    when(root.<Long>get("storeId")).thenReturn(storeIdPath);
                    when(root.<LocalDateTime>get("createdAt")).thenReturn(createdAtPath);
                    when(criteriaBuilder.equal(storeIdPath, STORE_ID)).thenReturn(predicate);
                    when(criteriaBuilder.greaterThanOrEqualTo(createdAtPath, expectedStart)).thenReturn(predicate);
                    when(criteriaBuilder.lessThan(createdAtPath, expectedEnd)).thenReturn(predicate);
                    when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);

                    specification.toPredicate(root, query, criteriaBuilder);

                    verify(criteriaBuilder).greaterThanOrEqualTo(createdAtPath, expectedStart);
                    verify(criteriaBuilder).lessThan(createdAtPath, expectedEnd);
                    return new PageImpl<Reservation>(List.of());
                });

        reservationService.getReservationsWithFilters(
                0,
                10,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "today-new"
        );
    }

    @Test
    void unassignedReservations_shouldUseStoreLocalNoonCutoff() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        ReflectionTestUtils.setField(
                reservationService,
                "clock",
                Clock.fixed(Instant.parse("2026-04-08T03:30:00Z"), ZoneOffset.UTC)
        );
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store("Asia/Tokyo")));
        when(reservationRepository.findUnassignedOrUnmappedByStoreId(STORE_ID, LocalDate.of(2026, 4, 9)))
                .thenReturn(List.of());

        reservationService.getUnassignedReservations();

        verify(reservationRepository).findUnassignedOrUnmappedByStoreId(STORE_ID, LocalDate.of(2026, 4, 9));
    }

    @Test
    void checkIn_shouldWriteActualCheckInInReservationTimestampStorageZone() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("UTC"));
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        Reservation reservation = reservation(201L);
        reservation.setStatus(server.demo.enums.ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(201L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeUtc = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        reservationService.checkIn(201L);
        LocalDateTime afterUtc = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1);

        assertTrue(!reservation.getActualCheckIn().isBefore(beforeUtc));
        assertTrue(!reservation.getActualCheckIn().isAfter(afterUtc));
    }

    @Test
    void checkOut_shouldWriteActualCheckOutInReservationTimestampStorageZone() {
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(ZoneId.of("UTC"));
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        Reservation reservation = reservation(202L);
        reservation.setStatus(server.demo.enums.ReservationStatus.CHECKED_IN);
        when(reservationRepository.findById(202L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeUtc = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        reservationService.checkOut(202L);
        LocalDateTime afterUtc = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1);

        assertTrue(!reservation.getActualCheckOut().isBefore(beforeUtc));
        assertTrue(!reservation.getActualCheckOut().isAfter(afterUtc));
    }

    private static Store store(String timezone) {
        Store store = new Store();
        store.setId(STORE_ID);
        store.setTimezone(timezone);
        return store;
    }

    private static Reservation reservation(Long id) {
        Channel channel = new Channel();
        channel.setId(1L);
        channel.setName("direct");

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStoreId(STORE_ID);
        reservation.setOrderNumber("R" + id);
        reservation.setGuestName("Test Guest");
        reservation.setChannel(channel);
        reservation.setCheckInDate(LocalDate.of(2026, 5, 31));
        reservation.setCheckOutDate(LocalDate.of(2026, 6, 1));
        reservation.setStatus(server.demo.enums.ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(LocalDateTime.of(2026, 5, 31, 14, 42, 32));
        reservation.setUpdatedAt(LocalDateTime.of(2026, 5, 31, 14, 42, 32));
        return reservation;
    }
}
