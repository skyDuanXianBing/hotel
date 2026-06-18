package server.demo.service;

import server.demo.entity.MessageKnowledgeTopic;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

record MessageKnowledgeThreadConversation(
        Long storeId,
        Long threadId,
        Long coveredUntilMessageId,
        SuMessageThread thread,
        SuMessagingThreadContext context,
        List<MessageKnowledgeThreadConversationMessage> messages
) {
    MessageKnowledgeThreadConversation {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}

record MessageKnowledgeThreadConversationMessage(
        Long id,
        LocalDateTime sentAt,
        SuMessagingSenderType senderType,
        String deliveryStatus,
        String senderName,
        String content,
        boolean eligibleAsEvidence
) {
    String roleLabel() {
        if (senderType == SuMessagingSenderType.STAFF) {
            return "员工";
        }
        return "用户";
    }
}

record MessageKnowledgeThreadPrompt(
        String prompt,
        int messageCount
) {
}

record MessageKnowledgeThreadParsedOutput(
        String schemaVersion,
        List<MessageKnowledgeThreadModelItem> items
) {
    MessageKnowledgeThreadParsedOutput {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

record MessageKnowledgeThreadModelItem(
        String clientItemId,
        String topicCode,
        String proposedTopicCode,
        String scopeType,
        String canonicalQuestion,
        String canonicalAnswer,
        String language,
        BigDecimal confidence,
        List<Long> sourceMessageIds,
        String evidenceSummary,
        String reusabilityReason
) {
    MessageKnowledgeThreadModelItem {
        sourceMessageIds = sourceMessageIds == null ? List.of() : List.copyOf(sourceMessageIds);
    }
}

record MessageKnowledgeThreadValidationResult(
        List<MessageKnowledgeThreadValidatedItem> validItems,
        List<MessageKnowledgeThreadRejectedItem> rejectedItems
) {
    MessageKnowledgeThreadValidationResult {
        validItems = validItems == null ? List.of() : List.copyOf(validItems);
        rejectedItems = rejectedItems == null ? List.of() : List.copyOf(rejectedItems);
    }
}

record MessageKnowledgeThreadRejectedItem(
        String clientItemId,
        List<Long> sourceMessageIds,
        String reasonCode,
        String reason
) {
    MessageKnowledgeThreadRejectedItem {
        sourceMessageIds = sourceMessageIds == null ? List.of() : List.copyOf(sourceMessageIds);
    }
}

record MessageKnowledgeThreadValidatedItem(
        String clientItemId,
        String topicCode,
        String topicDisplayName,
        String scopeType,
        Long scopeId,
        String scopeKey,
        String canonicalQuestion,
        String canonicalAnswer,
        String normalizedQuestion,
        String normalizedAnswer,
        String normalizedEvidence,
        String language,
        BigDecimal confidence,
        String status,
        List<Long> sourceMessageIds,
        List<MessageKnowledgeThreadConversationMessage> sourceMessages,
        LocalDateTime sourceTimestamp,
        String evidenceSummary,
        boolean generalizedOutput
) {
    MessageKnowledgeThreadValidatedItem {
        sourceMessageIds = sourceMessageIds == null ? List.of() : List.copyOf(sourceMessageIds);
        sourceMessages = sourceMessages == null ? List.of() : List.copyOf(sourceMessages);
    }
}

record MessageKnowledgeThreadSanitizedText(
        String text,
        boolean rejected,
        boolean generalized,
        List<String> reasonCodes
) {
    MessageKnowledgeThreadSanitizedText {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}

record MessageKnowledgeThreadSanitizerRequest(
        String text,
        SuMessagingThreadContext context
) {
}

record MessageKnowledgeThreadWriterResult(
        int writtenItems,
        int skippedDuplicateEvidence
) {
}

record MessageKnowledgeThreadValidationContext(
        Long storeId,
        Long threadId,
        SuMessagingThreadContext threadContext,
        List<MessageKnowledgeTopic> activeTopics,
        Map<Long, MessageKnowledgeThreadConversationMessage> inputMessagesById
) {
    MessageKnowledgeThreadValidationContext {
        activeTopics = activeTopics == null ? List.of() : List.copyOf(activeTopics);
        inputMessagesById = inputMessagesById == null ? Map.of() : Map.copyOf(inputMessagesById);
    }
}
