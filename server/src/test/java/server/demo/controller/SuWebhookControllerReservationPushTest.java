package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.config.SuMessagingWebhookAuthConfig;
import server.demo.entity.Store;
import server.demo.service.OtaReservationSyncService;
import server.demo.service.SuWebhookIdempotencyService;
import server.demo.repository.StoreRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuWebhookControllerReservationPushTest {

    private SuWebhookController controller;
    private StoreRepository storeRepository;
    private OtaReservationSyncService otaReservationSyncService;

    @BeforeEach
    void setUp() {
        storeRepository = Mockito.mock(StoreRepository.class);
        otaReservationSyncService = Mockito.mock(OtaReservationSyncService.class);
        SuWebhookIdempotencyService idempotencyService = Mockito.mock(SuWebhookIdempotencyService.class);
        SuMessagingWebhookAuthConfig authConfig = Mockito.mock(SuMessagingWebhookAuthConfig.class);
        Mockito.when(authConfig.isAuthEnabled()).thenReturn(false);

        controller = new SuWebhookController(
                new ObjectMapper(),
                storeRepository,
                otaReservationSyncService,
                idempotencyService,
                authConfig
        );
    }

    @Test
    void reservationNotif_accountLevel_reservationPushWithoutNotifIds_returnsSuccess() {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", 6L);
        Mockito.when(storeRepository.findBySuHotelId("S15KQEKHXP")).thenReturn(Optional.of(store));

        Mockito.when(otaReservationSyncService.upsertReservationsFromWebhook(Mockito.eq(6L), Mockito.anyList()))
                .thenReturn(new OtaReservationSyncService.UpsertOnlyResult(1, 0, 1, 0, 0, List.of()));

        String body = """
                {
                  "reservations": [
                    {
                      "hotel_id": "S15KQEKHXP",
                      "id": "SU-1",
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
        assertEquals("Success", response.getBody().get("Status"));
        Mockito.verify(otaReservationSyncService).upsertReservationsFromWebhook(Mockito.eq(6L), Mockito.anyList());
    }

    @Test
    void reservationNotif_storeLevel_reservationPushWithoutNotifIds_returnsSuccess() {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", 6L);
        Mockito.when(storeRepository.findBySuHotelId("S15KQEKHXP")).thenReturn(Optional.of(store));

        Mockito.when(otaReservationSyncService.upsertReservationsFromWebhook(Mockito.eq(6L), Mockito.anyList()))
                .thenReturn(new OtaReservationSyncService.UpsertOnlyResult(1, 0, 1, 0, 0, List.of()));

        String body = """
                {
                  "reservations": [
                    {
                      "hotel_id": "S15KQEKHXP",
                      "id": "SU-1",
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
        assertEquals("Success", response.getBody().get("Status"));
        Mockito.verify(otaReservationSyncService).upsertReservationsFromWebhook(Mockito.eq(6L), Mockito.anyList());
    }
}

