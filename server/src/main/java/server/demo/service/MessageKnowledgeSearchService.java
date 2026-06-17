package server.demo.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeEntry;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeEntryRepository;
import server.demo.repository.MessageKnowledgeItemRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MessageKnowledgeSearchService {
    private static final Logger logger = LoggerFactory.getLogger(MessageKnowledgeSearchService.class);

    public static final String STATUS_MATCHED = "MATCHED";
    public static final String STATUS_NO_MATCH = "NO_MATCH";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_FAILED = "FAILED";

    static final String WARNING_NO_SIMILAR_HISTORY = "NO_SIMILAR_HISTORY";
    static final String WARNING_LOW_CONFIDENCE_MATCH = "LOW_CONFIDENCE_MATCH";

    private static final int MAX_CANDIDATES_PER_SCOPE = 60;
    private static final int MAX_REFINED_CANDIDATES_PER_SCOPE = 25;
    private static final int MAX_MATCHES = 5;
    private static final double MATCH_THRESHOLD = 0.32;
    private static final double PARTIAL_THRESHOLD = 0.18;
    private static final double TOKEN_SCORE_WEIGHT = 0.55;
    private static final double ROOM_SCOPE_BOOST = 0.35;
    private static final double ROOM_TYPE_SCOPE_BOOST = 0.24;
    private static final double BOOKING_SCOPE_BOOST = 0.18;
    private static final double STORE_SCOPE_BOOST = 0.08;
    private static final double RECENT_BOOST = 0.08;
    private static final double POLICY_TOPIC_BOOST = 0.20;
    private static final double FACT_OVERLAP_UNIT_BOOST = 0.06;
    private static final double FACT_OVERLAP_MAX_BOOST = 0.12;
    private static final long RECENT_DAYS = 120;

    private final MessageKnowledgeEntryRepository knowledgeEntryRepository;
    private final SuMessagingAiTextService textService;
    private final MessageKnowledgeTopicService topicService;
    private MessageKnowledgeItemRepository knowledgeItemRepository;
    private MessageKnowledgeSemanticRetrievalService semanticRetrievalService;
    private MessageKnowledgeRerankService rerankService;
    private MessageKnowledgeTopicRemediationService topicRemediationService;

    public MessageKnowledgeSearchService(
            MessageKnowledgeEntryRepository knowledgeEntryRepository,
            SuMessagingAiTextService textService,
            MessageKnowledgeTopicService topicService
    ) {
        this.knowledgeEntryRepository = knowledgeEntryRepository;
        this.textService = textService;
        this.topicService = topicService;
    }

    @Autowired(required = false)
    public void setKnowledgeItemRepository(MessageKnowledgeItemRepository knowledgeItemRepository) {
        this.knowledgeItemRepository = knowledgeItemRepository;
    }

    @Autowired(required = false)
    public void setSemanticRetrievalService(MessageKnowledgeSemanticRetrievalService semanticRetrievalService) {
        this.semanticRetrievalService = semanticRetrievalService;
    }

    @Autowired(required = false)
    public void setRerankService(MessageKnowledgeRerankService rerankService) {
        this.rerankService = rerankService;
    }

    @Autowired(required = false)
    public void setTopicRemediationService(MessageKnowledgeTopicRemediationService topicRemediationService) {
        this.topicRemediationService = topicRemediationService;
    }

    public MessageKnowledgeSearchResult searchSimilar(
            Long storeId,
            Long currentThreadId,
            SuMessagingThreadContext context,
            String queryText,
            int limit
    ) {
        int normalizedLimit = Math.max(1, Math.min(limit, MAX_MATCHES));
        if (storeId == null || !hasText(queryText)) {
            return new MessageKnowledgeSearchResult(
                    STATUS_NO_MATCH,
                    List.of(),
                    List.of(WARNING_NO_SIMILAR_HISTORY)
            );
        }

        List<MessageKnowledgeMatch> semanticMatches = searchSemanticMatchesSafely(
                storeId,
                context,
                queryText,
                normalizedLimit
        );
        if (!semanticMatches.isEmpty()) {
            return buildResultFromMatches(semanticMatches, normalizedLimit);
        }

        List<String> queryTokens = textService.tokenize(queryText);
        if (queryTokens.isEmpty()) {
            return noMatchResult();
        }

        Set<String> queryTopics = topicService.detectTopics(queryText);
        Set<String> queryFacts = topicService.extractFactTokens(queryText);
        List<MessageKnowledgeItem> refinedCandidates = loadRefinedCandidates(storeId, context);
        List<MessageKnowledgeMatch> refinedMatches = scoreRefinedCandidates(
                refinedCandidates,
                context,
                queryTokens,
                queryTopics,
                queryFacts
        );
        if (!refinedMatches.isEmpty() && refinedMatches.get(0).getScore() >= MATCH_THRESHOLD) {
            return buildResultFromMatches(refinedMatches, normalizedLimit);
        }

        List<MessageKnowledgeEntry> candidates = loadCandidates(storeId, currentThreadId, context);
        candidates = suppressLegacyCandidatesCoveredByRefinedTopic(candidates, refinedCandidates, queryText);
        if (candidates.isEmpty() && refinedMatches.isEmpty()) {
            return noMatchResult();
        }

        List<MessageKnowledgeMatch> matches = new ArrayList<>();
        for (MessageKnowledgeEntry candidate : candidates) {
            MessageKnowledgeMatch match = scoreCandidate(candidate, context, queryTokens, queryTopics, queryFacts);
            if (match != null && match.getScore() >= PARTIAL_THRESHOLD) {
                matches.add(match);
            }
        }
        if (!refinedMatches.isEmpty()) {
            matches.addAll(refinedMatches);
        }

        matches.sort(Comparator.comparingDouble(MessageKnowledgeMatch::getScore).reversed());
        if (matches.size() > normalizedLimit) {
            matches = new ArrayList<>(matches.subList(0, normalizedLimit));
        }

        if (matches.isEmpty()) {
            return noMatchResult();
        }

        return buildResultFromMatches(matches, normalizedLimit);
    }

    private List<MessageKnowledgeMatch> searchSemanticMatchesSafely(
            Long storeId,
            SuMessagingThreadContext context,
            String queryText,
            int normalizedLimit
    ) {
        if (semanticRetrievalService == null || !semanticRetrievalService.isEnabled()) {
            return List.of();
        }
        try {
            List<MessageKnowledgeSemanticMatch> semanticMatches = semanticRetrievalService.search(
                    storeId,
                    context,
                    queryText
            );
            if (semanticMatches.isEmpty()) {
                return List.of();
            }
            if (rerankService != null && rerankService.isEnabled()) {
                MessageKnowledgeRerankService.RerankOutcome rerankOutcome =
                        rerankService.rerank(queryText, context, semanticMatches);
                if (rerankOutcome.fallbackToLegacy()) {
                    return List.of();
                }
                semanticMatches = rerankOutcome.matches();
            }
            return toKnowledgeMatches(semanticMatches, normalizedLimit);
        } catch (RuntimeException e) {
            logger.warn("Semantic message knowledge retrieval failed. storeId={}, error={}",
                    storeId, e.getMessage(), e);
            return List.of();
        }
    }

    private MessageKnowledgeSearchResult buildResultFromMatches(
            List<MessageKnowledgeMatch> matches,
            int normalizedLimit
    ) {
        if (matches == null || matches.isEmpty()) {
            return noMatchResult();
        }

        List<MessageKnowledgeMatch> limitedMatches = new ArrayList<>(matches);
        limitedMatches.sort(Comparator.comparingDouble(MessageKnowledgeMatch::getScore).reversed());
        if (limitedMatches.size() > normalizedLimit) {
            limitedMatches = new ArrayList<>(limitedMatches.subList(0, normalizedLimit));
        }

        double bestScore = limitedMatches.get(0).getScore();
        if (bestScore >= MATCH_THRESHOLD) {
            return new MessageKnowledgeSearchResult(STATUS_MATCHED, limitedMatches, List.of());
        }
        return new MessageKnowledgeSearchResult(
                STATUS_PARTIAL,
                limitedMatches,
                List.of(WARNING_LOW_CONFIDENCE_MATCH)
        );
    }

    private static MessageKnowledgeSearchResult noMatchResult() {
        return new MessageKnowledgeSearchResult(
                STATUS_NO_MATCH,
                List.of(),
                List.of(WARNING_NO_SIMILAR_HISTORY)
        );
    }

    private List<MessageKnowledgeEntry> loadCandidates(
            Long storeId,
            Long currentThreadId,
            SuMessagingThreadContext context
    ) {
        List<MessageKnowledgeEntry> candidates = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        if (context != null && context.getRoomId() != null) {
            addCandidates(
                    candidates,
                    seenKeys,
                    knowledgeEntryRepository.findActiveByStoreIdAndRoomId(
                            storeId,
                            context.getRoomId(),
                            MessageKnowledgeEntry.STATUS_ACTIVE,
                            currentThreadId,
                            PageRequest.of(0, MAX_CANDIDATES_PER_SCOPE)
                    )
            );
        }

        if (context != null && context.getRoomTypeId() != null) {
            addCandidates(
                    candidates,
                    seenKeys,
                    knowledgeEntryRepository.findActiveByStoreIdAndRoomTypeId(
                            storeId,
                            context.getRoomTypeId(),
                            MessageKnowledgeEntry.STATUS_ACTIVE,
                            currentThreadId,
                            PageRequest.of(0, MAX_CANDIDATES_PER_SCOPE)
                    )
            );
        }

        if (context != null
                && context.getChannelId() != null
                && context.getBookingKey() != null
                && !context.getBookingKey().isBlank()) {
            addCandidates(
                    candidates,
                    seenKeys,
                    knowledgeEntryRepository.findActiveByStoreIdAndChannelBookingKey(
                            storeId,
                            context.getChannelId(),
                            context.getBookingKey(),
                            MessageKnowledgeEntry.STATUS_ACTIVE,
                            currentThreadId,
                            PageRequest.of(0, MAX_CANDIDATES_PER_SCOPE)
                    )
            );
        }

        addCandidates(
                candidates,
                seenKeys,
                knowledgeEntryRepository.findActiveRecentByStoreId(
                        storeId,
                        MessageKnowledgeEntry.STATUS_ACTIVE,
                        currentThreadId,
                        PageRequest.of(0, MAX_CANDIDATES_PER_SCOPE)
                )
        );
        return candidates;
    }

    private List<MessageKnowledgeMatch> scoreRefinedCandidates(
            List<MessageKnowledgeItem> candidates,
            SuMessagingThreadContext context,
            List<String> queryTokens,
            Set<String> queryTopics,
            Set<String> queryFacts
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<MessageKnowledgeMatch> matches = new ArrayList<>();
        Set<String> seenAnswerKeys = new HashSet<>();
        for (MessageKnowledgeItem candidate : candidates) {
            MessageKnowledgeEntry syntheticEntry = toEntry(candidate);
            MessageKnowledgeMatch match = scoreCandidate(
                    syntheticEntry,
                    context,
                    queryTokens,
                    queryTopics,
                    queryFacts
            );
            if (match == null || match.getScore() < PARTIAL_THRESHOLD) {
                continue;
            }
            String answerKey = refinedAnswerKey(candidate);
            if (!seenAnswerKeys.add(answerKey)) {
                continue;
            }
            match.getMatchReasons().add("REFINED_FACT");
            match.setReusableFacts(refinedReusableFacts(candidate, match.getReusableFacts()));
            matches.add(match);
        }
        matches.sort(Comparator.comparingDouble(MessageKnowledgeMatch::getScore).reversed());
        return matches;
    }

    private List<MessageKnowledgeItem> loadRefinedCandidates(
            Long storeId,
            SuMessagingThreadContext context
    ) {
        if (knowledgeItemRepository == null || storeId == null) {
            return List.of();
        }

        List<MessageKnowledgeItem> candidates = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        if (context != null && context.getRoomId() != null) {
            addRefinedCandidates(
                    candidates,
                    seenKeys,
                    knowledgeItemRepository.findActiveByStoreIdAndRoomId(
                            storeId,
                            context.getRoomId(),
                            MessageKnowledgeItem.STATUS_ACTIVE,
                            PageRequest.of(0, MAX_REFINED_CANDIDATES_PER_SCOPE)
                    )
            );
        }
        if (context != null && context.getRoomTypeId() != null) {
            addRefinedCandidates(
                    candidates,
                    seenKeys,
                    knowledgeItemRepository.findActiveByStoreIdAndRoomTypeId(
                            storeId,
                            context.getRoomTypeId(),
                            MessageKnowledgeItem.STATUS_ACTIVE,
                            PageRequest.of(0, MAX_REFINED_CANDIDATES_PER_SCOPE)
                    )
            );
        }
        addRefinedCandidates(
                candidates,
                seenKeys,
                knowledgeItemRepository.findActiveStoreScopedByStoreId(
                        storeId,
                        MessageKnowledgeItem.STATUS_ACTIVE,
                        PageRequest.of(0, MAX_REFINED_CANDIDATES_PER_SCOPE)
                )
        );
        return deduplicateRemediatedCandidates(candidates);
    }

    private List<MessageKnowledgeEntry> suppressLegacyCandidatesCoveredByRefinedTopic(
            List<MessageKnowledgeEntry> candidates,
            List<MessageKnowledgeItem> refinedCandidates,
            String queryText
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (refinedCandidates == null || refinedCandidates.isEmpty()) {
            return candidates;
        }

        Set<String> queryTopicKeys = detectKnowledgeTopicKeys(queryText);
        if (queryTopicKeys.isEmpty()) {
            return candidates;
        }

        Set<String> refinedTopicKeys = new HashSet<>();
        for (MessageKnowledgeItem refinedCandidate : refinedCandidates) {
            if (refinedCandidate == null) {
                continue;
            }
            if (!MessageKnowledgeItem.STATUS_ACTIVE.equals(refinedCandidate.getStatus())) {
                continue;
            }
            String topicKey = normalizeTopicKey(refinedCandidate.getTopic());
            if (topicKey != null && queryTopicKeys.contains(topicKey)) {
                refinedTopicKeys.add(topicKey);
            }
        }
        if (refinedTopicKeys.isEmpty()) {
            return candidates;
        }

        List<MessageKnowledgeEntry> filteredCandidates = new ArrayList<>();
        for (MessageKnowledgeEntry candidate : candidates) {
            Set<String> candidateTopicKeys = detectKnowledgeTopicKeys(candidateCombinedText(candidate));
            if (!hasAnyTopicOverlap(refinedTopicKeys, candidateTopicKeys)) {
                filteredCandidates.add(candidate);
            }
        }
        return filteredCandidates;
    }

    private List<MessageKnowledgeItem> deduplicateRemediatedCandidates(List<MessageKnowledgeItem> candidates) {
        if (topicRemediationService == null) {
            return candidates;
        }
        return topicRemediationService.deduplicateItemsForRetrieval(candidates);
    }

    private MessageKnowledgeMatch scoreCandidate(
            MessageKnowledgeEntry candidate,
            SuMessagingThreadContext context,
            List<String> queryTokens,
            Set<String> queryTopics,
            Set<String> queryFacts
    ) {
        List<String> candidateTokens = textService.tokenize(candidate.getNormalizedText());
        int overlapCount = 0;
        for (String token : queryTokens) {
            if (candidateTokens.contains(token)) {
                overlapCount++;
            }
        }

        Set<String> candidateTopics = topicService.detectTopics(
                candidate.getQuestion(),
                candidate.getAnswer(),
                candidate.getNormalizedText()
        );
        boolean hasSharedTopic = topicService.hasSharedTopic(queryTopics, candidateTopics);
        if (hasPolicyTopicMismatch(queryTopics, candidateTopics, hasSharedTopic)) {
            return null;
        }

        Set<String> candidateFacts = topicService.extractFactTokens(candidateCombinedText(candidate));
        Set<String> sharedFacts = topicService.intersect(queryFacts, candidateFacts);
        int factOverlapCount = sharedFacts.size();
        if (overlapCount == 0 && !hasSharedTopic && factOverlapCount == 0) {
            return null;
        }

        double tokenScore = queryTokens.isEmpty() ? 0.0 : (double) overlapCount / queryTokens.size();
        String scopeLevel = resolveScopeLevel(candidate, context);
        double scopeBoost = scopeBoost(scopeLevel);
        double recentBoost = isRecent(candidate.getSourceTimestamp()) ? RECENT_BOOST : 0.0;
        double topicBoost = hasSharedTopic ? POLICY_TOPIC_BOOST : 0.0;
        double factBoost = Math.min(FACT_OVERLAP_MAX_BOOST, factOverlapCount * FACT_OVERLAP_UNIT_BOOST);
        double score = Math.min(
                1.0,
                tokenScore * TOKEN_SCORE_WEIGHT + scopeBoost + recentBoost + topicBoost + factBoost
        );

        List<String> reasons = new ArrayList<>();
        if (overlapCount > 0) {
            reasons.add("KEYWORD_OVERLAP");
        }
        if (hasSharedTopic) {
            reasons.add("POLICY_TOPIC_MATCH");
        }
        if (factOverlapCount > 0) {
            reasons.add("FACT_OVERLAP");
        }
        if (!SuMessagingThreadContext.SCOPE_STORE.equals(scopeLevel)) {
            reasons.add("SAME_" + scopeLevel);
        }
        if (recentBoost > 0.0) {
            reasons.add("RECENT");
        }
        reasons.add("STAFF_RESOLVED");

        return new MessageKnowledgeMatch(
                candidate,
                score,
                scopeLevel,
                reasons,
                new ArrayList<>(candidateTopics),
                topicService.extractReusableFacts(candidate.getAnswer())
        );
    }

    private static void addCandidates(
            List<MessageKnowledgeEntry> target,
            Set<String> seenKeys,
            List<MessageKnowledgeEntry> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        for (MessageKnowledgeEntry candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String key = candidateKey(candidate);
            if (seenKeys.add(key)) {
                target.add(candidate);
            }
        }
    }

    private List<MessageKnowledgeMatch> toKnowledgeMatches(
            List<MessageKnowledgeSemanticMatch> semanticMatches,
            int normalizedLimit
    ) {
        if (semanticMatches == null || semanticMatches.isEmpty()) {
            return List.of();
        }

        List<MessageKnowledgeMatch> matches = new ArrayList<>();
        Set<String> seenAnswerKeys = new HashSet<>();
        for (MessageKnowledgeSemanticMatch semanticMatch : semanticMatches) {
            if (matches.size() >= normalizedLimit) {
                break;
            }
            if (semanticMatch == null || semanticMatch.item() == null) {
                continue;
            }
            MessageKnowledgeItem item = semanticMatch.item();
            String answerKey = refinedAnswerKey(item);
            if (!seenAnswerKeys.add(answerKey)) {
                continue;
            }

            List<String> reasons = new ArrayList<>(semanticMatch.reasons());
            if (!reasons.contains("REFINED_FACT")) {
                reasons.add("REFINED_FACT");
            }
            Set<String> topics = topicService.detectTopics(
                    item.getQuestion(),
                    item.getAnswer(),
                    item.getSemanticText()
            );
            matches.add(new MessageKnowledgeMatch(
                    toEntry(item),
                    semanticMatch.score(),
                    semanticMatch.scopeLevel(),
                    reasons,
                    new ArrayList<>(topics),
                    refinedReusableFacts(item, List.of())
            ));
        }
        return matches;
    }

    private static void addRefinedCandidates(
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
            if (!MessageKnowledgeItem.STATUS_ACTIVE.equals(candidate.getStatus())) {
                continue;
            }
            String key = refinedCandidateKey(candidate);
            if (seenKeys.add(key)) {
                target.add(candidate);
            }
        }
    }

    private static MessageKnowledgeEntry toEntry(MessageKnowledgeItem item) {
        MessageKnowledgeEntry entry = new MessageKnowledgeEntry();
        entry.setId(item.getId());
        entry.setStoreId(item.getStoreId());
        entry.setScopeType(item.getScopeType());
        entry.setScopeId(item.getScopeId());
        entry.setThreadId(item.getThreadId());
        entry.setRoomId(item.getRoomId());
        entry.setRoomNumber(item.getRoomNumber());
        entry.setRoomTypeId(item.getRoomTypeId());
        entry.setRoomTypeName(item.getRoomTypeName());
        entry.setChannelId(item.getChannelId());
        entry.setQuestion(item.getQuestion());
        entry.setAnswer(item.getAnswer());
        entry.setNormalizedText(refinedNormalizedText(item));
        entry.setNormalizedHash(item.getNormalizedAnswerHash());
        entry.setLanguage(item.getLanguage());
        entry.setStatus(item.getStatus());
        entry.setPiiRedactionStatus(item.getPiiRedactionStatus());
        entry.setSourceTimestamp(item.getLastSeenAt());
        return entry;
    }

    private Set<String> detectKnowledgeTopicKeys(String text) {
        Set<String> topicKeys = new HashSet<>();
        if (!hasText(text)) {
            return topicKeys;
        }
        for (String topic : topicService.detectTopics(text)) {
            String topicKey = normalizeTopicKey(topic);
            if (topicKey != null) {
                topicKeys.add(topicKey);
            }
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        if (containsWifiTopic(normalized)) {
            topicKeys.add("wifi");
        }
        return topicKeys;
    }

    private static boolean containsWifiTopic(String normalizedText) {
        if (!hasText(normalizedText)) {
            return false;
        }
        String compact = normalizedText.replaceAll("\\s+", "");
        if (normalizedText.contains("wifi")
                || normalizedText.contains("wi-fi")
                || normalizedText.contains("wi fi")
                || normalizedText.contains("wireless")
                || normalizedText.contains("internet")) {
            return true;
        }
        return compact.contains("无线")
                || compact.contains("無線")
                || compact.contains("网络")
                || compact.contains("網路")
                || compact.contains("ネット")
                || compact.contains("ワイファイ")
                || compact.contains("インターネット");
    }

    private static String normalizeTopicKey(String topic) {
        if (!hasText(topic)) {
            return null;
        }
        return topic.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasAnyTopicOverlap(Set<String> firstTopics, Set<String> secondTopics) {
        if (firstTopics == null || secondTopics == null || firstTopics.isEmpty() || secondTopics.isEmpty()) {
            return false;
        }
        for (String topic : firstTopics) {
            if (secondTopics.contains(topic)) {
                return true;
            }
        }
        return false;
    }

    private static String refinedNormalizedText(MessageKnowledgeItem item) {
        StringBuilder builder = new StringBuilder();
        appendText(builder, item.getNormalizedQuestion());
        appendText(builder, item.getNormalizedAnswer());
        appendText(builder, item.getTopic());
        return builder.toString();
    }

    private static String refinedCandidateKey(MessageKnowledgeItem item) {
        if (item.getId() != null) {
            return "item:" + item.getId();
        }
        return refinedAnswerKey(item);
    }

    private static String refinedAnswerKey(MessageKnowledgeItem item) {
        if (item.getNormalizedAnswerHash() != null && !item.getNormalizedAnswerHash().isBlank()) {
            return "answer:" + item.getNormalizedAnswerHash();
        }
        if (item.getAnswer() != null && !item.getAnswer().isBlank()) {
            return "answer-text:" + item.getAnswer().trim();
        }
        return "item-empty:" + item.getTopicHash();
    }

    private static List<String> refinedReusableFacts(
            MessageKnowledgeItem item,
            List<String> existingFacts
    ) {
        List<String> facts = new ArrayList<>();
        if (existingFacts != null) {
            for (String fact : existingFacts) {
                if (hasText(fact) && !facts.contains(fact)) {
                    facts.add(fact);
                }
            }
        }
        if (facts.isEmpty() && item != null && hasText(item.getAnswer())) {
            facts.add(item.getAnswer().trim());
        }
        return facts;
    }

    private static String candidateKey(MessageKnowledgeEntry candidate) {
        if (candidate.getId() != null) {
            return "id:" + candidate.getId();
        }
        return "source:"
                + candidate.getSourceGuestMessageId()
                + ":"
                + candidate.getSourceStaffMessageId()
                + ":"
                + candidate.getNormalizedHash();
    }

    private static boolean hasPolicyTopicMismatch(
            Set<String> queryTopics,
            Set<String> candidateTopics,
            boolean hasSharedTopic
    ) {
        boolean hasAnyPolicyTopic = (queryTopics != null && !queryTopics.isEmpty())
                || (candidateTopics != null && !candidateTopics.isEmpty());
        return hasAnyPolicyTopic && !hasSharedTopic;
    }

    private static String candidateCombinedText(MessageKnowledgeEntry candidate) {
        if (candidate == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendText(builder, candidate.getQuestion());
        appendText(builder, candidate.getAnswer());
        appendText(builder, candidate.getNormalizedText());
        return builder.toString();
    }

    private static void appendText(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value);
    }

    private static String resolveScopeLevel(
            MessageKnowledgeEntry candidate,
            SuMessagingThreadContext context
    ) {
        if (candidate == null || context == null) {
            return SuMessagingThreadContext.SCOPE_STORE;
        }
        if (context.getRoomId() != null && context.getRoomId().equals(candidate.getRoomId())) {
            return SuMessagingThreadContext.SCOPE_ROOM;
        }
        if (context.getRoomTypeId() != null && context.getRoomTypeId().equals(candidate.getRoomTypeId())) {
            return SuMessagingThreadContext.SCOPE_ROOM_TYPE;
        }
        if (context.getChannelId() != null
                && context.getChannelId().equals(candidate.getChannelId())
                && context.getBookingKey() != null
                && context.getBookingKey().equals(candidate.getBookingKey())) {
            return SuMessagingThreadContext.SCOPE_BOOKING;
        }
        return SuMessagingThreadContext.SCOPE_STORE;
    }

    private static double scopeBoost(String scopeLevel) {
        if (SuMessagingThreadContext.SCOPE_ROOM.equals(scopeLevel)) {
            return ROOM_SCOPE_BOOST;
        }
        if (SuMessagingThreadContext.SCOPE_ROOM_TYPE.equals(scopeLevel)) {
            return ROOM_TYPE_SCOPE_BOOST;
        }
        if (SuMessagingThreadContext.SCOPE_BOOKING.equals(scopeLevel)) {
            return BOOKING_SCOPE_BOOST;
        }
        return STORE_SCOPE_BOOST;
    }

    private static boolean isRecent(LocalDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }
        long ageDays = Math.abs(Duration.between(timestamp, LocalDateTime.now()).toDays());
        return ageDays <= RECENT_DAYS;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
