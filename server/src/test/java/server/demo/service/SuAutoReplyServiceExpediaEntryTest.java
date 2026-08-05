package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.Channel;
import server.demo.entity.Store;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * P4 聚焦测试：自动回复入口 tryAutoReply 放行目录 messaging 能力集。
 * EXPEDIA(9) 放行且回复载荷 bookingid 必填、不带 threadid/guestid；
 * TRIP(339)/AGODA(189) 官方不支持消息，入口直接跳过。
 */
class SuAutoReplyServiceExpediaEntryTest {

    private static final Long STORE_ID = 10L;

    private final SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
    private final SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
    private final AutoMessageRepository autoMessageRepository = Mockito.mock(AutoMessageRepository.class);
    private final AutoMessageSendLogRepository sendLogRepository = Mockito.mock(AutoMessageSendLogRepository.class);
    private final ChannelRepository channelRepository = Mockito.mock(ChannelRepository.class);
    private final StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
    private final SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
    private final SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
    private final SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SuAutoReplyService createService() {
        return new SuAutoReplyService(
                threadRepository,
                messageRepository,
                autoMessageRepository,
                sendLogRepository,
                channelRepository,
                storeRepository,
                suApiClient,
                suAccessTokenService,
                realtimeGateway,
                objectMapper
        );
    }

    @Test
    void tryAutoReply_expediaThreadSendsBookingIdPayload() throws Exception {
        SuAutoReplyService service = createService();

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(STORE_ID);
        thread.setSuHotelId("STORE10");
        thread.setChannelId(9);
        thread.setThreadKey("EXP-123456");
        thread.setBookingId("EXP-123456");
        thread.setListingId("778899");
        thread.setGuestName("Grace");
        thread.setClosed(false);

        Channel channel = new Channel();
        channel.setId(66L);
        channel.setStoreId(STORE_ID);
        channel.setCode("EXPEDIA");

        AutoMessage template = new AutoMessage();
        template.setId(88L);
        template.setStoreId(STORE_ID);
        template.setAction(SuAutoReplyService.ACTION_GUEST_MESSAGE);
        template.setChannels("[66]");
        template.setRoomSelectionType("ALL_LOCAL");
        template.setMessage("Welcome {{guest_name}}");

        Store store = new Store();
        store.setId(STORE_ID);
        store.setName("Local Hotel");

        when(threadRepository.findByStoreIdAndId(STORE_ID, 5L)).thenReturn(Optional.of(thread));
        when(messageRepository.existsByThread_IdAndSenderType(5L, SuMessagingSenderType.STAFF)).thenReturn(false);
        when(sendLogRepository.existsByStoreIdAndActionAndTargetTypeAndTargetId(
                STORE_ID, SuAutoReplyService.ACTION_GUEST_MESSAGE, "SU_THREAD", 5L
        )).thenReturn(false);
        when(channelRepository.findByStoreIdAndCode(STORE_ID, "EXPEDIA")).thenReturn(Optional.of(channel));
        when(autoMessageRepository.findByStoreIdAndEnabledTrue(STORE_ID)).thenReturn(List.of(template));
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));
        when(sendLogRepository.save(any(AutoMessageSendLog.class))).thenAnswer(inv -> inv.getArgument(0));
        JsonNode ok = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.postMessagingAB(anyString(), any(Map.class))).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);
        when(suAccessTokenService.executeWithTokenRetry(any(), eq("messagingAB"))).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> action = (Function<String, Object>) inv.getArgument(0);
            return action.apply("token");
        });
        when(messageRepository.save(any(SuMessage.class))).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(202L);
            return message;
        });
        when(threadRepository.save(any(SuMessageThread.class))).thenAnswer(inv -> inv.getArgument(0));

        service.tryAutoReply(STORE_ID, 5L);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(suApiClient).postMessagingAB(eq("token"), payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertEquals("STORE10", payload.get("hotelid"));
        assertEquals("9", payload.get("channelid"));
        assertEquals("778899", payload.get("listingid"));
        assertEquals("EXP-123456", payload.get("bookingid"));
        assertEquals("Welcome Grace", payload.get("message"));
        // Expedia 官方不需要 threadid/guestid（仅 Airbnb/VRBO 需要）
        assertFalse(payload.containsKey("threadid"));
        assertFalse(payload.containsKey("guestid"));

        verify(realtimeGateway).broadcastMessageCreated(eq(STORE_ID), eq(5L), any());
    }

    @Test
    void tryAutoReply_tripAndAgodaThreadsAreSkippedAtEntry() {
        SuAutoReplyService service = createService();

        for (int suChannelId : new int[]{339, 189}) {
            SuMessageThread thread = new SuMessageThread();
            thread.setId(5L);
            thread.setStoreId(STORE_ID);
            thread.setChannelId(suChannelId);
            when(threadRepository.findByStoreIdAndId(STORE_ID, 5L)).thenReturn(Optional.of(thread));

            service.tryAutoReply(STORE_ID, 5L);
        }

        // 入口即返回：不查模板、不写消息、不调 Su
        verifyNoInteractions(messageRepository, sendLogRepository, channelRepository, autoMessageRepository, suApiClient);
    }

    @Test
    void tryAutoReply_vrboThreadIsSkippedAtEntry() {
        SuAutoReplyService service = createService();

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(STORE_ID);
        thread.setChannelId(253);
        when(threadRepository.findByStoreIdAndId(STORE_ID, 5L)).thenReturn(Optional.of(thread));

        service.tryAutoReply(STORE_ID, 5L);

        // VRBO(253) Su 官方支持消息但未接入，目录外同样拒绝
        verifyNoInteractions(messageRepository, sendLogRepository, channelRepository, autoMessageRepository, suApiClient);
    }
}
