package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomGroupMemberRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.SuReservationWebhookEventRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SuBusinessAutoMessageServiceRegistrationLinkTest {

    @Test
    void renderTemplate_shouldUseFrontendBaseUrlForRegistrationVariables() {
        RegistrationLinkService registrationLinkService = new RegistrationLinkService("test-secret", 90);
        SuBusinessAutoMessageService service = new SuBusinessAutoMessageService(
                mock(AutoMessageSendLogRepository.class),
                mock(StoreRepository.class),
                mock(ReservationRepository.class),
                mock(RoomTypeRepository.class),
                mock(RoomRepository.class),
                mock(RoomGroupMemberRepository.class),
                mock(SuMessageThreadRepository.class),
                mock(SuMessageRepository.class),
                mock(SuReservationWebhookEventRepository.class),
                mock(SuApiClient.class),
                mock(SuAccessTokenService.class),
                mock(SuMessagingRealtimeGateway.class),
                new ObjectMapper(),
                registrationLinkService,
                "http://localhost:8091/",
                "Auto Message"
        );

        Reservation reservation = new Reservation();
        reservation.setStoreId(26L);
        reservation.setOrderNumber("ORDER-LOCAL");
        reservation.setChannelOrderNumber("BOOK-LOCAL");

        Store store = new Store();
        store.setName("Local Hotel");

        String rendered = ReflectionTestUtils.invokeMethod(
                service,
                "renderTemplate",
                store,
                reservation,
                "{{registration_link}} {{checkin_form_link}}"
        );

        String[] links = rendered.split(" ");
        assertEquals(2, links.length);
        assertEquals(links[0], links[1]);
        assertTrue(links[0].startsWith("http://localhost:8091/rb/BOOK-LOCAL?t="));
        assertFalse(links[0].startsWith("http://localhost:8092/"));

        String token = links[0].substring(links[0].indexOf("?t=") + 3);
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken("BOOK-LOCAL", token);
        assertEquals(26L, claims.getStoreId());
    }
}
