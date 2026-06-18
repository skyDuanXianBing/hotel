package server.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.entity.MessageKnowledgeTopic;
import server.demo.enums.SuMessagingSenderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MessageKnowledgeThreadExtractionOutputValidator {
    private static final int MAX_ITEMS = 8;
    private static final int MAX_TEXT_LENGTH = 1200;
    private static final int MAX_EVIDENCE_SUMMARY_LENGTH = 1600;
    private static final BigDecimal DEFAULT_MIN_CANDIDATE_CONFIDENCE =
            BigDecimal.valueOf(0.6000).setScale(4, RoundingMode.HALF_UP);
    private static final BigDecimal DEFAULT_MIN_ACTIVE_CONFIDENCE =
            BigDecimal.valueOf(0.8000).setScale(4, RoundingMode.HALF_UP);
    private static final List<String> LOW_QUALITY_PHRASES = List.of(
            "contact the front desk",
            "ask the front desk",
            "we will check",
            "we will confirm",
            "please wait",
            "later",
            "请联系前台",
            "请咨询前台",
            "我们会确认",
            "稍后回复",
            "確認します",
            "フロントに"
    );

    private final MessageKnowledgeThreadOutputSanitizer sanitizer;
    private final SuMessagingAiTextService textService;

    @Value("${messaging.knowledge.thread-extractor.min-candidate-confidence:0.6}")
    private double minCandidateConfidence;

    @Value("${messaging.knowledge.thread-extractor.min-active-confidence:0.8}")
    private double minActiveConfidence;

    @Value("${messaging.knowledge.thread-extractor.max-output-items:8}")
    private int maxOutputItems;

    public MessageKnowledgeThreadExtractionOutputValidator(
            MessageKnowledgeThreadOutputSanitizer sanitizer,
            SuMessagingAiTextService textService
    ) {
        this.sanitizer = sanitizer;
        this.textService = textService;
    }

    public MessageKnowledgeThreadValidationResult validate(
            MessageKnowledgeThreadParsedOutput parsedOutput,
            MessageKnowledgeThreadValidationContext context
    ) {
        if (parsedOutput == null) {
            throw new IllegalArgumentException("parsedOutput is required");
        }
        if (context == null) {
            throw new IllegalArgumentException("validation context is required");
        }

        List<MessageKnowledgeThreadValidatedItem> validItems = new ArrayList<>();
        List<MessageKnowledgeThreadRejectedItem> rejectedItems = new ArrayList<>();
        int limit = resolveMaxOutputItems();
        if (parsedOutput.items().size() > limit) {
            throw new IllegalArgumentException("Thread extraction output exceeds item limit");
        }

        Map<String, MessageKnowledgeTopic> activeTopics = buildActiveTopicMap(context.activeTopics());
        for (MessageKnowledgeThreadModelItem item : parsedOutput.items()) {
            ValidationAttempt attempt = validateItem(item, context, activeTopics);
            if (attempt.validItem() != null) {
                validItems.add(attempt.validItem());
            } else {
                rejectedItems.add(attempt.rejectedItem());
            }
        }
        return new MessageKnowledgeThreadValidationResult(validItems, rejectedItems);
    }

    private ValidationAttempt validateItem(
            MessageKnowledgeThreadModelItem item,
            MessageKnowledgeThreadValidationContext context,
            Map<String, MessageKnowledgeTopic> activeTopics
    ) {
        List<String> errors = new ArrayList<>();
        String clientItemId = normalizeText(item.clientItemId());
        String topicCode = MessageKnowledgeTopicDictionaryService.normalizeTopicCode(item.topicCode());
        MessageKnowledgeTopic topic = activeTopics.get(topicCode);
        if (topicCode == null) {
            errors.add("missing_topic");
        } else if (topic == null) {
            errors.add("inactive_or_unknown_topic");
        }

        BigDecimal confidence = normalizeConfidence(item.confidence());
        if (confidence.compareTo(resolveMinCandidateConfidence()) < 0) {
            errors.add("confidence_below_threshold");
        }

        String canonicalQuestion = normalizeText(item.canonicalQuestion());
        String canonicalAnswer = normalizeText(item.canonicalAnswer());
        String evidenceSummary = normalizeText(item.evidenceSummary());
        if (canonicalQuestion == null) {
            errors.add("missing_question");
        } else if (canonicalQuestion.length() > MAX_TEXT_LENGTH) {
            errors.add("question_too_long");
        }
        if (canonicalAnswer == null) {
            errors.add("missing_answer");
        } else if (canonicalAnswer.length() > MAX_TEXT_LENGTH) {
            errors.add("answer_too_long");
        }
        if (evidenceSummary != null && evidenceSummary.length() > MAX_EVIDENCE_SUMMARY_LENGTH) {
            errors.add("evidence_summary_too_long");
        }
        if (isLowQualityAnswer(canonicalAnswer)) {
            errors.add("low_quality_answer");
        }

        SourceValidation sourceValidation = validateSources(item.sourceMessageIds(), context);
        errors.addAll(sourceValidation.errors());

        SanitizedFields sanitizedFields = sanitizeFields(
                canonicalQuestion,
                canonicalAnswer,
                evidenceSummary,
                context.threadContext()
        );
        errors.addAll(sanitizedFields.errors());

        ScopeSelection scopeSelection = resolveScope(item.scopeType(), topic, context.threadContext());
        errors.addAll(scopeSelection.errors());

        if (!errors.isEmpty()) {
            return ValidationAttempt.rejected(new MessageKnowledgeThreadRejectedItem(
                    clientItemId,
                    item.sourceMessageIds(),
                    firstErrorCode(errors),
                    String.join(",", errors)
            ));
        }

        String normalizedQuestion = textService.normalize(sanitizedFields.question());
        String normalizedAnswer = textService.normalize(sanitizedFields.answer());
        if (normalizedQuestion == null || normalizedQuestion.isBlank()) {
            return ValidationAttempt.rejected(new MessageKnowledgeThreadRejectedItem(
                    clientItemId,
                    item.sourceMessageIds(),
                    "normalized_question_blank",
                    "normalized_question_blank"
            ));
        }
        if (normalizedAnswer == null || normalizedAnswer.isBlank()) {
            return ValidationAttempt.rejected(new MessageKnowledgeThreadRejectedItem(
                    clientItemId,
                    item.sourceMessageIds(),
                    "normalized_answer_blank",
                    "normalized_answer_blank"
            ));
        }

        String status = resolveItemStatus(confidence, sanitizedFields.generalized());
        String language = normalizeLanguage(item.language(), sanitizedFields.question(), sanitizedFields.answer());
        String normalizedEvidence = textService.normalize(joinText(
                sanitizedFields.question(),
                sanitizedFields.answer(),
                sanitizedFields.evidenceSummary()
        ));

        return ValidationAttempt.valid(new MessageKnowledgeThreadValidatedItem(
                clientItemId,
                topicCode,
                topic.getDisplayName(),
                scopeSelection.scopeType(),
                scopeSelection.scopeId(),
                scopeSelection.scopeKey(),
                sanitizedFields.question(),
                sanitizedFields.answer(),
                normalizedQuestion,
                normalizedAnswer,
                normalizedEvidence,
                language,
                confidence,
                status,
                sourceValidation.sourceMessageIds(),
                sourceValidation.sourceMessages(),
                sourceValidation.sourceTimestamp(),
                sanitizedFields.evidenceSummary(),
                sanitizedFields.generalized()
        ));
    }

    private SourceValidation validateSources(
            List<Long> sourceMessageIds,
            MessageKnowledgeThreadValidationContext context
    ) {
        List<String> errors = new ArrayList<>();
        if (sourceMessageIds == null || sourceMessageIds.isEmpty()) {
            errors.add("missing_source_ids");
            return new SourceValidation(List.of(), List.of(), null, errors);
        }

        List<MessageKnowledgeThreadConversationMessage> sourceMessages = new ArrayList<>();
        List<Long> normalizedIds = new ArrayList<>();
        boolean hasEligibleStaff = false;
        LocalDateTime sourceTimestamp = null;
        for (Long sourceMessageId : sourceMessageIds) {
            if (sourceMessageId == null) {
                errors.add("null_source_id");
                continue;
            }
            MessageKnowledgeThreadConversationMessage message =
                    context.inputMessagesById().get(sourceMessageId);
            if (message == null) {
                errors.add("source_id_not_in_input");
                continue;
            }
            if (!message.eligibleAsEvidence()) {
                errors.add("source_message_not_eligible");
                continue;
            }
            normalizedIds.add(sourceMessageId);
            sourceMessages.add(message);
            if (message.senderType() == SuMessagingSenderType.STAFF) {
                hasEligibleStaff = true;
                sourceTimestamp = maxTime(sourceTimestamp, message.sentAt());
            }
        }
        if (!hasEligibleStaff) {
            errors.add("missing_eligible_staff_source");
        }
        if (sourceTimestamp == null) {
            sourceTimestamp = latestMessageTime(sourceMessages);
        }
        return new SourceValidation(normalizedIds, sourceMessages, sourceTimestamp, errors);
    }

    private SanitizedFields sanitizeFields(
            String question,
            String answer,
            String evidenceSummary,
            SuMessagingThreadContext context
    ) {
        List<String> errors = new ArrayList<>();
        MessageKnowledgeThreadSanitizedText sanitizedQuestion = sanitizer.sanitize(
                new MessageKnowledgeThreadSanitizerRequest(question, context)
        );
        MessageKnowledgeThreadSanitizedText sanitizedAnswer = sanitizer.sanitize(
                new MessageKnowledgeThreadSanitizerRequest(answer, context)
        );
        MessageKnowledgeThreadSanitizedText sanitizedEvidenceSummary = sanitizeOptional(evidenceSummary, context);
        addSanitizerErrors(errors, "question", sanitizedQuestion);
        addSanitizerErrors(errors, "answer", sanitizedAnswer);
        addSanitizerErrors(errors, "evidence_summary", sanitizedEvidenceSummary);
        boolean generalized = sanitizedQuestion.generalized()
                || sanitizedAnswer.generalized()
                || sanitizedEvidenceSummary.generalized();
        return new SanitizedFields(
                sanitizedQuestion.text(),
                sanitizedAnswer.text(),
                sanitizedEvidenceSummary.text(),
                generalized,
                errors
        );
    }

    private MessageKnowledgeThreadSanitizedText sanitizeOptional(
            String value,
            SuMessagingThreadContext context
    ) {
        if (value == null || value.isBlank()) {
            return new MessageKnowledgeThreadSanitizedText(null, false, false, List.of());
        }
        return sanitizer.sanitize(new MessageKnowledgeThreadSanitizerRequest(value, context));
    }

    private static void addSanitizerErrors(
            List<String> errors,
            String fieldName,
            MessageKnowledgeThreadSanitizedText sanitizedText
    ) {
        if (sanitizedText == null || !sanitizedText.rejected()) {
            return;
        }
        for (String reasonCode : sanitizedText.reasonCodes()) {
            errors.add(fieldName + "_" + reasonCode);
        }
    }

    private ScopeSelection resolveScope(
            String requestedScopeType,
            MessageKnowledgeTopic topic,
            SuMessagingThreadContext context
    ) {
        List<String> errors = new ArrayList<>();
        String scopeType = normalizeScopeType(requestedScopeType);
        if (scopeType == null && topic != null) {
            scopeType = normalizeScopeType(topic.getScopePreference());
        }
        if (scopeType == null) {
            scopeType = bestReusableScopeType(context);
        }
        if (SuMessagingThreadContext.SCOPE_BOOKING.equals(scopeType)) {
            errors.add("booking_scope_not_reusable");
            return new ScopeSelection(null, null, null, errors);
        }
        if (SuMessagingThreadContext.SCOPE_ROOM.equals(scopeType)) {
            if (context != null && context.getRoomId() != null) {
                return new ScopeSelection(scopeType, context.getRoomId(), "ROOM:" + context.getRoomId(), errors);
            }
            errors.add("room_scope_unavailable");
            return new ScopeSelection(null, null, null, errors);
        }
        if (SuMessagingThreadContext.SCOPE_ROOM_TYPE.equals(scopeType)) {
            if (context != null && context.getRoomTypeId() != null) {
                return new ScopeSelection(
                        scopeType,
                        context.getRoomTypeId(),
                        "ROOM_TYPE:" + context.getRoomTypeId(),
                        errors
                );
            }
            errors.add("room_type_scope_unavailable");
            return new ScopeSelection(null, null, null, errors);
        }
        if (SuMessagingThreadContext.SCOPE_STORE.equals(scopeType)) {
            return new ScopeSelection(scopeType, null, "STORE", errors);
        }
        errors.add("invalid_scope");
        return new ScopeSelection(null, null, null, errors);
    }

    private String resolveItemStatus(BigDecimal confidence, boolean generalizedOutput) {
        if (confidence.compareTo(resolveMinActiveConfidence()) >= 0 && !generalizedOutput) {
            return MessageKnowledgeItem.STATUS_ACTIVE;
        }
        return MessageKnowledgeItem.STATUS_CANDIDATE;
    }

    private BigDecimal resolveMinCandidateConfidence() {
        if (minCandidateConfidence < 0.0 || minCandidateConfidence > 1.0) {
            return DEFAULT_MIN_CANDIDATE_CONFIDENCE;
        }
        return BigDecimal.valueOf(minCandidateConfidence).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveMinActiveConfidence() {
        if (minActiveConfidence < 0.0 || minActiveConfidence > 1.0) {
            return DEFAULT_MIN_ACTIVE_CONFIDENCE;
        }
        return BigDecimal.valueOf(minActiveConfidence).setScale(4, RoundingMode.HALF_UP);
    }

    private int resolveMaxOutputItems() {
        if (maxOutputItems < 1) {
            return MAX_ITEMS;
        }
        return Math.min(maxOutputItems, MAX_ITEMS);
    }

    private static Map<String, MessageKnowledgeTopic> buildActiveTopicMap(List<MessageKnowledgeTopic> topics) {
        Map<String, MessageKnowledgeTopic> activeTopics = new LinkedHashMap<>();
        if (topics == null) {
            return activeTopics;
        }
        for (MessageKnowledgeTopic topic : topics) {
            if (topic == null || topic.getTopicCode() == null) {
                continue;
            }
            if (!MessageKnowledgeTopic.STATUS_ACTIVE.equals(topic.getStatus())) {
                continue;
            }
            String topicCode = MessageKnowledgeTopicDictionaryService.normalizeTopicCode(topic.getTopicCode());
            if (topicCode != null) {
                activeTopics.put(topicCode, topic);
            }
        }
        return activeTopics;
    }

    private static String bestReusableScopeType(SuMessagingThreadContext context) {
        if (context == null) {
            return SuMessagingThreadContext.SCOPE_STORE;
        }
        String scopeType = context.getBestScopeType();
        if (SuMessagingThreadContext.SCOPE_BOOKING.equals(scopeType)) {
            return SuMessagingThreadContext.SCOPE_STORE;
        }
        return scopeType;
    }

    private static String normalizeScopeType(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("ROOMTYPE".equals(normalized)) {
            return SuMessagingThreadContext.SCOPE_ROOM_TYPE;
        }
        if ("ROOM_TYPE".equals(normalized)) {
            return SuMessagingThreadContext.SCOPE_ROOM_TYPE;
        }
        if ("ROOM".equals(normalized)) {
            return SuMessagingThreadContext.SCOPE_ROOM;
        }
        if ("STORE".equals(normalized)) {
            return SuMessagingThreadContext.SCOPE_STORE;
        }
        if ("BOOKING".equals(normalized) || "RESERVATION".equals(normalized)) {
            return SuMessagingThreadContext.SCOPE_BOOKING;
        }
        return null;
    }

    private static BigDecimal normalizeConfidence(BigDecimal value) {
        if (value == null) {
            return BigDecimal.valueOf(0.7000).setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal zero = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal one = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        BigDecimal scaled = value.setScale(4, RoundingMode.HALF_UP);
        if (scaled.compareTo(zero) < 0) {
            return zero;
        }
        if (scaled.compareTo(one) > 0) {
            return one;
        }
        return scaled;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized;
    }

    private static boolean isLowQualityAnswer(String answer) {
        if (answer == null) {
            return false;
        }
        String lowerAnswer = answer.toLowerCase(Locale.ROOT);
        for (String phrase : LOW_QUALITY_PHRASES) {
            if (lowerAnswer.contains(phrase.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeLanguage(
            String language,
            String question,
            String answer
    ) {
        String normalized = normalizeText(language);
        if (normalized != null) {
            return normalized.toLowerCase(Locale.ROOT);
        }
        String combined = joinText(question, answer);
        if (combined.matches(".*[\\u4e00-\\u9fff].*")) {
            return "zh";
        }
        if (combined.matches(".*[\\u3040-\\u30ff].*")) {
            return "ja";
        }
        return "en";
    }

    private static String joinText(String... values) {
        List<String> parts = new ArrayList<>();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) {
                parts.add(normalized);
            }
        }
        return String.join(" ", parts);
    }

    private static LocalDateTime maxTime(LocalDateTime current, LocalDateTime candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
    }

    private static LocalDateTime latestMessageTime(List<MessageKnowledgeThreadConversationMessage> messages) {
        LocalDateTime latest = null;
        for (MessageKnowledgeThreadConversationMessage message : messages) {
            if (message != null) {
                latest = maxTime(latest, message.sentAt());
            }
        }
        return latest;
    }

    private static String firstErrorCode(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "invalid_item";
        }
        return errors.get(0);
    }

    private record SourceValidation(
            List<Long> sourceMessageIds,
            List<MessageKnowledgeThreadConversationMessage> sourceMessages,
            LocalDateTime sourceTimestamp,
            List<String> errors
    ) {
    }

    private record SanitizedFields(
            String question,
            String answer,
            String evidenceSummary,
            boolean generalized,
            List<String> errors
    ) {
    }

    private record ScopeSelection(
            String scopeType,
            Long scopeId,
            String scopeKey,
            List<String> errors
    ) {
    }

    private record ValidationAttempt(
            MessageKnowledgeThreadValidatedItem validItem,
            MessageKnowledgeThreadRejectedItem rejectedItem
    ) {
        static ValidationAttempt valid(MessageKnowledgeThreadValidatedItem item) {
            return new ValidationAttempt(item, null);
        }

        static ValidationAttempt rejected(MessageKnowledgeThreadRejectedItem item) {
            return new ValidationAttempt(null, item);
        }
    }
}
