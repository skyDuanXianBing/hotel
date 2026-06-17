package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MessageKnowledgeSemanticRetrievalService {
    private static final int DEFAULT_CANDIDATE_CAP = 12;
    private static final int DEFAULT_TOP_K = 8;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 12;
    private static final double DEFAULT_MIN_SCORE = 0.45;

    private final MessageKnowledgeItemRepository itemRepository;
    private final MessageKnowledgeEmbeddingProvider embeddingProvider;
    private final ObjectMapper objectMapper;
    private MessageKnowledgeTopicRemediationService topicRemediationService;

    @Value("${messaging.knowledge.semantic-retrieval.enabled:false}")
    private boolean enabled;

    @Value("${messaging.knowledge.semantic-retrieval.candidate-cap:12}")
    private int candidateCap;

    @Value("${messaging.knowledge.semantic-retrieval.top-k:8}")
    private int topK;

    @Value("${messaging.knowledge.semantic-retrieval.min-score:0.45}")
    private double minScore;

    public MessageKnowledgeSemanticRetrievalService(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEmbeddingProvider embeddingProvider,
            ObjectMapper objectMapper
    ) {
        this.itemRepository = itemRepository;
        this.embeddingProvider = embeddingProvider;
        this.objectMapper = objectMapper;
    }

    @Autowired(required = false)
    public void setTopicRemediationService(MessageKnowledgeTopicRemediationService topicRemediationService) {
        this.topicRemediationService = topicRemediationService;
    }

    public boolean isEnabled() {
        return enabled && embeddingProvider.isEnabled();
    }

    public List<MessageKnowledgeSemanticMatch> search(
            Long storeId,
            SuMessagingThreadContext context,
            String latestGuestMessage
    ) {
        if (storeId == null || !hasText(latestGuestMessage) || !isEnabled()) {
            return List.of();
        }

        List<MessageKnowledgeItem> candidates = filterUsableCandidates(
                storeId,
                context,
                loadReadyCandidates(storeId, context)
        );
        candidates = deduplicateRemediatedCandidates(candidates);
        if (candidates.isEmpty()) {
            return List.of();
        }

        MessageKnowledgeEmbeddingResponse response = embeddingProvider.embed(latestGuestMessage.trim());
        List<Double> queryVector = toVector(response);
        if (queryVector.isEmpty()) {
            return List.of();
        }

        List<MessageKnowledgeSemanticMatch> matches = new ArrayList<>();
        double threshold = normalizeMinScore(minScore);
        for (MessageKnowledgeItem item : candidates) {
            List<Double> itemVector = parseStoredVector(item.getEmbeddingVector());
            double score = cosine(queryVector, itemVector);
            if (score < threshold) {
                continue;
            }
            matches.add(new MessageKnowledgeSemanticMatch(
                    item,
                    score,
                    resolveScopeLevel(item, context),
                    buildReasons(item, context)
            ));
        }

        matches.sort(Comparator.comparingDouble(MessageKnowledgeSemanticMatch::score).reversed());
        int limit = normalizeLimit(topK, DEFAULT_TOP_K);
        if (matches.size() > limit) {
            return new ArrayList<>(matches.subList(0, limit));
        }
        return matches;
    }

    public static int normalizeLimit(
            Integer value,
            int fallback
    ) {
        int normalized = value == null ? fallback : value;
        if (normalized < MIN_LIMIT) {
            return MIN_LIMIT;
        }
        return Math.min(normalized, MAX_LIMIT);
    }

    private List<MessageKnowledgeItem> loadReadyCandidates(
            Long storeId,
            SuMessagingThreadContext context
    ) {
        List<MessageKnowledgeItem> candidates = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        int pageSize = normalizeLimit(candidateCap, DEFAULT_CANDIDATE_CAP);

        if (context != null && context.getRoomId() != null) {
            addCandidates(
                    candidates,
                    seenKeys,
                    itemRepository.findReadySemanticCandidatesByStoreIdAndRoomId(
                            storeId,
                            context.getRoomId(),
                            MessageKnowledgeItem.STATUS_ACTIVE,
                            MessageKnowledgeItem.EMBEDDING_STATUS_READY,
                            PageRequest.of(0, pageSize)
                    )
            );
        }

        if (context != null && context.getRoomTypeId() != null) {
            addCandidates(
                    candidates,
                    seenKeys,
                    itemRepository.findReadySemanticCandidatesByStoreIdAndRoomTypeId(
                            storeId,
                            context.getRoomTypeId(),
                            MessageKnowledgeItem.STATUS_ACTIVE,
                            MessageKnowledgeItem.EMBEDDING_STATUS_READY,
                            PageRequest.of(0, pageSize)
                    )
            );
        }

        if (context != null && context.getReservationId() != null) {
            addCandidates(
                    candidates,
                    seenKeys,
                    itemRepository.findReadySemanticCandidatesByStoreIdAndScope(
                            storeId,
                            SuMessagingThreadContext.SCOPE_BOOKING,
                            context.getReservationId(),
                            MessageKnowledgeItem.STATUS_ACTIVE,
                            MessageKnowledgeItem.EMBEDDING_STATUS_READY,
                            PageRequest.of(0, pageSize)
                    )
            );
        }

        addCandidates(
                candidates,
                seenKeys,
                itemRepository.findReadySemanticStoreScopedByStoreId(
                        storeId,
                        MessageKnowledgeItem.STATUS_ACTIVE,
                        MessageKnowledgeItem.EMBEDDING_STATUS_READY,
                        PageRequest.of(0, pageSize)
                )
        );
        return candidates;
    }

    private List<MessageKnowledgeItem> deduplicateRemediatedCandidates(List<MessageKnowledgeItem> candidates) {
        if (topicRemediationService == null) {
            return candidates;
        }
        return topicRemediationService.deduplicateItemsForRetrieval(candidates);
    }

    private List<MessageKnowledgeItem> filterUsableCandidates(
            Long storeId,
            SuMessagingThreadContext context,
            List<MessageKnowledgeItem> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<MessageKnowledgeItem> usableCandidates = new ArrayList<>();
        for (MessageKnowledgeItem candidate : candidates) {
            if (isUsableCandidate(storeId, context, candidate)) {
                usableCandidates.add(candidate);
            }
        }
        return usableCandidates;
    }

    private void addCandidates(
            List<MessageKnowledgeItem> target,
            Set<String> seenKeys,
            List<MessageKnowledgeItem> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (MessageKnowledgeItem candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String key = candidateKey(candidate);
            if (seenKeys.add(key)) {
                target.add(candidate);
            }
        }
    }

    private boolean isUsableCandidate(
            Long storeId,
            SuMessagingThreadContext context,
            MessageKnowledgeItem item
    ) {
        if (item == null || !storeId.equals(item.getStoreId())) {
            return false;
        }
        if (!MessageKnowledgeItem.STATUS_ACTIVE.equals(item.getStatus())) {
            return false;
        }
        if (!MessageKnowledgeItem.EMBEDDING_STATUS_READY.equals(item.getEmbeddingStatus())) {
            return false;
        }
        if (!hasText(item.getEmbeddingVector())) {
            return false;
        }
        return matchesCurrentScope(item, context);
    }

    private boolean matchesCurrentScope(
            MessageKnowledgeItem item,
            SuMessagingThreadContext context
    ) {
        String scopeType = item.getScopeType();
        if (SuMessagingThreadContext.SCOPE_STORE.equals(scopeType)) {
            return true;
        }
        if (context == null) {
            return false;
        }
        if (SuMessagingThreadContext.SCOPE_ROOM.equals(scopeType)) {
            return matchesLong(context.getRoomId(), item.getRoomId())
                    || matchesLong(context.getRoomId(), item.getScopeId());
        }
        if (SuMessagingThreadContext.SCOPE_ROOM_TYPE.equals(scopeType)) {
            return matchesLong(context.getRoomTypeId(), item.getRoomTypeId())
                    || matchesLong(context.getRoomTypeId(), item.getScopeId());
        }
        if (SuMessagingThreadContext.SCOPE_BOOKING.equals(scopeType)) {
            if (!matchesLong(context.getReservationId(), item.getScopeId())) {
                return false;
            }
            Integer itemChannelId = item.getChannelId();
            return itemChannelId == null || itemChannelId.equals(context.getChannelId());
        }
        return false;
    }

    private List<Double> toVector(MessageKnowledgeEmbeddingResponse response) {
        if (response == null || response.vector() == null || response.vector().isEmpty()) {
            return List.of();
        }
        List<Double> values = new ArrayList<>();
        for (Float value : response.vector()) {
            if (value == null) {
                return List.of();
            }
            values.add(value.doubleValue());
        }
        return values;
    }

    private List<Double> parseStoredVector(String vectorJson) {
        if (!hasText(vectorJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(vectorJson);
            if (!root.isArray()) {
                return List.of();
            }
            List<Double> values = new ArrayList<>();
            for (JsonNode node : root) {
                if (!node.isNumber()) {
                    return List.of();
                }
                values.add(node.asDouble());
            }
            return values;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static double cosine(
            List<Double> queryVector,
            List<Double> itemVector
    ) {
        if (queryVector == null || itemVector == null || queryVector.size() != itemVector.size()) {
            return 0.0;
        }
        if (queryVector.isEmpty()) {
            return 0.0;
        }

        double dot = 0.0;
        double queryNorm = 0.0;
        double itemNorm = 0.0;
        for (int index = 0; index < queryVector.size(); index++) {
            double queryValue = queryVector.get(index);
            double itemValue = itemVector.get(index);
            dot += queryValue * itemValue;
            queryNorm += queryValue * queryValue;
            itemNorm += itemValue * itemValue;
        }
        if (queryNorm <= 0.0 || itemNorm <= 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(queryNorm) * Math.sqrt(itemNorm));
    }

    private static List<String> buildReasons(
            MessageKnowledgeItem item,
            SuMessagingThreadContext context
    ) {
        List<String> reasons = new ArrayList<>();
        reasons.add("SEMANTIC_COSINE");
        String scopeLevel = resolveScopeLevel(item, context);
        if (!SuMessagingThreadContext.SCOPE_STORE.equals(scopeLevel)) {
            reasons.add("SAME_" + scopeLevel);
        }
        return reasons;
    }

    private static String resolveScopeLevel(
            MessageKnowledgeItem item,
            SuMessagingThreadContext context
    ) {
        if (item == null) {
            return SuMessagingThreadContext.SCOPE_STORE;
        }
        String scopeType = item.getScopeType();
        if (SuMessagingThreadContext.SCOPE_ROOM.equals(scopeType)) {
            return SuMessagingThreadContext.SCOPE_ROOM;
        }
        if (SuMessagingThreadContext.SCOPE_ROOM_TYPE.equals(scopeType)) {
            return SuMessagingThreadContext.SCOPE_ROOM_TYPE;
        }
        if (SuMessagingThreadContext.SCOPE_BOOKING.equals(scopeType)) {
            return SuMessagingThreadContext.SCOPE_BOOKING;
        }
        if (context == null) {
            return SuMessagingThreadContext.SCOPE_STORE;
        }
        return SuMessagingThreadContext.SCOPE_STORE;
    }

    private static String candidateKey(MessageKnowledgeItem item) {
        if (item.getId() != null) {
            return "item:" + item.getId();
        }
        if (hasText(item.getNormalizedAnswerHash())) {
            return "answer:" + item.getNormalizedAnswerHash();
        }
        return "scope-topic:" + item.getScopeKey() + ":" + item.getTopicHash();
    }

    private static double normalizeMinScore(double configuredValue) {
        if (configuredValue < 0.0 || configuredValue > 1.0) {
            return DEFAULT_MIN_SCORE;
        }
        return configuredValue;
    }

    private static boolean matchesLong(
            Long first,
            Long second
    ) {
        return first != null && first.equals(second);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
