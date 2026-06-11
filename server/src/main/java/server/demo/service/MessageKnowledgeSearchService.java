package server.demo.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeEntry;
import server.demo.repository.MessageKnowledgeEntryRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MessageKnowledgeSearchService {
    public static final String STATUS_MATCHED = "MATCHED";
    public static final String STATUS_NO_MATCH = "NO_MATCH";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_FAILED = "FAILED";

    static final String WARNING_NO_SIMILAR_HISTORY = "NO_SIMILAR_HISTORY";
    static final String WARNING_LOW_CONFIDENCE_MATCH = "LOW_CONFIDENCE_MATCH";

    private static final int MAX_CANDIDATES_PER_SCOPE = 60;
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

    public MessageKnowledgeSearchService(
            MessageKnowledgeEntryRepository knowledgeEntryRepository,
            SuMessagingAiTextService textService,
            MessageKnowledgeTopicService topicService
    ) {
        this.knowledgeEntryRepository = knowledgeEntryRepository;
        this.textService = textService;
        this.topicService = topicService;
    }

    public MessageKnowledgeSearchResult searchSimilar(
            Long storeId,
            Long currentThreadId,
            SuMessagingThreadContext context,
            String queryText,
            int limit
    ) {
        int normalizedLimit = Math.max(1, Math.min(limit, MAX_MATCHES));
        List<String> queryTokens = textService.tokenize(queryText);
        if (storeId == null || queryTokens.isEmpty()) {
            return new MessageKnowledgeSearchResult(
                    STATUS_NO_MATCH,
                    List.of(),
                    List.of(WARNING_NO_SIMILAR_HISTORY)
            );
        }

        List<MessageKnowledgeEntry> candidates = loadCandidates(storeId, currentThreadId, context);
        if (candidates.isEmpty()) {
            return new MessageKnowledgeSearchResult(
                    STATUS_NO_MATCH,
                    List.of(),
                    List.of(WARNING_NO_SIMILAR_HISTORY)
            );
        }

        Set<String> queryTopics = topicService.detectTopics(queryText);
        Set<String> queryFacts = topicService.extractFactTokens(queryText);
        List<MessageKnowledgeMatch> matches = new ArrayList<>();
        for (MessageKnowledgeEntry candidate : candidates) {
            MessageKnowledgeMatch match = scoreCandidate(candidate, context, queryTokens, queryTopics, queryFacts);
            if (match != null && match.getScore() >= PARTIAL_THRESHOLD) {
                matches.add(match);
            }
        }

        matches.sort(Comparator.comparingDouble(MessageKnowledgeMatch::getScore).reversed());
        if (matches.size() > normalizedLimit) {
            matches = new ArrayList<>(matches.subList(0, normalizedLimit));
        }

        if (matches.isEmpty()) {
            return new MessageKnowledgeSearchResult(
                    STATUS_NO_MATCH,
                    List.of(),
                    List.of(WARNING_NO_SIMILAR_HISTORY)
            );
        }

        double bestScore = matches.get(0).getScore();
        if (bestScore >= MATCH_THRESHOLD) {
            return new MessageKnowledgeSearchResult(STATUS_MATCHED, matches, List.of());
        }

        return new MessageKnowledgeSearchResult(
                STATUS_PARTIAL,
                matches,
                List.of(WARNING_LOW_CONFIDENCE_MATCH)
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
}
