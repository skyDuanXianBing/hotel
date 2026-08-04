package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.registration.AdminRegistrationReviewResponse;
import server.demo.dto.registration.AdminRegistrationDetailDTO;
import server.demo.dto.registration.AdminRegistrationListItemDTO;
import server.demo.dto.registration.AdminRegistrationReviewRequest;
import server.demo.dto.registration.RegistrationMessageLogDTO;
import server.demo.dto.registration.RegistrationSendMessageRequest;
import server.demo.entity.Channel;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationReviewLog;
import server.demo.entity.RegistrationReviewSettings;
import server.demo.entity.Reservation;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.RegistrationMessageType;
import server.demo.enums.RegistrationReviewAction;
import server.demo.enums.ReservationStatus;
import server.demo.exception.RegistrationReviewConflictException;
import server.demo.repository.RegistrationAttachmentRepository;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.repository.RegistrationMessageLogRepository;
import server.demo.repository.RegistrationReviewLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.StoreContextUtils;
import server.demo.i18n.TestApiMessages;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationAdminServiceTest {

    @BeforeAll
    static void installMessages() {
        TestApiMessages.install();
    }

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

    @Mock
    private RegistrationMessageService registrationMessageService;

    @Mock
    private SuMessagingRealtimeGateway realtimeGateway;

    @Mock
    private RegistrationReviewSettingsService registrationReviewSettingsService;

    @Mock
    private StoreRepository storeRepository;

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
                eq(true),
                eq(List.of("101", "102")),
                eq(7L),
                isNull(),
                isNull(),
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
                    null,
                    null,
                    null,
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
                true,
                List.of("101", "102"),
                7L,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void listPage_shouldReturnPagedItemsAndClampSize() {
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
        reservation.setStatus(ReservationStatus.CONFIRMED);
        Channel channel = new Channel();
        channel.setName("Booking.com");
        reservation.setChannel(channel);
        form.setReservation(reservation);

        when(registrationFormRepository.searchForAdminListPage(
                eq(26L),
                eq(RegistrationFormStatus.SUBMITTED),
                isNull(),
                isNull(),
                eq(false),
                eq(false),
                anyList(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(PageRequest.of(0, 100))
        )).thenReturn(new PageImpl<>(List.of(form), PageRequest.of(0, 100), 1));

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            Page<AdminRegistrationListItemDTO> result = service.listPage(
                    RegistrationFormStatus.SUBMITTED,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    500
            );

            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
            assertEquals(8L, result.getContent().get(0).getFormId());
            assertEquals("Booking.com", result.getContent().get(0).getChannelName());
        }
    }

    @Test
    void list_shouldOnlyIncludeCancelledArchiveForExplicitCancelledFilter() {
        RegistrationAdminService service = createService();
        when(registrationFormRepository.searchForAdminList(
                eq(26L),
                any(),
                any(),
                any(),
                anyBoolean(),
                anyBoolean(),
                anyList(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            service.list(null, null, null, null, null, null, null, null, null, null, null);
            service.list(null, null, ReservationStatus.CANCELLED, null, null, null, null, null, null, null, null);
            service.list(null, null, ReservationStatus.CONFIRMED, null, null, null, null, null, null, null, null);
        }

        ArgumentCaptor<Boolean> includeCancelledArchiveCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(registrationFormRepository, times(3)).searchForAdminList(
                eq(26L),
                any(),
                any(),
                any(),
                includeCancelledArchiveCaptor.capture(),
                anyBoolean(),
                anyList(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        assertEquals(List.of(false, true, false), includeCancelledArchiveCaptor.getAllValues());
    }

    @Test
    void list_shouldForwardDateRangesToRepository() {
        RegistrationAdminService service = createService();
        LocalDate checkInStartDate = LocalDate.of(2026, 6, 1);
        LocalDate checkInEndDate = LocalDate.of(2026, 6, 30);
        LocalDate checkOutStartDate = LocalDate.of(2026, 5, 1);
        LocalDate checkOutEndDate = LocalDate.of(2026, 5, 31);

        when(registrationFormRepository.searchForAdminList(
                eq(26L),
                isNull(),
                isNull(),
                isNull(),
                eq(false),
                eq(true),
                eq(List.of("101")),
                isNull(),
                eq(checkInStartDate),
                eq(checkInEndDate),
                eq(checkOutStartDate),
                eq(checkOutEndDate)
        )).thenReturn(List.of());

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            service.list(
                    null,
                    null,
                    null,
                    List.of("101"),
                    null,
                    null,
                    null,
                    checkInStartDate,
                    checkInEndDate,
                    checkOutStartDate,
                    checkOutEndDate
            );
        }

        verify(registrationFormRepository).searchForAdminList(
                26L,
                null,
                null,
                null,
                false,
                true,
                List.of("101"),
                null,
                checkInStartDate,
                checkInEndDate,
                checkOutStartDate,
                checkOutEndDate
        );
    }

    @Test
    void list_shouldForwardSingleSidedDateRangesToRepository() {
        RegistrationAdminService service = createService();
        LocalDate checkInStartDate = LocalDate.of(2026, 6, 1);
        LocalDate checkOutEndDate = LocalDate.of(2026, 5, 31);

        when(registrationFormRepository.searchForAdminList(
                eq(26L),
                isNull(),
                isNull(),
                isNull(),
                eq(false),
                eq(true),
                eq(List.of("101")),
                isNull(),
                eq(checkInStartDate),
                isNull(),
                isNull(),
                eq(checkOutEndDate)
        )).thenReturn(List.of());

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            service.list(
                    null,
                    null,
                    null,
                    List.of("101"),
                    null,
                    null,
                    null,
                    checkInStartDate,
                    null,
                    null,
                    checkOutEndDate
            );
        }

        verify(registrationFormRepository).searchForAdminList(
                26L,
                null,
                null,
                null,
                false,
                true,
                List.of("101"),
                null,
                checkInStartDate,
                null,
                null,
                checkOutEndDate
        );
    }

    @Test
    void list_shouldConvertLegacySingleDatesToSameDayRanges() {
        RegistrationAdminService service = createService();
        LocalDate checkInDate = LocalDate.of(2026, 6, 15);
        LocalDate checkOutDate = LocalDate.of(2026, 5, 20);

        when(registrationFormRepository.searchForAdminList(
                eq(26L),
                isNull(),
                isNull(),
                isNull(),
                eq(false),
                eq(true),
                eq(List.of("101")),
                isNull(),
                eq(checkInDate),
                eq(checkInDate),
                eq(checkOutDate),
                eq(checkOutDate)
        )).thenReturn(List.of());

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            service.list(
                    null,
                    null,
                    null,
                    List.of("101"),
                    null,
                    checkInDate,
                    checkOutDate,
                    null,
                    null,
                    null,
                    null
            );
        }

        verify(registrationFormRepository).searchForAdminList(
                26L,
                null,
                null,
                null,
                false,
                true,
                List.of("101"),
                null,
                checkInDate,
                checkInDate,
                checkOutDate,
                checkOutDate
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
        Channel channel = new Channel();
        channel.setName("Booking.com");
        reservation.setChannel(channel);

        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(reservation));
        when(registrationGuestRepository.findByFormIdOrderBySortOrderAsc(8L)).thenReturn(List.of());
        when(registrationAttachmentRepository.findByFormId(8L)).thenReturn(List.of());
        when(registrationReviewLogRepository.findByFormIdOrderByCreatedAtDesc(8L)).thenReturn(List.of());
        when(registrationMessageLogRepository.findByFormIdOrderByCreatedAtDesc(8L)).thenReturn(List.of());

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);

            AdminRegistrationDetailDTO result = service.detail(8L);

            assertEquals(88L, result.getReservationId());
            assertEquals(ReservationStatus.CANCELLED, result.getReservationStatus());
            assertEquals("Booking.com", result.getChannelName());
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

            RegistrationReviewConflictException ex = assertThrows(
                    RegistrationReviewConflictException.class,
                    () -> service.approve(8L, null)
            );

            assertEquals("已取消订单不能审核登记表", ex.getMessage());
        }

        verify(registrationFormRepository, never()).approveSubmitted(
                anyLong(),
                anyLong(),
                any(),
                any(LocalDateTime.class)
        );
        verify(registrationReviewLogRepository, never()).save(any(RegistrationReviewLog.class));
        verify(registrationMessageService, never()).sendMessage(
                anyLong(), anyLong(), anyLong(), any(RegistrationSendMessageRequest.class)
        );
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

            RegistrationReviewConflictException ex = assertThrows(
                    RegistrationReviewConflictException.class,
                    () -> service.reject(8L, null)
            );

            assertEquals("该登记表已审核", ex.getMessage());
        }

        verify(registrationFormRepository, never()).rejectSubmitted(
                anyLong(),
                anyLong(),
                any(),
                any(LocalDateTime.class)
        );
        verify(registrationReviewLogRepository, never()).save(any(RegistrationReviewLog.class));
        verify(registrationMessageService, never()).sendMessage(
                anyLong(), anyLong(), anyLong(), any(RegistrationSendMessageRequest.class)
        );
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

            RegistrationReviewConflictException ex = assertThrows(
                    RegistrationReviewConflictException.class,
                    () -> service.approve(8L, req)
            );

            assertEquals("登记表状态已变更，请刷新后重试", ex.getMessage());
        }

        verify(registrationReviewLogRepository, never()).save(any(RegistrationReviewLog.class));
        verify(registrationMessageService, never()).sendMessage(
                anyLong(), anyLong(), anyLong(), any(RegistrationSendMessageRequest.class)
        );
    }

    @Test
    void reject_shouldNotWriteLogOrSendMessageWhenSubmittedStateChangedConcurrently() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CONFIRMED);
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("missing passport");
        req.setGuestMessage("Please upload passport again");
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.rejectSubmitted(
                eq(26L),
                eq(8L),
                eq("missing passport"),
                any(LocalDateTime.class)
        )).thenReturn(0);

        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            RegistrationReviewConflictException ex = assertThrows(
                    RegistrationReviewConflictException.class,
                    () -> service.reject(8L, req)
            );

            assertEquals("登记表状态已变更，请刷新后重试", ex.getMessage());
        }

        verify(registrationReviewLogRepository, never()).save(any(RegistrationReviewLog.class));
        verify(registrationMessageService, never()).sendMessage(
                anyLong(), anyLong(), anyLong(), any(RegistrationSendMessageRequest.class)
        );
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
        verify(realtimeGateway).broadcastWorkbenchInvalidated(26L, "registration_review");
    }

    @Test
    void approve_shouldSendGuestMessageWithTranslationFlag() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CONFIRMED);
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("approved");
        req.setGuestMessage(" Please send this to the guest ");
        req.setSenderName("Front Desk");
        RegistrationMessageLogDTO messageLog = new RegistrationMessageLogDTO();

        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.approveSubmitted(
                eq(26L),
                eq(8L),
                eq("approved"),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(registrationMessageService.sendMessage(
                eq(26L),
                eq(7L),
                eq(8L),
                any(RegistrationSendMessageRequest.class)
        )).thenReturn(messageLog);

        AdminRegistrationReviewResponse result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            result = service.approve(8L, req);
        }

        ArgumentCaptor<RegistrationSendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(RegistrationSendMessageRequest.class);
        verify(registrationMessageService).sendMessage(eq(26L), eq(7L), eq(8L), requestCaptor.capture());
        RegistrationSendMessageRequest messageRequest = requestCaptor.getValue();
        assertTrue(result.isMessageAttempted());
        assertSame(messageLog, result.getMessageLog());
        assertEquals(RegistrationMessageType.APPROVED_INFO, messageRequest.getType());
        assertEquals("Please send this to the guest", messageRequest.getContent());
        assertEquals("Front Desk", messageRequest.getSenderName());
        assertTrue(messageRequest.isTranslateBeforeSend());
    }

    @Test
    void reject_shouldSendGuestMessageAsRejectRequestWithTranslationFlag() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CONFIRMED);
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("missing passport");
        req.setGuestMessage("Please upload passport again");
        req.setSenderName("Review Team");
        RegistrationMessageLogDTO messageLog = new RegistrationMessageLogDTO();

        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.rejectSubmitted(
                eq(26L),
                eq(8L),
                eq("missing passport"),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(registrationMessageService.sendMessage(
                eq(26L),
                eq(7L),
                eq(8L),
                any(RegistrationSendMessageRequest.class)
        )).thenReturn(messageLog);

        AdminRegistrationReviewResponse result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            result = service.reject(8L, req);
        }

        ArgumentCaptor<RegistrationSendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(RegistrationSendMessageRequest.class);
        verify(registrationMessageService).sendMessage(eq(26L), eq(7L), eq(8L), requestCaptor.capture());
        RegistrationSendMessageRequest messageRequest = requestCaptor.getValue();
        assertTrue(result.isMessageAttempted());
        assertSame(messageLog, result.getMessageLog());
        assertEquals(RegistrationMessageType.REJECT_REQUEST, messageRequest.getType());
        assertEquals("Please upload passport again", messageRequest.getContent());
        assertEquals("Review Team", messageRequest.getSenderName());
        assertTrue(messageRequest.isTranslateBeforeSend());
    }

    @Test
    void approve_shouldMarkReviewedWhenCheckInOutsideFinalizeWindow() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CONFIRMED);
        form.getReservation().setCheckInDate(LocalDate.of(2026, 10, 7));
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("looks ok");
        req.setGuestMessage("信息无误，入住前一周将通过");
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.markReviewed(eq(26L), eq(8L), eq("looks ok"))).thenReturn(1);

        AdminRegistrationReviewResponse result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            result = service.approve(8L, req);
        }

        assertEquals(RegistrationFormStatus.REVIEWED.name(), result.getFormStatus());
        verify(registrationFormRepository, never()).approveSubmitted(
                anyLong(), anyLong(), any(), any(LocalDateTime.class));
        ArgumentCaptor<RegistrationSendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(RegistrationSendMessageRequest.class);
        verify(registrationMessageService).sendMessage(eq(26L), eq(7L), eq(8L), requestCaptor.capture());
        assertEquals(RegistrationMessageType.REVIEWED_INFO, requestCaptor.getValue().getType());
        assertEquals("信息无误，入住前一周将通过", requestCaptor.getValue().getContent());
    }

    @Test
    void approve_shouldFinalizeImmediatelyWhenCheckInWithinFinalizeWindow() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.SUBMITTED, ReservationStatus.CONFIRMED);
        form.getReservation().setCheckInDate(LocalDate.of(2026, 7, 5));
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("approved");
        req.setGuestMessage("审查已通过");
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.approveSubmitted(
                eq(26L), eq(8L), eq("approved"), any(LocalDateTime.class))).thenReturn(1);

        AdminRegistrationReviewResponse result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            result = service.approve(8L, req);
        }

        assertEquals(RegistrationFormStatus.APPROVED.name(), result.getFormStatus());
        verify(registrationFormRepository, never()).markReviewed(anyLong(), anyLong(), any());
        ArgumentCaptor<RegistrationSendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(RegistrationSendMessageRequest.class);
        verify(registrationMessageService).sendMessage(eq(26L), eq(7L), eq(8L), requestCaptor.capture());
        assertEquals(RegistrationMessageType.APPROVED_INFO, requestCaptor.getValue().getType());
    }

    @Test
    void approve_shouldFinalizeImmediatelyWhenFormAlreadyReviewed() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.REVIEWED, ReservationStatus.CONFIRMED);
        form.getReservation().setCheckInDate(LocalDate.of(2026, 12, 1));
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("final approve early");
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.approveSubmitted(
                eq(26L), eq(8L), eq("final approve early"), any(LocalDateTime.class))).thenReturn(1);

        AdminRegistrationReviewResponse result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            result = service.approve(8L, req);
        }

        assertEquals(RegistrationFormStatus.APPROVED.name(), result.getFormStatus());
        verify(registrationFormRepository, never()).markReviewed(anyLong(), anyLong(), any());
    }

    @Test
    void reject_shouldAllowReviewedForm() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.REVIEWED, ReservationStatus.CONFIRMED);
        AdminRegistrationReviewRequest req = new AdminRegistrationReviewRequest();
        req.setNote("found issue later");
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(form.getReservation()));
        when(registrationFormRepository.rejectSubmitted(
                eq(26L), eq(8L), eq("found issue later"), any(LocalDateTime.class))).thenReturn(1);

        AdminRegistrationReviewResponse result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(26L);
            storeContextUtils.when(StoreContextUtils::requireUserId).thenReturn(7L);

            result = service.reject(8L, req);
        }

        assertEquals(RegistrationFormStatus.REJECTED.name(), result.getFormStatus());
        verify(registrationFormRepository).rejectSubmitted(
                eq(26L), eq(8L), eq("found issue later"), any(LocalDateTime.class));
    }

    @Test
    void autoFinalizeForm_shouldFinalizeAndSendConfiguredMessage() {
        RegistrationAdminService service = createService();
        RegistrationForm form = createForm(8L, RegistrationFormStatus.REVIEWED, ReservationStatus.CONFIRMED);
        RegistrationReviewSettings settings = RegistrationReviewSettings.defaultsFor(26L);
        when(registrationFormRepository.finalizeReviewed(eq(26L), eq(8L), any(LocalDateTime.class))).thenReturn(1);
        when(registrationFormRepository.findById(8L)).thenReturn(Optional.of(form));
        when(registrationReviewSettingsService.resolveFinalMessage(settings)).thenReturn("审查已通过，入住指南已开放");
        when(registrationMessageService.sendMessage(
                eq(26L), isNull(), eq(8L), any(RegistrationSendMessageRequest.class)))
                .thenReturn(new RegistrationMessageLogDTO());

        service.autoFinalizeForm(26L, 8L, settings);

        ArgumentCaptor<RegistrationReviewLog> logCaptor =
                ArgumentCaptor.forClass(RegistrationReviewLog.class);
        verify(registrationReviewLogRepository).save(logCaptor.capture());
        assertEquals(RegistrationReviewAction.AUTO_APPROVE, logCaptor.getValue().getAction());
        assertNull(logCaptor.getValue().getOperatorUserId());
        ArgumentCaptor<RegistrationSendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(RegistrationSendMessageRequest.class);
        verify(registrationMessageService).sendMessage(eq(26L), isNull(), eq(8L), requestCaptor.capture());
        assertEquals(RegistrationMessageType.APPROVED_INFO, requestCaptor.getValue().getType());
        assertEquals("审查已通过，入住指南已开放", requestCaptor.getValue().getContent());
        assertTrue(requestCaptor.getValue().isTranslateBeforeSend());
        verify(realtimeGateway).broadcastWorkbenchInvalidated(26L, "registration_review");
    }

    @Test
    void autoFinalizeForm_shouldSkipWhenStateAlreadyChanged() {
        RegistrationAdminService service = createService();
        RegistrationReviewSettings settings = RegistrationReviewSettings.defaultsFor(26L);
        when(registrationFormRepository.finalizeReviewed(eq(26L), eq(8L), any(LocalDateTime.class))).thenReturn(0);

        service.autoFinalizeForm(26L, 8L, settings);

        verify(registrationReviewLogRepository, never()).save(any(RegistrationReviewLog.class));
        verify(registrationMessageService, never()).sendMessage(
                anyLong(), any(), anyLong(), any(RegistrationSendMessageRequest.class));
        verify(realtimeGateway, never()).broadcastWorkbenchInvalidated(anyLong(), any());
    }

    private RegistrationAdminService createService() {
        RegistrationAdminService service = new RegistrationAdminService();
        ReflectionTestUtils.setField(service, "registrationFormRepository", registrationFormRepository);
        ReflectionTestUtils.setField(service, "registrationGuestRepository", registrationGuestRepository);
        ReflectionTestUtils.setField(service, "registrationReviewLogRepository", registrationReviewLogRepository);
        ReflectionTestUtils.setField(service, "registrationMessageLogRepository", registrationMessageLogRepository);
        ReflectionTestUtils.setField(service, "registrationAttachmentRepository", registrationAttachmentRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "registrationMessageService", registrationMessageService);
        ReflectionTestUtils.setField(service, "realtimeGateway", realtimeGateway);
        ReflectionTestUtils.setField(service, "registrationReviewSettingsService", registrationReviewSettingsService);
        ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneId.of("UTC")));
        lenient().when(registrationReviewSettingsService.getEffective(anyLong()))
                .thenAnswer(invocation -> RegistrationReviewSettings.defaultsFor(invocation.getArgument(0)));
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
