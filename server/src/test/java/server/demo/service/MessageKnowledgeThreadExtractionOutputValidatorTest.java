package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.entity.MessageKnowledgeTopic;
import server.demo.enums.SuMessagingSenderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageKnowledgeThreadExtractionOutputValidatorTest {

    @Test
    void validate_shouldAcceptReusableActiveItemWithEligibleStaffSource() {
        MessageKnowledgeThreadExtractionOutputValidator validator = newValidator();

        MessageKnowledgeThreadValidationResult result = validator.validate(
                new MessageKnowledgeThreadParsedOutput("message_knowledge_thread_v1", List.of(
                        modelItem(
                                "wifi",
                                "What is the WiFi policy?",
                                "WiFi is available in all rooms.",
                                BigDecimal.valueOf(0.91),
                                List.of(101L, 102L)
                        )
                )),
                validationContext()
        );

        assertEquals(1, result.validItems().size());
        assertEquals(0, result.rejectedItems().size());
        MessageKnowledgeThreadValidatedItem validItem = result.validItems().get(0);
        assertEquals(MessageKnowledgeItem.STATUS_ACTIVE, validItem.status());
        assertEquals("STORE", validItem.scopeKey());
        assertEquals(2, validItem.sourceMessageIds().size());
    }

    @Test
    void validate_shouldRejectMissingAndUnknownSourceIds() {
        MessageKnowledgeThreadExtractionOutputValidator validator = newValidator();

        MessageKnowledgeThreadValidationResult result = validator.validate(
                new MessageKnowledgeThreadParsedOutput("message_knowledge_thread_v1", List.of(
                        modelItem(
                                "wifi",
                                "What is the WiFi policy?",
                                "WiFi is available in all rooms.",
                                BigDecimal.valueOf(0.91),
                                List.of()
                        ),
                        modelItem(
                                "wifi",
                                "What is the WiFi policy?",
                                "WiFi is available in all rooms.",
                                BigDecimal.valueOf(0.91),
                                List.of(999L)
                        )
                )),
                validationContext()
        );

        assertEquals(0, result.validItems().size());
        assertEquals(2, result.rejectedItems().size());
        assertEquals("missing_source_ids", result.rejectedItems().get(0).reasonCode());
        assertTrue(result.rejectedItems().get(1).reason().contains("source_id_not_in_input"));
    }

    @Test
    void validate_shouldRejectFailedStaffSourceAndPiiOutput() {
        MessageKnowledgeThreadExtractionOutputValidator validator = newValidator();

        MessageKnowledgeThreadValidationResult result = validator.validate(
                new MessageKnowledgeThreadParsedOutput("message_knowledge_thread_v1", List.of(
                        modelItem(
                                "wifi",
                                "What is the WiFi policy?",
                                "WiFi is available in all rooms.",
                                BigDecimal.valueOf(0.91),
                                List.of(101L, 103L)
                        ),
                        modelItem(
                                "wifi",
                                "What is the WiFi policy?",
                                "Call +81 90-1234-5678 for the password.",
                                BigDecimal.valueOf(0.91),
                                List.of(101L, 102L)
                        )
                )),
                validationContext()
        );

        assertEquals(0, result.validItems().size());
        assertEquals(2, result.rejectedItems().size());
        assertTrue(result.rejectedItems().get(0).reason().contains("source_message_not_eligible"));
        assertTrue(result.rejectedItems().get(1).reason().contains("answer_phone"));
    }

    private static MessageKnowledgeThreadExtractionOutputValidator newValidator() {
        return new MessageKnowledgeThreadExtractionOutputValidator(
                new MessageKnowledgeThreadOutputSanitizer(),
                new SuMessagingAiTextService()
        );
    }

    private static MessageKnowledgeThreadValidationContext validationContext() {
        Map<Long, MessageKnowledgeThreadConversationMessage> messages = new LinkedHashMap<>();
        messages.put(101L, message(101L, SuMessagingSenderType.GUEST, "SENT", true));
        messages.put(102L, message(102L, SuMessagingSenderType.STAFF, "SENT", true));
        messages.put(103L, message(103L, SuMessagingSenderType.STAFF, "FAILED", false));

        SuMessagingThreadContext context = new SuMessagingThreadContext();
        context.setChannelId(19);
        return new MessageKnowledgeThreadValidationContext(
                26L,
                77L,
                context,
                List.of(activeTopic("wifi")),
                messages
        );
    }

    private static MessageKnowledgeThreadConversationMessage message(
            Long id,
            SuMessagingSenderType senderType,
            String status,
            boolean eligible
    ) {
        return new MessageKnowledgeThreadConversationMessage(
                id,
                LocalDateTime.of(2026, 6, 18, 12, 0).plusMinutes(id),
                senderType,
                status,
                null,
                "message " + id,
                eligible
        );
    }

    private static MessageKnowledgeThreadModelItem modelItem(
            String topicCode,
            String question,
            String answer,
            BigDecimal confidence,
            List<Long> sourceMessageIds
    ) {
        return new MessageKnowledgeThreadModelItem(
                "item-" + topicCode,
                topicCode,
                null,
                "STORE",
                question,
                answer,
                "en",
                confidence,
                sourceMessageIds,
                "supported by staff messages",
                "stable hotel rule"
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
}
