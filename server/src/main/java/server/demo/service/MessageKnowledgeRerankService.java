package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageKnowledgeRerankService {
    private static final int DEFAULT_CANDIDATE_CAP = 8;
    private static final int MAX_FACT_LENGTH = 220;
    private static final double DEFAULT_MIN_CONFIDENCE = 0.5;
    private static final String FALLBACK_LEGACY = "legacy";

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final SuMessagingAiRedactor redactor;

    @Value("${messaging.knowledge.semantic-rerank.enabled:false}")
    private boolean enabled;

    @Value("${messaging.knowledge.semantic-rerank.candidate-cap:8}")
    private int candidateCap;

    @Value("${messaging.knowledge.semantic-rerank.min-confidence:0.5}")
    private double minConfidence;

    @Value("${messaging.knowledge.semantic-rerank.invalid-output-fallback:cosine}")
    private String invalidOutputFallback;

    public MessageKnowledgeRerankService(
            ChatLanguageModel chatLanguageModel,
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

    public RerankOutcome rerank(
            String latestGuestMessage,
            SuMessagingThreadContext context,
            List<MessageKnowledgeSemanticMatch> candidates
    ) {
        if (!enabled || candidates == null || candidates.isEmpty()) {
            return RerankOutcome.cosine(candidates);
        }

        List<MessageKnowledgeSemanticMatch> limitedCandidates = limitCandidates(candidates);
        if (limitedCandidates.isEmpty()) {
            return RerankOutcome.cosine(List.of());
        }

        try {
            String prompt = buildPrompt(latestGuestMessage, context, limitedCandidates);
            String output = chatLanguageModel.generate(prompt);
            List<RerankDecision> decisions = parseDecisions(output);
            List<MessageKnowledgeSemanticMatch> selected = applyDecisions(limitedCandidates, decisions);
            return RerankOutcome.reranked(selected);
        } catch (RuntimeException e) {
            return invalidOutputFallback(limitedCandidates);
        }
    }

    private List<MessageKnowledgeSemanticMatch> limitCandidates(List<MessageKnowledgeSemanticMatch> candidates) {
        int limit = MessageKnowledgeSemanticRetrievalService.normalizeLimit(
                candidateCap,
                DEFAULT_CANDIDATE_CAP
        );
        if (candidates.size() <= limit) {
            return candidates;
        }
        return new ArrayList<>(candidates.subList(0, limit));
    }

    private String buildPrompt(
            String latestGuestMessage,
            SuMessagingThreadContext context,
            List<MessageKnowledgeSemanticMatch> candidates
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Rerank hotel reusable facts for the latest guest message.\n");
        prompt.append("Return JSON only with this shape: ");
        prompt.append("{\"items\":[{\"id\":123,\"relevant\":true,\"confidence\":0.0,\"reason\":\"short\"}]}.\n");
        prompt.append("Mark relevant=false for facts that do not directly answer the latest guest message.\n\n");
        prompt.append("Latest guest message: ").append(redact(latestGuestMessage)).append("\n");
        appendContext(prompt, context);
        prompt.append("Candidates:\n");
        for (MessageKnowledgeSemanticMatch candidate : candidates) {
            if (candidate == null || candidate.item() == null || candidate.item().getId() == null) {
                continue;
            }
            prompt.append("- id: ").append(candidate.item().getId()).append("\n");
            prompt.append("  scope: ").append(candidate.scopeLevel()).append("\n");
            prompt.append("  topic: ").append(redact(candidate.item().getTopic())).append("\n");
            prompt.append("  guest: ").append(redact(truncate(candidate.item().getQuestion()))).append("\n");
            prompt.append("  fact: ").append(redact(truncate(candidate.item().getAnswer()))).append("\n");
        }
        return prompt.toString();
    }

    private void appendContext(
            StringBuilder prompt,
            SuMessagingThreadContext context
    ) {
        prompt.append("Current context:\n");
        if (context == null) {
            prompt.append("- store scoped thread\n");
            return;
        }
        appendLine(prompt, "channel", context.getChannelName());
        appendLine(prompt, "room", context.getRoomNumber());
        appendLine(prompt, "roomType", context.getRoomTypeName());
        appendLine(prompt, "bookingKey", context.getBookingKey());
        appendLine(prompt, "checkIn", context.getCheckInDate() == null ? null : context.getCheckInDate().toString());
        appendLine(prompt, "checkOut", context.getCheckOutDate() == null ? null : context.getCheckOutDate().toString());
    }

    private void appendLine(
            StringBuilder prompt,
            String label,
            String value
    ) {
        if (!hasText(value)) {
            return;
        }
        prompt.append("- ").append(label).append(": ").append(redact(value)).append("\n");
    }

    private List<RerankDecision> parseDecisions(String output) {
        String json = extractJson(output);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new IllegalArgumentException("Rerank output must be a JSON object");
            }

            Map<Long, RerankDecision> decisions = new LinkedHashMap<>();
            boolean parsedItems = parseItemArray(root, decisions, "items");
            parsedItems = parseItemArray(root, decisions, "results") || parsedItems;
            parsedItems = parseItemArray(root, decisions, "candidates") || parsedItems;
            boolean parsedIds = parseIdArray(root, decisions, "relevantIds");
            parsedIds = parseIdArray(root, decisions, "relevant_ids") || parsedIds;
            if (!parsedItems && !parsedIds) {
                throw new IllegalArgumentException("Rerank output did not contain candidate decisions");
            }
            return new ArrayList<>(decisions.values());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid rerank JSON", e);
        }
    }

    private boolean parseItemArray(
            JsonNode root,
            Map<Long, RerankDecision> decisions,
            String fieldName
    ) {
        JsonNode items = root.get(fieldName);
        if (items == null) {
            return false;
        }
        if (!items.isArray()) {
            throw new IllegalArgumentException("Rerank field is not an array: " + fieldName);
        }
        for (JsonNode item : items) {
            Long id = readId(item);
            if (id == null) {
                continue;
            }
            boolean relevant = readBoolean(item, "relevant", true);
            double confidence = readDouble(item, "confidence", 1.0);
            String reason = readText(item, "reason");
            decisions.put(id, new RerankDecision(id, relevant, confidence, reason));
        }
        return true;
    }

    private boolean parseIdArray(
            JsonNode root,
            Map<Long, RerankDecision> decisions,
            String fieldName
    ) {
        JsonNode ids = root.get(fieldName);
        if (ids == null) {
            return false;
        }
        if (!ids.isArray()) {
            throw new IllegalArgumentException("Rerank id field is not an array: " + fieldName);
        }
        for (JsonNode idNode : ids) {
            Long id = readLong(idNode);
            if (id != null) {
                decisions.put(id, new RerankDecision(id, true, 1.0, null));
            }
        }
        return true;
    }

    private List<MessageKnowledgeSemanticMatch> applyDecisions(
            List<MessageKnowledgeSemanticMatch> candidates,
            List<RerankDecision> decisions
    ) {
        Map<Long, MessageKnowledgeSemanticMatch> candidateById = new LinkedHashMap<>();
        for (MessageKnowledgeSemanticMatch candidate : candidates) {
            if (candidate == null || candidate.item() == null || candidate.item().getId() == null) {
                continue;
            }
            candidateById.put(candidate.item().getId(), candidate);
        }

        List<MessageKnowledgeSemanticMatch> selected = new ArrayList<>();
        double threshold = normalizeConfidence(minConfidence);
        for (RerankDecision decision : decisions) {
            if (!decision.relevant() || decision.confidence() < threshold) {
                continue;
            }
            MessageKnowledgeSemanticMatch candidate = candidateById.get(decision.id());
            if (candidate == null) {
                continue;
            }
            selected.add(candidate.withRerank(decision.confidence(), decision.reason()));
        }
        return selected;
    }

    private RerankOutcome invalidOutputFallback(List<MessageKnowledgeSemanticMatch> candidates) {
        if (FALLBACK_LEGACY.equalsIgnoreCase(nullToBlank(invalidOutputFallback).trim())) {
            return RerankOutcome.legacyFallback();
        }
        return RerankOutcome.cosine(candidates);
    }

    private String extractJson(String output) {
        if (!hasText(output)) {
            throw new IllegalArgumentException("Rerank output is blank");
        }
        String trimmed = output.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Rerank output does not contain JSON");
        }
        return trimmed.substring(start, end + 1);
    }

    private Long readId(JsonNode item) {
        Long id = readLong(item.get("id"));
        if (id != null) {
            return id;
        }
        id = readLong(item.get("itemId"));
        if (id != null) {
            return id;
        }
        return readLong(item.get("item_id"));
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
            JsonNode item,
            String fieldName,
            boolean fallback
    ) {
        JsonNode node = item.get(fieldName);
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
            JsonNode item,
            String fieldName,
            double fallback
    ) {
        JsonNode node = item.get(fieldName);
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
            JsonNode item,
            String fieldName
    ) {
        JsonNode node = item.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private static double normalizeConfidence(double value) {
        if (value < 0.0 || value > 1.0) {
            return DEFAULT_MIN_CONFIDENCE;
        }
        return value;
    }

    private String redact(String value) {
        return redactor.redact(nullToBlank(value));
    }

    private static String truncate(String value) {
        String normalized = nullToBlank(value).trim();
        if (normalized.length() <= MAX_FACT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_FACT_LENGTH);
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record RerankOutcome(
            List<MessageKnowledgeSemanticMatch> matches,
            boolean fallbackToLegacy
    ) {
        public RerankOutcome {
            matches = matches == null ? List.of() : List.copyOf(matches);
        }

        static RerankOutcome cosine(List<MessageKnowledgeSemanticMatch> matches) {
            return new RerankOutcome(matches, false);
        }

        static RerankOutcome reranked(List<MessageKnowledgeSemanticMatch> matches) {
            return new RerankOutcome(matches, false);
        }

        static RerankOutcome legacyFallback() {
            return new RerankOutcome(List.of(), true);
        }
    }

    private record RerankDecision(
            Long id,
            boolean relevant,
            double confidence,
            String reason
    ) {
    }
}
