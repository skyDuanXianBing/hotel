package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.MessageKnowledgeTopic;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageKnowledgeTopicClassifierServiceTest {

    @Test
    void classify_shouldReturnFallbackAndAvoidModelCallWhenDisabled() {
        MessageKnowledgeTopicDictionaryService dictionaryService =
                Mockito.mock(MessageKnowledgeTopicDictionaryService.class);
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeTopicClassifierService service = newService(dictionaryService, chatLanguageModel);
        when(dictionaryService.findActiveTopics(26L, 50)).thenReturn(List.of(activeTopic("wifi")));

        MessageKnowledgeTopicClassifierService.TopicClassification classification =
                service.classify(26L, "what is the wifi", "wifi password is sakura");

        assertEquals(MessageKnowledgeTopicClassifierService.ACTION_FALLBACK, classification.action());
        verifyNoInteractions(chatLanguageModel);
    }

    @Test
    void classify_shouldAcceptUseExistingWhenTopicExistsAndConfidenceIsHigh() {
        MessageKnowledgeTopicDictionaryService dictionaryService =
                Mockito.mock(MessageKnowledgeTopicDictionaryService.class);
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeTopicClassifierService service = newService(dictionaryService, chatLanguageModel);
        enableClassifier(service);
        when(dictionaryService.findActiveTopics(26L, 50)).thenReturn(List.of(activeTopic("amenity")));
        when(chatLanguageModel.generate(anyString())).thenReturn("""
                {"action":"USE_EXISTING","topicCode":"amenity","confidence":0.93,"reason":"amenity matched"}
                """);

        MessageKnowledgeTopicClassifierService.TopicClassification classification =
                service.classify(26L, "do you have towels", "towels are in the room");

        assertTrue(classification.useExisting());
        assertEquals("amenity", classification.topicCode());
        assertEquals(BigDecimal.valueOf(0.9300).setScale(4), classification.confidence());
    }

    @Test
    void classify_shouldDowngradeHighConfidenceUseExistingWhenTopicIsNotActiveInDictionary() {
        MessageKnowledgeTopicDictionaryService dictionaryService =
                Mockito.mock(MessageKnowledgeTopicDictionaryService.class);
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeTopicClassifierService service = newService(dictionaryService, chatLanguageModel);
        enableClassifier(service);
        when(dictionaryService.findActiveTopics(26L, 50)).thenReturn(List.of(activeTopic("wifi")));
        when(dictionaryService.findActiveTopic(26L, "pool_hours")).thenReturn(Optional.empty());
        when(chatLanguageModel.generate(anyString())).thenReturn("""
                {"action":"USE_EXISTING","topicCode":"pool_hours","confidence":0.94,"reason":"pool hours matched"}
                """);

        MessageKnowledgeTopicClassifierService.TopicClassification classification =
                service.classify(26L, "what are pool hours", "the pool opens at 09:00");

        assertFalse(classification.useExisting());
        assertTrue(classification.needsReview());
        assertEquals(MessageKnowledgeTopicClassifierService.TOPIC_GENERAL_CANDIDATE, classification.topicCode());
        verify(dictionaryService, never()).createNeedsReviewCandidate(
                Mockito.anyLong(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any()
        );
    }

    @Test
    void classify_shouldTurnCreateNewIntoNeedsReviewWhenAutoCreateIsDisabled() {
        MessageKnowledgeTopicDictionaryService dictionaryService =
                Mockito.mock(MessageKnowledgeTopicDictionaryService.class);
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeTopicClassifierService service = newService(dictionaryService, chatLanguageModel);
        enableClassifier(service);
        when(dictionaryService.findActiveTopics(26L, 50)).thenReturn(List.of(activeTopic("wifi")));
        when(chatLanguageModel.generate(anyString())).thenReturn("""
                {"action":"CREATE_NEW","topicCode":"pool_hours","displayName":"Pool Hours","confidence":0.95}
                """);

        MessageKnowledgeTopicClassifierService.TopicClassification classification =
                service.classify(26L, "what are pool hours", "the pool opens at 09:00");

        assertTrue(classification.needsReview());
        assertEquals(MessageKnowledgeTopicClassifierService.TOPIC_GENERAL_CANDIDATE, classification.topicCode());
        verify(dictionaryService, never()).createNeedsReviewCandidate(
                Mockito.anyLong(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any()
        );
    }

    @Test
    void classify_shouldKeepAutoCreateDisabledAndUseSingleModelCallBudget() {
        MessageKnowledgeTopicDictionaryService dictionaryService =
                Mockito.mock(MessageKnowledgeTopicDictionaryService.class);
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeTopicClassifierService service = newService(dictionaryService, chatLanguageModel);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "minConfidence", 0.8);
        ReflectionTestUtils.setField(service, "maxTopicsPerStore", 50);
        ReflectionTestUtils.setField(service, "maxModelCallsPerRun", 1);
        ReflectionTestUtils.setField(service, "autoCreateEnabled", false);
        when(dictionaryService.findActiveTopics(26L, 50)).thenReturn(List.of(activeTopic("wifi")));
        when(chatLanguageModel.generate(anyString())).thenReturn("""
                {"action":"CREATE_NEW","topicCode":"pool_hours","displayName":"Pool Hours","confidence":0.95}
                """);

        MessageKnowledgeTopicClassifierService.TopicClassification firstClassification =
                service.classify(26L, "what are pool hours", "the pool opens at 09:00");
        MessageKnowledgeTopicClassifierService.TopicClassification secondClassification =
                service.classify(26L, "do you have parking", "parking is nearby");

        assertTrue(firstClassification.needsReview());
        assertEquals(MessageKnowledgeTopicClassifierService.ACTION_FALLBACK, secondClassification.action());
        assertEquals("model_call_budget_exhausted", secondClassification.reason());
        Mockito.verify(chatLanguageModel, Mockito.times(1)).generate(anyString());
        verify(dictionaryService, never()).createNeedsReviewCandidate(
                Mockito.anyLong(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any()
        );
    }

    @Test
    void classify_shouldTurnLowConfidenceUseExistingIntoNeedsReview() {
        MessageKnowledgeTopicDictionaryService dictionaryService =
                Mockito.mock(MessageKnowledgeTopicDictionaryService.class);
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeTopicClassifierService service = newService(dictionaryService, chatLanguageModel);
        enableClassifier(service);
        when(dictionaryService.findActiveTopics(26L, 50)).thenReturn(List.of(activeTopic("wifi")));
        when(chatLanguageModel.generate(anyString())).thenReturn("""
                {"action":"USE_EXISTING","topicCode":"wifi","confidence":0.40}
                """);

        MessageKnowledgeTopicClassifierService.TopicClassification classification =
                service.classify(26L, "maybe wifi", "maybe ask front desk");

        assertTrue(classification.needsReview());
        assertEquals(MessageKnowledgeTopicClassifierService.TOPIC_GENERAL_CANDIDATE, classification.topicCode());
    }

    private static MessageKnowledgeTopicClassifierService newService(
            MessageKnowledgeTopicDictionaryService dictionaryService,
            ChatLanguageModel chatLanguageModel
    ) {
        return new MessageKnowledgeTopicClassifierService(
                dictionaryService,
                chatLanguageModel,
                new ObjectMapper(),
                new SuMessagingAiRedactor()
        );
    }

    private static void enableClassifier(MessageKnowledgeTopicClassifierService service) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "minConfidence", 0.8);
        ReflectionTestUtils.setField(service, "maxTopicsPerStore", 50);
        ReflectionTestUtils.setField(service, "maxModelCallsPerRun", 10);
        ReflectionTestUtils.setField(service, "autoCreateEnabled", false);
    }

    private static MessageKnowledgeTopic activeTopic(String topicCode) {
        MessageKnowledgeTopic topic = new MessageKnowledgeTopic();
        topic.setStoreId(26L);
        topic.setTopicCode(topicCode);
        topic.setDisplayName(topicCode);
        topic.setStatus(MessageKnowledgeTopic.STATUS_ACTIVE);
        return topic;
    }
}
