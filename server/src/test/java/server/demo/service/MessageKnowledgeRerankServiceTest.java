package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.MessageKnowledgeItem;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MessageKnowledgeRerankServiceTest {

    @Test
    void rerank_shouldAcceptRelevantCandidateAndRejectIrrelevantCandidate() {
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeRerankService service = newService(chatLanguageModel);
        enableRerank(service);
        List<MessageKnowledgeSemanticMatch> candidates = List.of(
                newCandidate(101L, "wifi", "The WiFi password is sakura2026.", 0.91),
                newCandidate(102L, "breakfast", "Breakfast is served from 07:00 to 10:00.", 0.88)
        );

        when(chatLanguageModel.generate(any(String.class))).thenReturn("""
                {"items":[
                  {"id":101,"relevant":true,"confidence":0.93,"reason":"answers internet access"},
                  {"id":102,"relevant":false,"confidence":0.12,"reason":"breakfast is unrelated"}
                ]}
                """);

        MessageKnowledgeRerankService.RerankOutcome outcome = service.rerank(
                "インターネットは使えますか？",
                roomTypeContext(),
                candidates
        );

        assertFalse(outcome.fallbackToLegacy());
        assertEquals(1, outcome.matches().size());
        assertEquals(101L, outcome.matches().get(0).item().getId());
        assertTrue(outcome.matches().get(0).reasons().contains("AI_RERANK_SELECTED"));
        assertTrue(outcome.matches().get(0).reasons().stream()
                .anyMatch(reason -> reason.contains("answers internet access")));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(chatLanguageModel).generate(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("Latest guest message"));
        assertTrue(promptCaptor.getValue().contains("Deluxe"));
    }

    @Test
    void rerank_shouldFallbackToCosineResultsWhenJsonIsInvalidByDefault() {
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeRerankService service = newService(chatLanguageModel);
        enableRerank(service);
        List<MessageKnowledgeSemanticMatch> candidates = List.of(
                newCandidate(101L, "wifi", "The WiFi password is sakura2026.", 0.91),
                newCandidate(102L, "breakfast", "Breakfast is served from 07:00 to 10:00.", 0.88)
        );
        when(chatLanguageModel.generate(any(String.class))).thenReturn("not-json");

        MessageKnowledgeRerankService.RerankOutcome outcome = service.rerank(
                "Could you share internet details?",
                roomTypeContext(),
                candidates
        );

        assertFalse(outcome.fallbackToLegacy());
        assertEquals(2, outcome.matches().size());
        assertEquals(101L, outcome.matches().get(0).item().getId());
    }

    @Test
    void rerank_shouldRequestLegacyFallbackWhenConfiguredAndJsonIsInvalid() {
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeRerankService service = newService(chatLanguageModel);
        enableRerank(service);
        ReflectionTestUtils.setField(service, "invalidOutputFallback", "legacy");
        when(chatLanguageModel.generate(any(String.class))).thenReturn("{bad-json");

        MessageKnowledgeRerankService.RerankOutcome outcome = service.rerank(
                "Could you share internet details?",
                roomTypeContext(),
                List.of(newCandidate(101L, "wifi", "The WiFi password is sakura2026.", 0.91))
        );

        assertTrue(outcome.fallbackToLegacy());
        assertTrue(outcome.matches().isEmpty());
    }

    private static MessageKnowledgeRerankService newService(ChatLanguageModel chatLanguageModel) {
        return new MessageKnowledgeRerankService(
                chatLanguageModel,
                new ObjectMapper(),
                new SuMessagingAiRedactor()
        );
    }

    private static void enableRerank(MessageKnowledgeRerankService service) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "candidateCap", 8);
        ReflectionTestUtils.setField(service, "minConfidence", 0.5);
        ReflectionTestUtils.setField(service, "invalidOutputFallback", "cosine");
    }

    private static MessageKnowledgeSemanticMatch newCandidate(
            Long id,
            String topic,
            String answer,
            double score
    ) {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(id);
        item.setStoreId(26L);
        item.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        item.setScopeId(3L);
        item.setScopeKey("ROOM_TYPE:3");
        item.setRoomTypeId(3L);
        item.setRoomTypeName("Deluxe");
        item.setTopic(topic);
        item.setTopicHash("topic-hash-" + id);
        item.setQuestion("Guest asked about " + topic);
        item.setAnswer(answer);
        item.setNormalizedQuestion(item.getQuestion().toLowerCase());
        item.setNormalizedAnswer(answer.toLowerCase());
        item.setNormalizedAnswerHash("answer-hash-" + id);
        item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_READY);
        item.setEmbeddingVector("[1.0,0.0]");
        item.setSemanticText("Topic: " + topic + "\nCanonical fact: " + answer);
        item.setLastSeenAt(LocalDateTime.now());
        return new MessageKnowledgeSemanticMatch(
                item,
                score,
                SuMessagingThreadContext.SCOPE_ROOM_TYPE,
                List.of("SEMANTIC_COSINE", "SAME_ROOM_TYPE")
        );
    }

    private static SuMessagingThreadContext roomTypeContext() {
        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setRoomTypeId(3L);
        context.setRoomTypeName("Deluxe");
        return context;
    }
}
