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
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingServiceTest {

    @Test
    void handleInboundMessage_shouldUpsertThreadAndSaveMessage() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuAutoReplyService suAutoReplyService = Mockito.mock(SuAutoReplyService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                suAutoReplyService
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
            SuMessageThread t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        when(messageRepository.save(any())).thenAnswer(inv -> {
            SuMessage m = inv.getArgument(0);
            m.setId(100L);
            return m;
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
    }

    @Test
    void sendMessage_shouldCallSuMessagingABAndPersistStaffMessage() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuAutoReplyService suAutoReplyService = Mockito.mock(SuAutoReplyService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuMessagingService service = new SuMessagingService(
                threadRepository,
                messageRepository,
                suApiClient,
                suAccessTokenService,
                objectMapper,
                suAutoReplyService
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
            SuMessage m = inv.getArgument(0);
            m.setId(101L);
            return m;
        });
        when(threadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuMessagingSendRequest req = new SuMessagingSendRequest();
        req.setContent("Hi");
        req.setSenderName("客服A");

        service.sendMessage(10L, 5L, req);

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
        assertEquals("客服A", saved.getSenderName());
        assertTrue(saved.getIsRead());
    }
}
