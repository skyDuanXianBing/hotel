package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.persistence.Table;
import server.demo.entity.MessageKnowledgeEvidence;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.entity.MessageKnowledgeTopic;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.MessageKnowledgeEvidenceRepository;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageKnowledgeThreadExtractionServiceTest {

    @Test
    void extractThread_shouldWriteMultipleFactsFromSameSourceMessagesWithoutPairCollision() {
        ObjectMapper objectMapper = new ObjectMapper();
        SuMessagingAiTextService textService = new SuMessagingAiTextService();
        MessageKnowledgeThreadConversationLoader loader =
                Mockito.mock(MessageKnowledgeThreadConversationLoader.class);
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository =
                Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        MessageKnowledgeTopicDictionaryService topicDictionaryService =
                Mockito.mock(MessageKnowledgeTopicDictionaryService.class);
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);

        MessageKnowledgeThreadExtractionService service = new MessageKnowledgeThreadExtractionService(
                loader,
                new MessageKnowledgeThreadPromptBuilder(),
                new MessageKnowledgeThreadExtractionOutputParser(objectMapper),
                new MessageKnowledgeThreadExtractionOutputValidator(
                        new MessageKnowledgeThreadOutputSanitizer(),
                        textService
                ),
                new MessageKnowledgeThreadKnowledgeWriter(
                        itemRepository,
                        evidenceRepository,
                        new MessageKnowledgeEmbeddingTextService(objectMapper, textService),
                        textService,
                        objectMapper
                ),
                topicDictionaryService,
                chatLanguageModel
        );
        ReflectionTestUtils.setField(service, "threadExtractorEnabled", true);

        SuMessageThread thread = newThread();
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setChannelId(19);
        MessageKnowledgeThreadConversation conversation = new MessageKnowledgeThreadConversation(
                26L,
                77L,
                203L,
                thread,
                context,
                List.of(
                        message(201L, SuMessagingSenderType.GUEST, "Can I check out late?"),
                        message(202L, SuMessagingSenderType.STAFF, "Let me check that for you."),
                        message(203L, SuMessagingSenderType.STAFF, "Late checkout is available until 12:00 when occupancy allows.")
                )
        );
        when(loader.load(26L, 77L, 203L)).thenReturn(conversation);
        when(topicDictionaryService.findActiveTopics(26L, 50)).thenReturn(List.of(
                activeTopic("checkout"),
                activeTopic("luggage")
        ));
        when(chatLanguageModel.generate(anyString())).thenReturn("""
                {"schemaVersion":"message_knowledge_thread_v1","items":[
                  {"clientItemId":"late-checkout","topicCode":"checkout","scopeType":"STORE",
                   "canonicalQuestion":"Is late checkout available?",
                   "canonicalAnswer":"Late checkout is available until 12:00 when occupancy allows.",
                   "language":"en","confidence":0.93,
                   "sourceMessageIds":[201,202,203],
                   "evidenceSummary":"Staff confirmed the late checkout policy.",
                   "reusabilityReason":"This is a stable checkout policy."},
                  {"clientItemId":"luggage","topicCode":"luggage","scopeType":"STORE",
                   "canonicalQuestion":"Can guests leave luggage before checkout?",
                   "canonicalAnswer":"Guests can leave luggage at the front desk before checkout.",
                   "language":"en","confidence":0.91,
                   "sourceMessageIds":[201,202,203],
                   "evidenceSummary":"Staff confirmed the luggage storage policy in the same exchange.",
                   "reusabilityReason":"This is a stable checkout policy."}
                ]}
                """);
        when(evidenceRepository.existsByStoreIdAndSourceFingerprint(eq(26L), anyString())).thenReturn(false);
        when(itemRepository.findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
                eq(26L),
                eq("STORE"),
                anyString(),
                anyString()
        )).thenReturn(Optional.empty());
        when(itemRepository.save(any(MessageKnowledgeItem.class))).thenAnswer(invocation -> {
            MessageKnowledgeItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(501L);
            }
            return item;
        });
        when(evidenceRepository.save(any(MessageKnowledgeEvidence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MessageKnowledgeThreadExtractionSummary summary =
                service.extractThread(26L, 77L, 203L, "owner-1", "run-1");

        assertEquals("COMPLETED", summary.status());
        assertEquals(3, summary.totalMessages());
        assertEquals(3, summary.promptMessages());
        assertEquals(2, summary.parsedItems());
        assertEquals(2, summary.validItems());
        assertEquals(0, summary.rejectedItems());
        assertEquals(2, summary.writtenItems());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatLanguageModel).generate(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("[201] 用户: Can I check out late?"));
        assertTrue(promptCaptor.getValue().contains("[203] 员工: Late checkout is available until 12:00"));

        ArgumentCaptor<MessageKnowledgeEvidence> evidenceCaptor =
                ArgumentCaptor.forClass(MessageKnowledgeEvidence.class);
        verify(evidenceRepository, Mockito.times(2)).save(evidenceCaptor.capture());
        List<MessageKnowledgeEvidence> evidences = evidenceCaptor.getAllValues();
        MessageKnowledgeEvidence firstEvidence = evidences.get(0);
        MessageKnowledgeEvidence secondEvidence = evidences.get(1);
        assertThreadEvidence(firstEvidence);
        assertThreadEvidence(secondEvidence);
        assertFalse(firstEvidence.getSourceFingerprint().equals(secondEvidence.getSourceFingerprint()));
    }

    @Test
    void evidenceEntity_shouldNotKeepLegacyPairUniqueConstraint() {
        Table table = MessageKnowledgeEvidence.class.getAnnotation(Table.class);

        boolean hasLegacyPairUnique = Arrays.stream(table.uniqueConstraints())
                .anyMatch(constraint -> "uk_msg_knowledge_evidence_source".equals(constraint.name()));

        assertFalse(hasLegacyPairUnique);
    }

    private static SuMessageThread newThread() {
        SuMessageThread thread = new SuMessageThread();
        thread.setId(77L);
        thread.setStoreId(26L);
        thread.setChannelId(19);
        thread.setThreadKey("thread-77");
        thread.setSuHotelId("hotel-1");
        return thread;
    }

    private static MessageKnowledgeThreadConversationMessage message(
            Long id,
            SuMessagingSenderType senderType,
            String content
    ) {
        return new MessageKnowledgeThreadConversationMessage(
                id,
                LocalDateTime.of(2026, 6, 18, 12, 0).plusMinutes(id),
                senderType,
                "SENT",
                null,
                content,
                true
        );
    }

    private static MessageKnowledgeTopic activeTopic(String topicCode) {
        MessageKnowledgeTopic topic = new MessageKnowledgeTopic();
        topic.setStoreId(26L);
        topic.setTopicCode(topicCode);
        topic.setDisplayName(topicCode);
        topic.setScopePreference(SuMessagingThreadContext.SCOPE_STORE);
        topic.setStatus(MessageKnowledgeTopic.STATUS_ACTIVE);
        return topic;
    }

    private static void assertThreadEvidence(MessageKnowledgeEvidence evidence) {
        assertEquals(MessageKnowledgeEvidence.SOURCE_KIND_THREAD_CONVERSATION, evidence.getSourceKind());
        assertEquals("[201,202,203]", evidence.getSourceMessageIdsJson());
        assertEquals(201L, evidence.getSourceMessageStartId());
        assertEquals(203L, evidence.getSourceMessageEndId());
        assertNotNull(evidence.getSourceFingerprint());
        assertEquals(MessageKnowledgeThreadKnowledgeWriter.EXTRACTOR_VERSION, evidence.getExtractorVersion());
    }
}
