package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageKnowledgeDuplicateJudgeService {
    private static final double DEFAULT_MIN_CONFIDENCE = 0.82;
    private static final int DEFAULT_MAX_TEXT_LENGTH = 320;

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final SuMessagingAiRedactor redactor;

    @Value("${messaging.knowledge.ingest-dedup.enabled:false}")
    private boolean enabled;

    @Value("${messaging.knowledge.ingest-dedup.min-confidence:0.82}")
    private double minConfidence;

    @Value("${messaging.knowledge.ingest-dedup.max-text-length:320}")
    private int maxTextLength;

    public MessageKnowledgeDuplicateJudgeService(
            @Qualifier("messageKnowledgeDedupChatLanguageModel") ChatLanguageModel chatLanguageModel,
            ObjectMapper objectMapper,
            SuMessagingAiRedactor redactor
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
        this.redactor = redactor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public DuplicateJudgeResult judge(
            MessageKnowledgeThreadValidatedItem incomingItem,
            List<MessageKnowledgeItem> candidates
    ) {
        if (!enabled || incomingItem == null || candidates == null || candidates.isEmpty()) {
            return DuplicateJudgeResult.notDuplicate();
        }

        Map<Long, MessageKnowledgeItem> candidateById = mapCandidates(candidates);
        if (candidateById.isEmpty()) {
            return DuplicateJudgeResult.notDuplicate();
        }

        try {
            String prompt = buildPrompt(incomingItem, candidateById);
            String output = chatLanguageModel.generate(prompt);
            return parseAndValidate(output, candidateById);
        } catch (RuntimeException e) {
            return DuplicateJudgeResult.notDuplicate();
        }
    }

    private Map<Long, MessageKnowledgeItem> mapCandidates(List<MessageKnowledgeItem> candidates) {
        Map<Long, MessageKnowledgeItem> candidateById = new LinkedHashMap<>();
        for (MessageKnowledgeItem candidate : candidates) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            candidateById.put(candidate.getId(), candidate);
        }
        return candidateById;
    }

    private String buildPrompt(
            MessageKnowledgeThreadValidatedItem incomingItem,
            Map<Long, MessageKnowledgeItem> candidateById
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Judge whether a new hotel knowledge fact duplicates an existing fact.\n");
        prompt.append("Return JSON only with this exact shape: ");
        prompt.append("{\"duplicate\":true,\"confidence\":0.0,\"matchedItemId\":123,\"reason\":\"short\"}.\n");
        prompt.append("Use duplicate=false when facts differ by time, price, condition, eligibility, room, room type, channel, or uncertainty.\n");
        prompt.append("Only return a matchedItemId from the candidate list. If unsure, return duplicate=false.\n\n");
        prompt.append("New fact:\n");
        prompt.append("- scopeType: ").append(nullToBlank(incomingItem.scopeType())).append("\n");
        prompt.append("- scopeKey: ").append(nullToBlank(incomingItem.scopeKey())).append("\n");
        prompt.append("- topic: ").append(redact(incomingItem.topicCode())).append("\n");
        prompt.append("- question: ").append(redact(truncate(incomingItem.canonicalQuestion()))).append("\n");
        prompt.append("- answer: ").append(redact(truncate(incomingItem.canonicalAnswer()))).append("\n");
        prompt.append("- normalizedAnswer: ").append(redact(truncate(incomingItem.normalizedAnswer()))).append("\n\n");
        prompt.append("Candidates:\n");
        for (MessageKnowledgeItem candidate : candidateById.values()) {
            prompt.append("- itemId: ").append(candidate.getId()).append("\n");
            prompt.append("  status: ").append(nullToBlank(candidate.getStatus())).append("\n");
            prompt.append("  scopeType: ").append(nullToBlank(candidate.getScopeType())).append("\n");
            prompt.append("  scopeKey: ").append(nullToBlank(candidate.getScopeKey())).append("\n");
            prompt.append("  topic: ").append(redact(candidate.getTopic())).append("\n");
            prompt.append("  question: ").append(redact(truncate(candidate.getQuestion()))).append("\n");
            prompt.append("  answer: ").append(redact(truncate(candidate.getAnswer()))).append("\n");
            prompt.append("  normalizedAnswer: ").append(redact(truncate(candidate.getNormalizedAnswer()))).append("\n");
        }
        return prompt.toString();
    }

    private DuplicateJudgeResult parseAndValidate(
            String output,
            Map<Long, MessageKnowledgeItem> candidateById
    ) {
        String json = extractJson(output);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                return DuplicateJudgeResult.notDuplicate();
            }

            boolean duplicate = readBoolean(root, "duplicate", false);
            double confidence = readDouble(root, "confidence", 0.0);
            Long matchedItemId = readMatchedItemId(root);
            String reason = readText(root, "reason");
            double threshold = normalizeConfidence(minConfidence);
            if (!duplicate || confidence < threshold || matchedItemId == null) {
                return DuplicateJudgeResult.notDuplicate();
            }
            if (!candidateById.containsKey(matchedItemId)) {
                return DuplicateJudgeResult.notDuplicate();
            }
            return new DuplicateJudgeResult(true, matchedItemId, confidence, reason);
        } catch (JsonProcessingException e) {
            return DuplicateJudgeResult.notDuplicate();
        }
    }

    private String extractJson(String output) {
        if (!hasText(output)) {
            throw new IllegalArgumentException("Duplicate judge output is blank");
        }
        String trimmed = output.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Duplicate judge output does not contain JSON");
        }
        return trimmed.substring(start, end + 1);
    }

    private Long readMatchedItemId(JsonNode root) {
        Long id = readLong(root.get("matchedItemId"));
        if (id != null) {
            return id;
        }
        id = readLong(root.get("matched_item_id"));
        if (id != null) {
            return id;
        }
        return readLong(root.get("itemId"));
    }

    private Long readLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private boolean readBoolean(
            JsonNode root,
            String fieldName,
            boolean fallback
    ) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return fallback;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isTextual()) {
            return Boolean.parseBoolean(node.asText());
        }
        return fallback;
    }

    private double readDouble(
            JsonNode root,
            String fieldName,
            double fallback
    ) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return fallback;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            try {
                return Double.parseDouble(node.asText().trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private String readText(
            JsonNode root,
            String fieldName
    ) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private double normalizeConfidence(double value) {
        if (value < 0.0 || value > 1.0) {
            return DEFAULT_MIN_CONFIDENCE;
        }
        return value;
    }

    private String truncate(String value) {
        String normalized = nullToBlank(value).trim();
        int limit = normalizeTextLimit(maxTextLength);
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit).trim();
    }

    private int normalizeTextLimit(int value) {
        if (value < 80 || value > 1200) {
            return DEFAULT_MAX_TEXT_LENGTH;
        }
        return value;
    }

    private String redact(String value) {
        return redactor.redact(nullToBlank(value));
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record DuplicateJudgeResult(
            boolean duplicate,
            Long matchedItemId,
            double confidence,
            String reason
    ) {
        public static DuplicateJudgeResult notDuplicate() {
            return new DuplicateJudgeResult(false, null, 0.0, null);
        }
    }
}
