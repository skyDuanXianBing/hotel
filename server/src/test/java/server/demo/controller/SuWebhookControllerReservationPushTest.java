package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.config.SuMessagingWebhookAuthConfig;
import server.demo.entity.Store;
import server.demo.service.OtaReservationSyncService;
import server.demo.service.SuReservationWebhookCompensationService;
import server.demo.service.SuWebhookAsyncProcessor;
import server.demo.service.SuWebhookIdempotencyService;
import server.demo.repository.StoreRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuWebhookControllerReservationPushTest {

    private SuWebhookController controller;
    private StoreRepository storeRepository;
    private OtaReservationSyncService otaReservationSyncService;
    private SuWebhookAsyncProcessor asyncProcessor;
    private SuWebhookIdempotencyService idempotencyService;
    private SuReservationWebhookCompensationService compensationService;

    @BeforeEach
    void setUp() {
        storeRepository = Mockito.mock(StoreRepository.class);
        otaReservationSyncService = Mockito.mock(OtaReservationSyncService.class);
        idempotencyService = Mockito.mock(SuWebhookIdempotencyService.class);
        SuMessagingWebhookAuthConfig authConfig = Mockito.mock(SuMessagingWebhookAuthConfig.class);
        Mockito.when(authConfig.isAuthEnabled()).thenReturn(false);
        asyncProcessor = Mockito.mock(SuWebhookAsyncProcessor.class);
        compensationService = Mockito.mock(SuReservationWebhookCompensationService.class);

        controller = new SuWebhookController(
                new ObjectMapper(),
                storeRepository,
                otaReservationSyncService,
                idempotencyService,
                authConfig,
                asyncProcessor,
                compensationService
        );
    }

    @Test
    void reservationNotif_accountLevel_notifIds_returnsWebhookAckBodyAndQueuesJob() {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", 14L);
        Mockito.when(storeRepository.findBySuHotelId("S15KQEKHXP")).thenReturn(Optional.of(store));

        Mockito.when(idempotencyService.markProcessingAndReturnNew(Mockito.eq("S15KQEKHXP"), Mockito.anyList()))
                .thenReturn(Set.of("N1"));

        Mockito.when(compensationService.processDueEventsOnce(Mockito.anyInt())).thenReturn(1);

        String body = """
                {
                  "hotel_id": "S15KQEKHXP",
                  "reservation_notif": {
                    "reservation_notif_id": ["N1"]
                  }
                }
                """;

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<Map<String, Object>> response = controller.handleReservationNotif(req, body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> reservationNotif = (Map<String, Object>) response.getBody().get("reservation_notif");
        assertEquals(List.of("N1"), reservationNotif.get("reservation_notif_id"));

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(asyncProcessor).submit(Mockito.anyString(), taskCaptor.capture());
        taskCaptor.getValue().run();

        Mockito.verify(compensationService).recordPullNotifIds(Mockito.eq(14L), Mockito.eq("S15KQEKHXP"), Mockito.eq(Set.of("N1")));
        Mockito.verify(idempotencyService).markDone(Mockito.eq("S15KQEKHXP"), Mockito.eq(Set.of("N1")));
        Mockito.verify(compensationService).processDueEventsOnce(Mockito.anyInt());
    }

    @Test
    void reservationNotif_accountLevel_reservationPushWithoutNotifIds_returnsSuccess() {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", 6L);
        Mockito.when(storeRepository.findBySuHotelId("S15KQEKHXP")).thenReturn(Optional.of(store));

        Mockito.when(compensationService.processDueEventsOnce(Mockito.anyInt())).thenReturn(1);

        String body = """
                {
                  "reservations": [
                    {
                      "hotel_id": "S15KQEKHXP",
                      "id": "SU-1",
                      "reservation_notif_id": "N1",
                      "channel_booking_id": "275986510",
                      "status": "new",
                      "affiliation": { "OTA_Code": "19" },
                      "arrival_date": "2026-01-28",
                      "departure_date": "2026-01-29",
                      "guest_name": "xiao lin",
                      "totalprice": "100",
                      "rooms": [ { "id": "6-101" } ]
                    }
                  ]
                }
                """;

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<Map<String, Object>> response = controller.handleReservationNotif(req, body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> reservationNotif = (Map<String, Object>) response.getBody().get("reservation_notif");
        assertEquals(List.of("N1"), reservationNotif.get("reservation_notif_id"));

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(asyncProcessor).submit(Mockito.anyString(), taskCaptor.capture());
        taskCaptor.getValue().run();
        Mockito.verify(compensationService).recordPushReservations(Mockito.eq(6L), Mockito.eq("S15KQEKHXP"), Mockito.anyList());
        Mockito.verify(compensationService).processDueEventsOnce(Mockito.anyInt());
    }

    @Test
    void reservationNotif_storeLevel_reservationPushWithoutNotifIds_returnsSuccess() {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", 6L);
        Mockito.when(storeRepository.findBySuHotelId("S15KQEKHXP")).thenReturn(Optional.of(store));

        Mockito.when(compensationService.processDueEventsOnce(Mockito.anyInt())).thenReturn(1);

        String body = """
                {
                  "reservations": [
                    {
                      "hotel_id": "S15KQEKHXP",
                      "id": "SU-1",
                      "reservation_notif_id": "N1",
                      "channel_booking_id": "275986510",
                      "status": "new",
                      "affiliation": { "OTA_Code": "19" },
                      "arrival_date": "2026-01-28",
                      "departure_date": "2026-01-29",
                      "guest_name": "xiao lin",
                      "totalprice": "100",
                      "rooms": [ { "id": "6-101" } ]
                    }
                  ]
                }
                """;

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<Map<String, Object>> response = controller.handleReservationNotif(req, "S15KQEKHXP", body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> reservationNotif = (Map<String, Object>) response.getBody().get("reservation_notif");
        assertEquals(List.of("N1"), reservationNotif.get("reservation_notif_id"));

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(asyncProcessor).submit(Mockito.anyString(), taskCaptor.capture());
        taskCaptor.getValue().run();
        Mockito.verify(compensationService).recordPushReservations(Mockito.eq(6L), Mockito.eq("S15KQEKHXP"), Mockito.anyList());
        Mockito.verify(compensationService).processDueEventsOnce(Mockito.anyInt());
    }
}

