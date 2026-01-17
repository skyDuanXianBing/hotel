package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.Reservation;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.ReservationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceLabsStatusServiceTest {

    @Test
    void queryReservationStatusByRoomType_shouldReturnEmptyWhenNoReservations() {
        PriceLabsApiClient apiClient = mock(PriceLabsApiClient.class);
        PriceLabsIntegrationRepository integrationRepo = mock(PriceLabsIntegrationRepository.class);
        ReservationRepository reservationRepo = mock(ReservationRepository.class);

        PriceLabsIntegration integration = new PriceLabsIntegration(5L);
        integration.setIsEnabled(true);
        when(integrationRepo.findByStoreId(5L)).thenReturn(Optional.of(integration));
        when(reservationRepo.findByStoreIdAndRoomTypeIdOverlappingDateRangeWithRoomType(
                5L, 10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)))
                .thenReturn(List.of());

        PriceLabsStatusService service = new PriceLabsStatusService();
        ReflectionTestUtils.setField(service, "apiClient", apiClient);
        ReflectionTestUtils.setField(service, "integrationRepo", integrationRepo);
        ReflectionTestUtils.setField(service, "reservationRepo", reservationRepo);

        PriceLabsApiClient.PriceLabsResponse res = service.queryReservationStatusByRoomType(
                5L, 10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));

        assertNotNull(res);
        assertNotNull(res.getSuccess());
        assertNotNull(res.getFailure());
        verify(apiClient, never()).queryStatus(any());
    }

    @Test
    void queryReservationStatusByRoomType_shouldQueryStatusWithOrderNumbers() {
        PriceLabsApiClient apiClient = mock(PriceLabsApiClient.class);
        PriceLabsIntegrationRepository integrationRepo = mock(PriceLabsIntegrationRepository.class);
        ReservationRepository reservationRepo = mock(ReservationRepository.class);

        PriceLabsIntegration integration = new PriceLabsIntegration(5L);
        integration.setIsEnabled(true);
        when(integrationRepo.findByStoreId(5L)).thenReturn(Optional.of(integration));

        Reservation r1 = new Reservation();
        r1.setOrderNumber("RSV-1");
        Reservation r2 = new Reservation();
        r2.setOrderNumber("RSV-2");
        Reservation r3 = new Reservation();
        r3.setOrderNumber(" ");

        when(reservationRepo.findByStoreIdAndRoomTypeIdOverlappingDateRangeWithRoomType(
                5L, 10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)))
                .thenReturn(List.of(r1, r2, r3));

        PriceLabsApiClient.PriceLabsResponse response = new PriceLabsApiClient.PriceLabsResponse();
        when(apiClient.queryStatus(any())).thenReturn(response);

        PriceLabsStatusService service = new PriceLabsStatusService();
        ReflectionTestUtils.setField(service, "apiClient", apiClient);
        ReflectionTestUtils.setField(service, "integrationRepo", integrationRepo);
        ReflectionTestUtils.setField(service, "reservationRepo", reservationRepo);

        PriceLabsApiClient.PriceLabsResponse res = service.queryReservationStatusByRoomType(
                5L, 10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));

        assertEquals(response, res);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(apiClient).queryStatus(captor.capture());
        @SuppressWarnings("unchecked")
        List<PriceLabsApiClient.StatusReq> sent = captor.getValue();
        assertEquals(2, sent.size());
        assertEquals("RSV-1", sent.get(0).getId());
        assertEquals("reservation", sent.get(0).getType());
        assertEquals("RSV-2", sent.get(1).getId());
    }
}
