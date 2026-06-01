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
import server.demo.entity.Room;
import server.demo.entity.RoomType;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
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
    void list_shouldForwardReservationStatusAndStayDateBoundaryFiltersAndMapReservationStatus() {
        RegistrationAdminService service = new RegistrationAdminService();
        ReflectionTestUtils.setField(service, "registrationFormRepository", registrationFormRepository);
        ReflectionTestUtils.setField(service, "registrationGuestRepository", registrationGuestRepository);
        ReflectionTestUtils.setField(service, "registrationReviewLogRepository", registrationReviewLogRepository);
        ReflectionTestUtils.setField(service, "registrationMessageLogRepository", registrationMessageLogRepository);
        ReflectionTestUtils.setField(service, "registrationAttachmentRepository", registrationAttachmentRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        LocalDate checkInDate = LocalDate.of(2026, 5, 1);
        LocalDate checkOutDate = LocalDate.of(2026, 5, 3);

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

        RoomType roomType = new RoomType();
        roomType.setName("Double");
        Room room = new Room();
        room.setRoomNumber("301");
        room.setRoomType(roomType);
        reservation.setRoom(room);

        Channel channel = new Channel();
        channel.setName("Booking.com");
        reservation.setChannel(channel);
        form.setReservation(reservation);

        when(registrationFormRepository.searchForAdminList(
                eq(26L),
                isNull(),
                isNull(),
                eq(ReservationStatus.CANCELLED),
                eq(checkInDate),
                eq(checkOutDate),
                isNull(),
                eq(9L)
        )).thenReturn(List.of(form));

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            List<AdminRegistrationListItemDTO> result = service.list(
                    null,
                    null,
                    ReservationStatus.CANCELLED,
                    checkInDate,
                    checkOutDate,
                    List.of(" 301 "),
                    9L
            );

            assertEquals(1, result.size());
            assertEquals(ReservationStatus.CANCELLED, result.get(0).getReservationStatus());
            assertEquals("Booking.com", result.get(0).getChannelName());
            assertEquals("OTA-8", result.get(0).getChannelOrderNumber());
            assertEquals("301", result.get(0).getRoomNumber());
            assertEquals("Double", result.get(0).getRoomTypeName());
        }

        verify(registrationFormRepository).searchForAdminList(
                26L,
                null,
                null,
                ReservationStatus.CANCELLED,
                checkInDate,
                checkOutDate,
                null,
                9L
        );
    }

    @Test
    void approve_shouldRejectCancelledReservation() {
        RegistrationAdminService service = new RegistrationAdminService();
        ReflectionTestUtils.setField(service, "registrationFormRepository", registrationFormRepository);
        ReflectionTestUtils.setField(service, "registrationReviewLogRepository", registrationReviewLogRepository);

        Reservation reservation = new Reservation();
        reservation.setStoreId(26L);
        reservation.setStatus(ReservationStatus.CANCELLED);

        RegistrationForm form = new RegistrationForm();
        form.setId(12L);
        form.setReservation(reservation);

        when(registrationFormRepository.findById(12L)).thenReturn(Optional.of(form));

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            RuntimeException error = assertThrows(RuntimeException.class, () -> service.approve(12L, null));
            assertEquals("已取消订单不可审查", error.getMessage());
        }

        verify(registrationFormRepository, never()).save(form);
    }
}
