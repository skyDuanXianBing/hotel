package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.MessageKnowledgeTopic;
import server.demo.repository.MessageKnowledgeTopicRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MessageKnowledgeTopicDictionaryService {
    public static final int DEFAULT_TOPIC_LIMIT = 50;
    private static final int MAX_TOPIC_CODE_LENGTH = 120;
    private static final int MAX_DISPLAY_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private static final List<DefaultTopic> DEFAULT_TOPICS = List.of(
            new DefaultTopic(
                    "wifi",
                    "WiFi",
                    "Wireless internet network name, password, and connection guidance.",
                    List.of("wi-fi", "internet", "wireless", "无线", "無線", "ネット"),
                    List.of("What is the WiFi password?", "WiFi密码是多少？"),
                    List.of("The WiFi password is sakura2026."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "breakfast",
                    "Breakfast",
                    "Breakfast hours, location, price, and included meal details.",
                    List.of("morning meal", "buffet", "早餐", "朝食"),
                    List.of("What time is breakfast?"),
                    List.of("Breakfast is served from 07:00 to 10:00."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "parking",
                    "Parking",
                    "Parking availability, price, location, and reservation policy.",
                    List.of("car park", "parking lot", "停车", "停車", "駐車"),
                    List.of("Do you have parking?"),
                    List.of("Parking is first come, first served."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "checkout",
                    "Checkout",
                    "Checkout time, late checkout, and departure instructions.",
                    List.of("check out", "late checkout", "退房", "チェックアウト"),
                    List.of("What time is checkout?"),
                    List.of("Checkout is before 11:00."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "checkin",
                    "Check-in",
                    "Check-in time, early check-in, and arrival instructions.",
                    List.of("check in", "arrival", "入住", "チェックイン"),
                    List.of("What time can I check in?"),
                    List.of("Check-in starts at 15:00."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "pet_policy",
                    "Pet Policy",
                    "Rules for bringing pets or service animals.",
                    List.of("pets", "pet friendly", "mascotas", "animaux", "ペット", "반려동물"),
                    List.of("Do you allow pets?"),
                    List.of("Pets are not allowed in the rooms."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "shuttle",
                    "Shuttle",
                    "Airport shuttle, pickup, transfer, and related transport service.",
                    List.of("airport transfer", "airport pickup", "shuttle bus", "机场接送", "送迎", "셔틀"),
                    List.of("Do you have an airport shuttle?"),
                    List.of("Airport shuttle requires advance reservation."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "luggage",
                    "Luggage",
                    "Luggage storage before check-in or after checkout.",
                    List.of("baggage", "bags", "行李", "荷物"),
                    List.of("Can I store my luggage?"),
                    List.of("We can keep luggage at the front desk."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "amenity",
                    "Amenity",
                    "Towels, toiletries, room supplies, and shared amenities.",
                    List.of("towel", "toiletries", "supplies", "毛巾", "アメニティ"),
                    List.of("Do you provide towels?"),
                    List.of("Towels are provided in the room."),
                    SuMessagingThreadContext.SCOPE_ROOM_TYPE
            ),
            new DefaultTopic(
                    "trash",
                    "Trash",
                    "Garbage disposal, sorting, pickup, and recycling rules.",
                    List.of("garbage", "rubbish", "recycling", "垃圾", "ゴミ"),
                    List.of("Where should I put trash?"),
                    List.of("Please put trash in the designated area."),
                    SuMessagingThreadContext.SCOPE_STORE
            ),
            new DefaultTopic(
                    "location",
                    "Location",
                    "Address, nearby station, directions, and access route.",
                    List.of("address", "station", "directions", "地址", "车站", "駅"),
                    List.of("What is the address?"),
                    List.of("The nearest station is a short walk away."),
                    SuMessagingThreadContext.SCOPE_STORE
            )
    );

    private final MessageKnowledgeTopicRepository topicRepository;
    private final ObjectMapper objectMapper;

    public MessageKnowledgeTopicDictionaryService(
            MessageKnowledgeTopicRepository topicRepository,
            ObjectMapper objectMapper
    ) {
        this.topicRepository = topicRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<MessageKnowledgeTopic> findActiveTopics(Long storeId, int maxTopics) {
        requireStoreId(storeId);
        ensureDefaultTopics(storeId);
        int limit = normalizeTopicLimit(maxTopics);
        return topicRepository.findByStoreIdAndStatus(
                storeId,
                MessageKnowledgeTopic.STATUS_ACTIVE,
                PageRequest.of(0, limit)
        );
    }

    @Transactional
    public Optional<MessageKnowledgeTopic> findActiveTopic(Long storeId, String topicCode) {
        requireStoreId(storeId);
        String normalizedCode = normalizeTopicCode(topicCode);
        if (normalizedCode == null) {
            return Optional.empty();
        }
        ensureDefaultTopics(storeId);
        return topicRepository.findByStoreIdAndTopicCode(storeId, normalizedCode)
                .filter(topic -> MessageKnowledgeTopic.STATUS_ACTIVE.equals(topic.getStatus()));
    }

    @Transactional
    public MessageKnowledgeTopic createNeedsReviewCandidate(
            Long storeId,
            String proposedTopicCode,
            String displayName,
            String description,
            BigDecimal confidence
    ) {
        requireStoreId(storeId);
        String normalizedCode = normalizeTopicCode(proposedTopicCode);
        if (normalizedCode == null) {
            normalizedCode = "general_candidate";
        }
        Optional<MessageKnowledgeTopic> existing = topicRepository.findByStoreIdAndTopicCode(storeId, normalizedCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        MessageKnowledgeTopic topic = new MessageKnowledgeTopic();
        topic.setStoreId(storeId);
        topic.setTopicCode(normalizedCode);
        topic.setDisplayName(truncate(firstNonBlank(displayName, humanizeTopicCode(normalizedCode)), MAX_DISPLAY_NAME_LENGTH));
        topic.setDescription(truncate(firstNonBlank(description, "AI proposed topic pending review."), MAX_DESCRIPTION_LENGTH));
        topic.setAliasesJson(toJson(List.of()));
        topic.setExampleQuestionsJson(toJson(List.of()));
        topic.setExampleAnswersJson(toJson(List.of()));
        topic.setScopePreference(SuMessagingThreadContext.SCOPE_STORE);
        topic.setStatus(MessageKnowledgeTopic.STATUS_NEEDS_REVIEW);
        topic.setSource(MessageKnowledgeTopic.SOURCE_AI_CANDIDATE);
        topic.setConfidence(normalizeConfidence(confidence));
        try {
            return topicRepository.save(topic);
        } catch (DataIntegrityViolationException e) {
            return topicRepository.findByStoreIdAndTopicCode(storeId, normalizedCode)
                    .orElseThrow(() -> e);
        }
    }

    public int countDefaultTopics() {
        return DEFAULT_TOPICS.size();
    }

    public static String normalizeTopicCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_:]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MAX_TOPIC_CODE_LENGTH) {
            return normalized.substring(0, MAX_TOPIC_CODE_LENGTH);
        }
        return normalized;
    }

    private void ensureDefaultTopics(Long storeId) {
        List<MessageKnowledgeTopic> missingTopics = new ArrayList<>();
        for (DefaultTopic defaultTopic : DEFAULT_TOPICS) {
            Optional<MessageKnowledgeTopic> existing =
                    topicRepository.findByStoreIdAndTopicCode(storeId, defaultTopic.topicCode());
            if (existing.isPresent()) {
                continue;
            }
            missingTopics.add(toEntity(storeId, defaultTopic));
        }
        if (missingTopics.isEmpty()) {
            return;
        }
        try {
            topicRepository.saveAll(missingTopics);
        } catch (DataIntegrityViolationException ignored) {
            // Another worker may have initialized the same store dictionary.
        }
    }

    private MessageKnowledgeTopic toEntity(Long storeId, DefaultTopic defaultTopic) {
        MessageKnowledgeTopic topic = new MessageKnowledgeTopic();
        topic.setStoreId(storeId);
        topic.setTopicCode(defaultTopic.topicCode());
        topic.setDisplayName(defaultTopic.displayName());
        topic.setDescription(defaultTopic.description());
        topic.setAliasesJson(toJson(defaultTopic.aliases()));
        topic.setExampleQuestionsJson(toJson(defaultTopic.exampleQuestions()));
        topic.setExampleAnswersJson(toJson(defaultTopic.exampleAnswers()));
        topic.setScopePreference(defaultTopic.scopePreference());
        topic.setStatus(MessageKnowledgeTopic.STATUS_ACTIVE);
        topic.setSource(MessageKnowledgeTopic.SOURCE_DEFAULT);
        topic.setConfidence(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
        return topic;
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize message knowledge topic dictionary JSON", e);
        }
    }

    private static int normalizeTopicLimit(int maxTopics) {
        if (maxTopics < 1) {
            return DEFAULT_TOPIC_LIMIT;
        }
        return Math.min(maxTopics, DEFAULT_TOPIC_LIMIT);
    }

    private static BigDecimal normalizeConfidence(BigDecimal value) {
        if (value == null) {
            return null;
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

    private static String humanizeTopicCode(String topicCode) {
        String normalized = normalizeTopicCode(topicCode);
        if (normalized == null) {
            return "General Candidate";
        }
        String[] parts = normalized.replace(':', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        if (builder.length() == 0) {
            return "General Candidate";
        }
        return builder.toString();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static void requireStoreId(Long storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("Store context is required");
        }
    }

    private record DefaultTopic(
            String topicCode,
            String displayName,
            String description,
            List<String> aliases,
            List<String> exampleQuestions,
            List<String> exampleAnswers,
            String scopePreference
    ) {
    }
}
