package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomGroupMemberRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.SuReservationWebhookEventRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void trySendForReservation_shouldMarkKnowledgeThreadDirtyAfterSavingLocalMessage() throws Exception {
        AutoMessageSendLogRepository sendLogRepository = mock(AutoMessageSendLogRepository.class);
        StoreRepository storeRepository = mock(StoreRepository.class);
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomGroupMemberRepository roomGroupMemberRepository = mock(RoomGroupMemberRepository.class);
        SuMessageThreadRepository threadRepository = mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = mock(SuMessageRepository.class);
        SuReservationWebhookEventRepository webhookEventRepository = mock(SuReservationWebhookEventRepository.class);
        SuApiClient suApiClient = mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = mock(SuMessagingRealtimeGateway.class);
        MessageKnowledgeThreadDirtyMarker dirtyMarker = mock(MessageKnowledgeThreadDirtyMarker.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuBusinessAutoMessageService service = new SuBusinessAutoMessageService(
                sendLogRepository,
                storeRepository,
                reservationRepository,
                roomTypeRepository,
                roomRepository,
                roomGroupMemberRepository,
                threadRepository,
                messageRepository,
                webhookEventRepository,
                suApiClient,
                suAccessTokenService,
                realtimeGateway,
                objectMapper,
                new RegistrationLinkService("test-secret", 90),
                "http://localhost:8091/",
                "Auto Message"
        );
        service.setKnowledgeThreadDirtyMarker(dirtyMarker);

        Channel channel = new Channel();
        channel.setId(33L);
        channel.setStoreId(26L);
        channel.setCode("AIRBNB");

        Reservation reservation = new Reservation();
        reservation.setId(900L);
        reservation.setStoreId(26L);
        reservation.setChannel(channel);
        reservation.setGuestName("Alice");
        reservation.setOrderNumber("ORDER-LOCAL");
        reservation.setChannelOrderNumber("AIR-BOOKING-1");

        AutoMessage template = new AutoMessage();
        template.setId(88L);
        template.setStoreId(26L);
        template.setChannels("[33]");
        template.setRoomSelectionType("ALL_LOCAL");
        template.setMessage("Welcome {{guest_name}}");

        Store store = new Store();
        store.setId(26L);
        store.setName("Local Hotel");

        SuMessageThread thread = new SuMessageThread();
        thread.setId(77L);
        thread.setStoreId(26L);
        thread.setSuHotelId("STORE26");
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setThreadKey("T77");
        thread.setThreadId("T77");
        thread.setGuestId("G77");
        thread.setBookingId("AIR-BOOKING-1");
        thread.setListingId("LISTING77");
        thread.setClosed(false);

        when(sendLogRepository.findByStoreIdAndActionAndTargetTypeAndTargetId(
                26L, "AM:88", "RESERVATION", 900L
        )).thenReturn(Optional.empty());
        when(sendLogRepository.save(any(AutoMessageSendLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storeRepository.findById(26L)).thenReturn(Optional.of(store));
        when(threadRepository.findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(
                26L, SuMessagingService.CHANNEL_AIRBNB, "AIR-BOOKING-1"
        )).thenReturn(Optional.of(thread));
        when(threadRepository.save(any(SuMessageThread.class))).thenAnswer(inv -> inv.getArgument(0));

        JsonNode ok = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.postMessagingAB(anyString(), any(Map.class))).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);
        when(suAccessTokenService.executeWithTokenRetry(any(), eq("messagingAB"))).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> action = (Function<String, Object>) inv.getArgument(0);
            return action.apply("token");
        });
        when(messageRepository.saveAndFlush(any(SuMessage.class))).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(404L);
            return message;
        });
        when(messageRepository.save(any(SuMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        service.trySendForReservation(
                26L,
                reservation,
                template,
                LocalDateTime.of(2026, 6, 18, 10, 0),
                Duration.ZERO
        );

        ArgumentCaptor<SuMessage> dirtyCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(dirtyMarker).markDirty(dirtyCaptor.capture());
        SuMessage dirtyMessage = dirtyCaptor.getValue();
        assertEquals(404L, dirtyMessage.getId());
        assertEquals(26L, dirtyMessage.getStoreId());
        assertEquals(77L, dirtyMessage.getThread().getId());
        assertEquals(SuMessagingSenderType.STAFF, dirtyMessage.getSenderType());
        assertEquals("Welcome Alice", dirtyMessage.getContent());
        verify(realtimeGateway).broadcastWorkbenchInvalidated(26L, "message");
    }
}
