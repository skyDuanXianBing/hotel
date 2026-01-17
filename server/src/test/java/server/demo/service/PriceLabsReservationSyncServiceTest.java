package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsReservationSyncServiceTest {

    @Test
    void pushReservationById_shouldSendReservationsPayload() {
        PriceLabsApiClient apiClient = mock(PriceLabsApiClient.class);
        PriceLabsConnectionRepository connectionRepo = mock(PriceLabsConnectionRepository.class);
        PriceLabsIntegrationRepository integrationRepo = mock(PriceLabsIntegrationRepository.class);
        ReservationRepository reservationRepo = mock(ReservationRepository.class);
        StoreRepository storeRepo = mock(StoreRepository.class);

        PriceLabsIntegration integration = new PriceLabsIntegration(5L);
        integration.setIsEnabled(true);
        when(integrationRepo.findByStoreId(5L)).thenReturn(Optional.of(integration));

        Store store = new Store();
        store.setId(5L);
        store.setCurrency("JPY");
        when(storeRepo.findById(5L)).thenReturn(Optional.of(store));

        RoomType roomType = new RoomType();
        roomType.setId(35L);
        roomType.setStoreId(5L);

        PriceLabsConnection conn = new PriceLabsConnection();
        conn.setId(100L);
        conn.setStoreId(5L);
        conn.setRoomType(roomType);
        conn.setIsEnabled(true);
        conn.setPriceLabsListingId("store_5_room_type_35");
        when(connectionRepo.findByStoreIdAndIsEnabledTrue(5L)).thenReturn(List.of(conn));

        Room room = new Room();
        room.setId(77L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        room.setStoreId(5L);

        Reservation reservation = new Reservation();
        reservation.setId(99L);
        reservation.setStoreId(5L);
        reservation.setRoom(room);
        reservation.setCheckInDate(LocalDate.of(2026, 1, 15));
        reservation.setCheckOutDate(LocalDate.of(2026, 1, 18));
        reservation.setTotalAmount(new BigDecimal("1234.00"));
        reservation.setOrderNumber("RSV-99");
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        reservation.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepo.findByStoreIdAndIdWithRoomType(5L, 99L)).thenReturn(Optional.of(reservation));

        PriceLabsApiClient.PriceLabsResponse ok = new PriceLabsApiClient.PriceLabsResponse();
        ok.setSuccess(List.of("RSV-99"));
        ok.setFailure(List.of());
        when(apiClient.pushReservations(any())).thenReturn(ok);

        PriceLabsReservationSyncService service = new PriceLabsReservationSyncService();
        ReflectionTestUtils.setField(service, "apiClient", apiClient);
        ReflectionTestUtils.setField(service, "connectionRepo", connectionRepo);
        ReflectionTestUtils.setField(service, "integrationRepo", integrationRepo);
        ReflectionTestUtils.setField(service, "reservationRepo", reservationRepo);
        ReflectionTestUtils.setField(service, "storeRepo", storeRepo);

        service.pushReservationById(5L, 99L);

        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(apiClient, atLeastOnce()).pushReservations(captor.capture());
        @SuppressWarnings("unchecked")
        List<PriceLabsApiClient.ReservationsByListing> sent = (List<PriceLabsApiClient.ReservationsByListing>) captor.getValue();

        assertEquals(1, sent.size());
        assertEquals("store_5_room_type_35", sent.get(0).getListingId());
        assertNotNull(sent.get(0).getData());
        assertEquals(1, sent.get(0).getData().size());
        assertEquals("RSV-99", sent.get(0).getData().get(0).getReservationId());
        assertEquals("JPY", sent.get(0).getData().get(0).getCurrency());
        assertEquals("booked", sent.get(0).getData().get(0).getStatus());
        assertEquals("2026-01-15", sent.get(0).getData().get(0).getStartDate());
        assertEquals("2026-01-18", sent.get(0).getData().get(0).getEndDate());
        assertEquals(3, sent.get(0).getData().get(0).getTotalDays());

        verify(integrationRepo, times(1)).save(any(PriceLabsIntegration.class));
    }
}
