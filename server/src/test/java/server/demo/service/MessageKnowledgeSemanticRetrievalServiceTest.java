package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageKnowledgeSemanticRetrievalServiceTest {

    @Test
    void search_shouldReturnVectorHitWithoutExactKeywordOverlap() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeSemanticRetrievalService service = newService(itemRepository, provider);
        enableSemantic(service);

        SuMessagingThreadContext context = roomTypeContext();
        MessageKnowledgeItem wifi = newReadyItem(
                101L,
                26L,
                3L,
                "wifi",
                "What is the WiFi password?",
                "The WiFi password is sakura2026.",
                "[0.99,0.01]"
        );
        MessageKnowledgeItem breakfast = newReadyItem(
                102L,
                26L,
                3L,
                "breakfast",
                "What time is breakfast?",
                "Breakfast is served from 07:00 to 10:00.",
                "[0.0,1.0]"
        );

        when(provider.isEnabled()).thenReturn(true);
        when(provider.embed("インターネットは使えますか？")).thenReturn(new MessageKnowledgeEmbeddingResponse(
                List.of(1.0f, 0.0f),
                "mock",
                "mock-embedding",
                2
        ));
        when(itemRepository.findReadySemanticCandidatesByStoreIdAndRoomTypeId(
                eq(26L),
                eq(3L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                eq(MessageKnowledgeItem.EMBEDDING_STATUS_READY),
                any(Pageable.class)
        )).thenReturn(List.of(wifi, breakfast));
        when(itemRepository.findReadySemanticStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                eq(MessageKnowledgeItem.EMBEDDING_STATUS_READY),
                any(Pageable.class)
        )).thenReturn(List.of());

        List<MessageKnowledgeSemanticMatch> matches = service.search(
                26L,
                context,
                "インターネットは使えますか？"
        );

        assertEquals(1, matches.size());
        assertEquals(101L, matches.get(0).item().getId());
        assertEquals("The WiFi password is sakura2026.", matches.get(0).item().getAnswer());
        assertTrue(matches.get(0).reasons().contains("SEMANTIC_COSINE"));
    }

    @Test
    void search_shouldNotLoadCandidatesOrEmbedWhenProviderDisabled() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeSemanticRetrievalService service = newService(itemRepository, provider);
        enableSemantic(service);
        when(provider.isEnabled()).thenReturn(false);

        List<MessageKnowledgeSemanticMatch> matches = service.search(
                26L,
                roomTypeContext(),
                "Could you share the breakfast time?"
        );

        assertTrue(matches.isEmpty());
        verify(provider).isEnabled();
        verify(provider, never()).embed(any());
        verifyNoInteractions(itemRepository);
    }

    @Test
    void search_shouldIgnoreInactiveWrongStoreAndWrongScopeBeforeEmbedding() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeSemanticRetrievalService service = newService(itemRepository, provider);
        enableSemantic(service);

        SuMessagingThreadContext context = roomTypeContext();
        MessageKnowledgeItem inactive = newReadyItem(
                201L,
                26L,
                3L,
                "wifi",
                "What is the WiFi password?",
                "The WiFi password is sakura2026.",
                "[1.0,0.0]"
        );
        inactive.setStatus(MessageKnowledgeItem.STATUS_ARCHIVED);
        MessageKnowledgeItem wrongStore = newReadyItem(
                202L,
                99L,
                3L,
                "wifi",
                "What is the WiFi password?",
                "The WiFi password is other-store.",
                "[1.0,0.0]"
        );
        MessageKnowledgeItem wrongScope = newReadyItem(
                203L,
                26L,
                9L,
                "wifi",
                "What is the WiFi password?",
                "The WiFi password is wrong-room-type.",
                "[1.0,0.0]"
        );

        when(provider.isEnabled()).thenReturn(true);
        when(itemRepository.findReadySemanticCandidatesByStoreIdAndRoomTypeId(
                eq(26L),
                eq(3L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                eq(MessageKnowledgeItem.EMBEDDING_STATUS_READY),
                any(Pageable.class)
        )).thenReturn(List.of(inactive, wrongStore, wrongScope));
        when(itemRepository.findReadySemanticStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                eq(MessageKnowledgeItem.EMBEDDING_STATUS_READY),
                any(Pageable.class)
        )).thenReturn(List.of());

        List<MessageKnowledgeSemanticMatch> matches = service.search(
                26L,
                context,
                "インターネットは使えますか？"
        );

        assertTrue(matches.isEmpty());
        verify(provider, never()).embed(any());
    }

    @Test
    void search_shouldDeduplicateLegacyLocationShuttleWhenCorrectTopicExists() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeSemanticRetrievalService service = newService(itemRepository, provider);
        enableSemantic(service);

        SuMessagingThreadContext context = roomTypeContext();
        MessageKnowledgeItem legacyShuttle = newReadyItem(
                301L,
                26L,
                3L,
                "location",
                "Do you have an airport shuttle?",
                "The airport shuttle leaves from the lobby.",
                "[1.0,0.0]"
        );
        legacyShuttle.setLastSeenAt(LocalDateTime.now().plusDays(1));
        MessageKnowledgeItem currentShuttle = newReadyItem(
                302L,
                26L,
                3L,
                "shuttle",
                "Do you have an airport shuttle?",
                "The airport shuttle can be reserved at the front desk.",
                "[0.99,0.01]"
        );
        currentShuttle.setLastSeenAt(LocalDateTime.now().minusDays(1));

        when(provider.isEnabled()).thenReturn(true);
        when(provider.embed("Do you have an airport transfer?")).thenReturn(new MessageKnowledgeEmbeddingResponse(
                List.of(1.0f, 0.0f),
                "mock",
                "mock-embedding",
                2
        ));
        when(itemRepository.findReadySemanticCandidatesByStoreIdAndRoomTypeId(
                eq(26L),
                eq(3L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                eq(MessageKnowledgeItem.EMBEDDING_STATUS_READY),
                any(Pageable.class)
        )).thenReturn(List.of(legacyShuttle, currentShuttle));
        when(itemRepository.findReadySemanticStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                eq(MessageKnowledgeItem.EMBEDDING_STATUS_READY),
                any(Pageable.class)
        )).thenReturn(List.of());

        List<MessageKnowledgeSemanticMatch> matches = service.search(
                26L,
                context,
                "Do you have an airport transfer?"
        );

        assertEquals(1, matches.size());
        assertEquals(302L, matches.get(0).item().getId());
        assertEquals("shuttle", matches.get(0).item().getTopic());
        assertEquals(
                "The airport shuttle can be reserved at the front desk.",
                matches.get(0).item().getAnswer()
        );
    }

    private static MessageKnowledgeSemanticRetrievalService newService(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEmbeddingProvider provider
    ) {
        MessageKnowledgeTopicService topicService = new MessageKnowledgeTopicService();
        MessageKnowledgeSemanticRetrievalService service = new MessageKnowledgeSemanticRetrievalService(
                itemRepository,
                provider,
                new ObjectMapper()
        );
        service.setTopicRemediationService(new MessageKnowledgeTopicRemediationService(topicService));
        return service;
    }

    private static void enableSemantic(MessageKnowledgeSemanticRetrievalService service) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "candidateCap", 12);
        ReflectionTestUtils.setField(service, "topK", 8);
        ReflectionTestUtils.setField(service, "minScore", 0.8);
    }

    private static SuMessagingThreadContext roomTypeContext() {
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomTypeId(3L);
        context.setRoomTypeName("Deluxe");
        return context;
    }

    private static MessageKnowledgeItem newReadyItem(
            Long id,
            Long storeId,
            Long roomTypeId,
            String topic,
            String question,
            String answer,
            String vector
    ) {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(id);
        item.setStoreId(storeId);
        item.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        item.setScopeId(roomTypeId);
        item.setScopeKey("ROOM_TYPE:" + roomTypeId);
        item.setThreadId(77L);
        item.setRoomTypeId(roomTypeId);
        item.setRoomTypeName("Deluxe");
        item.setTopic(topic);
        item.setTopicHash("topic-hash-" + id);
        item.setQuestion(question);
        item.setAnswer(answer);
        item.setNormalizedQuestion(question.toLowerCase());
        item.setNormalizedAnswer(answer.toLowerCase());
        item.setNormalizedAnswerHash("answer-hash-" + id);
        item.setLanguage("en");
        item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_READY);
        item.setEmbeddingVector(vector);
        item.setSemanticText("Topic: " + topic + "\nCanonical fact: " + answer);
        item.setLastSeenAt(LocalDateTime.now());
        return item;
    }
}
