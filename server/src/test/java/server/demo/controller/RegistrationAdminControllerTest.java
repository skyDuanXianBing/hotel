package server.demo.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.entity.Reservation;
import server.demo.repository.ReservationRepository;
import server.demo.service.RegistrationLinkService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrationAdminControllerTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void generateLink_shouldUseFrontendBaseUrlForGuestAccessLink() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        RegistrationLinkService registrationLinkService = new RegistrationLinkService("test-secret", 90);
        RegistrationAdminController controller = new RegistrationAdminController();
        ReflectionTestUtils.setField(controller, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(controller, "registrationLinkService", registrationLinkService);
        ReflectionTestUtils.setField(controller, "frontendBaseUrl", "http://localhost:8091/");

        Reservation reservation = new Reservation();
        reservation.setStoreId(26L);
        reservation.setOrderNumber("ORDER-LOCAL");
        when(reservationRepository.findByStoreIdAndOrderNumber(26L, "ORDER-LOCAL"))
                .thenReturn(Optional.of(reservation));
        StoreContextHolder.setContext(new StoreContext(7L, 26L, "owner"));

        ApiResponse<String> response = controller.generateLink("ORDER-LOCAL");

        assertTrue(response.isSuccess());
        assertTrue(response.getData().startsWith("http://localhost:8091/r/ORDER-LOCAL?t="));
        assertFalse(response.getData().startsWith("http://localhost:8092/"));
    }
}
