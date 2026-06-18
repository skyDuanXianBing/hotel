package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageKnowledgeThreadConversationLoader {
    private static final int DEFAULT_MAX_MESSAGES = 500;
    private static final String DELIVERY_STATUS_FAILED = "FAILED";
    private static final String DELIVERY_STATUS_SENDING = "SENDING";

    private final SuMessageRepository messageRepository;
    private final SuMessageThreadRepository threadRepository;
    private final SuMessagingThreadContextResolver contextResolver;

    @Value("${messaging.knowledge.thread-extractor.max-messages:500}")
    private int maxMessages;

    public MessageKnowledgeThreadConversationLoader(
            SuMessageRepository messageRepository,
            SuMessageThreadRepository threadRepository,
            SuMessagingThreadContextResolver contextResolver
    ) {
        this.messageRepository = messageRepository;
        this.threadRepository = threadRepository;
        this.contextResolver = contextResolver;
    }

    public MessageKnowledgeThreadConversation load(
            Long storeId,
            Long threadId,
            Long coveredUntilMessageId
    ) {
        requireIdentifier(storeId, "storeId");
        requireIdentifier(threadId, "threadId");
        requireIdentifier(coveredUntilMessageId, "coveredUntilMessageId");

        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId)
                .orElseThrow(() -> new IllegalArgumentException("Message thread not found for store"));
        int limit = resolveMaxMessages();
        List<SuMessage> messages = messageRepository.findByStoreIdAndThreadIdUpToMessageIdOrderBySentAtAsc(
                storeId,
                threadId,
                coveredUntilMessageId,
                PageRequest.of(0, limit + 1)
        );
        if (messages.size() > limit) {
            throw new IllegalArgumentException("Thread conversation exceeds extractor message limit");
        }

        List<MessageKnowledgeThreadConversationMessage> conversationMessages = new ArrayList<>();
        for (SuMessage message : messages) {
            MessageKnowledgeThreadConversationMessage conversationMessage = toConversationMessage(
                    storeId,
                    threadId,
                    message
            );
            if (conversationMessage != null) {
                conversationMessages.add(conversationMessage);
            }
        }

        SuMessagingThreadContext context = contextResolver.resolveForIndex(storeId, thread);
        return new MessageKnowledgeThreadConversation(
                storeId,
                threadId,
                coveredUntilMessageId,
                thread,
                context,
                conversationMessages
        );
    }

    private MessageKnowledgeThreadConversationMessage toConversationMessage(
            Long storeId,
            Long threadId,
            SuMessage message
    ) {
        if (message == null || message.getId() == null) {
            return null;
        }
        if (!storeId.equals(message.getStoreId())) {
            return null;
        }
        if (message.getThread() == null || !threadId.equals(message.getThread().getId())) {
            return null;
        }
        String content = normalizeMessageContent(message.getContent());
        if (content == null) {
            return null;
        }
        boolean eligibleAsEvidence = isEligibleAsEvidence(message);
        return new MessageKnowledgeThreadConversationMessage(
                message.getId(),
                message.getSentAt(),
                message.getSenderType(),
                message.getDeliveryStatus(),
                message.getSenderName(),
                content,
                eligibleAsEvidence
        );
    }

    private static boolean isEligibleAsEvidence(SuMessage message) {
        if (message == null) {
            return false;
        }
        if (message.getSenderType() != SuMessagingSenderType.STAFF) {
            return true;
        }
        String deliveryStatus = message.getDeliveryStatus();
        if (deliveryStatus == null || deliveryStatus.isBlank()) {
            return true;
        }
        String normalizedStatus = deliveryStatus.trim();
        if (DELIVERY_STATUS_FAILED.equalsIgnoreCase(normalizedStatus)) {
            return false;
        }
        return !DELIVERY_STATUS_SENDING.equalsIgnoreCase(normalizedStatus);
    }

    private int resolveMaxMessages() {
        if (maxMessages < 1) {
            return DEFAULT_MAX_MESSAGES;
        }
        return maxMessages;
    }

    private static String normalizeMessageContent(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized;
    }

    private static void requireIdentifier(Long value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
