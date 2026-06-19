package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.entity.MessageKnowledgeEvidence;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.entity.SuMessageThread;
import server.demo.repository.MessageKnowledgeEvidenceRepository;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageKnowledgeThreadKnowledgeWriterDedupTest {

    @Test
    void write_shouldNotCallAiDedupWhenExactHashHits() {
        WriterFixture fixture = newFixture();
        MessageKnowledgeItem exactItem = existingItem(101L);
        when(fixture.evidenceRepository.existsByStoreIdAndSourceFingerprint(eq(26L), anyString()))
                .thenReturn(false);
        when(fixture.itemRepository.findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
                eq(26L),
                eq("ROOM_TYPE:3"),
                anyString(),
                anyString()
        )).thenReturn(Optional.of(exactItem));

        MessageKnowledgeThreadWriterResult result = fixture.writer.write(
                26L,
                thread(),
                roomTypeContext(),
                List.of(validItem("Late checkout is available until noon if occupancy allows."))
        );

        assertEquals(1, result.writtenItems());
        assertEquals(0, result.skippedDuplicateEvidence());
        verifyNoInteractions(fixture.duplicateCandidateService);
        verifyNoInteractions(fixture.duplicateJudgeService);
        verify(fixture.embeddingTextService).refreshSemanticFields(exactItem, false);
    }

    @Test
    void write_shouldAppendEvidenceToAiDuplicateWithoutOverwritingExistingItem() {
        WriterFixture fixture = newFixture();
        MessageKnowledgeItem duplicateItem = existingItem(101L);
        duplicateItem.setAnswer("Late checkout is available until 12:00 when occupancy allows.");
        duplicateItem.setNormalizedAnswer("late checkout is available until 12 00 when occupancy allows");
        duplicateItem.setFactHash("existing-fact-hash");
        duplicateItem.setEvidenceCount(2);
        duplicateItem.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_READY);
        when(fixture.evidenceRepository.existsByStoreIdAndSourceFingerprint(eq(26L), anyString()))
                .thenReturn(false);
        when(fixture.itemRepository.findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
                eq(26L),
                eq("ROOM_TYPE:3"),
                anyString(),
                anyString()
        )).thenReturn(Optional.empty());
        when(fixture.duplicateJudgeService.isEnabled()).thenReturn(true);
        when(fixture.duplicateCandidateService.findCandidates(
                eq(26L),
                eq("ROOM_TYPE:3"),
                eq("checkout"),
                anyString(),
                anyString()
        )).thenReturn(List.of(duplicateItem));
        when(fixture.duplicateJudgeService.judge(any(), eq(List.of(duplicateItem))))
                .thenReturn(new MessageKnowledgeDuplicateJudgeService.DuplicateJudgeResult(
                        true,
                        101L,
                        0.92,
                        "same policy"
                ));

        MessageKnowledgeThreadWriterResult result = fixture.writer.write(
                26L,
                thread(),
                roomTypeContext(),
                List.of(validItem("Late checkout is available until noon if occupancy allows."))
        );

        assertEquals(1, result.writtenItems());
        assertEquals(3, duplicateItem.getEvidenceCount());
        assertEquals("Late checkout is available until 12:00 when occupancy allows.", duplicateItem.getAnswer());
        assertEquals("existing-fact-hash", duplicateItem.getFactHash());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_READY, duplicateItem.getEmbeddingStatus());
        verify(fixture.embeddingTextService, never()).refreshSemanticFields(any(), anyBoolean());

        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(fixture.evidenceRepository).save(evidenceCaptor.capture());
        assertSame(duplicateItem, evidenceCaptor.getValue().getItem());
        assertEquals("Late checkout is available until noon if occupancy allows.", evidenceCaptor.getValue().getAnswer());
    }

    @Test
    void write_shouldCreateNewItemWhenAiJudgeSaysNotDuplicate() {
        WriterFixture fixture = newFixture();
        MessageKnowledgeItem candidate = existingItem(101L);
        when(fixture.evidenceRepository.existsByStoreIdAndSourceFingerprint(eq(26L), anyString()))
                .thenReturn(false);
        when(fixture.itemRepository.findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
                eq(26L),
                eq("ROOM_TYPE:3"),
                anyString(),
                anyString()
        )).thenReturn(Optional.empty());
        when(fixture.duplicateJudgeService.isEnabled()).thenReturn(true);
        when(fixture.duplicateCandidateService.findCandidates(
                eq(26L),
                eq("ROOM_TYPE:3"),
                eq("checkout"),
                anyString(),
                anyString()
        )).thenReturn(List.of(candidate));
        when(fixture.duplicateJudgeService.judge(any(), eq(List.of(candidate))))
                .thenReturn(MessageKnowledgeDuplicateJudgeService.DuplicateJudgeResult.notDuplicate());

        fixture.writer.write(
                26L,
                thread(),
                roomTypeContext(),
                List.of(validItem("Late checkout costs 200 JPY per hour."))
        );

        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        verify(fixture.itemRepository, Mockito.times(2)).save(itemCaptor.capture());
        MessageKnowledgeItem savedItem = itemCaptor.getAllValues().get(0);
        assertEquals(501L, savedItem.getId());
        assertEquals("Late checkout costs 200 JPY per hour.", savedItem.getAnswer());
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, savedItem.getStatus());
        verify(fixture.embeddingTextService).refreshSemanticFields(savedItem, true);
    }

    @Test
    void write_shouldCreateNewItemWhenAiDedupFails() {
        WriterFixture fixture = newFixture();
        when(fixture.evidenceRepository.existsByStoreIdAndSourceFingerprint(eq(26L), anyString()))
                .thenReturn(false);
        when(fixture.itemRepository.findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
                eq(26L),
                eq("ROOM_TYPE:3"),
                anyString(),
                anyString()
        )).thenReturn(Optional.empty());
        when(fixture.duplicateJudgeService.isEnabled()).thenReturn(true);
        when(fixture.duplicateCandidateService.findCandidates(
                eq(26L),
                eq("ROOM_TYPE:3"),
                eq("checkout"),
                anyString(),
                anyString()
        )).thenThrow(new IllegalStateException("candidate lookup failed"));

        MessageKnowledgeThreadWriterResult result = fixture.writer.write(
                26L,
                thread(),
                roomTypeContext(),
                List.of(validItem("Late checkout costs 200 JPY per hour."))
        );

        assertEquals(1, result.writtenItems());
        ArgumentCaptor<MessageKnowledgeItem> itemCaptor = ArgumentCaptor.forClass(MessageKnowledgeItem.class);
        verify(fixture.itemRepository, Mockito.times(2)).save(itemCaptor.capture());
        assertEquals(501L, itemCaptor.getAllValues().get(0).getId());
    }

    @Test
    void write_shouldKeepRejectedExactItemRejected() {
        WriterFixture fixture = newFixture();
        MessageKnowledgeItem rejectedItem = existingItem(101L);
        rejectedItem.setStatus(MessageKnowledgeItem.STATUS_REJECTED);
        when(fixture.evidenceRepository.existsByStoreIdAndSourceFingerprint(eq(26L), anyString()))
                .thenReturn(false);
        when(fixture.itemRepository.findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
                eq(26L),
                eq("ROOM_TYPE:3"),
                anyString(),
                anyString()
        )).thenReturn(Optional.of(rejectedItem));

        fixture.writer.write(
                26L,
                thread(),
                roomTypeContext(),
                List.of(validItem("Late checkout is available until noon if occupancy allows."))
        );

        assertEquals(MessageKnowledgeItem.STATUS_REJECTED, rejectedItem.getStatus());
        verifyNoInteractions(fixture.duplicateCandidateService);
        verifyNoInteractions(fixture.duplicateJudgeService);
    }

    @Test
    void write_shouldSkipDuplicateEvidenceBeforeExactOrAiDedup() {
        WriterFixture fixture = newFixture();
        when(fixture.evidenceRepository.existsByStoreIdAndSourceFingerprint(eq(26L), anyString()))
                .thenReturn(true);

        MessageKnowledgeThreadWriterResult result = fixture.writer.write(
                26L,
                thread(),
                roomTypeContext(),
                List.of(validItem("Late checkout is available until noon if occupancy allows."))
        );

        assertEquals(0, result.writtenItems());
        assertEquals(1, result.skippedDuplicateEvidence());
        verify(fixture.itemRepository, never()).findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
                any(),
                anyString(),
                anyString(),
                anyString()
        );
        verifyNoInteractions(fixture.duplicateCandidateService);
        verifyNoInteractions(fixture.duplicateJudgeService);
    }

    private static WriterFixture newFixture() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        MessageKnowledgeEmbeddingTextService embeddingTextService =
                Mockito.mock(MessageKnowledgeEmbeddingTextService.class);
        MessageKnowledgeDuplicateCandidateService duplicateCandidateService =
                Mockito.mock(MessageKnowledgeDuplicateCandidateService.class);
        MessageKnowledgeDuplicateJudgeService duplicateJudgeService =
                Mockito.mock(MessageKnowledgeDuplicateJudgeService.class);
        SuMessagingAiTextService textService = new SuMessagingAiTextService();
        MessageKnowledgeThreadKnowledgeWriter writer = new MessageKnowledgeThreadKnowledgeWriter(
                itemRepository,
                evidenceRepository,
                embeddingTextService,
                textService,
                new ObjectMapper()
        );
        writer.setDuplicateCandidateService(duplicateCandidateService);
        writer.setDuplicateJudgeService(duplicateJudgeService);

        AtomicLong nextId = new AtomicLong(501L);
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(nextId.getAndIncrement());
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return new WriterFixture(
                writer,
                itemRepository,
                evidenceRepository,
                embeddingTextService,
                duplicateCandidateService,
                duplicateJudgeService
        );
    }

    private static MessageKnowledgeThreadValidatedItem validItem(String answer) {
        SuMessagingAiTextService textService = new SuMessagingAiTextService();
        String normalizedAnswer = textService.normalize(answer);
        return new MessageKnowledgeThreadValidatedItem(
                "client-1",
                "checkout",
                "Checkout",
                SuMessagingThreadContext.SCOPE_ROOM_TYPE,
                3L,
                "ROOM_TYPE:3",
                "Can I check out late?",
                answer,
                "can i check out late",
                normalizedAnswer,
                "guest asked late checkout and staff answered " + normalizedAnswer,
                "en",
                BigDecimal.valueOf(0.93),
                MessageKnowledgeItem.STATUS_ACTIVE,
                List.of(201L, 202L),
                List.of(),
                LocalDateTime.of(2026, 6, 18, 12, 0),
                "Staff confirmed late checkout.",
                false
        );
    }

    private static MessageKnowledgeItem existingItem(Long id) {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(id);
        item.setStoreId(26L);
        item.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        item.setScopeId(3L);
        item.setScopeKey("ROOM_TYPE:3");
        item.setThreadId(77L);
        item.setRoomTypeId(3L);
        item.setRoomTypeName("Deluxe");
        item.setTopic("checkout");
        item.setTopicHash("topic-hash");
        item.setFactHash("fact-hash");
        item.setQuestion("Is late checkout available?");
        item.setAnswer("Late checkout is available until noon if occupancy allows.");
        item.setNormalizedQuestion("is late checkout available");
        item.setNormalizedAnswer("late checkout is available until noon if occupancy allows");
        item.setNormalizedAnswerHash("answer-hash");
        item.setLanguage("en");
        item.setConfidence(BigDecimal.valueOf(0.9));
        item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        item.setEvidenceCount(1);
        item.setFirstSeenAt(LocalDateTime.of(2026, 6, 18, 10, 0));
        item.setLastSeenAt(LocalDateTime.of(2026, 6, 18, 10, 0));
        return item;
    }

    private static SuMessageThread thread() {
        SuMessageThread thread = new SuMessageThread();
        thread.setId(77L);
        thread.setStoreId(26L);
        thread.setThreadKey("thread-77");
        thread.setChannelId(19);
        return thread;
    }

    private static SuMessagingThreadContext roomTypeContext() {
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomTypeId(3L);
        context.setRoomTypeName("Deluxe");
        context.setChannelId(19);
        return context;
    }

    private record WriterFixture(
            MessageKnowledgeThreadKnowledgeWriter writer,
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEvidenceRepository evidenceRepository,
            MessageKnowledgeEmbeddingTextService embeddingTextService,
            MessageKnowledgeDuplicateCandidateService duplicateCandidateService,
            MessageKnowledgeDuplicateJudgeService duplicateJudgeService
    ) {
    }
}
