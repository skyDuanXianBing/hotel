package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.dto.registration.RegistrationLinkInboxItemDTO;
import server.demo.entity.RegistrationLinkInboxItem;
import server.demo.entity.Reservation;
import server.demo.enums.ReservationStatus;
import server.demo.repository.RegistrationLinkInboxRepository;
import server.demo.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationLinkInboxServiceTest {

    @Test
    void listTop200_shouldFilterByReservationStatus() {
        RegistrationLinkInboxRepository inboxRepository = Mockito.mock(RegistrationLinkInboxRepository.class);
        RegistrationLinkService registrationLinkService = Mockito.mock(RegistrationLinkService.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = Mockito.mock(ReservationBookingKeyResolver.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        RegistrationLinkInboxService service = new RegistrationLinkInboxService(
                inboxRepository,
                registrationLinkService,
                reservationBookingKeyResolver,
                reservationRepository,
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

    @Test
    void recordIfAbsent_shouldUseFrontendBaseUrlForPublicBookingLink() {
        RegistrationLinkInboxRepository inboxRepository = Mockito.mock(RegistrationLinkInboxRepository.class);
        RegistrationLinkService registrationLinkService = new RegistrationLinkService("test-secret", 90);
        ReservationBookingKeyResolver reservationBookingKeyResolver = Mockito.mock(ReservationBookingKeyResolver.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        RegistrationLinkInboxService service = new RegistrationLinkInboxService(
                inboxRepository,
                registrationLinkService,
                reservationBookingKeyResolver,
                reservationRepository,
                "http://localhost:8091"
        );

        when(inboxRepository.findByStoreIdAndBookingKey(26L, "BOOK-LOCAL"))
                .thenReturn(Optional.empty());

        service.recordIfAbsent(26L, "BOOK-LOCAL", "Alice", null, null, 1);

        ArgumentCaptor<RegistrationLinkInboxItem> captor = ArgumentCaptor.forClass(RegistrationLinkInboxItem.class);
        verify(inboxRepository).save(captor.capture());

        String linkUrl = captor.getValue().getLinkUrl();
        assertTrue(linkUrl.startsWith("http://localhost:8091/rb/BOOK-LOCAL?t="));

        String token = linkUrl.substring(linkUrl.indexOf("?t=") + 3);
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken("BOOK-LOCAL", token);
        assertEquals(26L, claims.getStoreId());
    }

    @Test
    void listTop200_shouldRewriteStoredServerHostToFrontendBaseUrlAndPreserveTokenPathQuery() {
        RegistrationLinkInboxRepository inboxRepository = Mockito.mock(RegistrationLinkInboxRepository.class);
        RegistrationLinkService registrationLinkService = Mockito.mock(RegistrationLinkService.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = Mockito.mock(ReservationBookingKeyResolver.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);

        RegistrationLinkInboxService service = new RegistrationLinkInboxService(
                inboxRepository,
                registrationLinkService,
                reservationBookingKeyResolver,
                reservationRepository,
                "http://localhost:8091/"
        );

        RegistrationLinkInboxItem item = new RegistrationLinkInboxItem();
        item.setId(1L);
        item.setStoreId(26L);
        item.setBookingKey("BOOK-LOCAL");
        item.setLinkUrl("http://localhost:8092/rb/BOOK-LOCAL?t=store-token&lang=zh");
        item.setRoomCount(1);
        setCreatedAt(item, LocalDateTime.of(2026, 5, 24, 10, 0));

        when(inboxRepository.findTop200ByStoreIdOrderByCreatedAtDesc(26L))
                .thenReturn(List.of(item));

        List<RegistrationLinkInboxItemDTO> result = service.listTop200(26L, null);

        assertEquals(1, result.size());
        assertEquals("http://localhost:8091/rb/BOOK-LOCAL?t=store-token&lang=zh", result.get(0).getLinkUrl());
    }

    @Test
    void backfillMissingForStore_shouldCreateMissingInboxItemsAndAggregateRoomCount() {
        RegistrationLinkInboxRepository inboxRepository = Mockito.mock(RegistrationLinkInboxRepository.class);
        RegistrationLinkService registrationLinkService = new RegistrationLinkService("test-secret", 90);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        ReservationBookingKeyResolver reservationBookingKeyResolver = new ReservationBookingKeyResolver(reservationRepository);

        RegistrationLinkInboxService service = new RegistrationLinkInboxService(
                inboxRepository,
                registrationLinkService,
                reservationBookingKeyResolver,
                reservationRepository,
                "https://example.com"
        );

        Reservation firstRoom = new Reservation();
        firstRoom.setChannelOrderNumber("BOOK-1");
        firstRoom.setGuestName("Alice");
        firstRoom.setCheckInDate(LocalDate.of(2026, 6, 1));
        firstRoom.setCheckOutDate(LocalDate.of(2026, 6, 3));

        Reservation secondRoom = new Reservation();
        secondRoom.setChannelOrderNumber("BOOK-1");
        secondRoom.setCheckInDate(LocalDate.of(2026, 6, 2));
        secondRoom.setCheckOutDate(LocalDate.of(2026, 6, 5));

        Reservation manualOrder = new Reservation();
        manualOrder.setOrderNumber("MANUAL-1");
        manualOrder.setGuestName("Bob");

        Reservation missingKey = new Reservation();

        RegistrationLinkInboxItem existingManualItem = new RegistrationLinkInboxItem();
        existingManualItem.setStoreId(26L);
        existingManualItem.setBookingKey("MANUAL-1");

        when(reservationRepository.findByStoreId(26L))
                .thenReturn(List.of(firstRoom, secondRoom, manualOrder, missingKey));
        when(inboxRepository.findByStoreIdAndBookingKey(26L, "BOOK-1"))
                .thenReturn(Optional.empty());
        when(inboxRepository.findByStoreIdAndBookingKey(26L, "MANUAL-1"))
                .thenReturn(Optional.of(existingManualItem));

        RegistrationLinkInboxService.BackfillResult result = service.backfillMissingForStore(26L);

        assertEquals(4, result.scannedCount());
        assertEquals(2, result.eligibleCount());
        assertEquals(1, result.createdCount());
        assertEquals(1, result.skippedMissingBookingKey());

        ArgumentCaptor<RegistrationLinkInboxItem> captor = ArgumentCaptor.forClass(RegistrationLinkInboxItem.class);
        verify(inboxRepository).save(captor.capture());

        RegistrationLinkInboxItem saved = captor.getValue();
        assertEquals(26L, saved.getStoreId());
        assertEquals("BOOK-1", saved.getBookingKey());
        assertEquals("Alice", saved.getGuestName());
        assertEquals(LocalDate.of(2026, 6, 1), saved.getCheckInDate());
        assertEquals(LocalDate.of(2026, 6, 5), saved.getCheckOutDate());
        assertEquals(2, saved.getRoomCount());
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
