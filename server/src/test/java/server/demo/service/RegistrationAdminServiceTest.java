package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.registration.AdminRegistrationDetailDTO;
import server.demo.dto.registration.AdminRegistrationListItemDTO;
import server.demo.dto.registration.AdminRegistrationReviewRequest;
import server.demo.entity.Channel;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationReviewLog;
import server.demo.entity.Reservation;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.RegistrationReviewAction;
import server.demo.enums.ReservationStatus;
import server.demo.repository.RegistrationAttachmentRepository;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.repository.RegistrationMessageLogRepository;
import server.demo.repository.RegistrationReviewLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.util.StoreContextUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    void list_shouldForwardReservationAndRoomFiltersAndMapReservationStatus() {
        RegistrationAdminService service = createService();
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
                eq(true),
                eq(List.of("101", "102")),
                eq(7L),
                isNull(),
                isNull()
        )).thenReturn(List.of(form));

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            List<AdminRegistrationListItemDTO> result = service.list(
                    null,
                    null,
                    ReservationStatus.CANCELLED,
                    List.of(" 101 ", "", "102", "101"),
                    7L,
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
                true,
                List.of("101", "102"),
                7L,
                null,
                null
        );
    }

    @Test
    void detail_shouldMapReservationStatus() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CANCELLED);
        Reservation reservation = form.getReservation();
        reservation.setChannelOrderNumber("OTA-8");
        reservation.setGuestName("Test Guest");
        reservation.setCheckInDate(LocalDate.of(2026, 5, 1));
        reservation.setCheckOutDate(LocalDate.of(2026, 5, 3));

        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(reservation));
        when(registrationGuestRepository.findByFormIdOrderBySortOrderAsc(8L)).thenReturn(List.of());
        when(registrationAttachmentRepository.findByFormId(8L)).thenReturn(List.of());
        when(registrationReviewLogRepository.findByFormIdOrderByCreatedAtDesc(8L)).thenReturn(List.of());
        when(registrationMessageLogRepository.findByFormIdOrderByCreatedAtDesc(8L)).thenReturn(List.of());

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            AdminRegistrationDetailDTO result = service.detail(8L);

            assertEquals(ReservationStatus.CANCELLED, result.getReservationStatus());
        }
    }

    @Test
    void approve_shouldRejectCancelledReservationAndNotWriteLog() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CANCELLED);
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approve(8L, null));

            assertEquals("已取消订单不能审核登记表", ex.getMessage());
        }

        verify(registrationFormRepository, never()).approveSubmitted(
                anyLong(),
                anyLong(),
                any(),
                any(LocalDateTime.class)
        );
        verify(registrationReviewLogRepository, never()).save(any(RegistrationReviewLog.class));
    }

    @Test
    void reject_shouldRejectNonSubmittedFormAndNotWriteLog() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.APPROVED, ReservationStatus.CONFIRMED);
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.reject(8L, null));

            assertEquals("只有已提交的登记表可以审核", ex.getMessage());
        }

        verify(registrationFormRepository, never()).rejectSubmitted(
                anyLong(),
                anyLong(),
                any(),
                any(LocalDateTime.class)
        );
        verify(registrationReviewLogRepository, never()).save(any(RegistrationReviewLog.class));
    }

    @Test
    void approve_shouldNotWriteLogWhenSubmittedStateChangedConcurrently() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CONFIRMED);
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("ok");
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.approveSubmitted(
                eq(26L),
                eq(8L),
                eq("ok"),
                any(LocalDateTime.class)
        )).thenReturn(0);

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.approve(8L, req));

            assertEquals("登记表状态已变更，请刷新后重试", ex.getMessage());
        }

        verify(registrationReviewLogRepository, never()).save(any(RegistrationReviewLog.class));
    }

    @Test
    void approve_shouldWriteLogAfterConditionalUpdate() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CONFIRMED);
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("approved");
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.approveSubmitted(
                eq(26L),
                eq(8L),
                eq("approved"),
                any(LocalDateTime.class)
        )).thenReturn(1);

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            service.approve(8L, req);
        }

        org.mockito.ArgumentCaptor<RegistrationReviewLog> logCaptor =
                org.mockito.ArgumentCaptor.forClass(RegistrationReviewLog.class);
        verify(registrationReviewLogRepository).save(logCaptor.capture());
        assertEquals(RegistrationReviewAction.APPROVE, logCaptor.getValue().getAction());
        assertEquals(7L, logCaptor.getValue().getOperatorUserId());
        assertEquals("approved", logCaptor.getValue().getNote());
    }

    private RegistrationAdminService createService() {
        RegistrationAdminService service = new RegistrationAdminService();
        ReflectionTestUtils.setField(service, "registrationFormRepository", registrationFormRepository);
        ReflectionTestUtils.setField(service, "registrationGuestRepository", registrationGuestRepository);
        ReflectionTestUtils.setField(service, "registrationReviewLogRepository", registrationReviewLogRepository);
        ReflectionTestUtils.setField(service, "registrationMessageLogRepository", registrationMessageLogRepository);
        ReflectionTestUtils.setField(service, "registrationAttachmentRepository", registrationAttachmentRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        return service;
    }

    private static RegistrationForm createForm(
            Long formId,
            RegistrationFormStatus formStatus,
            ReservationStatus reservationStatus
    ) {
        Reservation reservation = new Reservation();
        reservation.setId(88L);
        reservation.setStoreId(26L);
        reservation.setOrderNumber("ORD-8");
        reservation.setStatus(reservationStatus);

        RegistrationForm form = new RegistrationForm();
        form.setId(formId);
        form.setReservation(reservation);
        form.setStoreId(26L);
        form.setOrderNumber("ORD-8");
        form.setStatus(formStatus);
        return form;
    }
}
