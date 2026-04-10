package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.ReservationRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingServiceTest {

    @Test
    void handleInboundMessage_shouldUpsertThreadSaveMessageAndBroadcast() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        String raw = """
                {
                  "message": "Hello",
                  "guestid": "G1",
                  "bookingid": "B1",
                  "listingid": "L1",
                  "bookingflag": "B",
                  "messageid": "M1",
                  "channel_id": "244",
                  "threadid": "T1",
                  "hotelid": "STORE10",
                  "booking_details": {
                    "listing_name": "My Listing"
                  },
                  "user_details": {
                    "users": [{"first_name": "Alice"}]
                  }
                }
                """;
        JsonNode root = objectMapper.readTree(raw);

        when(messageRepository.findByStoreIdAndExternalMessageId(10L, "M1")).thenReturn(Optional.empty());
        when(threadRepository.findByStoreIdAndChannelIdAndThreadKey(10L, 244, "T1")).thenReturn(Optional.empty());
        when(threadRepository.save(any())).thenAnswer(inv -> {
            SuMessageThread thread = inv.getArgument(0);
            thread.setId(99L);
            return thread;
        });
        when(messageRepository.save(any())).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(100L);
            return message;
        });

        service.handleInboundMessage(10L, "STORE10", root, raw);

        ArgumentCaptor<SuMessageThread> threadCaptor = ArgumentCaptor.forClass(SuMessageThread.class);
        verify(threadRepository).save(threadCaptor.capture());
        SuMessageThread savedThread = threadCaptor.getValue();
        assertEquals(10L, savedThread.getStoreId());
        assertEquals("STORE10", savedThread.getSuHotelId());
        assertEquals(244, savedThread.getChannelId());
        assertEquals("T1", savedThread.getThreadKey());
        assertEquals("T1", savedThread.getThreadId());
        assertEquals("B1", savedThread.getBookingId());
        assertEquals("G1", savedThread.getGuestId());
        assertEquals("L1", savedThread.getListingId());
        assertEquals("My Listing", savedThread.getListingName());
        assertEquals("Alice", savedThread.getGuestName());
        assertEquals("Hello", savedThread.getLastMessage());

        ArgumentCaptor<SuMessage> msgCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(messageRepository).save(msgCaptor.capture());
        SuMessage savedMsg = msgCaptor.getValue();
        assertEquals(10L, savedMsg.getStoreId());
        assertEquals("M1", savedMsg.getExternalMessageId());
        assertEquals(SuMessagingSenderType.GUEST, savedMsg.getSenderType());
        assertEquals("Hello", savedMsg.getContent());
        assertFalse(savedMsg.getIsRead());
        assertNotNull(savedMsg.getThread());

        verify(realtimeGateway).broadcastMessageCreated(eq(10L), eq(99L), any());
    }

    @Test
    void handleInboundMessage_shouldNormalizeBookingThreadIdentity() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        String raw = """
                {
                  "message": "Hello",
                  "bookingid": "SU26-6022279490_W39FVCQYSN-1775048446300",
                  "threadid": "SU26-6022279490_W39FVCQYSN-1775048446300",
                  "listingid": "14844797",
                  "messageid": "M2",
                  "channel_id": "19",
                  "hotelid": "STORE10"
                }
                """;
        JsonNode root = objectMapper.readTree(raw);

        when(messageRepository.findByStoreIdAndExternalMessageId(10L, "M2")).thenReturn(Optional.empty());
        when(threadRepository.findByStoreIdAndChannelIdAndThreadKey(10L, 19, "6022279490")).thenReturn(Optional.empty());
        when(threadRepository.save(any())).thenAnswer(inv -> {
            SuMessageThread thread = inv.getArgument(0);
            thread.setId(199L);
            return thread;
        });
        when(messageRepository.save(any())).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(200L);
            return message;
        });

        service.handleInboundMessage(10L, "STORE10", root, raw);

        ArgumentCaptor<SuMessageThread> threadCaptor = ArgumentCaptor.forClass(SuMessageThread.class);
        verify(threadRepository).save(threadCaptor.capture());
        SuMessageThread savedThread = threadCaptor.getValue();
        assertEquals("6022279490", savedThread.getThreadKey());
        assertEquals("6022279490", savedThread.getBookingId());
        assertEquals("SU26-6022279490_W39FVCQYSN-1775048446300", savedThread.getThreadId());
    }

    @Test
    void sendMessage_shouldCallSuMessagingABPersistStaffMessageAndBroadcast() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                realtimeGateway
        );

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(10L);
        thread.setSuHotelId("STORE10");
        thread.setChannelId(244);
        thread.setThreadId("T1");
        thread.setGuestId("G1");
        thread.setBookingId("B1");
        thread.setListingId("L1");
        thread.setClosed(false);

        when(threadRepository.findByStoreIdAndId(10L, 5L)).thenReturn(Optional.of(thread));

        JsonNode ok = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.postMessagingAB(anyString(), any())).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);
        when(suAccessTokenService.executeWithTokenRetry(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> fn = (Function<String, Object>) inv.getArgument(0);
            return fn.apply("token");
        });
        when(messageRepository.save(any())).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(101L);
            return message;
        });
        when(threadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuMessagingSendRequest request = new SuMessagingSendRequest();
        request.setContent("Hi");
        request.setSenderName("�ͷ�A");

        service.sendMessage(10L, 5L, request);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postMessagingAB(eq("token"), payloadCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals("STORE10", payload.get("hotelid"));
        assertEquals("244", payload.get("channelid"));
        assertEquals("L1", payload.get("listingid"));
        assertEquals("T1", payload.get("threadid"));
        assertEquals("G1", payload.get("guestid"));
        assertEquals("Hi", payload.get("message"));

        ArgumentCaptor<SuMessage> msgCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(messageRepository).save(msgCaptor.capture());
        SuMessage saved = msgCaptor.getValue();
        assertEquals(SuMessagingSenderType.STAFF, saved.getSenderType());
        assertEquals("�ͷ�A", saved.getSenderName());
        assertEquals(Boolean.TRUE, saved.getIsRead());

        verify(realtimeGateway).broadcastMessageCreated(eq(10L), eq(5L), any());
    }
}
