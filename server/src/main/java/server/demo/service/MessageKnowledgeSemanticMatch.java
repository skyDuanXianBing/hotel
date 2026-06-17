package server.demo.service;

import server.demo.entity.MessageKnowledgeItem;

import java.util.ArrayList;
import java.util.List;

public record MessageKnowledgeSemanticMatch(
        MessageKnowledgeItem item,
        double score,
        String scopeLevel,
        List<String> reasons
) {
    public MessageKnowledgeSemanticMatch {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public MessageKnowledgeSemanticMatch withRerank(
            double confidence,
            String reason
    ) {
        List<String> updatedReasons = new ArrayList<>(reasons);
        updatedReasons.add("AI_RERANK_SELECTED");
        if (hasText(reason)) {
            updatedReasons.add("AI_RERANK_REASON:" + reason.trim());
        }
        double updatedScore = Math.max(score, Math.min(1.0, confidence));
        return new MessageKnowledgeSemanticMatch(item, updatedScore, scopeLevel, updatedReasons);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
