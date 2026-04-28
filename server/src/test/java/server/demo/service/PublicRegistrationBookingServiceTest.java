package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.dto.registration.PublicRegistrationBookingResponse;
import server.demo.dto.registration.PublicRegistrationResponse;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class PublicRegistrationBookingServiceTest {

    @Test
    void getBooking_shouldResolveShortBookingKeyFromOrderNumberLikeValue() {
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        PublicRegistrationService publicRegistrationService = Mockito.mock(PublicRegistrationService.class);
        RegistrationLinkService registrationLinkService = Mockito.mock(RegistrationLinkService.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);

        PublicRegistrationBookingService service = new PublicRegistrationBookingService(
                reservationRepository,
                publicRegistrationService,
                registrationLinkService,
                storeRepository,
                reservationBookingKeyResolver
        );

        Reservation reservation = new Reservation();
        reservation.setId(144L);
        reservation.setStoreId(26L);
        reservation.setGuestName("Haowen Chen");
        reservation.setOrderNumber("SU26-HM855QW52K_W39FVCQYSN-1775039201599");
        reservation.setCheckInDate(LocalDate.of(2026, 4, 29));
        reservation.setCheckOutDate(LocalDate.of(2026, 5, 3));

        PublicRegistrationResponse roomForm = new PublicRegistrationResponse();
        roomForm.setCheckInDate(LocalDate.of(2026, 4, 29));
        roomForm.setCheckOutDate(LocalDate.of(2026, 5, 3));

        Store store = new Store();
        store.setName("Lotto Hotel");

        when(reservationRepository.findByStoreIdAndExternalBookingKey(26L, "HM855QW52K")).thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndChannelOrderNumber(26L, "HM855QW52K")).thenReturn(List.of());
        when(reservationRepository.findByStoreIdAndOrderNumber(26L, "HM855QW52K")).thenReturn(Optional.empty());
        when(reservationRepository.findByStoreIdAndOrderNumberContainingIgnoreCase(26L, "HM855QW52K"))
                .thenReturn(List.of(reservation));
        when(publicRegistrationService.getOrCreate(26L, "SU26-HM855QW52K_W39FVCQYSN-1775039201599")).thenReturn(roomForm);
        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));
        when(registrationLinkService.generateToken(26L, "SU26-HM855QW52K_W39FVCQYSN-1775039201599")).thenReturn("token-1");

        PublicRegistrationBookingResponse response = service.getBooking(26L, "HM855QW52K");

        assertEquals("HM855QW52K", response.getBookingKey());
        assertEquals("Haowen Chen", response.getGuestName());
        assertEquals(LocalDate.of(2026, 4, 29), response.getCheckInDate());
        assertEquals(LocalDate.of(2026, 5, 3), response.getCheckOutDate());
        assertEquals(1, response.getRooms().size());
        assertEquals("SU26-HM855QW52K_W39FVCQYSN-1775039201599", response.getRooms().get(0).getOrderNumber());
    }

    @Test
    void assertRoomBelongsToBooking_shouldAcceptDerivedBookingKey() {
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        PublicRegistrationService publicRegistrationService = Mockito.mock(PublicRegistrationService.class);
        RegistrationLinkService registrationLinkService = Mockito.mock(RegistrationLinkService.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);

        PublicRegistrationBookingService service = new PublicRegistrationBookingService(
                reservationRepository,
                publicRegistrationService,
                registrationLinkService,
                storeRepository,
                reservationBookingKeyResolver
        );

        Reservation reservation = new Reservation();
        reservation.setOrderNumber("SU26-HM855QW52K_W39FVCQYSN-1775039201599");

        when(reservationRepository.findByStoreIdAndOrderNumber(26L, "SU26-HM855QW52K_W39FVCQYSN-1775039201599"))
                .thenReturn(Optional.of(reservation));

        assertDoesNotThrow(() -> service.assertRoomBelongsToBooking(
                26L,
                "HM855QW52K",
                "SU26-HM855QW52K_W39FVCQYSN-1775039201599"
        ));
    }
}
