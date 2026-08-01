package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeTopic;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageKnowledgeThreadPromptBuilder {
    private static final int DEFAULT_MAX_INPUT_CHARS = 200000;
    private static final int DEFAULT_MAX_MESSAGE_CHARS = 1600;
    private static final int DEFAULT_MAX_OUTPUT_ITEMS = 8;

    @Value("${messaging.knowledge.thread-extractor.max-input-chars:200000}")
    private int maxInputChars;

    @Value("${messaging.knowledge.thread-extractor.max-message-chars:1600}")
    private int maxMessageChars;

    @Value("${messaging.knowledge.thread-extractor.max-output-items:8}")
    private int maxOutputItems;

    public MessageKnowledgeThreadPrompt buildPrompt(
            MessageKnowledgeThreadConversation conversation,
            List<MessageKnowledgeTopic> activeTopics
    ) {
        if (conversation == null) {
            throw new IllegalArgumentException("conversation is required");
        }
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a hotel reusable knowledge extractor.\n");
        prompt.append("Return strict JSON only. Do not answer the guest.\n");
        prompt.append("Extract stable reusable hotel facts only. Do not output private, order-specific, ");
        prompt.append("guest-specific, one-time, payment, identity, email, phone, confirmation, ");
        prompt.append("booking, door-code, or tokenized URL values.\n");
        prompt.append("Each item must include sourceMessageIds from the conversation.\n");
        prompt.append("A staff message with evidence=false cannot be used as evidence.\n");
        prompt.append("If no reusable knowledge exists, return an empty items array.\n");
        prompt.append("JSON shape:\n");
        prompt.append("{\"schemaVersion\":\"message_knowledge_thread_v1\",\"items\":[");
        prompt.append("{\"clientItemId\":\"i1\",\"topicCode\":\"wifi\",\"scopeType\":\"STORE|ROOM_TYPE|ROOM\",");
        prompt.append("\"canonicalQuestion\":\"short reusable question\",");
        prompt.append("\"canonicalAnswer\":\"stable reusable answer\",");
        prompt.append("\"language\":\"en|zh|ja|mixed\",");
        prompt.append("\"confidence\":0.0,");
        prompt.append("\"sourceMessageIds\":[123,124],");
        prompt.append("\"evidenceSummary\":\"short non-private summary\",");
        prompt.append("\"reusabilityReason\":\"why reusable\"}");
        prompt.append("]}.\n");
        prompt.append("Maximum items: ").append(resolveMaxOutputItems()).append(".\n\n");

        appendThreadContext(prompt, conversation.context());
        appendTopics(prompt, activeTopics);
        prompt.append("Conversation lines:\n");
        List<String> lines = new ArrayList<>();
        for (MessageKnowledgeThreadConversationMessage message : conversation.messages()) {
            lines.add(conversationLine(message));
        }
        // Keep the newest lines that fit the input budget; drop the oldest instead of failing
        int budget = resolveMaxInputChars() - prompt.length();
        int startIndex = lines.size();
        int used = 0;
        while (startIndex > 0) {
            String candidate = lines.get(startIndex - 1);
            if (startIndex < lines.size() && used + candidate.length() > budget) {
                break;
            }
            used += candidate.length();
            startIndex--;
        }
        int omitted = startIndex;
        if (omitted > 0) {
            prompt.append("- (")
                    .append(omitted)
                    .append(" oldest message(s) omitted to fit the input limit)\n");
        }
        for (int i = startIndex; i < lines.size(); i++) {
            prompt.append(lines.get(i));
        }
        return new MessageKnowledgeThreadPrompt(prompt.toString(), lines.size() - omitted);
    }

    private void appendThreadContext(
            StringBuilder prompt,
            SuMessagingThreadContext context
    ) {
        prompt.append("Thread context:\n");
        if (context == null) {
            prompt.append("- scope: STORE\n\n");
            return;
        }
        appendLine(prompt, "channel", context.getChannelName());
        appendLine(prompt, "roomNumber", context.getRoomNumber());
        appendLine(prompt, "roomTypeName", context.getRoomTypeName());
        appendLine(prompt, "matchStatus", context.getMatchStatus());
        prompt.append("- bestScope: ").append(context.getBestScopeType()).append("\n\n");
    }

    private void appendTopics(
            StringBuilder prompt,
            List<MessageKnowledgeTopic> activeTopics
    ) {
        prompt.append("Allowed active topics:\n");
        if (activeTopics == null || activeTopics.isEmpty()) {
            prompt.append("- none\n\n");
            return;
        }
        for (MessageKnowledgeTopic topic : activeTopics) {
            if (topic == null || topic.getTopicCode() == null) {
                continue;
            }
            prompt.append("- topicCode: ").append(cleanPromptValue(topic.getTopicCode())).append("\n");
            appendLine(prompt, "displayName", topic.getDisplayName());
            appendLine(prompt, "description", topic.getDescription());
            appendLine(prompt, "scopePreference", topic.getScopePreference());
        }
        prompt.append("\n");
    }

    private String conversationLine(
            MessageKnowledgeThreadConversationMessage message
    ) {
        StringBuilder line = new StringBuilder();
        line.append("[")
                .append(message.id())
                .append("] ")
                .append(message.roleLabel())
                .append(": ")
                .append(truncateMessage(message.content()));
        line.append(" [status=");
        if (message.deliveryStatus() == null || message.deliveryStatus().isBlank()) {
            line.append("UNKNOWN");
        } else {
            line.append(cleanPromptValue(message.deliveryStatus()));
        }
        line.append(", evidence=")
                .append(message.eligibleAsEvidence())
                .append("]\n");
        return line.toString();
    }

    private void appendLine(StringBuilder prompt, String label, String value) {
        String normalized = cleanPromptValue(value);
        if (normalized == null) {
            return;
        }
        prompt.append("- ").append(label).append(": ").append(normalized).append("\n");
    }

    private String truncateMessage(String value) {
        String normalized = cleanPromptValue(value);
        if (normalized == null) {
            return "";
        }
        int limit = resolveMaxMessageChars();
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit).trim();
    }

    private int resolveMaxInputChars() {
        if (maxInputChars < 1000) {
            return DEFAULT_MAX_INPUT_CHARS;
        }
        return maxInputChars;
    }

    private int resolveMaxMessageChars() {
        if (maxMessageChars < 100) {
            return DEFAULT_MAX_MESSAGE_CHARS;
        }
        return maxMessageChars;
    }

    private int resolveMaxOutputItems() {
        if (maxOutputItems < 1) {
            return DEFAULT_MAX_OUTPUT_ITEMS;
        }
        return maxOutputItems;
    }

    private static String cleanPromptValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized;
    }
}
