package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MessageKnowledgeTopicRemediationService {
    private static final String REFINED_TOPIC_PET_POLICY = "pet_policy";
    private static final String REFINED_TOPIC_SHUTTLE = "shuttle";

    private final MessageKnowledgeTopicService topicService;

    public MessageKnowledgeTopicRemediationService(MessageKnowledgeTopicService topicService) {
        this.topicService = topicService;
    }

    public List<MessageKnowledgeItem> deduplicateItemsForRetrieval(List<MessageKnowledgeItem> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<MessageKnowledgeItem> deduplicated = new ArrayList<>();
        Map<String, Integer> remediatedIndexByKey = new HashMap<>();
        for (MessageKnowledgeItem candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            String key = buildRemediationKey(candidate);
            if (key == null) {
                deduplicated.add(candidate);
                continue;
            }

            Integer existingIndex = remediatedIndexByKey.get(key);
            if (existingIndex == null) {
                remediatedIndexByKey.put(key, deduplicated.size());
                deduplicated.add(candidate);
                continue;
            }

            MessageKnowledgeItem existing = deduplicated.get(existingIndex);
            if (shouldPrefer(candidate, existing)) {
                deduplicated.set(existingIndex, candidate);
            }
        }
        return deduplicated;
    }

    private String buildRemediationKey(MessageKnowledgeItem item) {
        String currentTopic = resolveCurrentTopic(item);
        if (currentTopic == null) {
            return null;
        }
        Long storeId = item.getStoreId();
        String scopeKey = item.getScopeKey();
        if (storeId == null || !hasText(scopeKey)) {
            return null;
        }
        return storeId + "|" + scopeKey.trim() + "|" + currentTopic;
    }

    private String resolveCurrentTopic(MessageKnowledgeItem item) {
        String storedTopic = normalizeTopic(item.getTopic());
        if (REFINED_TOPIC_PET_POLICY.equals(storedTopic) || REFINED_TOPIC_SHUTTLE.equals(storedTopic)) {
            return storedTopic;
        }

        Set<String> detectedTopics = topicService.detectTopics(buildDetectionText(item));
        if (detectedTopics.contains(MessageKnowledgeTopicService.TOPIC_PET_POLICY)) {
            return REFINED_TOPIC_PET_POLICY;
        }
        if (detectedTopics.contains(MessageKnowledgeTopicService.TOPIC_SHUTTLE)) {
            return REFINED_TOPIC_SHUTTLE;
        }
        return null;
    }

    private boolean shouldPrefer(
            MessageKnowledgeItem candidate,
            MessageKnowledgeItem existing
    ) {
        String candidateCurrentTopic = resolveCurrentTopic(candidate);
        String existingCurrentTopic = resolveCurrentTopic(existing);
        boolean candidateHasCurrentTopic = hasStoredTopic(candidate, candidateCurrentTopic);
        boolean existingHasCurrentTopic = hasStoredTopic(existing, existingCurrentTopic);
        if (candidateHasCurrentTopic != existingHasCurrentTopic) {
            return candidateHasCurrentTopic;
        }

        boolean candidateReady = MessageKnowledgeItem.EMBEDDING_STATUS_READY.equals(candidate.getEmbeddingStatus());
        boolean existingReady = MessageKnowledgeItem.EMBEDDING_STATUS_READY.equals(existing.getEmbeddingStatus());
        if (candidateReady != existingReady) {
            return candidateReady;
        }

        LocalDateTime candidateSeenAt = candidate.getLastSeenAt();
        LocalDateTime existingSeenAt = existing.getLastSeenAt();
        if (candidateSeenAt != null && existingSeenAt != null && !candidateSeenAt.equals(existingSeenAt)) {
            return candidateSeenAt.isAfter(existingSeenAt);
        }
        if (candidateSeenAt != null && existingSeenAt == null) {
            return true;
        }
        if (candidateSeenAt == null && existingSeenAt != null) {
            return false;
        }

        Long candidateId = candidate.getId();
        Long existingId = existing.getId();
        if (candidateId == null || existingId == null) {
            return false;
        }
        return candidateId > existingId;
    }

    private static boolean hasStoredTopic(
            MessageKnowledgeItem item,
            String currentTopic
    ) {
        return item != null && hasText(currentTopic) && currentTopic.equals(normalizeTopic(item.getTopic()));
    }

    private static String buildDetectionText(MessageKnowledgeItem item) {
        StringBuilder builder = new StringBuilder();
        appendText(builder, item.getQuestion());
        appendText(builder, item.getAnswer());
        appendText(builder, item.getNormalizedQuestion());
        appendText(builder, item.getNormalizedAnswer());
        appendText(builder, item.getSemanticText());
        appendText(builder, item.getSearchIntentsJson());
        return builder.toString();
    }

    private static void appendText(StringBuilder builder, String value) {
        if (!hasText(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    private static String normalizeTopic(String topic) {
        if (!hasText(topic)) {
            return null;
        }
        return topic.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
