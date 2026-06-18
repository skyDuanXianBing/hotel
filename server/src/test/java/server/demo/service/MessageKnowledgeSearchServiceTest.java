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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageKnowledgeSearchServiceTest {

    @Test
    void searchSimilar_shouldUseSemanticRetrievalBeforeRefinedKeywordRules() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeSearchService service = newSearchService();
        service.setSemanticRetrievalService(newSemanticService(itemRepository, provider));

        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomTypeId(3L);
        context.setRoomTypeName("Deluxe");
        MessageKnowledgeItem wifi = newKnowledgeItem(
                901L,
                "What is the WiFi password?",
                "The WiFi password is sakura2026."
        );
        wifi.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_READY);
        wifi.setEmbeddingVector("[0.99,0.01]");
        wifi.setSemanticText("Topic: wifi\nCanonical fact: The WiFi password is sakura2026.");

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
        )).thenReturn(List.of(wifi));
        when(itemRepository.findReadySemanticStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                eq(MessageKnowledgeItem.EMBEDDING_STATUS_READY),
                any(Pageable.class)
        )).thenReturn(List.of());

        MessageKnowledgeSearchResult result = service.searchSimilar(
                26L,
                77L,
                context,
                "インターネットは使えますか？",
                3
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_MATCHED, result.getRetrievalStatus());
        assertEquals(1, result.getMatches().size());
        assertEquals("The WiFi password is sakura2026.", result.getMatches().get(0).getCandidate().getAnswer());
        assertTrue(result.getMatches().get(0).getMatchReasons().contains("SEMANTIC_COSINE"));
        assertTrue(result.getMatches().get(0).getMatchReasons().contains("REFINED_FACT"));
        assertTrue(result.getMatches().get(0).getReusableFacts().contains("The WiFi password is sakura2026."));
    }

    @Test
    void searchSimilar_shouldUseActiveRefinedFactsWhenSemanticProviderDisabled() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeEmbeddingProvider provider = Mockito.mock(MessageKnowledgeEmbeddingProvider.class);
        MessageKnowledgeSearchService service = newSearchService();
        service.setKnowledgeItemRepository(itemRepository);
        service.setSemanticRetrievalService(newSemanticService(itemRepository, provider));
        when(provider.isEnabled()).thenReturn(false);

        MessageKnowledgeItem item = newKnowledgeItem(
                9L,
                "What is the wifi password?",
                "The wifi password is sakura2026."
        );
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomTypeId(3L);
        when(itemRepository.findActiveByStoreIdAndRoomTypeId(
                eq(26L),
                eq(3L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of(item));
        when(itemRepository.findActiveStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of());

        MessageKnowledgeSearchResult result = service.searchSimilar(
                26L,
                77L,
                context,
                "Can you share the wifi password?",
                3
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_MATCHED, result.getRetrievalStatus());
        assertEquals(1, result.getMatches().size());
        assertEquals("The wifi password is sakura2026.", result.getMatches().get(0).getCandidate().getAnswer());
        assertTrue(result.getMatches().get(0).getMatchReasons().contains("REFINED_FACT"));
    }

    @Test
    void searchSimilar_shouldReturnNoMatchWithoutRefinedRepositoryOrSemanticResults() {
        MessageKnowledgeSearchService service = newSearchService();

        MessageKnowledgeSearchResult result = service.searchSimilar(
                26L,
                77L,
                null,
                "Is late checkout possible?",
                3
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_NO_MATCH, result.getRetrievalStatus());
        assertTrue(result.getMatches().isEmpty());
    }

    @Test
    void searchSimilar_shouldDeduplicateLegacyTopicItemWhenCorrectTopicExists() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeTopicService topicService = new MessageKnowledgeTopicService();
        MessageKnowledgeSearchService service = new MessageKnowledgeSearchService(
                new SuMessagingAiTextService(),
                topicService
        );
        service.setKnowledgeItemRepository(itemRepository);
        service.setTopicRemediationService(new MessageKnowledgeTopicRemediationService(topicService));

        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomTypeId(3L);
        MessageKnowledgeItem legacyPetPolicy = newKnowledgeItem(
                11L,
                "Do you allow pets?",
                "Pets are not allowed in the rooms."
        );
        legacyPetPolicy.setTopic("general:do:you:allow:pets");
        legacyPetPolicy.setTopicHash("legacy-pet-topic-hash");
        legacyPetPolicy.setNormalizedAnswerHash("legacy-pet-answer-hash");
        legacyPetPolicy.setLastSeenAt(LocalDateTime.now().plusDays(1));

        MessageKnowledgeItem currentPetPolicy = newKnowledgeItem(
                12L,
                "Do you allow pets?",
                "Pets are not allowed anywhere in the property."
        );
        currentPetPolicy.setTopic("pet_policy");
        currentPetPolicy.setTopicHash("current-pet-topic-hash");
        currentPetPolicy.setNormalizedAnswerHash("current-pet-answer-hash");
        currentPetPolicy.setLastSeenAt(LocalDateTime.now().minusDays(1));

        when(itemRepository.findActiveByStoreIdAndRoomTypeId(
                eq(26L),
                eq(3L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of(legacyPetPolicy, currentPetPolicy));
        when(itemRepository.findActiveStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of());

        MessageKnowledgeSearchResult result = service.searchSimilar(
                26L,
                77L,
                context,
                "Do you allow pets?",
                3
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_MATCHED, result.getRetrievalStatus());
        assertEquals(1, result.getMatches().size());
        assertEquals(
                "Pets are not allowed anywhere in the property.",
                result.getMatches().get(0).getCandidate().getAnswer()
        );
        assertTrue(result.getMatches().get(0).getPolicyTopics()
                .contains(MessageKnowledgeTopicService.TOPIC_PET_POLICY));
    }

    @Test
    void searchSimilar_shouldIgnoreRejectedRefinedItemsEvenIfRepositoryReturnsThem() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeSearchService service = newSearchService();
        service.setKnowledgeItemRepository(itemRepository);

        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomTypeId(3L);
        MessageKnowledgeItem item = newKnowledgeItem(
                10L,
                "What is the wifi password?",
                "The wifi password is sakura2026."
        );
        item.setStatus("REJECTED");

        when(itemRepository.findActiveByStoreIdAndRoomTypeId(
                eq(26L),
                eq(3L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of(item));
        when(itemRepository.findActiveStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of());

        MessageKnowledgeSearchResult result = service.searchSimilar(
                26L,
                77L,
                context,
                "Can you share the wifi password?",
                3
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_NO_MATCH, result.getRetrievalStatus());
        assertTrue(result.getMatches().isEmpty());
    }

    @Test
    void searchSimilar_shouldUseStoreScopedPolicyFactsForBa03Topics() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeSearchService service = newSearchService();
        service.setKnowledgeItemRepository(itemRepository);
        when(itemRepository.findActiveStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of(
                newStoreKnowledgeItem(
                        1L,
                        "Can I request late checkout?",
                        "Late checkout is available until 12:30 for 1500 JPY when there is no same-day arrival."
                ),
                newStoreKnowledgeItem(
                        2L,
                        "Do you have parking?",
                        "Parking costs 1000 JPY per night. Spaces are first come, first served."
                ),
                newStoreKnowledgeItem(
                        3L,
                        "Can I store my luggage?",
                        "We can keep luggage at the front desk from 09:00 to 18:00."
                ),
                newStoreKnowledgeItem(
                        4L,
                        "What time is breakfast?",
                        "Breakfast is served 07:00-10:00 at Sakura. It is 1800 JPY per adult."
                )
        ));

        assertPolicyMatchContainsFacts(service, "Is late checkout possible?", "12:30", "1500 JPY");
        assertPolicyMatchContainsFacts(service, "Do you have parking for my car?", "1000 JPY", "first come");
        assertPolicyMatchContainsFacts(service, "Can you keep our bags before check-in?", "09:00", "18:00");
        assertPolicyMatchContainsFacts(service, "What time is breakfast and how much?", "07:00-10:00", "1800 JPY");
    }

    @Test
    void searchSimilar_shouldNotLeakPolicyFactsForPetQuestion() {
        MessageKnowledgeItemRepository itemRepository = Mockito.mock(MessageKnowledgeItemRepository.class);
        MessageKnowledgeSearchService service = newSearchService();
        service.setKnowledgeItemRepository(itemRepository);
        when(itemRepository.findActiveStoreScopedByStoreId(
                eq(26L),
                eq(MessageKnowledgeItem.STATUS_ACTIVE),
                any(Pageable.class)
        )).thenReturn(List.of(
                newStoreKnowledgeItem(
                        1L,
                        "Can I request late checkout?",
                        "Late checkout is available until 12:30 for 1500 JPY."
                ),
                newStoreKnowledgeItem(
                        2L,
                        "What time is breakfast?",
                        "Breakfast is served 07:00-10:00 at Sakura."
                )
        ));

        MessageKnowledgeSearchResult result = service.searchSimilar(
                26L,
                77L,
                null,
                "Can I bring my pet?",
                3
        );

        assertEquals(MessageKnowledgeSearchService.STATUS_NO_MATCH, result.getRetrievalStatus());
        assertTrue(result.getMatches().isEmpty());
    }

    @Test
    void topicService_shouldDetectPetPolicyShuttleAndLocationPolicyTopics() {
        MessageKnowledgeTopicService topicService = new MessageKnowledgeTopicService();

        assertEquals(
                Set.of(MessageKnowledgeTopicService.TOPIC_PET_POLICY),
                topicService.detectTopics("Do you allow pets?")
        );
        assertEquals(
                Set.of(MessageKnowledgeTopicService.TOPIC_PET_POLICY),
                topicService.detectTopics("¿Aceptan mascotas?")
        );
        assertEquals(
                Set.of(MessageKnowledgeTopicService.TOPIC_SHUTTLE),
                topicService.detectTopics("Navette aeroport?")
        );
        assertEquals(
                Set.of(MessageKnowledgeTopicService.TOPIC_SHUTTLE),
                topicService.detectTopics("Do you have an airport shuttle near the station?")
        );
        assertEquals(
                Set.of(MessageKnowledgeTopicService.TOPIC_LOCATION),
                topicService.detectTopics("What is your address near the nearest station?")
        );
    }

    private static MessageKnowledgeSearchService newSearchService() {
        return new MessageKnowledgeSearchService(
                new SuMessagingAiTextService(),
                new MessageKnowledgeTopicService()
        );
    }

    private static void assertPolicyMatchContainsFacts(
            MessageKnowledgeSearchService service,
            String query,
            String... expectedFacts
    ) {
        MessageKnowledgeSearchResult result = service.searchSimilar(26L, 77L, null, query, 3);

        assertEquals(MessageKnowledgeSearchService.STATUS_MATCHED, result.getRetrievalStatus());
        assertFalse(result.getMatches().isEmpty());
        MessageKnowledgeMatch topMatch = result.getMatches().get(0);
        for (String expectedFact : expectedFacts) {
            assertTrue(
                    topMatch.getReusableFacts().stream().anyMatch(fact -> fact.contains(expectedFact)),
                    "Expected reusable facts to contain: " + expectedFact
            );
        }
    }

    private static MessageKnowledgeItem newStoreKnowledgeItem(Long id, String question, String answer) {
        MessageKnowledgeItem item = newKnowledgeItem(id, question, answer);
        item.setScopeType(SuMessagingThreadContext.SCOPE_STORE);
        item.setScopeId(null);
        item.setScopeKey(SuMessagingThreadContext.SCOPE_STORE);
        item.setRoomTypeId(null);
        item.setRoomTypeName(null);
        return item;
    }

    private static MessageKnowledgeItem newKnowledgeItem(Long id, String question, String answer) {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(id);
        item.setStoreId(26L);
        item.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        item.setScopeId(3L);
        item.setScopeKey("ROOM_TYPE:3");
        item.setThreadId(11L);
        item.setRoomTypeId(3L);
        item.setRoomTypeName("Deluxe");
        item.setTopic("wifi");
        item.setTopicHash("topic-hash-" + id);
        item.setQuestion(question);
        item.setAnswer(answer);
        item.setNormalizedQuestion(question.toLowerCase());
        item.setNormalizedAnswer(answer.toLowerCase());
        item.setNormalizedAnswerHash("answer-hash-" + id);
        item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        item.setLastSeenAt(LocalDateTime.now());
        return item;
    }

    private static MessageKnowledgeSemanticRetrievalService newSemanticService(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEmbeddingProvider provider
    ) {
        MessageKnowledgeSemanticRetrievalService service = new MessageKnowledgeSemanticRetrievalService(
                itemRepository,
                provider,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "candidateCap", 12);
        ReflectionTestUtils.setField(service, "topK", 8);
        ReflectionTestUtils.setField(service, "minScore", 0.8);
        return service;
    }
}
