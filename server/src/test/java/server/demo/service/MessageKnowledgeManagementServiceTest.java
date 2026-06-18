package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import server.demo.dto.MessageKnowledgeEvidenceResponse;
import server.demo.dto.MessageKnowledgeItemDTO;
import server.demo.dto.MessageKnowledgeItemPageResponse;
import server.demo.entity.MessageKnowledgeEvidence;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeEvidenceRepository;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageKnowledgeManagementServiceTest {

    @Test
    void listItems_shouldUseStoreScopedFiltersAndMapConflictStatusForApi() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository = Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        MessageKnowledgeManagementService service =
                new MessageKnowledgeManagementService(itemRepository, evidenceRepository);
        MessageKnowledgeItem item = newItem(9L);
        item.setStatus("CONFLICT");

        when(itemRepository.searchManagementItems(
                eq(26L),
                eq("CONFLICT"),
                eq("%wifi%"),
                eq("ROOM_TYPE"),
                eq("wifi"),
                eq(PageRequest.of(0, 100))
        )).thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 100), 1));
        when(evidenceRepository.findDistinctLanguagesByStoreIdAndItemId(26L, 9L))
                .thenReturn(List.of("en", "zh", "ja"));

        MessageKnowledgeItemPageResponse response = service.listItems(
                26L,
                -1,
                200,
                "CONFLICTED",
                "Wifi",
                "room_type",
                "WiFi"
        );

        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getItems().size());
        assertEquals("CONFLICTED", response.getContent().get(0).getStatus());
        assertEquals("Wifi - Deluxe", response.getContent().get(0).getTitle());
        assertEquals("en", response.getContent().get(0).getLanguage());
        assertEquals(List.of("en", "zh", "ja"), response.getContent().get(0).getEvidenceLanguages());
        assertEquals("en, zh, ja", response.getContent().get(0).getLanguageSummary());
        assertEquals(MessageKnowledgeItem.EMBEDDING_STATUS_READY, response.getContent().get(0).getEmbeddingStatus());
        assertEquals("openai", response.getContent().get(0).getEmbeddingProvider());
        assertEquals("text-embedding-3-small", response.getContent().get(0).getEmbeddingModel());
        assertEquals(1536, response.getContent().get(0).getEmbeddingDimensions());
        assertEquals(
                OffsetDateTime.of(2026, 6, 15, 10, 25, 0, 0, ZoneOffset.UTC),
                response.getContent().get(0).getEmbeddingUpdatedAt()
        );
        assertEquals("provider throttled", response.getContent().get(0).getEmbeddingError());
        assertEquals(100, response.getSize());
        assertFalse(response.isHasNext());
    }

    @Test
    void getEvidence_shouldReadEvidenceByStoreAndItemId() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository = Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        MessageKnowledgeManagementService service =
                new MessageKnowledgeManagementService(itemRepository, evidenceRepository);
        MessageKnowledgeItem item = newItem(9L);
        MessageKnowledgeEvidence evidence = newEvidence(item);

        when(itemRepository.findByStoreIdAndId(26L, 9L)).thenReturn(Optional.of(item));
        when(evidenceRepository.findByStoreIdAndItemIdOrderBySourceTimestampDesc(26L, 9L))
                .thenReturn(List.of(evidence));

        MessageKnowledgeEvidenceResponse response = service.getEvidence(26L, 9L);

        assertEquals(9L, response.getItem().getId());
        assertEquals("en, zh", response.getItem().getLanguageSummary());
        assertEquals(1, response.getEvidence().size());
        assertEquals(
                MessageKnowledgeEvidence.SOURCE_KIND_MESSAGE_PAIR,
                response.getEvidence().get(0).getSourceType()
        );
        assertEquals("zh", response.getEvidence().get(0).getLanguage());
        assertEquals("Guest: What is the wifi password?\nStaff: The wifi password is sakura2026.", response.getEvidence().get(0).getSourceText());
        verify(evidenceRepository).findByStoreIdAndItemIdOrderBySourceTimestampDesc(26L, 9L);
    }

    @Test
    void getEvidence_shouldMapThreadConversationEvidenceWithoutPairAssumption() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository = Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        MessageKnowledgeManagementService service =
                new MessageKnowledgeManagementService(itemRepository, evidenceRepository);
        MessageKnowledgeItem item = newItem(9L);
        MessageKnowledgeEvidence evidence = newThreadEvidence(item);

        when(itemRepository.findByStoreIdAndId(26L, 9L)).thenReturn(Optional.of(item));
        when(evidenceRepository.findByStoreIdAndItemIdOrderBySourceTimestampDesc(26L, 9L))
                .thenReturn(List.of(evidence));

        MessageKnowledgeEvidenceResponse response = service.getEvidence(26L, 9L);

        assertEquals(1, response.getEvidence().size());
        assertEquals(
                MessageKnowledgeEvidence.SOURCE_KIND_THREAD_CONVERSATION,
                response.getEvidence().get(0).getSourceType()
        );
        assertEquals(
                MessageKnowledgeEvidence.SOURCE_KIND_THREAD_CONVERSATION,
                response.getEvidence().get(0).getSourceKind()
        );
        assertEquals(77L, response.getEvidence().get(0).getThreadId());
        assertEquals(List.of(101L, 102L, 103L), response.getEvidence().get(0).getSourceMessageIds());
        assertEquals(101L, response.getEvidence().get(0).getSourceMessageStartId());
        assertEquals(103L, response.getEvidence().get(0).getSourceMessageEndId());
        assertEquals("thread-v1", response.getEvidence().get(0).getExtractorVersion());
        assertEquals("Airbnb - Deluxe - 会话证据", response.getEvidence().get(0).getSourceTitle());
        assertEquals(
                "Question: Can I leave bags after checkout?\n"
                        + "Answer: Luggage storage is available at the front desk until 18:00.\n"
                        + "Source messages: 101, 102, 103\n"
                        + "Thread: 77",
                response.getEvidence().get(0).getSourceText()
        );
        assertEquals(
                "Luggage storage is available at the front desk until 18:00.",
                response.getEvidence().get(0).getStaffMessage()
        );
    }

    @Test
    void approve_shouldSetItemActiveWithinStore() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEvidenceRepository evidenceRepository = Mockito.mock(MessageKnowledgeEvidenceRepository.class);
        MessageKnowledgeManagementService service =
                new MessageKnowledgeManagementService(itemRepository, evidenceRepository);
        MessageKnowledgeItem item = newItem(9L);
        item.setStatus("REJECTED");

        when(itemRepository.findByStoreIdAndId(26L, 9L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(evidenceRepository.findDistinctLanguagesByStoreIdAndItemId(26L, 9L))
                .thenReturn(List.of("en"));

        MessageKnowledgeItemDTO dto = service.approve(26L, 9L);

        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, item.getStatus());
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, dto.getStatus());
        assertEquals("en", dto.getLanguageSummary());
        verify(itemRepository).findByStoreIdAndId(26L, 9L);
        verify(itemRepository).save(item);
    }

    private static MessageKnowledgeItem newItem(Long id) {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(id);
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
        item.setEvidenceCount(2);
        item.setConfidence(BigDecimal.valueOf(0.8));
        item.setCreatedAt(LocalDateTime.of(2026, 6, 15, 10, 0));
        item.setUpdatedAt(LocalDateTime.of(2026, 6, 15, 10, 30));
        item.setLastSeenAt(LocalDateTime.of(2026, 6, 15, 10, 20));
        item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_READY);
        item.setEmbeddingProvider("openai");
        item.setEmbeddingModel("text-embedding-3-small");
        item.setEmbeddingDimensions(1536);
        item.setEmbeddingUpdatedAt(LocalDateTime.of(2026, 6, 15, 10, 25));
        item.setEmbeddingError("provider throttled");
        return item;
    }

    private static MessageKnowledgeEvidence newEvidence(MessageKnowledgeItem item) {
        MessageKnowledgeEvidence evidence = new MessageKnowledgeEvidence();
        evidence.setId(11L);
        evidence.setStoreId(26L);
        evidence.setItem(item);
        evidence.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        evidence.setScopeId(3L);
        evidence.setScopeKey("ROOM_TYPE:3");
        evidence.setThreadId(77L);
        evidence.setRoomTypeId(3L);
        evidence.setRoomTypeName("Deluxe");
        evidence.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        evidence.setQuestion("What is the wifi password?");
        evidence.setAnswer("The wifi password is sakura2026.");
        evidence.setNormalizedText("what is the wifi password the wifi password is sakura2026");
        evidence.setNormalizedHash("evidence-hash");
        evidence.setLanguage("zh");
        evidence.setConfidence(BigDecimal.valueOf(0.8));
        evidence.setSourceTimestamp(LocalDateTime.of(2026, 6, 15, 10, 20));
        evidence.setCreatedAt(LocalDateTime.of(2026, 6, 15, 10, 21));
        return evidence;
    }

    private static MessageKnowledgeEvidence newThreadEvidence(MessageKnowledgeItem item) {
        MessageKnowledgeEvidence evidence = new MessageKnowledgeEvidence();
        evidence.setId(12L);
        evidence.setStoreId(26L);
        evidence.setItem(item);
        evidence.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        evidence.setScopeId(3L);
        evidence.setScopeKey("ROOM_TYPE:3");
        evidence.setThreadId(77L);
        evidence.setRoomTypeId(3L);
        evidence.setRoomTypeName("Deluxe");
        evidence.setChannelId(SuMessagingService.CHANNEL_AIRBNB);
        evidence.setQuestion("Can I leave bags after checkout?");
        evidence.setAnswer("Luggage storage is available at the front desk until 18:00.");
        evidence.setNormalizedText("luggage storage available front desk until 18:00");
        evidence.setNormalizedHash("thread-evidence-hash");
        evidence.setLanguage("en");
        evidence.setConfidence(BigDecimal.valueOf(0.88));
        evidence.setSourceKind(MessageKnowledgeEvidence.SOURCE_KIND_THREAD_CONVERSATION);
        evidence.setSourceMessageIdsJson("[101,102,103]");
        evidence.setSourceMessageStartId(101L);
        evidence.setSourceMessageEndId(103L);
        evidence.setExtractorVersion("thread-v1");
        evidence.setSourceFingerprint("fingerprint-1");
        evidence.setSourceTimestamp(LocalDateTime.of(2026, 6, 15, 10, 22));
        evidence.setCreatedAt(LocalDateTime.of(2026, 6, 15, 10, 23));
        return evidence;
    }
}
