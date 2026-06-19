package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageKnowledgeDuplicateCandidateService {
    private static final int DEFAULT_CANDIDATE_CAP = 5;

    private final MessageKnowledgeItemRepository itemRepository;

    @Value("${messaging.knowledge.ingest-dedup.candidate-cap:5}")
    private int candidateCap;

    public MessageKnowledgeDuplicateCandidateService(MessageKnowledgeItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<MessageKnowledgeItem> findCandidates(
            Long storeId,
            String scopeKey,
            String topic,
            String topicHash,
            String factHash
    ) {
        if (storeId == null || !hasText(scopeKey) || !hasText(topic) || !hasText(topicHash)) {
            return List.of();
        }

        int limit = MessageKnowledgeSemanticRetrievalService.normalizeLimit(
                candidateCap,
                DEFAULT_CANDIDATE_CAP
        );
        List<MessageKnowledgeItem> loadedItems = itemRepository.findIngestDuplicateCandidates(
                storeId,
                scopeKey,
                topic,
                topicHash,
                List.of(MessageKnowledgeItem.STATUS_ACTIVE, MessageKnowledgeItem.STATUS_CANDIDATE),
                PageRequest.of(0, limit)
        );
        if (loadedItems == null || loadedItems.isEmpty()) {
            return List.of();
        }

        List<MessageKnowledgeItem> candidates = new ArrayList<>();
        for (MessageKnowledgeItem item : loadedItems) {
            if (isCandidate(storeId, scopeKey, topic, topicHash, factHash, item)) {
                candidates.add(item);
            }
        }
        return candidates;
    }

    private boolean isCandidate(
            Long storeId,
            String scopeKey,
            String topic,
            String topicHash,
            String factHash,
            MessageKnowledgeItem item
    ) {
        if (item == null || item.getId() == null) {
            return false;
        }
        if (!storeId.equals(item.getStoreId())) {
            return false;
        }
        if (!scopeKey.equals(item.getScopeKey())) {
            return false;
        }
        if (!topic.equals(item.getTopic())) {
            return false;
        }
        if (!topicHash.equals(item.getTopicHash())) {
            return false;
        }
        if (hasText(factHash) && factHash.equals(item.getFactHash())) {
            return false;
        }
        return MessageKnowledgeItem.STATUS_ACTIVE.equals(item.getStatus())
                || MessageKnowledgeItem.STATUS_CANDIDATE.equals(item.getStatus());
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
