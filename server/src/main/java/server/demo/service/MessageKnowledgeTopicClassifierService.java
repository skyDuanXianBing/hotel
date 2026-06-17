package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeTopic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MessageKnowledgeTopicClassifierService {
    private static final Logger logger = LoggerFactory.getLogger(MessageKnowledgeTopicClassifierService.class);

    public static final String ACTION_USE_EXISTING = "USE_EXISTING";
    public static final String ACTION_CREATE_NEW = "CREATE_NEW";
    public static final String ACTION_NEEDS_REVIEW = "NEEDS_REVIEW";
    public static final String ACTION_FALLBACK = "FALLBACK";
    public static final String TOPIC_GENERAL_CANDIDATE = "general_candidate";

    private static final double DEFAULT_MIN_CONFIDENCE = 0.8;
    private static final int DEFAULT_MAX_TOPICS_PER_STORE = 50;
    private static final int MAX_PROMPT_TEXT_LENGTH = 700;
    private static final int MAX_REASON_LENGTH = 180;
    private static final long CALL_WINDOW_MS = 60_000L;
    private static final List<String> UNSAFE_CREATE_NEW_PHRASES = List.of(
            "booking number",
            "reservation number",
            "confirmation number",
            "order number",
            "guest name",
            "room number",
            "temporary code",
            "one-time code",
            "one time code",
            "临时代码",
            "一次性代码",
            "订单号",
            "预订号",
            "預訂號",
            "房号",
            "房號",
            "客人姓名"
    );

    private final MessageKnowledgeTopicDictionaryService dictionaryService;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final SuMessagingAiRedactor redactor;
    private final AtomicInteger modelCallsInWindow = new AtomicInteger(0);

    private volatile long modelCallWindowStartedAtMs;

    @Value("${messaging.knowledge.topic-classifier.enabled:false}")
    private boolean enabled;

    @Value("${messaging.knowledge.topic-classifier.min-confidence:0.8}")
    private double minConfidence;

    @Value("${messaging.knowledge.topic-classifier.max-topics-per-store:50}")
    private int maxTopicsPerStore;

    @Value("${messaging.knowledge.topic-classifier.max-model-calls-per-run:0}")
    private int maxModelCallsPerRun;

    @Value("${messaging.knowledge.topic-classifier.auto-create-enabled:false}")
    private boolean autoCreateEnabled;

    public MessageKnowledgeTopicClassifierService(
            MessageKnowledgeTopicDictionaryService dictionaryService,
            ChatLanguageModel chatLanguageModel,
            ObjectMapper objectMapper,
            SuMessagingAiRedactor redactor
    ) {
        this.dictionaryService = dictionaryService;
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
        this.redactor = redactor;
    }

    public TopicClassification classify(
            Long storeId,
            String normalizedQuestion,
            String normalizedAnswer
    ) {
        if (storeId == null || !hasText(normalizedQuestion) || !hasText(normalizedAnswer)) {
            return TopicClassification.fallback("missing_input");
        }

        List<MessageKnowledgeTopic> activeTopics = dictionaryService.findActiveTopics(
                storeId,
                resolveMaxTopicsPerStore()
        );
        if (!enabled) {
            return TopicClassification.fallback("classifier_disabled");
        }
        if (activeTopics.isEmpty()) {
            return TopicClassification.fallback("empty_dictionary");
        }
        if (!reserveModelCall()) {
            return TopicClassification.fallback("model_call_budget_exhausted");
        }

        try {
            String prompt = buildPrompt(normalizedQuestion, normalizedAnswer, activeTopics);
            String output = chatLanguageModel.generate(prompt);
            ModelDecision decision = parseDecision(output);
            return applyDecision(storeId, activeTopics, normalizedQuestion, normalizedAnswer, decision);
        } catch (RuntimeException e) {
            logger.warn("AI message knowledge topic classification failed. storeId={}, error={}",
                    storeId, e.getMessage(), e);
            return TopicClassification.fallback("model_failed");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private TopicClassification applyDecision(
            Long storeId,
            List<MessageKnowledgeTopic> activeTopics,
            String normalizedQuestion,
            String normalizedAnswer,
            ModelDecision decision
    ) {
        String action = normalizeAction(decision.action());
        BigDecimal confidence = normalizeConfidence(decision.confidence());
        if (confidence.doubleValue() < resolveMinConfidence()) {
            return TopicClassification.needsReview(
                    TOPIC_GENERAL_CANDIDATE,
                    confidence,
                    firstNonBlank(decision.reason(), "classifier_confidence_below_threshold")
            );
        }

        if (ACTION_USE_EXISTING.equals(action)) {
            return applyUseExisting(storeId, activeTopics, decision, confidence);
        }
        if (ACTION_CREATE_NEW.equals(action)) {
            return applyCreateNew(storeId, activeTopics, normalizedQuestion, normalizedAnswer, decision, confidence);
        }
        if (ACTION_NEEDS_REVIEW.equals(action)) {
            return TopicClassification.needsReview(
                    TOPIC_GENERAL_CANDIDATE,
                    confidence,
                    firstNonBlank(decision.reason(), "classifier_requested_review")
            );
        }
        return TopicClassification.fallback("unknown_classifier_action");
    }

    private TopicClassification applyUseExisting(
            Long storeId,
            List<MessageKnowledgeTopic> activeTopics,
            ModelDecision decision,
            BigDecimal confidence
    ) {
        String topicCode = MessageKnowledgeTopicDictionaryService.normalizeTopicCode(decision.topicCode());
        if (topicCode == null) {
            return TopicClassification.needsReview(
                    TOPIC_GENERAL_CANDIDATE,
                    confidence,
                    "missing_topic_code"
            );
        }
        if (!containsActiveTopic(activeTopics, topicCode)
                && dictionaryService.findActiveTopic(storeId, topicCode).isEmpty()) {
            return TopicClassification.needsReview(
                    TOPIC_GENERAL_CANDIDATE,
                    confidence,
                    "topic_code_not_in_dictionary"
            );
        }
        return TopicClassification.useExisting(
                topicCode,
                confidence,
                firstNonBlank(decision.reason(), "matched_existing_topic")
        );
    }

    private TopicClassification applyCreateNew(
            Long storeId,
            List<MessageKnowledgeTopic> activeTopics,
            String normalizedQuestion,
            String normalizedAnswer,
            ModelDecision decision,
            BigDecimal confidence
    ) {
        if (containsUnsafeCreateNewSource(normalizedQuestion, normalizedAnswer)) {
            return TopicClassification.needsReview(
                    TOPIC_GENERAL_CANDIDATE,
                    confidence,
                    "unsafe_create_new_source"
            );
        }

        if (autoCreateEnabled && activeTopics.size() < resolveMaxTopicsPerStore()) {
            dictionaryService.createNeedsReviewCandidate(
                    storeId,
                    decision.topicCode(),
                    decision.displayName(),
                    decision.description(),
                    confidence
            );
        }
        return TopicClassification.needsReview(
                TOPIC_GENERAL_CANDIDATE,
                confidence,
                firstNonBlank(decision.reason(), "create_new_requires_review")
        );
    }

    private String buildPrompt(
            String normalizedQuestion,
            String normalizedAnswer,
            List<MessageKnowledgeTopic> activeTopics
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Classify a hotel reusable knowledge topic.\n");
        prompt.append("Return strict JSON only. Shape: ");
        prompt.append("{\"action\":\"USE_EXISTING|CREATE_NEW|NEEDS_REVIEW\",");
        prompt.append("\"topicCode\":\"wifi\",\"confidence\":0.0,");
        prompt.append("\"displayName\":\"short\",\"description\":\"short\",\"reason\":\"short\"}.\n");
        prompt.append("Use USE_EXISTING only when one existing topic clearly matches.\n");
        prompt.append("Use CREATE_NEW only for reusable, non-private, stable hotel policy topics.\n");
        prompt.append("Use NEEDS_REVIEW for uncertain, temporary, private, order-specific, room-specific guest data.\n\n");
        prompt.append("Existing topics:\n");
        for (MessageKnowledgeTopic topic : activeTopics) {
            prompt.append("- topicCode: ").append(redact(topic.getTopicCode())).append("\n");
            prompt.append("  displayName: ").append(redact(topic.getDisplayName())).append("\n");
            appendPromptLine(prompt, "description", topic.getDescription());
            appendPromptLine(prompt, "aliasesJson", topic.getAliasesJson());
            appendPromptLine(prompt, "exampleQuestionsJson", topic.getExampleQuestionsJson());
            appendPromptLine(prompt, "exampleAnswersJson", topic.getExampleAnswersJson());
            appendPromptLine(prompt, "scopePreference", topic.getScopePreference());
        }
        prompt.append("\nGuest question: ").append(redact(truncate(normalizedQuestion, MAX_PROMPT_TEXT_LENGTH))).append("\n");
        prompt.append("Staff answer: ").append(redact(truncate(normalizedAnswer, MAX_PROMPT_TEXT_LENGTH))).append("\n");
        return prompt.toString();
    }

    private void appendPromptLine(StringBuilder prompt, String label, String value) {
        if (!hasText(value)) {
            return;
        }
        prompt.append("  ").append(label).append(": ").append(redact(truncate(value, MAX_PROMPT_TEXT_LENGTH))).append("\n");
    }

    private ModelDecision parseDecision(String output) {
        String json = extractJson(output);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new IllegalArgumentException("Topic classifier output must be a JSON object");
            }
            return new ModelDecision(
                    readText(root, "action"),
                    firstNonBlank(readText(root, "topicCode"), readText(root, "topic_code")),
                    readConfidence(root),
                    readText(root, "displayName"),
                    readText(root, "description"),
                    readText(root, "reason")
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid topic classifier JSON", e);
        }
    }

    private String extractJson(String output) {
        if (!hasText(output)) {
            throw new IllegalArgumentException("Topic classifier output is blank");
        }
        String trimmed = output.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Topic classifier output does not contain JSON");
        }
        return trimmed.substring(start, end + 1);
    }

    private static String readText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static BigDecimal readConfidence(JsonNode node) {
        JsonNode confidence = node.get("confidence");
        if (confidence == null || confidence.isNull()) {
            return BigDecimal.ZERO;
        }
        if (confidence.isNumber()) {
            return BigDecimal.valueOf(confidence.asDouble());
        }
        if (confidence.isTextual()) {
            try {
                return new BigDecimal(confidence.asText().trim());
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean reserveModelCall() {
        int maxCalls = resolveMaxModelCallsPerRun();
        if (maxCalls < 1) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (modelCallWindowStartedAtMs == 0L || now - modelCallWindowStartedAtMs >= CALL_WINDOW_MS) {
                modelCallWindowStartedAtMs = now;
                modelCallsInWindow.set(0);
            }
            if (modelCallsInWindow.get() >= maxCalls) {
                return false;
            }
            modelCallsInWindow.incrementAndGet();
            return true;
        }
    }

    private int resolveMaxTopicsPerStore() {
        if (maxTopicsPerStore < 1) {
            return DEFAULT_MAX_TOPICS_PER_STORE;
        }
        return Math.min(maxTopicsPerStore, DEFAULT_MAX_TOPICS_PER_STORE);
    }

    private int resolveMaxModelCallsPerRun() {
        return Math.max(0, maxModelCallsPerRun);
    }

    private double resolveMinConfidence() {
        if (minConfidence < 0.0 || minConfidence > 1.0) {
            return DEFAULT_MIN_CONFIDENCE;
        }
        return minConfidence;
    }

    private static BigDecimal normalizeConfidence(BigDecimal value) {
        BigDecimal zero = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal one = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        if (value == null) {
            return zero;
        }
        BigDecimal scaled = value.setScale(4, RoundingMode.HALF_UP);
        if (scaled.compareTo(zero) < 0) {
            return zero;
        }
        if (scaled.compareTo(one) > 0) {
            return one;
        }
        return scaled;
    }

    private static boolean containsActiveTopic(List<MessageKnowledgeTopic> activeTopics, String topicCode) {
        if (activeTopics == null || topicCode == null) {
            return false;
        }
        for (MessageKnowledgeTopic topic : activeTopics) {
            if (topic == null) {
                continue;
            }
            if (topicCode.equals(topic.getTopicCode())
                    && MessageKnowledgeTopic.STATUS_ACTIVE.equals(topic.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsUnsafeCreateNewSource(String normalizedQuestion, String normalizedAnswer) {
        String combined = joinText(normalizedQuestion, normalizedAnswer).toLowerCase(Locale.ROOT);
        if (!hasText(combined)) {
            return true;
        }
        for (String phrase : UNSAFE_CREATE_NEW_PHRASES) {
            if (combined.contains(phrase.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeAction(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String redact(String value) {
        return redactor.redact(value == null ? "" : value);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return truncate(first.trim(), MAX_REASON_LENGTH);
        }
        if (second == null) {
            return null;
        }
        return truncate(second.trim(), MAX_REASON_LENGTH);
    }

    private static String joinText(String first, String second) {
        Set<String> values = new LinkedHashSet<>();
        if (hasText(first)) {
            values.add(first);
        }
        if (hasText(second)) {
            values.add(second);
        }
        return String.join(" ", values);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record TopicClassification(
            String action,
            String topicCode,
            BigDecimal confidence,
            String reason
    ) {
        public boolean useExisting() {
            return ACTION_USE_EXISTING.equals(action) && hasText(topicCode);
        }

        public boolean needsReview() {
            return ACTION_NEEDS_REVIEW.equals(action);
        }

        public static TopicClassification useExisting(
                String topicCode,
                BigDecimal confidence,
                String reason
        ) {
            return new TopicClassification(ACTION_USE_EXISTING, topicCode, confidence, reason);
        }

        public static TopicClassification needsReview(
                String topicCode,
                BigDecimal confidence,
                String reason
        ) {
            return new TopicClassification(ACTION_NEEDS_REVIEW, topicCode, confidence, reason);
        }

        public static TopicClassification fallback(String reason) {
            return new TopicClassification(ACTION_FALLBACK, null, BigDecimal.ZERO, reason);
        }
    }

    private record ModelDecision(
            String action,
            String topicCode,
            BigDecimal confidence,
            String displayName,
            String description,
            String reason
    ) {
    }
}
