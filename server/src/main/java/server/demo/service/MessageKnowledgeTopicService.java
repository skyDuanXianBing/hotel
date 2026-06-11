package server.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageKnowledgeTopicService {
    public static final String TOPIC_LATE_CHECKOUT = "LATE_CHECKOUT";
    public static final String TOPIC_PARKING = "PARKING";
    public static final String TOPIC_LUGGAGE = "LUGGAGE";
    public static final String TOPIC_BREAKFAST = "BREAKFAST";

    private static final int MAX_REUSABLE_FACTS = 4;
    private static final int MAX_FACT_LENGTH = 260;
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?i)\\b\\d{1,2}[:：]\\d{2}(?:\\s*(?:-|~|–|—|to|至|到)\\s*\\d{1,2}[:：]\\d{2})?\\b"
    );
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?i)(?:¥\\s*\\d{1,6}|\\d{1,6}\\s*(?:jpy|yen|円|日元)|(?:jpy|yen)\\s*\\d{1,6})"
    );
    private static final Pattern FLOOR_PATTERN = Pattern.compile(
            "(?i)(?:\\b\\d+(?:st|nd|rd|th)?\\s*floor\\b|[一二三四五六七八九十0-9]+\\s*[楼樓階层層])"
    );
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile(
            "[\\r\\n]+|(?<=[。！？!?])\\s*|(?<=[.!?])\\s+"
    );

    private static final List<TopicRule> TOPIC_RULES = List.of(
            new TopicRule(TOPIC_LATE_CHECKOUT, List.of(
                    "late checkout",
                    "late check out",
                    "late check-out",
                    "check out late",
                    "check out later",
                    "checkout later",
                    "checkout extension",
                    "extend checkout",
                    "延迟退房",
                    "延时退房",
                    "延後退房",
                    "退房延迟",
                    "退房延後",
                    "推迟退房",
                    "レイトチェックアウト",
                    "チェックアウト延長",
                    "늦은 체크아웃"
            )),
            new TopicRule(TOPIC_PARKING, List.of(
                    "parking",
                    "park my car",
                    "park a car",
                    "car park",
                    "parking lot",
                    "parking space",
                    "park",
                    "停车",
                    "停車",
                    "车位",
                    "車位",
                    "停车场",
                    "停車場",
                    "駐車",
                    "駐車場",
                    "주차"
            )),
            new TopicRule(TOPIC_LUGGAGE, List.of(
                    "luggage",
                    "baggage",
                    "bag",
                    "bags",
                    "bag storage",
                    "store bag",
                    "store bags",
                    "keep our bags",
                    "keep my bags",
                    "leave bag",
                    "leave bags",
                    "leave our bags",
                    "leave my bags",
                    "keep luggage",
                    "keep bags",
                    "寄存行李",
                    "行李寄存",
                    "寄放行李",
                    "行李寄放",
                    "行李保管",
                    "寄存包",
                    "荷物預かり",
                    "荷物保管",
                    "짐 보관"
            )),
            new TopicRule(TOPIC_BREAKFAST, List.of(
                    "breakfast",
                    "morning meal",
                    "breakfast buffet",
                    "早餐",
                    "早饭",
                    "早飯",
                    "朝食",
                    "조식"
            ))
    );

    private static final List<String> FACT_PHRASES = List.of(
            "first come first served",
            "first-come-first-served",
            "subject to availability",
            "front desk",
            "reception",
            "lobby",
            "restaurant",
            "sakura",
            "per night",
            "per person",
            "先到先得",
            "先到先着",
            "视房态",
            "視房態",
            "前台",
            "櫃台",
            "柜台",
            "大堂",
            "大厅",
            "餐厅",
            "餐廳",
            "每晚",
            "每人",
            "無料",
            "免费",
            "免費"
    );

    private static final List<String> FACT_INDICATOR_PHRASES = List.of(
            "until",
            "from",
            "between",
            "before",
            "after",
            "fee",
            "charge",
            "cost",
            "free",
            "reservation",
            "available",
            "为止",
            "至",
            "到",
            "之前",
            "之后",
            "以後",
            "费用",
            "費用",
            "收费",
            "收費",
            "免费",
            "免費",
            "预约",
            "預約",
            "可用",
            "可以"
    );

    public Set<String> detectTopics(String text) {
        String normalized = normalizeForContains(text);
        String compact = normalized.replace(" ", "");
        Set<String> topics = new LinkedHashSet<>();
        for (TopicRule rule : TOPIC_RULES) {
            if (rule.matches(normalized, compact)) {
                topics.add(rule.topicCode);
            }
        }
        return topics;
    }

    public Set<String> detectTopics(String firstText, String secondText, String thirdText) {
        return detectTopics(joinNonBlank(firstText, secondText, thirdText));
    }

    public boolean hasSharedTopic(Set<String> firstTopics, Set<String> secondTopics) {
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

    public Set<String> extractFactTokens(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }

        addPatternMatches(tokens, text, TIME_PATTERN);
        addPatternMatches(tokens, text, MONEY_PATTERN);
        addPatternMatches(tokens, text, FLOOR_PATTERN);

        String normalized = normalizeForContains(text);
        for (String phrase : FACT_PHRASES) {
            String normalizedPhrase = normalizeForContains(phrase);
            if (containsPhrase(normalized, normalized.replace(" ", ""), normalizedPhrase)) {
                tokens.add(normalizedPhrase);
            }
        }
        return tokens;
    }

    public List<String> extractReusableFacts(String answer) {
        List<String> facts = new ArrayList<>();
        if (answer == null || answer.isBlank()) {
            return facts;
        }

        String[] parts = SENTENCE_SPLIT_PATTERN.split(answer);
        for (String part : parts) {
            if (facts.size() >= MAX_REUSABLE_FACTS) {
                return facts;
            }
            String cleaned = cleanFact(part);
            if (cleaned.isBlank()) {
                continue;
            }
            if (containsConcreteFact(cleaned)) {
                addFact(facts, cleaned);
            }
        }

        if (facts.isEmpty() && containsConcreteFact(answer)) {
            addFact(facts, cleanFact(answer));
        }
        return facts;
    }

    public Set<String> intersect(Set<String> firstValues, Set<String> secondValues) {
        Set<String> values = new LinkedHashSet<>();
        if (firstValues == null || secondValues == null) {
            return values;
        }
        for (String value : firstValues) {
            if (secondValues.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private boolean containsConcreteFact(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (!extractFactTokens(value).isEmpty()) {
            return true;
        }

        String normalized = normalizeForContains(value);
        for (String phrase : FACT_INDICATOR_PHRASES) {
            String normalizedPhrase = normalizeForContains(phrase);
            if (containsPhrase(normalized, normalized.replace(" ", ""), normalizedPhrase)) {
                return true;
            }
        }
        return false;
    }

    private static void addPatternMatches(Set<String> tokens, String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = normalizeFactToken(matcher.group());
            if (!value.isBlank()) {
                tokens.add(value);
            }
        }
    }

    private static void addFact(List<String> facts, String fact) {
        String cleaned = cleanFact(fact);
        if (cleaned.isBlank()) {
            return;
        }
        if (cleaned.length() > MAX_FACT_LENGTH) {
            cleaned = cleaned.substring(0, MAX_FACT_LENGTH).trim();
        }
        if (!facts.contains(cleaned)) {
            facts.add(cleaned);
        }
    }

    private static String cleanFact(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static String normalizeFactToken(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('：', ':')
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static String joinNonBlank(String firstText, String secondText, String thirdText) {
        StringBuilder builder = new StringBuilder();
        appendNonBlank(builder, firstText);
        appendNonBlank(builder, secondText);
        appendNonBlank(builder, thirdText);
        return builder.toString();
    }

    private static void appendNonBlank(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value);
    }

    private static String normalizeForContains(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('：', ':')
                .replace('　', ' ')
                .replaceAll("[\\p{Punct}\\p{P}]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static boolean containsPhrase(String normalized, String compact, String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return false;
        }
        String normalizedPhrase = normalizeForContains(phrase);
        if (normalizedPhrase.isBlank()) {
            return false;
        }
        if (normalized.contains(normalizedPhrase)) {
            return true;
        }
        return compact.contains(normalizedPhrase.replace(" ", ""));
    }

    private static class TopicRule {
        private final String topicCode;
        private final List<String> phrases;

        private TopicRule(String topicCode, List<String> phrases) {
            this.topicCode = topicCode;
            this.phrases = phrases;
        }

        private boolean matches(String normalized, String compact) {
            for (String phrase : phrases) {
                if (containsPhrase(normalized, compact, phrase)) {
                    return true;
                }
            }
            return false;
        }
    }
}
