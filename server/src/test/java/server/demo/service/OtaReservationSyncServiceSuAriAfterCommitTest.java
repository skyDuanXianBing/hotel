package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import server.demo.repository.ReservationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OtaReservationSyncServiceSuAriAfterCommitTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void scheduleSuAvailabilitySyncAfterCommit_defersEnqueueUntilAfterCommitWhenTransactionIsActive() {
        SuAriAutoSyncService suAriAutoSyncService = mock(SuAriAutoSyncService.class);
        OtaReservationSyncService service = createService(suAriAutoSyncService);
        List<SuAriAutoSyncService.DateRange> ranges = List.of(
                new SuAriAutoSyncService.DateRange(LocalDate.of(2026, 5, 21), LocalDate.of(2026, 5, 21))
        );
        Set<Long> roomTypeIds = Set.of(65L);

        TransactionSynchronizationManager.initSynchronization();

        service.scheduleSuAvailabilitySyncAfterCommit(
                26L,
                "HOTEL26",
                "notif-1",
                "SU26-ORDER-1",
                776L,
                "ari-trace-1",
                ranges,
                roomTypeIds
        );

        verify(suAriAutoSyncService, times(0)).enqueueForStoreDateRanges(
                eq(26L),
                eq("su_reservation_webhook"),
                eq(ranges),
                eq(roomTypeIds),
                isNull(),
                eq(true),
                eq(false),
                eq(false),
                eq(false)
        );

        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(suAriAutoSyncService, times(1)).enqueueForStoreDateRanges(
                eq(26L),
                eq("su_reservation_webhook"),
                eq(ranges),
                eq(roomTypeIds),
                isNull(),
                eq(true),
                eq(false),
                eq(false),
                eq(false)
        );
    }

    @Test
    void scheduleSuAvailabilitySyncAfterCommit_runsImmediatelyWhenTransactionIsInactive() {
        SuAriAutoSyncService suAriAutoSyncService = mock(SuAriAutoSyncService.class);
        OtaReservationSyncService service = createService(suAriAutoSyncService);
        List<SuAriAutoSyncService.DateRange> ranges = List.of(
                new SuAriAutoSyncService.DateRange(LocalDate.of(2026, 5, 21), LocalDate.of(2026, 5, 21))
        );
        Set<Long> roomTypeIds = Set.of(65L);

        service.scheduleSuAvailabilitySyncAfterCommit(
                26L,
                "HOTEL26",
                "notif-2",
                "SU26-ORDER-2",
                777L,
                "ari-trace-2",
                ranges,
                roomTypeIds
        );

        verify(suAriAutoSyncService, times(1)).enqueueForStoreDateRanges(
                eq(26L),
                eq("su_reservation_webhook"),
                eq(ranges),
                eq(roomTypeIds),
                isNull(),
                eq(true),
                eq(false),
                eq(false),
                eq(false)
        );
    }

    private static OtaReservationSyncService createService(SuAriAutoSyncService suAriAutoSyncService) {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        return new OtaReservationSyncService(
                null,
                null,
                null,
                null,
                null,
                reservationRepository,
                null,
                null,
                transactionManager,
                null,
                null,
                null,
                null,
                null,
                suAriAutoSyncService,
                null
        );
    }
}
