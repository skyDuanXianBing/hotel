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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuAutoReplyServiceTest {

    @Test
    void tryAutoReply_shouldMarkKnowledgeThreadDirtyAfterSavingLocalReply() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        AutoMessageRepository autoMessageRepository = Mockito.mock(AutoMessageRepository.class);
        AutoMessageSendLogRepository sendLogRepository = Mockito.mock(AutoMessageSendLogRepository.class);
        ChannelRepository channelRepository = Mockito.mock(ChannelRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        MessageKnowledgeThreadDirtyMarker dirtyMarker = Mockito.mock(MessageKnowledgeThreadDirtyMarker.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuAutoReplyService service = new SuAutoReplyService(
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
        service.setKnowledgeThreadDirtyMarker(dirtyMarker);

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(10L);
        thread.setSuHotelId("STORE10");
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setThreadKey("T1");
        thread.setThreadId("T1");
        thread.setGuestId("G1");
        thread.setBookingId("B1");
        thread.setListingId("L1");
        thread.setGuestName("Alice");
        thread.setClosed(false);

        Channel channel = new Channel();
        channel.setId(33L);
        channel.setStoreId(10L);
        channel.setCode("AIRBNB");

        AutoMessage template = new AutoMessage();
        template.setId(88L);
        template.setStoreId(10L);
        template.setAction(SuAutoReplyService.ACTION_GUEST_MESSAGE);
        template.setChannels("[33]");
        template.setRoomSelectionType("ALL_LOCAL");
        template.setMessage("Welcome {{guest_name}}");

        Store store = new Store();
        store.setId(10L);
        store.setName("Local Hotel");

        when(threadRepository.findByStoreIdAndId(10L, 5L)).thenReturn(Optional.of(thread));
        when(messageRepository.existsByThread_IdAndSenderType(5L, SuMessagingSenderType.STAFF)).thenReturn(false);
        when(sendLogRepository.existsByStoreIdAndActionAndTargetTypeAndTargetId(
                10L, SuAutoReplyService.ACTION_GUEST_MESSAGE, "SU_THREAD", 5L
        )).thenReturn(false);
        when(channelRepository.findByStoreIdAndCode(10L, "AIRBNB")).thenReturn(Optional.of(channel));
        when(autoMessageRepository.findByStoreIdAndEnabledTrue(10L)).thenReturn(List.of(template));
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
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

        service.tryAutoReply(10L, 5L);

        ArgumentCaptor<SuMessage> dirtyCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(dirtyMarker).markDirty(dirtyCaptor.capture());
        SuMessage dirtyMessage = dirtyCaptor.getValue();
        assertEquals(202L, dirtyMessage.getId());
        assertEquals(10L, dirtyMessage.getStoreId());
        assertEquals(5L, dirtyMessage.getThread().getId());
        assertEquals(SuMessagingSenderType.STAFF, dirtyMessage.getSenderType());
        assertEquals("SENT", dirtyMessage.getDeliveryStatus());
        assertEquals("Welcome Alice", dirtyMessage.getContent());
        verify(realtimeGateway).broadcastMessageCreated(eq(10L), eq(5L), any());
        verify(realtimeGateway).broadcastWorkbenchInvalidated(10L, "message");
    }
}
