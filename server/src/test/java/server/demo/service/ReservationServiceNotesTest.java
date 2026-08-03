package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ReservationDTO;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ReservationRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceNotesTest {

    private static final Long USER_ID = 7L;
    private static final Long STORE_ID = 26L;

    @Mock private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void updateReservationNotes_updatesNotesWithinStore() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        Reservation reservation = reservationInStore(STORE_ID);
        when(reservationRepository.findById(42L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationDTO dto = reservationService.updateReservationNotes(42L, "  客人要高楼层、无烟房  ");

        assertEquals("客人要高楼层、无烟房", reservation.getNotes());
        assertEquals("客人要高楼层、无烟房", dto.getNotes());
    }

    @Test
    void updateReservationNotes_allowsClearingNotes() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        Reservation reservation = reservationInStore(STORE_ID);
        reservation.setNotes("旧备注");
        when(reservationRepository.findById(42L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationDTO dto = reservationService.updateReservationNotes(42L, null);

        assertNull(reservation.getNotes());
        assertNull(dto.getNotes());

        reservation.setNotes("旧备注");
        ReservationDTO clearedByWhitespace = reservationService.updateReservationNotes(42L, "   ");
        assertNull(reservation.getNotes());
        assertNull(clearedByWhitespace.getNotes());
    }

    @Test
    void updateReservationNotes_rejectsCrossStoreAccess() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        Reservation reservation = reservationInStore(999L);
        when(reservationRepository.findById(42L)).thenReturn(Optional.of(reservation));

        assertThrows(RuntimeException.class, () -> reservationService.updateReservationNotes(42L, "x"));
    }

    @Test
    void updateReservationNotes_throwsWhenReservationMissing() {
        StoreContextHolder.setContext(new StoreContext(USER_ID, STORE_ID, "ADMIN"));
        when(reservationRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> reservationService.updateReservationNotes(42L, "x"));
    }

    private Reservation reservationInStore(Long storeId) {
        Channel channel = new Channel();
        channel.setId(1L);
        channel.setName("Airbnb");

        Reservation reservation = new Reservation();
        reservation.setId(42L);
        reservation.setStoreId(storeId);
        reservation.setChannel(channel);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservation;
    }
}
