package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SuAiAutoReplyServiceTest {

    @Test
    void tryAutoReply_shouldSendAiReplyPersistStaffMessageAndBroadcast() throws Exception {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        AutoMessageSendLogRepository sendLogRepository = Mockito.mock(AutoMessageSendLogRepository.class);
        ChatService chatService = Mockito.mock(ChatService.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        MessageKnowledgeThreadDirtyMarker dirtyMarker = Mockito.mock(MessageKnowledgeThreadDirtyMarker.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuAiAutoReplyService service = new SuAiAutoReplyService(
                threadRepository,
                messageRepository,
                sendLogRepository,
                chatService,
                suApiClient,
                suAccessTokenService,
                realtimeGateway,
                objectMapper
        );
        service.setKnowledgeThreadDirtyMarker(dirtyMarker);
        ReflectionTestUtils.setField(service, "senderName", "AI客服");
        ReflectionTestUtils.setField(service, "maxInputLength", 1200);

        SuMessageThread thread = new SuMessageThread();
        thread.setId(5L);
        thread.setStoreId(10L);
        thread.setSuHotelId("STORE10");
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setThreadId("T1");
        thread.setGuestId("G1");
        thread.setBookingId("B1");
        thread.setListingId("L1");
        thread.setClosed(false);

        SuMessage trigger = new SuMessage();
        trigger.setId(101L);
        trigger.setStoreId(10L);
        trigger.setThread(thread);
        trigger.setSenderType(SuMessagingSenderType.GUEST);
        trigger.setContent("��ã�������Լ�����ס��");

        when(sendLogRepository.existsByStoreIdAndActionAndTargetTypeAndTargetId(
                10L, SuAiAutoReplyService.ACTION_AI_GUEST_MESSAGE, SuAiAutoReplyService.TARGET_TYPE_SU_MESSAGE, 101L
        )).thenReturn(false);
        when(threadRepository.findByStoreIdAndId(10L, 5L)).thenReturn(Optional.of(thread));
        when(messageRepository.findById(101L)).thenReturn(Optional.of(trigger));
        when(sendLogRepository.save(any(AutoMessageSendLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse aiResponse = ChatMessageResponse.success("���ã����� 3 ���ɰ�����ס��", "sid_1");
        when(chatService.processMessage(any(ChatMessageRequest.class))).thenReturn(aiResponse);

        JsonNode ok = objectMapper.readTree("{\"Status\":\"Success\"}");
        when(suApiClient.postMessagingAB(anyString(), any(Map.class))).thenReturn(ok);
        when(suApiClient.isSuSuccess(ok)).thenReturn(true);
        when(suAccessTokenService.executeWithTokenRetry(any(), eq("messagingAB"))).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<String, Object> fn = (Function<String, Object>) inv.getArgument(0);
            return fn.apply("token");
        });
        when(messageRepository.save(any(SuMessage.class))).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(202L);
            return message;
        });
        when(messageRepository.saveAndFlush(any(SuMessage.class))).thenAnswer(inv -> {
            SuMessage message = inv.getArgument(0);
            message.setId(202L);
            return message;
        });
        when(threadRepository.save(any(SuMessageThread.class))).thenAnswer(inv -> inv.getArgument(0));

        service.tryAutoReply(10L, 5L, 101L);

        verify(chatService).processMessage(any(ChatMessageRequest.class));
        verify(suApiClient).postMessagingAB(eq("token"), any(Map.class));

        ArgumentCaptor<SuMessage> msgCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(messageRepository).save(msgCaptor.capture());
        SuMessage saved = msgCaptor.getValue();
        assertEquals(SuMessagingSenderType.STAFF, saved.getSenderType());
        assertEquals("���ã����� 3 ���ɰ�����ס��", saved.getContent());
        assertEquals("SENT", saved.getDeliveryStatus());

        verify(threadRepository).save(any(SuMessageThread.class));
        verify(sendLogRepository, atLeast(2)).save(any(AutoMessageSendLog.class));
        verify(realtimeGateway).broadcastMessageCreated(eq(10L), eq(5L), any());
        verify(realtimeGateway).broadcastWorkbenchInvalidated(10L, "message");
        verify(realtimeGateway, never()).broadcastMessageUpdated(any(), any(), any());

        ArgumentCaptor<SuMessage> dirtyCaptor = ArgumentCaptor.forClass(SuMessage.class);
        verify(dirtyMarker).markDirty(dirtyCaptor.capture());
        SuMessage dirtyMessage = dirtyCaptor.getValue();
        assertEquals(202L, dirtyMessage.getId());
        assertEquals(10L, dirtyMessage.getStoreId());
        assertEquals(5L, dirtyMessage.getThread().getId());
        assertEquals(SuMessagingSenderType.STAFF, dirtyMessage.getSenderType());
    }

    @Test
    void tryAutoReply_shouldSkipWhenTriggerMessageMissing() {
        SuMessageThreadRepository threadRepository = Mockito.mock(SuMessageThreadRepository.class);
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        AutoMessageSendLogRepository sendLogRepository = Mockito.mock(AutoMessageSendLogRepository.class);
        ChatService chatService = Mockito.mock(ChatService.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);
        SuAccessTokenService suAccessTokenService = Mockito.mock(SuAccessTokenService.class);
        SuMessagingRealtimeGateway realtimeGateway = Mockito.mock(SuMessagingRealtimeGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SuAiAutoReplyService service = new SuAiAutoReplyService(
                threadRepository,
                messageRepository,
                sendLogRepository,
                chatService,
                suApiClient,
                suAccessTokenService,
                realtimeGateway,
                objectMapper
        );
        ReflectionTestUtils.setField(service, "maxInputLength", 1200);

        when(sendLogRepository.existsByStoreIdAndActionAndTargetTypeAndTargetId(any(), any(), any(), any())).thenReturn(false);
        when(threadRepository.findByStoreIdAndId(10L, 5L)).thenReturn(Optional.empty());

        service.tryAutoReply(10L, 5L, 101L);

        verifyNoInteractions(chatService, suApiClient, suAccessTokenService, realtimeGateway);
    }
}
