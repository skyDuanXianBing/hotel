package server.demo.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.Reservation;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationFilterOperationDateTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AutoMessageTriggerService autoMessageTriggerService;

    @Mock
    private PriceLabsReservationSyncService priceLabsReservationSyncService;

    @Mock
    private CleaningTaskAutoService cleaningTaskAutoService;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private ReservationService reservationService;

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void getReservationsWithFilters_todayCheckin_shouldUseOperationDateCheckInDate() {
        StoreContextHolder.setContext(new StoreContext(11L, 7L, "ADMIN"));
        stubEmptyReservationPage();

        LocalDate rangeStart = LocalDate.of(2026, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2026, 1, 31);
        LocalDate operationDate = LocalDate.of(2026, 5, 12);

        reservationService.getReservationsWithFilters(
                0, 25, null, null, null, null, null, null, null,
                rangeStart, rangeEnd, operationDate, "today-checkin");

        CriteriaMocks criteria = invokeCapturedSpecification(CriteriaMocks.checkIn());
        verify(criteria.criteriaBuilder).equal(criteria.checkInDatePath, operationDate);
        verify(criteria.criteriaBuilder, never()).between(criteria.checkInDatePath, rangeStart, rangeEnd);
    }

    @Test
    void getReservationsWithFilters_todayCheckout_shouldUseOperationDateCheckOutDate() {
        StoreContextHolder.setContext(new StoreContext(11L, 7L, "ADMIN"));
        stubEmptyReservationPage();

        LocalDate operationDate = LocalDate.of(2026, 5, 13);

        reservationService.getReservationsWithFilters(
                0, 25, null, null, null, null, null, null, null,
                null, null, operationDate, "today-checkout");

        CriteriaMocks criteria = invokeCapturedSpecification(CriteriaMocks.checkOut());
        verify(criteria.criteriaBuilder).equal(criteria.checkOutDatePath, operationDate);
    }

    @Test
    void getReservationsWithFilters_todayNew_shouldUseOperationDateCreatedAtRange() {
        StoreContextHolder.setContext(new StoreContext(11L, 7L, "ADMIN"));
        stubEmptyReservationPage();

        LocalDate operationDate = LocalDate.of(2026, 5, 14);
        LocalDateTime startOfDay = operationDate.atStartOfDay();
        LocalDateTime endOfDay = operationDate.plusDays(1).atStartOfDay();

        reservationService.getReservationsWithFilters(
                0, 25, null, null, null, null, null, null, null,
                null, null, operationDate, "today-new");

        CriteriaMocks criteria = invokeCapturedSpecification(CriteriaMocks.createdAt());
        verify(criteria.criteriaBuilder).between(criteria.createdAtPath, startOfDay, endOfDay);
    }

    @Test
    void getReservationsWithFilters_all_shouldKeepCheckInDateRange() {
        StoreContextHolder.setContext(new StoreContext(11L, 7L, "ADMIN"));
        stubEmptyReservationPage();

        LocalDate rangeStart = LocalDate.of(2026, 6, 1);
        LocalDate rangeEnd = LocalDate.of(2026, 6, 30);

        reservationService.getReservationsWithFilters(
                0, 25, null, null, null, null, null, null, null,
                rangeStart, rangeEnd, null, null);

        CriteriaMocks criteria = invokeCapturedSpecification(CriteriaMocks.checkIn());
        verify(criteria.criteriaBuilder).between(criteria.checkInDatePath, rangeStart, rangeEnd);
    }

    private void stubEmptyReservationPage() {
        when(reservationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private CriteriaMocks invokeCapturedSpecification(CriteriaMocks criteria) {
        Specification<Reservation> specification = captureSpecification();
        specification.toPredicate(criteria.root, criteria.query, criteria.criteriaBuilder);
        return criteria;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Specification<Reservation> captureSpecification() {
        ArgumentCaptor<Specification<Reservation>> captor =
                ArgumentCaptor.forClass((Class) Specification.class);
        verify(reservationRepository).findAll(captor.capture(), any(Pageable.class));
        return captor.getValue();
    }

    private static class CriteriaMocks {
        private final Root<Reservation> root;
        private final CriteriaQuery<?> query;
        private final CriteriaBuilder criteriaBuilder;
        private final Path<Long> storeIdPath;
        private final Path<LocalDate> checkInDatePath;
        private final Path<LocalDate> checkOutDatePath;
        private final Path<LocalDateTime> createdAtPath;

        @SuppressWarnings("unchecked")
        private CriteriaMocks(boolean includeCheckInDate, boolean includeCheckOutDate, boolean includeCreatedAt) {
            root = mock(Root.class);
            query = mock(CriteriaQuery.class);
            criteriaBuilder = mock(CriteriaBuilder.class);
            storeIdPath = mock(Path.class);
            checkInDatePath = includeCheckInDate ? mock(Path.class) : null;
            checkOutDatePath = includeCheckOutDate ? mock(Path.class) : null;
            createdAtPath = includeCreatedAt ? mock(Path.class) : null;

            stubRootPath("storeId", storeIdPath);
            if (includeCheckInDate) {
                stubRootPath("checkInDate", checkInDatePath);
            }
            if (includeCheckOutDate) {
                stubRootPath("checkOutDate", checkOutDatePath);
            }
            if (includeCreatedAt) {
                stubRootPath("createdAt", createdAtPath);
            }
        }

        private static CriteriaMocks checkIn() {
            return new CriteriaMocks(true, false, false);
        }

        private static CriteriaMocks checkOut() {
            return new CriteriaMocks(false, true, false);
        }

        private static CriteriaMocks createdAt() {
            return new CriteriaMocks(false, false, true);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void stubRootPath(String field, Path<?> path) {
            when(root.get(field)).thenReturn((Path) path);
        }
    }
}
