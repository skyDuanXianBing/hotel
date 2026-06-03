package server.demo.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.annotation.RequirePermission;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.registration.AdminRegistrationListItemDTO;
import server.demo.dto.registration.AdminRegistrationReviewRequest;
import server.demo.dto.registration.RegistrationMessageLogDTO;
import server.demo.dto.registration.RegistrationSendMessageRequest;
import server.demo.entity.Reservation;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ReservationRepository;
import server.demo.service.RegistrationAdminService;
import server.demo.service.RegistrationLinkService;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationAdminControllerTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void list_shouldMergeRoomNumberParamsAndForwardRoomGroupId() {
        RegistrationAdminService registrationAdminService = mock(RegistrationAdminService.class);
        RegistrationAdminController controller = new RegistrationAdminController();
        ReflectionTestUtils.setField(controller, "registrationAdminService", registrationAdminService);

        LocalDate checkInDate = LocalDate.of(2026, 5, 1);
        LocalDate checkOutDate = LocalDate.of(2026, 5, 3);

        ApiResponse<List<AdminRegistrationListItemDTO>> response = controller.list(
                RegistrationFormStatus.SUBMITTED,
                3L,
                ReservationStatus.CANCELLED,
                List.of("101", " ", "102"),
                List.of("102", "103"),
                9L,
                checkInDate,
                checkOutDate
        );

        assertTrue(response.isSuccess());
        ArgumentCaptor<List<String>> roomNumbersCaptor = ArgumentCaptor.forClass(List.class);
        verify(registrationAdminService).list(
                eq(RegistrationFormStatus.SUBMITTED),
                eq(3L),
                eq(ReservationStatus.CANCELLED),
                roomNumbersCaptor.capture(),
                eq(9L),
                eq(checkInDate),
                eq(checkOutDate)
        );
        assertEquals(List.of("101", "102", "103"), roomNumbersCaptor.getValue());
    }

    @Test
    void registrationReviewEndpoints_shouldRequireStatsViewPermission() throws NoSuchMethodException {
        assertStatsViewPermission(RegistrationAdminController.class.getMethod(
                "list",
                RegistrationFormStatus.class,
                Long.class,
                ReservationStatus.class,
                List.class,
                List.class,
                Long.class,
                LocalDate.class,
                LocalDate.class
        ));
        assertStatsViewPermission(RegistrationAdminController.class.getMethod("detail", Long.class));
        assertStatsViewPermission(RegistrationAdminController.class.getMethod(
                "approve",
                Long.class,
                AdminRegistrationReviewRequest.class
        ));
        assertStatsViewPermission(RegistrationAdminController.class.getMethod(
                "reject",
                Long.class,
                AdminRegistrationReviewRequest.class
        ));
        assertStatsViewPermission(RegistrationAdminController.class.getMethod(
                "sendMessage",
                Long.class,
                RegistrationSendMessageRequest.class
        ));
        assertStatsViewPermission(RegistrationAdminController.class.getMethod("downloadPdf", Long.class));
        assertStatsViewPermission(RegistrationAdminController.class.getMethod(
                "downloadAttachment",
                Long.class,
                Long.class
        ));
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

    private static void assertStatsViewPermission(Method method) {
        RequirePermission permission = method.getAnnotation(RequirePermission.class);
        assertNotNull(permission);
        assertEquals(PermissionModule.STATISTICS, permission.module());
        assertEquals(PermissionAction.VIEW_STATS, permission.action());
    }
}
