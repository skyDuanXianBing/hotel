package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import server.demo.entity.Reservation;
import server.demo.repository.ReservationRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtaReservationSyncServiceUpsertLookupTest {

    @Test
    void resolveReservationTargetForUpsert_shouldUseUniqueChannelFallbackWhenRoomReservationIdChanges() {
        ReservationRepository repository = mock(ReservationRepository.class);
        OtaReservationSyncService service = createService(repository);

        Reservation existing = new Reservation();
        existing.setOrderNumber("SU26-6588792273_W39FVCQYSN-OLD");

        when(repository.findByStoreIdAndOrderNumber(7L, "SU26-6588792273_W39FVCQYSN-NEW"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndSuReservationIdAndRoomReservationId(7L, "6588792273_W39FVCQYSN", "1775048736013"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndChannelOrderNumber(7L, "6588792273"))
                .thenReturn(List.of(existing));

        OtaReservationSyncService.ReservationLookupResult result = service.resolveReservationTargetForUpsert(
                7L,
                "SU26-6588792273_W39FVCQYSN-NEW",
                "6588792273_W39FVCQYSN",
                "1775048736013",
                "6588792273"
        );

        assertSame(existing, result.reservation());
        assertEquals("CHANNEL_ORDER_UNIQUE", result.matchStrategy());
        assertEquals("SU26-6588792273_W39FVCQYSN-OLD", result.resolvedOrderNumber());
    }

    @Test
    void resolveReservationTargetForUpsert_shouldPreferSuRoomKeyBeforeChannelFallback() {
        ReservationRepository repository = mock(ReservationRepository.class);
        OtaReservationSyncService service = createService(repository);

        Reservation existing = new Reservation();
        existing.setOrderNumber("SU26-EXISTING");

        when(repository.findByStoreIdAndOrderNumber(8L, "SU26-NEW"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndSuReservationIdAndRoomReservationId(8L, "R-100", "ROOM-2"))
                .thenReturn(Optional.of(existing));

        OtaReservationSyncService.ReservationLookupResult result = service.resolveReservationTargetForUpsert(
                8L,
                "SU26-NEW",
                "R-100",
                "ROOM-2",
                "BOOKING-200"
        );

        assertSame(existing, result.reservation());
        assertEquals("SU_ROOM_KEY", result.matchStrategy());
        assertEquals("SU26-EXISTING", result.resolvedOrderNumber());
        verify(repository, never()).findByStoreIdAndChannelOrderNumber(8L, "BOOKING-200");
    }

    @Test
    void resolveReservationTargetForUpsert_shouldInsertWhenChannelOrderMapsToMultipleRows() {
        ReservationRepository repository = mock(ReservationRepository.class);
        OtaReservationSyncService service = createService(repository);

        when(repository.findByStoreIdAndOrderNumber(9L, "SU26-NEW"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndSuReservationIdAndRoomReservationId(9L, "R-200", "ROOM-3"))
                .thenReturn(Optional.empty());
        when(repository.findByStoreIdAndChannelOrderNumber(9L, "BOOKING-300"))
                .thenReturn(List.of(new Reservation(), new Reservation()));

        OtaReservationSyncService.ReservationLookupResult result = service.resolveReservationTargetForUpsert(
                9L,
                "SU26-NEW",
                "R-200",
                "ROOM-3",
                "BOOKING-300"
        );

        assertEquals("INSERT_NEW", result.matchStrategy());
        assertEquals("SU26-NEW", result.resolvedOrderNumber());
    }

    private static OtaReservationSyncService createService(ReservationRepository reservationRepository) {
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
                null,
                null
        );
    }
}
