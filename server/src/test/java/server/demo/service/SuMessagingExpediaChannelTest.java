package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P4 聚焦测试：消息链路在通用 messagingAB 路径上放行 EXPEDIA(9)，
 * TRIP(339)/AGODA(189) 继续被入口闸门拒绝；Booking 专属附件 API 不向 Expedia 开放。
 */
class SuMessagingExpediaChannelTest {

    @org.junit.jupiter.api.BeforeAll
    static void installApiMessages() {
        // 纯 Mockito 单测无 Spring 上下文；显式安装 i18n 服务使 ApiMessages 返回真实文案
        server.demo.i18n.TestApiMessages.install();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void handleInboundMessage_expediaCreatesBookingIdKeyedThread() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        SuMessagingService service = newService(threadRepository, messageRepository, realtimeGateway);

        String raw = """
                {
                  "message": "Need late check-in",
                  "guestid": "EG1",
                  "bookingid": "EXP-123456",
                  "listingid": "778899",
                  "bookingflag": "B",
                  "messageid": "EM1",
                  "channel_id": "9",
                  "threadid": "ET1",
                  "hotelid": "STORE10"
                }
                """;
        JsonNode root = objectMapper.readTree(raw);

        when(messageRepository.findByStoreIdAndExternalMessageId(10L, "EM1")).thenReturn(Optional.empty());
        when(threadRepository.findForUpdateByStoreIdAndChannelIdAndThreadKey(10L, 9, "EXP-123456"))
                .thenReturn(Optional.empty());
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
        SuMessageThread thread = threadCaptor.getValue();
        assertEquals(Integer.valueOf(9), thread.getChannelId());
        // Expedia 官方 bookingid 必填，与 Booking 同以 bookingid 为会话键
        assertEquals("EXP-123456", thread.getThreadKey());
        assertEquals("EXP-123456", thread.getBookingId());
        assertEquals("ET1", thread.getThreadId());
        assertEquals("778899", thread.getListingId());
        assertEquals("Need late check-in", thread.getLastMessage());

        ArgumentCaptor<SuMessage> messageCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertEquals("EM1", messageCaptor.getValue().getExternalMessageId());
        verify(realtimeGateway).broadcastMessageCreated(eq(10L), eq(99L), any());
    }

    @Test
    void handleInboundMessage_tripAndAgodaAreIgnoredBeforeAnyWrite() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        SuMessagingService service = newService(threadRepository, messageRepository, realtimeGateway);

        for (String channelId : new String[]{"339", "189"}) {
            String raw = """
                    {
                      "message": "Hello",
                      "bookingid": "X1",
                      "listingid": "778899",
                      "bookingflag": "B",
                      "messageid": "M-%s",
                      "channel_id": "%s",
                      "threadid": "T-%s",
                      "hotelid": "STORE10"
                    }
                    """.formatted(channelId, channelId, channelId);
            service.handleInboundMessage(10L, "STORE10", objectMapper.readTree(raw), raw);
        }

        verify(threadRepository, never()).save(any());
        verify(messageRepository, never()).save(any());
        verify(realtimeGateway, never()).broadcastMessageCreated(any(), any(), any());
    }

    @Test
    void sendMessage_expediaPayloadRequiresBookingIdAndOmitsThreadFields() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingService service = newService(
                threadRepository,
                messageRepository,
                realtimeGateway,
                suApiClient,
                suAccessTokenService
        );

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(10L);
        thread.setSuHotelId("STORE10");
        thread.setChannelId(9);
        thread.setThreadKey("EXP-123456");
        thread.setBookingId("EXP-123456");
        thread.setListingId("778899");
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
        request.setContent("Sure, no problem");
        service.sendMessage(10L, 5L, request);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postMessagingAB(eq("token"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals("STORE10", payload.get("hotelid"));
        assertEquals("9", payload.get("channelid"));
        assertEquals("778899", payload.get("listingid"));
        assertEquals("EXP-123456", payload.get("bookingid"));
        assertEquals("Sure, no problem", payload.get("message"));
        // Expedia 官方不需要 threadid/guestid（仅 Airbnb/VRBO 需要）
        assertFalse(payload.containsKey("threadid"));
        assertFalse(payload.containsKey("guestid"));
    }

    @Test
    void sendMessage_expediaWithoutBookingIdFailsBeforeCallingSu() {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuMessagingService service = newService(threadRepository, messageRepository, realtimeGateway, suApiClient, null);

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(10L);
        thread.setSuHotelId("STORE10");
        thread.setChannelId(9);
        thread.setListingId("778899");
        thread.setClosed(false);

        when(threadRepository.findByStoreIdAndId(10L, 5L)).thenReturn(Optional.of(thread));

        SuMessagingSendRequest request = new SuMessagingSendRequest();
        request.setContent("Hi");
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.sendMessage(10L, 5L, request)
        );
        assertEquals("Expedia 回复需要 bookingid，但当前会话缺少必要字段", ex.getMessage());
        verify(suApiClient, never()).postMessagingAB(anyString(), any());
    }

    private SuMessagingService newService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            SuMessagingRealtimeGateway realtimeGateway
    ) {
        return newService(
                threadRepository,
                messageRepository,
                realtimeGateway,
                Mockito.mock(SuApiClient.class),
                Mockito.mock(SuAccessTokenService.class)
        );
    }

    private SuMessagingService newService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            SuMessagingRealtimeGateway realtimeGateway,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService
    ) {
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        return new SuMessagingService(
                threadRepository,
                messageRepository,
                reservationRepository,
                new ReservationBookingKeyResolver(reservationRepository),
                Mockito.mock(StoreRepository.class),
                Clock.systemDefaultZone(),
                suApiClient,
                suAccessTokenService != null ? suAccessTokenService : Mockito.mock(SuAccessTokenService.class),
                objectMapper,
                realtimeGateway,
                Mockito.mock(OtaIntegrationService.class),
                Mockito.mock(RoomTypeRepository.class)
        );
    }
}
