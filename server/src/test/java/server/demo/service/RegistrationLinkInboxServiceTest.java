package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.dto.registration.RegistrationLinkInboxItemDTO;
import server.demo.entity.RegistrationLinkInboxItem;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.RegistrationLinkInboxRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class RegistrationLinkInboxServiceTest {

    @Test
    void listTop200_shouldFilterByReservationStatus() {
        RegistrationLinkInboxRepository inboxRepository = Mockito.mock(RegistrationLinkInboxRepository.class);
        RegistrationLinkService registrationLinkService = Mockito.mock(RegistrationLinkService.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = Mockito.mock(ReservationBookingKeyResolver.class);

        RegistrationLinkInboxService service = new RegistrationLinkInboxService(
                inboxRepository,
                registrationLinkService,
                reservationBookingKeyResolver,
                "https://example.com"
        );

        RegistrationLinkInboxItem confirmedItem = new RegistrationLinkInboxItem();
        confirmedItem.setId(1L);
        confirmedItem.setStoreId(26L);
        confirmedItem.setBookingKey("BOOK-1");
        confirmedItem.setLinkUrl("https://old-host/r/BOOK-1?t=1");
        confirmedItem.setRoomCount(1);
        confirmedItem.setGuestName("Tom");

        RegistrationLinkInboxItem cancelledItem = new RegistrationLinkInboxItem();
        cancelledItem.setId(2L);
        cancelledItem.setStoreId(26L);
        cancelledItem.setBookingKey("BOOK-2");
        cancelledItem.setLinkUrl("https://old-host/r/BOOK-2?t=2");
        cancelledItem.setRoomCount(1);
        cancelledItem.setGuestName("Jerry");

        var createdAt = LocalDateTime.of(2026, 4, 30, 10, 0);
        confirmedItem.setId(1L);
        cancelledItem.setId(2L);

        setCreatedAt(confirmedItem, createdAt);
        setCreatedAt(cancelledItem, createdAt.plusMinutes(1));

        Reservation confirmedReservation = new Reservation();
        confirmedReservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation cancelledReservation = new Reservation();
        cancelledReservation.setStatus(ReservationStatus.CANCELLED);

        when(inboxRepository.findTop200ByStoreIdOrderByCreatedAtDesc(26L))
                .thenReturn(List.of(cancelledItem, confirmedItem));
        when(reservationBookingKeyResolver.findReservationsByBookingKey(26L, "BOOK-1"))
                .thenReturn(List.of(confirmedReservation));
        when(reservationBookingKeyResolver.findReservationsByBookingKey(26L, "BOOK-2"))
                .thenReturn(List.of(cancelledReservation));

        List<RegistrationLinkInboxItemDTO> result = service.listTop200(26L, ReservationStatus.CONFIRMED);

        assertEquals(1, result.size());
        assertEquals("BOOK-1", result.get(0).getBookingKey());
        assertEquals(ReservationStatus.CONFIRMED, result.get(0).getReservationStatus());
        assertEquals("https://example.com/r/BOOK-1?t=1", result.get(0).getLinkUrl());
    }

    private static void setCreatedAt(RegistrationLinkInboxItem item, LocalDateTime createdAt) {
        try {
            var field = RegistrationLinkInboxItem.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(item, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
