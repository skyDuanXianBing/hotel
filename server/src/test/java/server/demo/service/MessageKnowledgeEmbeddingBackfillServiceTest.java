package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageKnowledgeEmbeddingBackfillServiceTest {

    @Test
    void normalizeBatchSize_shouldClampToOneThroughThree() {
        assertEquals(1, MessageKnowledgeEmbeddingBackfillService.normalizeBatchSize(null));
        assertEquals(1, MessageKnowledgeEmbeddingBackfillService.normalizeBatchSize(-5));
        assertEquals(2, MessageKnowledgeEmbeddingBackfillService.normalizeBatchSize(2));
        assertEquals(3, MessageKnowledgeEmbeddingBackfillService.normalizeBatchSize(99));
    }

    @Test
    void runScheduledBackfill_shouldNotExecuteWhenDisabledByDefault() {
        MessageKnowledgeEmbeddingBackfillService backfillService =
                Mockito.mock(MessageKnowledgeEmbeddingBackfillService.class);
        MessageKnowledgeEmbeddingBackfillScheduler scheduler =
                new MessageKnowledgeEmbeddingBackfillScheduler(backfillService);

        scheduler.runScheduledBackfill();

        verifyNoInteractions(backfillService);
    }

    @Test
    void runOnceForStore_shouldSkipWhenProviderDisabled() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeEmbeddingBackfillService service = newService(itemRepository, provider);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxCallsPerRun", 1);
        when(provider.isEnabled()).thenReturn(false);

        MessageKnowledgeEmbeddingBackfillResult result = service.runOnceForStore(26L);

        assertEquals(0, result.attempted());
        assertEquals("embedding provider disabled", result.skippedReason());
        verifyNoInteractions(itemRepository);
        verify(provider).isEnabled();
        verify(provider, never()).embed(any());
    }

    @Test
    void runOnceForStore_shouldRespectMaxCallsZeroBeforeProviderCall() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeEmbeddingBackfillService service = newService(itemRepository, provider);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxCallsPerRun", 0);

        MessageKnowledgeEmbeddingBackfillResult result = service.runOnceForStore(26L);

        assertEquals(0, result.attempted());
        assertEquals("embedding max calls per run is zero", result.skippedReason());
        verifyNoInteractions(itemRepository, provider);
    }

    @Test
    void runOnceForStore_shouldClampBatchFillSemanticTextAndStoreReadyVector() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeEmbeddingBackfillService service = newService(itemRepository, provider);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxCallsPerRun", 10);
        ReflectionTestUtils.setField(service, "batchSize", 99);
        MessageKnowledgeItem item = newItem();

        when(provider.isEnabled()).thenReturn(true);
        when(itemRepository.findEmbeddingBackfillCandidates(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                anyList(),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(item));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(provider.embed(any())).thenReturn(new MessageKnowledgeEmbeddingResponse(
                List.of(0.25f, 0.5f),
                "mock",
                "mock-embedding",
                2
        ));

        MessageKnowledgeEmbeddingBackfillResult result = service.runOnceForStore(26L);

        assertEquals(1, result.attempted());
        assertEquals(1, result.succeeded());
        assertEquals(0, result.failed());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_READY, item.getEmbeddingStatus());
        assertEquals("[0.25,0.5]", item.getEmbeddingVector());
        assertEquals("mock", item.getEmbeddingProvider());
        assertEquals("mock-embedding", item.getEmbeddingModel());
        assertEquals(2, item.getEmbeddingDimensions());
        assertNotNull(item.getEmbeddingInputHash());
        assertNull(item.getEmbeddingError());
        assertNull(item.getEmbeddingLeaseOwner());
        assertNull(item.getEmbeddingLeaseUntil());
        assertEquals(1, item.getEmbeddingAttemptCount());
        assertTrue(item.getSemanticText().contains("Canonical fact: The wifi password is sakura2026."));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findEmbeddingBackfillCandidates(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                anyList(),
                any(LocalDateTime.class),
                pageableCaptor.capture()
        );
        assertEquals(3, pageableCaptor.getValue().getPageSize());

        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        verify(provider).embed(inputCaptor.capture());
        assertTrue(inputCaptor.getValue().contains("Topic: wifi"));
        assertTrue(inputCaptor.getValue().contains("Scope: ROOM_TYPE Deluxe"));
    }

    @Test
    void runOnceForStore_shouldMarkFailureAndBackoffWhenProviderThrows() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeEmbeddingBackfillService service = newService(itemRepository, provider);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "maxCallsPerRun", 1);
        ReflectionTestUtils.setField(service, "failureBackoffMs", 250L);
        MessageKnowledgeItem item = newItem();
        item.setSemanticText("Topic: wifi\nCanonical fact: The wifi password is sakura2026.");

        when(provider.isEnabled()).thenReturn(true);
        when(itemRepository.findEmbeddingBackfillCandidates(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                anyList(),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(item));
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(provider.embed(any())).thenThrow(new IllegalStateException("provider timeout"));

        MessageKnowledgeEmbeddingBackfillResult result = service.runOnceForStore(26L);

        assertEquals(1, result.attempted());
        assertEquals(0, result.succeeded());
        assertEquals(1, result.failed());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_FAILED, item.getEmbeddingStatus());
        assertEquals("provider timeout", item.getEmbeddingError());
        assertNotNull(item.getEmbeddingUpdatedAt());
        assertNotNull(item.getEmbeddingNextAttemptAt());
        assertNull(item.getEmbeddingLeaseOwner());
        assertNull(item.getEmbeddingLeaseUntil());
        assertEquals(1, item.getEmbeddingAttemptCount());
    }

    private static MessageKnowledgeEmbeddingBackfillService newService(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEmbeddingProvider provider
    ) {
        ObjectMapper objectMapper = new ObjectMapper();
        SuMessagingAiTextService textService = new SuMessagingAiTextService();
        return new MessageKnowledgeEmbeddingBackfillService(
                itemRepository,
                provider,
                new MessageKnowledgeEmbeddingTextService(objectMapper, textService),
                objectMapper
        );
    }

    private static MessageKnowledgeItem newItem() {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(501L);
        item.setStoreId(26L);
        item.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        item.setScopeId(3L);
        item.setScopeKey("ROOM_TYPE:3");
        item.setThreadId(77L);
        item.setRoomTypeId(3L);
        item.setRoomTypeName("Deluxe");
        item.setTopic("wifi");
        item.setTopicHash("topic-hash");
        item.setQuestion("What is the wifi password?");
        item.setAnswer("The wifi password is sakura2026.");
        item.setNormalizedQuestion("what is the wifi password");
        item.setNormalizedAnswer("the wifi password is sakura2026");
        item.setNormalizedAnswerHash("answer-hash");
        item.setLanguage("en");
        item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_PENDING);
        item.setEmbeddingAttemptCount(0);
        item.setEvidenceCount(2);
        item.setConfidence(BigDecimal.valueOf(0.8));
        return item;
    }
}
