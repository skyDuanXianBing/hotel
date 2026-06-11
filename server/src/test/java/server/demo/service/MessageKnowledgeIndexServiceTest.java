package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import server.demo.entity.MessageKnowledgeEntry;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.MessageKnowledgeEntryRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.util.UtcTimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class MessageKnowledgeIndexServiceTest {

    @Test
    void indexRecentStoreMessages_shouldUseParameterizedWindowAndLimit() {
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        MessageKnowledgeEntryRepository entryRepository = Mockito.mock(MessageKnowledgeEntryRepository.class);
        SuMessagingThreadContextResolver contextResolver = Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeIndexService service = new MessageKnowledgeIndexService(
                messageRepository,
                entryRepository,
                contextResolver,
                new SuMessagingAiRedactor(),
                new SuMessagingAiTextService()
        );

        when(messageRepository.findRecentMessagesForKnowledgeIndex(
                eq(26L),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of());

        int indexed = service.indexRecentStoreMessages(26L, 30, 25);

        assertEquals(0, indexed);
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findRecentMessagesForKnowledgeIndex(
                eq(26L),
                sinceCaptor.capture(),
                pageableCaptor.capture()
        );
        assertEquals(25, pageableCaptor.getValue().getPageSize());
        long ageDays = Duration.between(sinceCaptor.getValue(), UtcTimeUtil.nowLocalDateTime()).toDays();
        assertTrue(ageDays >= 29 && ageDays <= 30);
    }

    @Test
    void indexMessages_shouldUpsertGuestStaffPairWithRedactedText() {
        SuMessageRepository messageRepository = Mockito.mock(SuMessageRepository.class);
        MessageKnowledgeEntryRepository entryRepository = Mockito.mock(MessageKnowledgeEntryRepository.class);
        SuMessagingThreadContextResolver contextResolver = Mockito.mock(SuMessagingThreadContextResolver.class);
        MessageKnowledgeIndexService service = new MessageKnowledgeIndexService(
                messageRepository,
                entryRepository,
                contextResolver,
                new SuMessagingAiRedactor(),
                new SuMessagingAiTextService()
        );

        SuMessageThread thread = new SuMessageThread();
        thread.setId(77L);
        thread.setStoreId(26L);
        thread.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        thread.setBookingId("B1");

        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomId(9L);
        context.setRoomTypeId(3L);
        context.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        context.setBookingKey("B1");
        when(contextResolver.resolveForIndex(26L, thread)).thenReturn(context);
        MessageKnowledgeEntry existing = new MessageKnowledgeEntry();
        existing.setId(999L);
        when(entryRepository.findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageIdAndNormalizedHash(
                anyLong(),
                anyLong(),
                anyLong(),
                anyString()
        )).thenReturn(Optional.empty(), Optional.of(existing));
        when(entryRepository.save(any(MessageKnowledgeEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        SuMessage guest = newMessage(101L, thread, SuMessagingSenderType.GUEST, "My phone is 13800138000. Late checkout?");
        SuMessage staff = newMessage(102L, thread, SuMessagingSenderType.STAFF, "Yes, late checkout is possible.");

        int indexed = service.indexMessages(26L, List.of(guest, staff));
        int indexedAgain = service.indexMessages(26L, List.of(guest, staff));

        assertEquals(1, indexed);
        assertEquals(1, indexedAgain);
        ArgumentCaptor<MessageKnowledgeEntry> entryCaptor = ArgumentCaptor.forClass(MessageKnowledgeEntry.class);
        verify(entryRepository, times(2)).save(entryCaptor.capture());
        MessageKnowledgeEntry saved = entryCaptor.getAllValues().get(0);
        MessageKnowledgeEntry savedAgain = entryCaptor.getAllValues().get(1);
        assertEquals(26L, saved.getStoreId());
        assertEquals(77L, saved.getThreadId());
        assertEquals(9L, saved.getRoomId());
        assertEquals(101L, saved.getSourceGuestMessageId());
        assertEquals(102L, saved.getSourceStaffMessageId());
        assertFalse(saved.getQuestion().contains("13800138000"));
        assertEquals(MessageKnowledgeEntry.REDACTION_STATUS_REDACTED, saved.getPiiRedactionStatus());
        assertEquals(999L, savedAgain.getId());
    }

    private static SuMessage newMessage(
            Long id,
            SuMessageThread thread,
            SuMessagingSenderType senderType,
            String content
    ) {
        SuMessage message = new SuMessage();
        message.setId(id);
        message.setStoreId(thread.getStoreId());
        message.setThread(thread);
        message.setSenderType(senderType);
        message.setContent(content);
        message.setSentAt(LocalDateTime.of(2026, 6, 10, 10, 0).plusMinutes(id));
        message.setDeliveryStatus("SENT");
        return message;
    }
}
