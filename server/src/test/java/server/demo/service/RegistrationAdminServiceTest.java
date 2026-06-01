package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.registration.AdminRegistrationListItemDTO;
import server.demo.entity.Channel;
import server.demo.entity.RegistrationForm;
import server.demo.entity.Reservation;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.ReservationStatus;
import server.demo.repository.RegistrationAttachmentRepository;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.repository.RegistrationMessageLogRepository;
import server.demo.repository.RegistrationReviewLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.util.StoreContextUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationAdminServiceTest {

    @Mock
    private RegistrationFormRepository registrationFormRepository;

    @Mock
    private RegistrationGuestRepository registrationGuestRepository;

    @Mock
    private RegistrationReviewLogRepository registrationReviewLogRepository;

    @Mock
    private RegistrationMessageLogRepository registrationMessageLogRepository;

    @Mock
    private RegistrationAttachmentRepository registrationAttachmentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Test
    void list_shouldForwardReservationStatusFilterAndMapReservationStatus() {
        RegistrationAdminService service = new RegistrationAdminService();
        ReflectionTestUtils.setField(service, "registrationFormRepository", registrationFormRepository);
        ReflectionTestUtils.setField(service, "registrationGuestRepository", registrationGuestRepository);
        ReflectionTestUtils.setField(service, "registrationReviewLogRepository", registrationReviewLogRepository);
        ReflectionTestUtils.setField(service, "registrationMessageLogRepository", registrationMessageLogRepository);
        ReflectionTestUtils.setField(service, "registrationAttachmentRepository", registrationAttachmentRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);

        RegistrationForm form = new RegistrationForm();
        form.setId(8L);
        form.setOrderNumber("ORD-8");
        form.setStatus(RegistrationFormStatus.SUBMITTED);

        Reservation reservation = new Reservation();
        reservation.setGuestName("Test Guest");
        reservation.setCheckInDate(LocalDate.of(2026, 5, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 5, 3));
        reservation.setChannelOrderNumber("OTA-8");
        reservation.setStatus(ReservationStatus.CANCELLED);

        Channel channel = new Channel();
        channel.setName("Booking.com");
        reservation.setChannel(channel);
        form.setReservation(reservation);

        when(registrationFormRepository.searchForAdminList(
                eq(26L),
                isNull(),
                isNull(),
                eq(ReservationStatus.CANCELLED),
                isNull(),
                isNull()
        )).thenReturn(List.of(form));

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            List<AdminRegistrationListItemDTO> result = service.list(
                    null,
                    null,
                    ReservationStatus.CANCELLED,
                    null,
                    null
            );

            assertEquals(1, result.size());
            assertEquals(ReservationStatus.CANCELLED, result.get(0).getReservationStatus());
            assertEquals("Booking.com", result.get(0).getChannelName());
            assertEquals("OTA-8", result.get(0).getChannelOrderNumber());
        }

        verify(registrationFormRepository).searchForAdminList(
                26L,
                null,
                null,
                ReservationStatus.CANCELLED,
                null,
                null
        );
    }
}
