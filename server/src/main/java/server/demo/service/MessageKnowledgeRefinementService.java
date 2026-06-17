package server.demo.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeEvidence;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.MessageKnowledgeEvidenceRepository;
import server.demo.repository.MessageKnowledgeItemRepository;
import server.demo.util.UtcTimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageKnowledgeRefinementService {
    private static final String DELIVERY_STATUS_FAILED = "FAILED";
    private static final String DELIVERY_STATUS_SENDING = "SENDING";
    private static final int MAX_TOPIC_LENGTH = 120;
    private static final int MAX_GENERAL_TOPIC_TOKENS = 6;
    private static final int MAX_SCOPE_KEY_LENGTH = 80;
    private static final double ROOM_CONFIDENCE = 0.78;
    private static final double ROOM_TYPE_CONFIDENCE = 0.72;
    private static final double STORE_CONFIDENCE = 0.62;
    private static final String TOPIC_WIFI = "wifi";
    private static final String TOPIC_BREAKFAST = "breakfast";
    private static final String TOPIC_PET_POLICY = "pet_policy";
    private static final String TOPIC_SHUTTLE = "shuttle";
    private static final Pattern WIFI_PASSWORD_PATTERN = Pattern.compile(
            "(?iu)(?:password|passcode|密码|密碼|パスワード|暗証番号)\\s*(?:is|:|：|=|为|是|為|は)?\\s*([A-Za-z0-9][A-Za-z0-9_@#%&*+\\-.]{2,63})"
    );
    private static final Pattern WIFI_SSID_PATTERN = Pattern.compile(
            "(?iu)(?:ssid|network\\s+name|wifi\\s+name|wi\\s+fi\\s+name|wi-fi\\s+name|无线名称|無線名稱|网络名称|網路名稱|ネットワーク名)\\s*(?:is|:|：|=|为|是|為|は)?\\s*([\\p{L}\\p{N}][\\p{L}\\p{N}_\\- ]{1,80}?)(?=\\s*(?:\\b(?:and\\s+)?(?:password|passcode)\\b|密码|密碼|パスワード|暗証番号|[,，.;；。、]|$))"
    );
    private static final Pattern TIME_VALUE_PATTERN = Pattern.compile(
            "(?i)\\b\\d{1,2}\\s*[:：]\\s*\\d{2}(?:\\s*(?:-|~|–|—|to|至|到)\\s*\\d{1,2}\\s*[:：]\\s*\\d{2})?\\b"
    );
    private static final Pattern MONEY_VALUE_PATTERN = Pattern.compile(
            "(?i)(?:¥\\s*\\d{1,6}|\\d{1,6}\\s*(?:jpy|yen|円|日元)|(?:jpy|yen)\\s*\\d{1,6})"
    );
    private static final List<String> REDACTION_MARKERS = List.of(
            "[redacted-url]",
            "[redacted-email]",
            "[redacted-phone]",
            "[redacted-number]"
    );
    private static final List<String> PRIVATE_KEYWORDS = List.of(
            "passport",
            "credit card",
            "card number",
            "payment card",
            "booking number",
            "reservation number",
            "confirmation number",
            "order number",
            "guest name",
            "room number",
            "temporary code",
            "one-time code",
            "one time code",
            "phone number",
            "email address",
            "护照",
            "信用卡",
            "预订号",
            "订单号",
            "房号",
            "临时代码",
            "一次性代码",
            "客人姓名",
            "手机号",
            "邮箱",
            "パスポート",
            "予約番号",
            "電話番号",
            "メール"
    );

    private final MessageKnowledgeItemRepository itemRepository;
    private final MessageKnowledgeEvidenceRepository evidenceRepository;
    private final SuMessagingThreadContextResolver contextResolver;
    private final SuMessagingAiRedactor redactor;
    private final SuMessagingAiTextService textService;
    private final MessageKnowledgeEmbeddingTextService embeddingTextService;
    private MessageKnowledgeTopicClassifierService topicClassifierService;

    @Value("${messaging.knowledge.refinement.max-model-calls:0}")
    private int maxModelCalls;

    public MessageKnowledgeRefinementService(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEvidenceRepository evidenceRepository,
            SuMessagingThreadContextResolver contextResolver,
            SuMessagingAiRedactor redactor,
            SuMessagingAiTextService textService,
            MessageKnowledgeEmbeddingTextService embeddingTextService
    ) {
        this.itemRepository = itemRepository;
        this.evidenceRepository = evidenceRepository;
        this.contextResolver = contextResolver;
        this.redactor = redactor;
        this.textService = textService;
        this.embeddingTextService = embeddingTextService;
    }

    @Autowired(required = false)
    public void setTopicClassifierService(MessageKnowledgeTopicClassifierService topicClassifierService) {
        this.topicClassifierService = topicClassifierService;
    }

    @Transactional
    public boolean refineSourcePair(
            Long storeId,
            SuMessageThread thread,
            SuMessage guestMessage,
            SuMessage staffMessage
    ) {
        if (!isValidSourcePair(storeId, thread, guestMessage, staffMessage)) {
            return false;
        }

        boolean evidenceExists = evidenceRepository
                .findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageId(
                        storeId,
                        guestMessage.getId(),
                        staffMessage.getId()
                )
                .isPresent();
        if (evidenceExists) {
            return false;
        }

        SuMessagingThreadContext context = contextResolver.resolveForIndex(storeId, thread);
        if (!isReusableContext(context)) {
            return false;
        }

        String question = redactor.redact(guestMessage.getContent());
        String answer = redactor.redact(staffMessage.getContent());
        if (!hasText(question) || !hasText(answer)) {
            return false;
        }
        if (containsPrivateOrRedactedContent(question, answer)) {
            return false;
        }

        String normalizedQuestion = textService.normalize(question);
        String normalizedAnswer = textService.normalize(answer);
        if (!hasText(normalizedQuestion) || !hasText(normalizedAnswer)) {
            return false;
        }

        LocalDateTime sourceSeenAt = resolveSeenAt(staffMessage.getSentAt());
        LocalDateTime refinedAt = UtcTimeUtil.nowLocalDateTime();
        String scopeType = resolveReusableScopeType(context);
        Long scopeId = resolveScopeId(scopeType, context);
        String scopeKey = buildScopeKey(scopeType, scopeId);
        TopicSelection topicSelection = determineTopic(storeId, normalizedQuestion, normalizedAnswer);
        if (topicSelection.needsReview()) {
            return false;
        }
        String topic = topicSelection.topic();
        String topicHash = textService.sha256(scopeKey + "|" + topic);
        String normalizedEvidence = textService.normalize(question + " " + answer);
        String normalizedHash = textService.sha256(normalizedEvidence);
        String normalizedAnswerHash = textService.sha256(normalizedAnswer);
        String language = detectLanguage(question + " " + answer);
        BigDecimal confidence = calculateConfidence(scopeType, topic, topicSelection.confidence());

        MessageKnowledgeItem item = itemRepository
                .findByStoreIdAndScopeKeyAndTopicHash(storeId, scopeKey, topicHash)
                .orElseGet(MessageKnowledgeItem::new);
        boolean newItem = item.getId() == null;
        boolean sourceCanUpdateCanonical = canUpdateCanonicalFromSource(newItem, item, sourceSeenAt);
        boolean missingCanonicalQuestion = isMissingCanonicalQuestion(item);
        boolean updateCanonicalText = sourceCanUpdateCanonical
                && shouldUpdateCanonicalText(newItem, item, answer, normalizedAnswer);

        if (sourceCanUpdateCanonical) {
            applyItemFields(
                    item,
                    storeId,
                    context,
                    thread,
                    scopeType,
                    scopeId,
                    scopeKey,
                    topic,
                    topicHash,
                    question,
                    answer,
                    normalizedQuestion,
                    normalizedAnswer,
                    normalizedAnswerHash,
                    language,
                    confidence,
                    sourceSeenAt,
                    refinedAt,
                    updateCanonicalText
            );
            if (newItem || updateCanonicalText || missingCanonicalQuestion) {
                embeddingTextService.refreshSemanticFields(item, newItem);
            }
            itemRepository.save(item);
        }

        MessageKnowledgeEvidence evidence = buildEvidence(
                item,
                storeId,
                context,
                thread,
                scopeType,
                scopeId,
                scopeKey,
                guestMessage,
                staffMessage,
                question,
                answer,
                normalizedEvidence,
                normalizedHash,
                language,
                confidence
        );
        evidenceRepository.save(evidence);

        int evidenceCount = item.getEvidenceCount() == null ? 0 : item.getEvidenceCount();
        item.setEvidenceCount(evidenceCount + 1);
        if (sourceCanUpdateCanonical) {
            item.setLastSeenAt(sourceSeenAt);
            item.setRefinedAt(refinedAt);
        }
        itemRepository.save(item);
        return true;
    }

    public int getMaxModelCalls() {
        return Math.max(0, maxModelCalls);
    }

    private boolean isValidSourcePair(
            Long storeId,
            SuMessageThread thread,
            SuMessage guestMessage,
            SuMessage staffMessage
    ) {
        if (storeId == null || thread == null || guestMessage == null || staffMessage == null) {
            return false;
        }
        if (guestMessage.getId() == null || staffMessage.getId() == null) {
            return false;
        }
        if (!storeId.equals(thread.getStoreId())) {
            return false;
        }
        if (!storeId.equals(guestMessage.getStoreId()) || !storeId.equals(staffMessage.getStoreId())) {
            return false;
        }
        if (guestMessage.getSenderType() != SuMessagingSenderType.GUEST) {
            return false;
        }
        if (staffMessage.getSenderType() != SuMessagingSenderType.STAFF) {
            return false;
        }
        if (!isSuccessfulStaffReply(staffMessage)) {
            return false;
        }
        return hasText(guestMessage.getContent()) && hasText(staffMessage.getContent());
    }

    private static boolean isSuccessfulStaffReply(SuMessage message) {
        String deliveryStatus = message.getDeliveryStatus();
        if (!hasText(deliveryStatus)) {
            return true;
        }
        return !DELIVERY_STATUS_FAILED.equalsIgnoreCase(deliveryStatus)
                && !DELIVERY_STATUS_SENDING.equalsIgnoreCase(deliveryStatus);
    }

    private static boolean isReusableContext(SuMessagingThreadContext context) {
        if (context == null) {
            return false;
        }
        if (context.getRoomId() != null || context.getRoomTypeId() != null) {
            return true;
        }
        if (context.getReservationId() != null) {
            return false;
        }
        return true;
    }

    private static String resolveReusableScopeType(SuMessagingThreadContext context) {
        if (context.getRoomId() != null) {
            return SuMessagingThreadContext.SCOPE_ROOM;
        }
        if (context.getRoomTypeId() != null) {
            return SuMessagingThreadContext.SCOPE_ROOM_TYPE;
        }
        return SuMessagingThreadContext.SCOPE_STORE;
    }

    private static Long resolveScopeId(String scopeType, SuMessagingThreadContext context) {
        if (SuMessagingThreadContext.SCOPE_ROOM.equals(scopeType)) {
            return context.getRoomId();
        }
        if (SuMessagingThreadContext.SCOPE_ROOM_TYPE.equals(scopeType)) {
            return context.getRoomTypeId();
        }
        return null;
    }

    private static String buildScopeKey(String scopeType, Long scopeId) {
        String key = scopeType;
        if (scopeId != null) {
            key = scopeType + ":" + scopeId;
        }
        if (key.length() > MAX_SCOPE_KEY_LENGTH) {
            return key.substring(0, MAX_SCOPE_KEY_LENGTH);
        }
        return key;
    }

    private static boolean containsPrivateOrRedactedContent(String question, String answer) {
        String combined = (question + " " + answer).toLowerCase(Locale.ROOT);
        for (String marker : REDACTION_MARKERS) {
            if (combined.contains(marker)) {
                return true;
            }
        }
        for (String keyword : PRIVATE_KEYWORDS) {
            if (combined.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private TopicSelection determineTopic(
            Long storeId,
            String normalizedQuestion,
            String normalizedAnswer
    ) {
        if (topicClassifierService != null) {
            MessageKnowledgeTopicClassifierService.TopicClassification classification =
                    topicClassifierService.classify(storeId, normalizedQuestion, normalizedAnswer);
            if (classification.useExisting()) {
                return TopicSelection.reusable(classification.topicCode(), classification.confidence());
            }
            if (classification.needsReview()) {
                return TopicSelection.reviewRequired();
            }
        }
        return TopicSelection.reusable(determineFallbackTopic(normalizedQuestion, normalizedAnswer), null);
    }

    private String determineFallbackTopic(String normalizedQuestion, String normalizedAnswer) {
        String combined = normalizedQuestion + " " + normalizedAnswer;
        String classifiedTopic = classifyTopic(combined);
        if (classifiedTopic != null) {
            return classifiedTopic;
        }

        List<String> questionTokens = textService.tokenize(normalizedQuestion);
        String generalTopic = buildGeneralTopic(questionTokens);
        if (generalTopic != null) {
            return generalTopic;
        }

        List<String> answerTokens = textService.tokenize(normalizedAnswer);
        generalTopic = buildGeneralTopic(answerTokens);
        if (generalTopic != null) {
            return generalTopic;
        }

        return "general";
    }

    private static String classifyTopic(String combined) {
        if (containsAny(combined, "wifi", "wi fi", "无线", "ネット", "ワイファイ")) {
            return TOPIC_WIFI;
        }
        if (containsAny(
                combined,
                "無線",
                "无线网络",
                "無線網路",
                "网络密码",
                "網路密碼"
        )) {
            return TOPIC_WIFI;
        }
        if (containsAny(combined, "parking", "park", "停车", "駐車")) {
            return "parking";
        }
        if (containsAny(combined, "停車")) {
            return "parking";
        }
        if (containsAny(combined, "luggage", "baggage", "行李", "荷物")) {
            return "luggage";
        }
        if (containsAny(combined, "checkout", "check out", "退房", "チェックアウト")) {
            return "checkout";
        }
        if (containsAny(combined, "checkin", "check in", "入住", "チェックイン")) {
            return "checkin";
        }
        if (containsAny(
                combined,
                "breakfast",
                "morning meal",
                "早餐",
                "早饭",
                "早飯",
                "朝食",
                "朝ごはん",
                "朝ご飯"
        )) {
            return TOPIC_BREAKFAST;
        }
        if (containsAny(
                combined,
                "pet policy",
                "pets",
                "allow pets",
                "bring a pet",
                "bring my pet",
                "pet friendly",
                "mascota",
                "mascotas",
                "perro",
                "perros",
                "gato",
                "gatos",
                "animaux",
                "animal de compagnie",
                "chien",
                "ペット",
                "犬",
                "猫",
                "반려동물",
                "애완동물",
                "강아지",
                "고양이"
        )) {
            return TOPIC_PET_POLICY;
        }
        if (containsAny(
                combined,
                "airport shuttle",
                "shuttle",
                "shuttle bus",
                "airport transfer",
                "transfer from airport",
                "transfer to airport",
                "airport pickup",
                "airport pick up",
                "pickup from airport",
                "pick up from airport",
                "navette",
                "traslado",
                "transporte al aeropuerto",
                "servicio de transporte",
                "机场接送",
                "機場接送",
                "接送",
                "班车",
                "班車",
                "送迎",
                "シャトル",
                "空港送迎",
                "셔틀",
                "공항 픽업"
        )) {
            return TOPIC_SHUTTLE;
        }
        if (containsAny(combined, "address", "location", "station", "地址", "位置", "车站", "駅")) {
            return "location";
        }
        if (containsAny(combined, "trash", "garbage", "rubbish", "垃圾", "ゴミ")) {
            return "trash";
        }
        if (containsAny(combined, "towel", "amenity", "毛巾", "アメニティ", "タオル")) {
            return "amenity";
        }
        return null;
    }

    private static boolean containsAny(String value, String first, String... rest) {
        if (value.contains(first)) {
            return true;
        }
        for (String candidate : rest) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String buildGeneralTopic(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder("general");
        int added = 0;
        for (String token : tokens) {
            if (!hasText(token)) {
                continue;
            }
            builder.append(':').append(token);
            added++;
            if (added >= MAX_GENERAL_TOPIC_TOKENS) {
                break;
            }
        }
        String topic = builder.toString();
        if (topic.length() > MAX_TOPIC_LENGTH) {
            return topic.substring(0, MAX_TOPIC_LENGTH);
        }
        return topic;
    }

    private static BigDecimal calculateConfidence(
            String scopeType,
            String topic,
            BigDecimal classifierConfidence
    ) {
        if (classifierConfidence != null && classifierConfidence.compareTo(BigDecimal.ZERO) > 0) {
            return classifierConfidence.setScale(4, RoundingMode.HALF_UP);
        }
        return calculateConfidence(scopeType, topic);
    }

    private static BigDecimal calculateConfidence(String scopeType, String topic) {
        double value = STORE_CONFIDENCE;
        if (SuMessagingThreadContext.SCOPE_ROOM.equals(scopeType)) {
            value = ROOM_CONFIDENCE;
        } else if (SuMessagingThreadContext.SCOPE_ROOM_TYPE.equals(scopeType)) {
            value = ROOM_TYPE_CONFIDENCE;
        }
        if ("general".equals(topic)) {
            value = Math.min(value, STORE_CONFIDENCE);
        }
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static boolean shouldUpdateCanonicalText(
            boolean newItem,
            MessageKnowledgeItem existingItem,
            String newAnswer,
            String newNormalizedAnswer
    ) {
        if (newItem) {
            return true;
        }
        if (existingItem == null) {
            return true;
        }
        String existingNormalizedAnswer = existingItem.getNormalizedAnswer();
        if (!hasText(existingNormalizedAnswer)) {
            return true;
        }
        if (!hasText(newNormalizedAnswer)) {
            return false;
        }
        if (existingNormalizedAnswer.equals(newNormalizedAnswer)) {
            return false;
        }

        if (TOPIC_WIFI.equals(existingItem.getTopic())) {
            FactComparison wifiComparison = compareWifiFacts(existingItem.getAnswer(), newAnswer);
            if (wifiComparison.comparable() && !wifiComparison.conflicting()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canUpdateCanonicalFromSource(
            boolean newItem,
            MessageKnowledgeItem existingItem,
            LocalDateTime sourceSeenAt
    ) {
        if (newItem || existingItem == null) {
            return true;
        }
        LocalDateTime currentCanonicalSeenAt = existingItem.getLastSeenAt();
        if (currentCanonicalSeenAt == null) {
            return true;
        }
        return !sourceSeenAt.isBefore(currentCanonicalSeenAt);
    }

    private static boolean isMissingCanonicalQuestion(MessageKnowledgeItem item) {
        return item != null
                && (item.getQuestion() == null || item.getNormalizedQuestion() == null);
    }

    private static FactComparison compareWifiFacts(String existingAnswer, String newAnswer) {
        WifiFacts existingFacts = extractWifiFacts(existingAnswer);
        WifiFacts newFacts = extractWifiFacts(newAnswer);
        boolean comparable = false;
        boolean conflicting = false;

        if (hasText(existingFacts.password()) && hasText(newFacts.password())) {
            comparable = true;
            if (!existingFacts.password().equals(newFacts.password())) {
                conflicting = true;
            }
        }
        if (hasText(existingFacts.ssid()) && hasText(newFacts.ssid())) {
            comparable = true;
            if (!existingFacts.ssid().equals(newFacts.ssid())) {
                conflicting = true;
            }
        }
        return new FactComparison(comparable, conflicting);
    }

    private static WifiFacts extractWifiFacts(String answer) {
        String password = firstMatch(answer, WIFI_PASSWORD_PATTERN);
        String ssid = firstMatch(answer, WIFI_SSID_PATTERN);
        return new WifiFacts(normalizePassword(password), normalizeSsid(ssid));
    }

    private static boolean hasOpposingComparableValues(String existingAnswer, String newAnswer) {
        if (hasDisjointComparableValues(existingAnswer, newAnswer, TIME_VALUE_PATTERN)) {
            return true;
        }
        return hasDisjointComparableValues(existingAnswer, newAnswer, MONEY_VALUE_PATTERN);
    }

    private static boolean hasDisjointComparableValues(
            String existingAnswer,
            String newAnswer,
            Pattern pattern
    ) {
        Set<String> existingValues = extractPatternValues(existingAnswer, pattern);
        Set<String> newValues = extractPatternValues(newAnswer, pattern);
        if (existingValues.isEmpty() || newValues.isEmpty()) {
            return false;
        }
        for (String newValue : newValues) {
            if (existingValues.contains(newValue)) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> extractPatternValues(String text, Pattern pattern) {
        Set<String> values = new LinkedHashSet<>();
        if (!hasText(text)) {
            return values;
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group();
            String normalized = normalizeComparableValue(value);
            if (hasText(normalized)) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static String firstMatch(String text, Pattern pattern) {
        if (!hasText(text)) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static String normalizePassword(String value) {
        if (!hasText(value)) {
            return null;
        }
        return trimTrailingPunctuation(value.trim());
    }

    private static String normalizeSsid(String value) {
        if (!hasText(value)) {
            return null;
        }
        return trimTrailingPunctuation(value.trim())
                .replaceAll("\\s{2,}", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String normalizeComparableValue(String value) {
        if (!hasText(value)) {
            return null;
        }
        return trimTrailingPunctuation(value)
                .toLowerCase(Locale.ROOT)
                .replace('：', ':')
                .replaceAll("\\s+", "");
    }

    private static String trimTrailingPunctuation(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\p{Punct}\\p{P}]+$", "").trim();
    }

    private static void applyItemFields(
            MessageKnowledgeItem item,
            Long storeId,
            SuMessagingThreadContext context,
            SuMessageThread thread,
            String scopeType,
            Long scopeId,
            String scopeKey,
            String topic,
            String topicHash,
            String question,
            String answer,
            String normalizedQuestion,
            String normalizedAnswer,
            String normalizedAnswerHash,
            String language,
            BigDecimal confidence,
            LocalDateTime seenAt,
            LocalDateTime refinedAt,
            boolean updateCanonicalText
    ) {
        item.setStoreId(storeId);
        item.setScopeType(scopeType);
        item.setScopeId(scopeId);
        item.setScopeKey(scopeKey);
        item.setThreadId(thread.getId());
        item.setRoomId(context.getRoomId());
        item.setRoomNumber(context.getRoomNumber());
        item.setRoomTypeId(context.getRoomTypeId());
        item.setRoomTypeName(context.getRoomTypeName());
        item.setChannelId(context.getChannelId());
        item.setTopic(topic);
        item.setTopicHash(topicHash);
        if (updateCanonicalText || item.getQuestion() == null) {
            item.setQuestion(question);
            item.setNormalizedQuestion(normalizedQuestion);
        }
        if (updateCanonicalText) {
            item.setAnswer(answer);
            item.setNormalizedAnswer(normalizedAnswer);
            item.setNormalizedAnswerHash(normalizedAnswerHash);
        }
        if (item.getAnswer() == null) {
            item.setAnswer(answer);
        }
        if (item.getNormalizedAnswer() == null) {
            item.setNormalizedAnswer(normalizedAnswer);
        }
        if (item.getNormalizedAnswerHash() == null) {
            item.setNormalizedAnswerHash(normalizedAnswerHash);
        }
        if (item.getNormalizedQuestion() == null) {
            item.setNormalizedQuestion(normalizedQuestion);
        }
        if (updateCanonicalText || item.getLanguage() == null) {
            item.setLanguage(language);
        }
        item.setConfidence(confidence);
        item.setPiiRedactionStatus(MessageKnowledgeItem.REDACTION_STATUS_REDACTED);
        if (item.getStatus() == null || MessageKnowledgeItem.STATUS_CONFLICT.equals(item.getStatus())) {
            item.setStatus(MessageKnowledgeItem.STATUS_ACTIVE);
        }
        if (item.getFirstSeenAt() == null) {
            item.setFirstSeenAt(seenAt);
        }
        item.setLastSeenAt(seenAt);
        item.setRefinedAt(refinedAt);
    }

    private static MessageKnowledgeEvidence buildEvidence(
            MessageKnowledgeItem item,
            Long storeId,
            SuMessagingThreadContext context,
            SuMessageThread thread,
            String scopeType,
            Long scopeId,
            String scopeKey,
            SuMessage guestMessage,
            SuMessage staffMessage,
            String question,
            String answer,
            String normalizedEvidence,
            String normalizedHash,
            String language,
            BigDecimal confidence
    ) {
        MessageKnowledgeEvidence evidence = new MessageKnowledgeEvidence();
        evidence.setStoreId(storeId);
        evidence.setItem(item);
        evidence.setScopeType(scopeType);
        evidence.setScopeId(scopeId);
        evidence.setScopeKey(scopeKey);
        evidence.setThreadId(thread.getId());
        evidence.setReservationId(context.getReservationId());
        evidence.setRoomId(context.getRoomId());
        evidence.setRoomNumber(context.getRoomNumber());
        evidence.setRoomTypeId(context.getRoomTypeId());
        evidence.setRoomTypeName(context.getRoomTypeName());
        evidence.setChannelId(context.getChannelId());
        evidence.setBookingKey(context.getBookingKey());
        evidence.setSourceGuestMessageId(guestMessage.getId());
        evidence.setSourceStaffMessageId(staffMessage.getId());
        evidence.setSourceTimestamp(staffMessage.getSentAt());
        evidence.setQuestion(question);
        evidence.setAnswer(answer);
        evidence.setNormalizedText(normalizedEvidence);
        evidence.setNormalizedHash(normalizedHash);
        evidence.setLanguage(language);
        evidence.setConfidence(confidence);
        evidence.setPiiRedactionStatus(MessageKnowledgeEvidence.REDACTION_STATUS_REDACTED);
        evidence.setStatus(MessageKnowledgeEvidence.STATUS_ACTIVE);
        return evidence;
    }

    private static LocalDateTime resolveSeenAt(LocalDateTime sourceTimestamp) {
        if (sourceTimestamp != null) {
            return sourceTimestamp;
        }
        return UtcTimeUtil.nowLocalDateTime();
    }

    private static String detectLanguage(String text) {
        if (!hasText(text)) {
            return null;
        }
        if (text.matches(".*[\\u3040-\\u30FF].*")) {
            return "ja";
        }
        if (text.matches(".*[\\uAC00-\\uD7AF].*")) {
            return "ko";
        }
        if (text.matches(".*[\\u4E00-\\u9FFF].*")) {
            if (containsAny(text, "無", "網", "密碼", "嗎", "這", "裡", "號", "預", "訂", "車", "場", "費", "櫃", "臺", "灣")) {
                return "zh-TW";
            }
            return "zh";
        }
        return "en";
    }

    private record WifiFacts(String password, String ssid) {
    }

    private record FactComparison(boolean comparable, boolean conflicting) {
    }

    private record TopicSelection(
            String topic,
            BigDecimal confidence,
            boolean needsReview
    ) {
        static TopicSelection reusable(String topic, BigDecimal confidence) {
            return new TopicSelection(topic, confidence, false);
        }

        static TopicSelection reviewRequired() {
            return new TopicSelection(MessageKnowledgeTopicClassifierService.TOPIC_GENERAL_CANDIDATE, null, true);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
