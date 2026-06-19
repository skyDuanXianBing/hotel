package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.MessageKnowledgeItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MessageKnowledgeDuplicateJudgeServiceTest {

    @Test
    void judge_shouldAcceptHighConfidenceCandidateIdFromPrompt() {
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeDuplicateJudgeService service = newService(chatLanguageModel);
        enableJudge(service);
        MessageKnowledgeItem candidate = candidateItem(
                101L,
                "checkout",
                "Late checkout is available until 12:00 when occupancy allows."
        );
        when(chatLanguageModel.generate(anyString())).thenReturn("""
                {"duplicate":true,"confidence":0.91,"matchedItemId":101,"reason":"same checkout policy"}
                """);

        MessageKnowledgeDuplicateJudgeService.DuplicateJudgeResult result =
                service.judge(incomingItem(), List.of(candidate));

        assertTrue(result.duplicate());
        assertEquals(101L, result.matchedItemId());
        assertEquals(0.91, result.confidence());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(chatLanguageModel).generate(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("New fact"));
        assertTrue(promptCaptor.getValue().contains("itemId: 101"));
        assertTrue(promptCaptor.getValue().contains("scopeKey: ROOM_TYPE:3"));
    }

    @Test
    void judge_shouldRejectLowConfidenceAndUnknownCandidateId() {
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeDuplicateJudgeService service = newService(chatLanguageModel);
        enableJudge(service);
        when(chatLanguageModel.generate(anyString())).thenReturn("""
                {"duplicate":true,"confidence":0.70,"matchedItemId":999,"reason":"weak"}
                """);

        MessageKnowledgeDuplicateJudgeService.DuplicateJudgeResult result =
                service.judge(incomingItem(), List.of(candidateItem(
                        101L,
                        "checkout",
                        "Late checkout is available until 12:00 when occupancy allows."
                )));

        assertFalse(result.duplicate());
        assertNull(result.matchedItemId());
    }

    @Test
    void judge_shouldReturnNotDuplicateWhenJsonIsInvalid() {
        ChatLanguageModel chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        MessageKnowledgeDuplicateJudgeService service = newService(chatLanguageModel);
        enableJudge(service);
        when(chatLanguageModel.generate(anyString())).thenReturn("not-json");

        MessageKnowledgeDuplicateJudgeService.DuplicateJudgeResult result =
                service.judge(incomingItem(), List.of(candidateItem(
                        101L,
                        "checkout",
                        "Late checkout is available until 12:00 when occupancy allows."
                )));

        assertFalse(result.duplicate());
    }

    private static MessageKnowledgeDuplicateJudgeService newService(ChatLanguageModel chatLanguageModel) {
        return new MessageKnowledgeDuplicateJudgeService(
                chatLanguageModel,
                new ObjectMapper(),
                new SuMessagingAiRedactor()
        );
    }

    private static void enableJudge(MessageKnowledgeDuplicateJudgeService service) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "minConfidence", 0.82);
        ReflectionTestUtils.setField(service, "maxTextLength", 320);
    }

    private static MessageKnowledgeThreadValidatedItem incomingItem() {
        return new MessageKnowledgeThreadValidatedItem(
                "client-1",
                "checkout",
                "Checkout",
                SuMessagingThreadContext.SCOPE_ROOM_TYPE,
                3L,
                "ROOM_TYPE:3",
                "Can I check out late?",
                "Late checkout is available until noon if occupancy allows.",
                "can i check out late",
                "late checkout is available until noon if occupancy allows",
                "guest asked late checkout and staff confirmed noon when available",
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

    private static MessageKnowledgeItem candidateItem(
            Long id,
            String topic,
            String answer
    ) {
        MessageKnowledgeItem item = new MessageKnowledgeItem();
        item.setId(id);
        item.setStoreId(26L);
        item.setScopeType(SuMessagingThreadContext.SCOPE_ROOM_TYPE);
        item.setScopeId(3L);
        item.setScopeKey("ROOM_TYPE:3");
        item.setTopic(topic);
        item.setTopicHash("topic-hash");
        item.setQuestion("Is late checkout available?");
        item.setAnswer(answer);
        item.setNormalizedQuestion("is late checkout available");
        item.setNormalizedAnswer(answer.toLowerCase());
        item.setNormalizedAnswerHash("answer-hash");
        item.setFactHash("fact-hash");
        item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        return item;
    }
}
