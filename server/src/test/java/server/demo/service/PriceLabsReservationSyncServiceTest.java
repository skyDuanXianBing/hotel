package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomBlockout;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.enums.RoomBlockoutType;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
import server.demo.repository.StoreRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
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
        store.setTimezone("Asia/Tokyo");
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
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 23, 30));
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
        ReflectionTestUtils.setField(
                service,
                "clock",
                Clock.fixed(Instant.parse("2026-01-01T15:30:00Z"), ZoneOffset.UTC)
        );

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
        assertEquals("2026-01-02", sent.get(0).getData().get(0).getBookedTime());
        assertEquals(3, sent.get(0).getData().get(0).getTotalDays());

        verify(integrationRepo, times(1)).save(any(PriceLabsIntegration.class));
    }

    @Test
    void pushCancelledBlockouts_shouldSendCanceledReservationsPayload() {
        PriceLabsApiClient apiClient = mock(PriceLabsApiClient.class);
        PriceLabsConnectionRepository connectionRepo = mock(PriceLabsConnectionRepository.class);
        PriceLabsIntegrationRepository integrationRepo = mock(PriceLabsIntegrationRepository.class);
        ReservationRepository reservationRepo = mock(ReservationRepository.class);
        RoomBlockoutRepository roomBlockoutRepo = mock(RoomBlockoutRepository.class);
        StoreRepository storeRepo = mock(StoreRepository.class);

        PriceLabsIntegration integration = new PriceLabsIntegration(5L);
        integration.setIsEnabled(true);
        when(integrationRepo.findByStoreId(5L)).thenReturn(Optional.of(integration));

        Store store = new Store();
        store.setId(5L);
        store.setCurrency("JPY");
        store.setTimezone("Asia/Tokyo");
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
        room.setId(97L);

        RoomBlockout blockout = new RoomBlockout(5L, room, LocalDate.of(2026, 3, 20), RoomBlockoutType.STOP, "x");
        ReflectionTestUtils.setField(blockout, "createdAt", LocalDateTime.of(2026, 3, 10, 23, 30));
        ReflectionTestUtils.setField(blockout, "updatedAt", LocalDateTime.of(2026, 3, 12, 10, 0));

        PriceLabsApiClient.PriceLabsResponse ok = new PriceLabsApiClient.PriceLabsResponse();
        ok.setSuccess(List.of("blk_5_97_2026-03-20_stop"));
        ok.setFailure(List.of());
        when(apiClient.pushReservations(any())).thenReturn(ok);

        PriceLabsReservationSyncService service = new PriceLabsReservationSyncService();
        ReflectionTestUtils.setField(service, "apiClient", apiClient);
        ReflectionTestUtils.setField(service, "connectionRepo", connectionRepo);
        ReflectionTestUtils.setField(service, "integrationRepo", integrationRepo);
        ReflectionTestUtils.setField(service, "reservationRepo", reservationRepo);
        ReflectionTestUtils.setField(service, "roomBlockoutRepo", roomBlockoutRepo);
        ReflectionTestUtils.setField(service, "storeRepo", storeRepo);
        ReflectionTestUtils.setField(
                service,
                "clock",
                Clock.fixed(Instant.parse("2026-03-10T15:30:00Z"), ZoneOffset.UTC)
        );

        service.pushCancelledBlockouts(
                5L,
                List.of(blockout),
                Map.of(97L, 35L),
                LocalDate.of(2026, 3, 17)
        );

        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(apiClient, atLeastOnce()).pushReservations(captor.capture());
        @SuppressWarnings("unchecked")
        List<PriceLabsApiClient.ReservationsByListing> sent = (List<PriceLabsApiClient.ReservationsByListing>) captor.getValue();

        assertEquals(1, sent.size());
        assertEquals("store_5_room_type_35", sent.get(0).getListingId());
        assertNotNull(sent.get(0).getData());
        assertEquals(1, sent.get(0).getData().size());
        assertEquals("blk_5_97_2026-03-20_stop", sent.get(0).getData().get(0).getReservationId());
        assertEquals("canceled", sent.get(0).getData().get(0).getStatus());
        assertEquals("2026-03-17", sent.get(0).getData().get(0).getCancelTime());
        assertEquals("2026-03-20", sent.get(0).getData().get(0).getStartDate());
        assertEquals("2026-03-21", sent.get(0).getData().get(0).getEndDate());
        assertEquals("2026-03-11", sent.get(0).getData().get(0).getBookedTime());

        verify(integrationRepo, times(1)).save(any(PriceLabsIntegration.class));
    }
}
