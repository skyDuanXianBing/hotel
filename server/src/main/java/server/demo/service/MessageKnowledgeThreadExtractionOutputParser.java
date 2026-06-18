package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MessageKnowledgeThreadExtractionOutputParser {

    private final ObjectMapper objectMapper;

    public MessageKnowledgeThreadExtractionOutputParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MessageKnowledgeThreadParsedOutput parse(String output) {
        String json = extractJson(output);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new IllegalArgumentException("Thread extraction output must be a JSON object");
            }
            JsonNode itemsNode = root.get("items");
            if (itemsNode == null) {
                throw new IllegalArgumentException("Thread extraction output must contain items array");
            }
            if (!itemsNode.isArray()) {
                throw new IllegalArgumentException("Thread extraction items must be an array");
            }
            List<MessageKnowledgeThreadModelItem> items = new ArrayList<>();
            for (JsonNode itemNode : itemsNode) {
                if (!itemNode.isObject()) {
                    throw new IllegalArgumentException("Thread extraction item must be a JSON object");
                }
                items.add(parseItem(itemNode));
            }
            return new MessageKnowledgeThreadParsedOutput(readText(root, "schemaVersion", "schema_version"), items);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid thread extraction JSON", e);
        }
    }

    private MessageKnowledgeThreadModelItem parseItem(JsonNode itemNode) {
        return new MessageKnowledgeThreadModelItem(
                readText(itemNode, "clientItemId", "client_item_id", "id"),
                readText(itemNode, "topicCode", "topic_code", "topic"),
                readText(itemNode, "proposedTopicCode", "proposed_topic_code"),
                readText(itemNode, "scopeType", "scope_type", "scope"),
                readText(itemNode, "canonicalQuestion", "canonical_question", "question"),
                readText(itemNode, "canonicalAnswer", "canonical_answer", "answer"),
                readText(itemNode, "language", "lang"),
                readDecimal(itemNode, "confidence"),
                readSourceMessageIds(itemNode),
                readText(itemNode, "evidenceSummary", "evidence_summary", "supportingEvidence"),
                readText(itemNode, "reusabilityReason", "reusability_reason", "reason")
        );
    }

    private List<Long> readSourceMessageIds(JsonNode itemNode) {
        JsonNode idsNode = firstPresent(itemNode, "sourceMessageIds", "source_message_ids", "sourceIds", "source_ids");
        if (idsNode == null || idsNode.isNull()) {
            return List.of();
        }
        if (!idsNode.isArray()) {
            throw new IllegalArgumentException("sourceMessageIds must be an array");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (JsonNode idNode : idsNode) {
            Long id = readLong(idNode);
            if (id != null) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }

    private String extractJson(String output) {
        if (output == null || output.trim().isBlank()) {
            throw new IllegalArgumentException("Thread extraction output is blank");
        }
        String trimmed = output.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Thread extraction output does not contain JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private static String readText(JsonNode node, String... fieldNames) {
        JsonNode value = firstPresent(node, fieldNames);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        if (text == null || text.trim().isBlank()) {
            return null;
        }
        return text.trim();
    }

    private static BigDecimal readDecimal(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return BigDecimal.valueOf(value.asDouble());
        }
        if (value.isTextual()) {
            try {
                return new BigDecimal(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long readLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static JsonNode firstPresent(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
