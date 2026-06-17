package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MessageKnowledgeEmbeddingTextService {
    private static final int MAX_INTENTS = 8;

    private final ObjectMapper objectMapper;
    private final SuMessagingAiTextService textService;

    public MessageKnowledgeEmbeddingTextService(
            ObjectMapper objectMapper,
            SuMessagingAiTextService textService
    ) {
        this.objectMapper = objectMapper;
        this.textService = textService;
    }

    public SemanticRefreshResult refreshSemanticFields(
            MessageKnowledgeItem item,
            boolean newItem
    ) {
        if (item == null) {
            return new SemanticRefreshResult(false, null, null);
        }

        String semanticText = buildSemanticText(item);
        String searchIntentsJson = buildSearchIntentsJson(item);
        boolean changed = !Objects.equals(nullIfBlank(item.getSemanticText()), nullIfBlank(semanticText))
                || !Objects.equals(nullIfBlank(item.getSearchIntentsJson()), nullIfBlank(searchIntentsJson));

        item.setSemanticText(semanticText);
        item.setSearchIntentsJson(searchIntentsJson);
        if (newItem) {
            markPending(item);
        } else if (changed) {
            markStaleOrPending(item);
        }
        return new SemanticRefreshResult(changed, semanticText, searchIntentsJson);
    }

    public String inputHash(String input) {
        return textService.sha256(input);
    }

    private String buildSemanticText(MessageKnowledgeItem item) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Topic", item.getTopic());
        appendLine(builder, "Scope", buildScopeText(item));
        appendLine(builder, "Language", item.getLanguage());
        appendLine(builder, "Guest question", item.getQuestion());
        appendLine(builder, "Canonical fact", item.getAnswer());
        appendLine(builder, "Normalized question", item.getNormalizedQuestion());
        appendLine(builder, "Normalized fact", item.getNormalizedAnswer());
        return nullIfBlank(builder.toString());
    }

    private String buildSearchIntentsJson(MessageKnowledgeItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("topic", nullIfBlank(item.getTopic()));
        payload.put("scopeType", nullIfBlank(item.getScopeType()));
        payload.put("scopeName", nullIfBlank(buildScopeText(item)));
        payload.put("language", nullIfBlank(item.getLanguage()));
        payload.put("queries", buildQueries(item));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build message knowledge search intents JSON", e);
        }
    }

    private List<String> buildQueries(MessageKnowledgeItem item) {
        Set<String> queries = new LinkedHashSet<>();
        addIntent(queries, item.getQuestion());
        addIntent(queries, item.getNormalizedQuestion());
        addIntent(queries, item.getTopic());
        addIntent(queries, item.getAnswer());
        addIntent(queries, item.getNormalizedAnswer());
        addIntent(queries, item.getRoomNumber());
        addIntent(queries, item.getRoomTypeName());
        return new ArrayList<>(queries);
    }

    private void addIntent(Set<String> queries, String value) {
        if (queries.size() >= MAX_INTENTS) {
            return;
        }
        String normalized = nullIfBlank(value);
        if (normalized != null) {
            queries.add(normalized);
        }
    }

    private static String buildScopeText(MessageKnowledgeItem item) {
        String scopeType = nullIfBlank(item.getScopeType());
        String roomNumber = nullIfBlank(item.getRoomNumber());
        String roomTypeName = nullIfBlank(item.getRoomTypeName());
        if (SuMessagingThreadContext.SCOPE_ROOM.equals(scopeType) && roomNumber != null) {
            return scopeType + " " + roomNumber;
        }
        if (SuMessagingThreadContext.SCOPE_ROOM_TYPE.equals(scopeType) && roomTypeName != null) {
            return scopeType + " " + roomTypeName;
        }
        if (scopeType != null) {
            Long scopeId = item.getScopeId();
            if (scopeId != null) {
                return scopeType + " " + scopeId;
            }
            return scopeType;
        }
        return null;
    }

    private static void appendLine(StringBuilder builder, String label, String value) {
        String normalized = nullIfBlank(value);
        if (normalized == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(label).append(": ").append(normalized);
    }

    private static void markPending(MessageKnowledgeItem item) {
        item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_PENDING);
        item.setEmbeddingVector(null);
        item.setEmbeddingProvider(null);
        item.setEmbeddingModel(null);
        item.setEmbeddingDimensions(null);
        item.setEmbeddingInputHash(null);
        item.setEmbeddingError(null);
        item.setEmbeddingUpdatedAt(null);
        item.setEmbeddingNextAttemptAt(null);
        item.setEmbeddingLeaseOwner(null);
        item.setEmbeddingLeaseUntil(null);
    }

    private static void markStaleOrPending(MessageKnowledgeItem item) {
        String currentStatus = item.getEmbeddingStatus();
        if (MessageKnowledgeItem.EMBEDDING_STATUS_READY.equals(currentStatus)) {
            item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_STALE);
        } else {
            item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_PENDING);
            item.setEmbeddingVector(null);
            item.setEmbeddingProvider(null);
            item.setEmbeddingModel(null);
            item.setEmbeddingDimensions(null);
            item.setEmbeddingInputHash(null);
            item.setEmbeddingUpdatedAt(null);
        }
        item.setEmbeddingError(null);
        item.setEmbeddingNextAttemptAt(null);
        item.setEmbeddingLeaseOwner(null);
        item.setEmbeddingLeaseUntil(null);
    }

    private static String nullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized;
    }

    public record SemanticRefreshResult(
            boolean changed,
            String semanticText,
            String searchIntentsJson
    ) {
    }
}
