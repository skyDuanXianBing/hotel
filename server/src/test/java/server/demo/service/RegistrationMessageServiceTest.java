package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.dto.registration.RegistrationMessageLogDTO;
import server.demo.dto.registration.RegistrationSendMessageRequest;
import server.demo.entity.Channel;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationGuest;
import server.demo.entity.RegistrationMessageLog;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.entity.SuMessageThread;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.RegistrationMessageType;
import server.demo.enums.RegistrationSendStatus;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.RegistrationGuestRepository;
import server.demo.repository.RegistrationMessageLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void sendMessage_shouldFallbackToEnglishAndSendOriginalWhenTranslationFails() {
        RegistrationFormRepository formRepository = mock(RegistrationFormRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        SuMessagingService suMessagingService = mock(SuMessagingService.class);
        RegistrationMessageLogRepository messageLogRepository = mock(RegistrationMessageLogRepository.class);
        RegistrationLinkService registrationLinkService = new RegistrationLinkService("test-secret", 90);
        ReservationBookingKeyResolver bookingKeyResolver = mock(ReservationBookingKeyResolver.class);
        AiTranslationService aiTranslationService = mock(AiTranslationService.class);
        RegistrationGuestRepository registrationGuestRepository = mock(RegistrationGuestRepository.class);
        RegistrationLanguageResolver languageResolver = new RegistrationLanguageResolver(registrationGuestRepository);
        RegistrationMessageService service = new RegistrationMessageService(
                formRepository,
                reservationRepository,
                storeRepository,
                threadRepository,
                suMessagingService,
                messageLogRepository,
                registrationLinkService,
                bookingKeyResolver,
                aiTranslationService,
                languageResolver,
                "http://localhost:8091/"
        );
        RegistrationForm form = createMessageForm();
        Reservation reservation = form.getReservation();
        Store store = new Store();
        store.setName("Local Hotel");
        SuMessageThread thread = createBookingThread();
        RegistrationGuest guest = new RegistrationGuest();
        guest.setNationality("Atlantis");
        guest.setCountry("Unknownland");
        RegistrationSendMessageRequest req = new RegistrationSendMessageRequest();
        req.setType(RegistrationMessageType.APPROVED_INFO);
        req.setContent("Please check in");
        req.setSenderName("Front Desk");
        req.setTranslateBeforeSend(true);

        when(formRepository.findById(8L)).thenReturn(Optional.of(form));
        when(reservationRepository.findById(88L)).thenReturn(Optional.of(reservation));
        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));
        when(bookingKeyResolver.resolvePrimaryBookingKey(reservation)).thenReturn("BOOK-LOCAL");
        when(bookingKeyResolver.buildReservationLookupCandidates(reservation)).thenReturn(List.of("BOOK-LOCAL"));
        when(threadRepository.findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(
                26L,
                SuMessagingService.CHANNEL_BOOKING,
                "BOOK-LOCAL"
        )).thenReturn(Optional.of(thread));
        when(registrationGuestRepository.findByFormIdOrderBySortOrderAsc(8L)).thenReturn(List.of(guest));
        when(aiTranslationService.translate(eq("Please check in"), any(RegistrationTargetLanguage.class)))
                .thenAnswer(invocation -> {
                    RegistrationTargetLanguage targetLanguage = invocation.getArgument(1);
                    return AiTranslationResult.fallback("Please check in", targetLanguage, "MODEL_DOWN");
                });
        when(messageLogRepository.save(any(RegistrationMessageLog.class)))
                .thenAnswer(invocation -> {
                    RegistrationMessageLog log = invocation.getArgument(0);
                    log.setId(900L);
                    return log;
                });

        RegistrationMessageLogDTO result = service.sendMessage(26L, 7L, 8L, req);

        ArgumentCaptor<RegistrationTargetLanguage> languageCaptor =
                ArgumentCaptor.forClass(RegistrationTargetLanguage.class);
        verify(aiTranslationService).translate(eq("Please check in"), languageCaptor.capture());
        RegistrationTargetLanguage targetLanguage = languageCaptor.getValue();
        assertEquals("en", targetLanguage.getCode());
        assertEquals("English", targetLanguage.getName());
        assertFalse(targetLanguage.isResolved());
        assertEquals("UNRESOLVED_GUEST_LANGUAGE", targetLanguage.getFallbackReason());

        ArgumentCaptor<SuMessagingSendRequest> sendCaptor =
                ArgumentCaptor.forClass(SuMessagingSendRequest.class);
        verify(suMessagingService).sendMessage(eq(26L), eq(333L), sendCaptor.capture());
        assertEquals("Please check in", sendCaptor.getValue().getContent());
        assertEquals("Front Desk", sendCaptor.getValue().getSenderName());

        assertEquals(RegistrationSendStatus.SENT, result.getSendStatus());
        assertEquals("Please check in", result.getContent());
        assertEquals("en", result.getTargetLanguageCode());
        assertEquals("English", result.getTargetLanguageName());
        assertEquals("UNRESOLVED_GUEST_LANGUAGE", result.getTargetLanguageFallbackReason());
        assertEquals(Boolean.FALSE, result.getTranslated());
        assertEquals("MODEL_DOWN", result.getTranslationError());
    }

    private static RegistrationForm createMessageForm() {
        Channel channel = new Channel();
        channel.setCode("BOOKING");

        Reservation reservation = new Reservation();
        reservation.setId(88L);
        reservation.setStoreId(26L);
        reservation.setOrderNumber("ORDER-LOCAL");
        reservation.setChannel(channel);

        RegistrationForm form = new RegistrationForm();
        form.setId(8L);
        form.setStoreId(26L);
        form.setOrderNumber("ORDER-LOCAL");
        form.setStatus(RegistrationFormStatus.APPROVED);
        form.setReservation(reservation);
        return form;
    }

    private static SuMessageThread createBookingThread() {
        SuMessageThread thread = new SuMessageThread();
        thread.setId(333L);
        thread.setStoreId(26L);
        thread.setSuHotelId("HOTEL-LOCAL");
        thread.setChannelId(SuMessagingService.CHANNEL_BOOKING);
        thread.setThreadKey("BOOK-LOCAL");
        thread.setBookingId("BOOK-LOCAL");
        thread.setListingId("LISTING-LOCAL");
        return thread;
    }
}
