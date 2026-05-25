package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationMessageLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrationMessageServiceTest {

    @Test
    void buildVariables_shouldUseFrontendBaseUrlForRegistrationLink() {
        RegistrationLinkService registrationLinkService = new RegistrationLinkService("test-secret", 90);
        ReservationBookingKeyResolver bookingKeyResolver = mock(ReservationBookingKeyResolver.class);
        RegistrationMessageService service = new RegistrationMessageService(
                mock(RegistrationFormRepository.class),
                mock(ReservationRepository.class),
                mock(StoreRepository.class),
                mock(SuMessageThreadRepository.class),
                mock(SuMessagingService.class),
                mock(RegistrationMessageLogRepository.class),
                registrationLinkService,
                bookingKeyResolver,
                "http://localhost:8091/"
        );

        Reservation reservation = new Reservation();
        reservation.setStoreId(26L);
        reservation.setOrderNumber("ORDER-LOCAL");
        when(bookingKeyResolver.resolvePrimaryBookingKey(reservation)).thenReturn("BOOK-LOCAL");

        Store store = new Store();
        store.setName("Local Hotel");

        @SuppressWarnings("unchecked")
        Map<String, String> variables = ReflectionTestUtils.invokeMethod(
                service,
                "buildVariables",
                store,
                reservation
        );

        String registrationLink = variables.get("registration_link");
        assertTrue(registrationLink.startsWith("http://localhost:8091/rb/BOOK-LOCAL?t="));
        assertFalse(registrationLink.startsWith("http://localhost:8092/"));

        String token = registrationLink.substring(registrationLink.indexOf("?t=") + 3);
        RegistrationLinkService.Claims claims = registrationLinkService.verifyToken("BOOK-LOCAL", token);
        assertEquals(26L, claims.getStoreId());
    }
}
