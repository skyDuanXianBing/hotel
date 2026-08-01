package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageKnowledgeThreadPromptBuilderTest {

    @Test
    void buildPrompt_shouldKeepNewestMessagesAndDropOldestWhenInputExceedsLimit() {
        MessageKnowledgeThreadPromptBuilder builder = new MessageKnowledgeThreadPromptBuilder();
        ReflectionTestUtils.setField(builder, "maxInputChars", 2000);
        ReflectionTestUtils.setField(builder, "maxMessageChars", 400);

        MessageKnowledgeThreadConversation conversation = new MessageKnowledgeThreadConversation(
                26L,
                77L,
                105L,
                new SuMessageThread(),
                null,
                List.of(
                        message(101L, "oldest message " + "x".repeat(380)),
                        message(102L, "older message " + "x".repeat(380)),
                        message(103L, "middle message " + "x".repeat(380)),
                        message(104L, "newer message " + "x".repeat(380)),
                        message(105L, "newest message " + "x".repeat(380))
                )
        );

        MessageKnowledgeThreadPrompt prompt = builder.buildPrompt(conversation, List.of());

        assertTrue(prompt.prompt().contains("[105]"));
        assertTrue(prompt.prompt().contains("oldest message(s) omitted"));
        assertFalse(prompt.prompt().contains("[101]"));
        assertTrue(prompt.messageCount() >= 1);
        assertTrue(prompt.messageCount() < conversation.messages().size());
    }

    @Test
    void buildPrompt_shouldKeepAllMessagesWhenInputFitsLimit() {
        MessageKnowledgeThreadPromptBuilder builder = new MessageKnowledgeThreadPromptBuilder();

        MessageKnowledgeThreadConversation conversation = new MessageKnowledgeThreadConversation(
                26L,
                77L,
                102L,
                new SuMessageThread(),
                null,
                List.of(
                        message(101L, "first message"),
                        message(102L, "second message")
                )
        );

        MessageKnowledgeThreadPrompt prompt = builder.buildPrompt(conversation, List.of());

        assertTrue(prompt.prompt().contains("[101]"));
        assertTrue(prompt.prompt().contains("[102]"));
        assertFalse(prompt.prompt().contains("omitted"));
        assertTrue(prompt.messageCount() == 2);
    }

    private static MessageKnowledgeThreadConversationMessage message(Long id, String content) {
        return new MessageKnowledgeThreadConversationMessage(
                id,
                LocalDateTime.of(2026, 6, 18, 12, 0).plusMinutes(id),
                SuMessagingSenderType.GUEST,
                "SENT",
                null,
                content,
                true
        );
    }
}
