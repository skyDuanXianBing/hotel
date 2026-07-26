package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Component
public class SuReviewPayloadMapper {

    private static final DateTimeFormatter SU_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    public SuReviewPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NormalizedReview fromPull(JsonNode reviewNode, String hotelId, LocalDateTime syncedAt) {
        if (reviewNode == null || !reviewNode.isObject()) {
            throw new IllegalArgumentException("Su Review Pull 返回了无效评价对象");
        }
        JsonNode guestInfo = objectOrEmpty(reviewNode.get("guest_info"));
        JsonNode replyFlags = objectOrEmpty(reviewNode.get("reply_flags"));
        JsonNode ratings = objectOrEmpty(guestInfo.get("review_json"));

        Integer channelId = readInteger(reviewNode, "channel_id");
        if (channelId == null) {
            channelId = readInteger(guestInfo, "channel_id");
        }
        String reviewType = defaultReviewType(readText(guestInfo, "review_type"));

        return new NormalizedReview(
                requireChannelId(channelId),
                requireText(readText(guestInfo, "channel_review_id"), "channel_review_id"),
                reviewType,
                requireText(firstText(reviewNode, "channel_hotel_id", "channel_property_id"),
                        "channel_property_id"),
                readText(reviewNode, "listing_id"),
                firstText(reviewNode, "booking_id", "channel_booking_id"),
                hotelId,
                readText(reviewNode, "guest_name"),
                readText(reviewNode, "property_name"),
                parseLocalDate(readText(guestInfo, "check_in")),
                parseLocalDate(readText(guestInfo, "check_out")),
                readText(guestInfo, "review_title"),
                readText(guestInfo, "review"),
                readText(guestInfo, "negative_review"),
                readText(guestInfo, "private_feedback"),
                readDecimal(guestInfo, "overall_score"),
                ratings,
                readText(guestInfo, "review_reply"),
                parseDateTime(readText(guestInfo, "reply_time")),
                readText(guestInfo, "review_status"),
                readBoolean(replyFlags, "reply_to_guest"),
                readBoolean(replyFlags, "review_the_guest"),
                parseDateTime(readText(guestInfo, "received_time")),
                firstText(reviewNode, "Ruid", "ruid"),
                hash(reviewNode),
                syncedAt
        );
    }

    public NormalizedReview fromPush(JsonNode reviewNode, String routedHotelId, LocalDateTime syncedAt) {
        if (reviewNode == null || !reviewNode.isObject()) {
            throw new IllegalArgumentException("Su Review Push 请求包含无效评价对象");
        }
        String payloadHotelId = firstText(reviewNode, "hotel_id", "hotelid", "hotelId");
        if (payloadHotelId == null || !payloadHotelId.trim().equalsIgnoreCase(routedHotelId)) {
            throw new IllegalArgumentException("Review Push 的 hotel_id 与路由门店不一致");
        }

        JsonNode rawScoreNode = reviewNode.get("review_score");
        JsonNode scoreNode = objectOrEmpty(rawScoreNode);
        BigDecimal overallScore = rawScoreNode != null && rawScoreNode.isValueNode()
                ? parseDecimal(rawScoreNode.asText(null))
                : readDecimal(scoreNode, "review_score");
        String reviewType = defaultReviewType(readText(reviewNode, "review_type"));

        return new NormalizedReview(
                requireChannelId(readInteger(reviewNode, "channel_id")),
                requireText(readText(reviewNode, "channel_review_id"), "channel_review_id"),
                reviewType,
                requireText(firstText(reviewNode, "channel_property_id", "channel_hotel_id"),
                        "channel_property_id"),
                readText(reviewNode, "listing_id"),
                firstText(reviewNode, "channel_booking_id", "booking_id"),
                routedHotelId,
                firstText(reviewNode, "reviewee_name", "guest_name"),
                readText(reviewNode, "property_name"),
                parseLocalDate(readText(reviewNode, "check_in")),
                parseLocalDate(readText(reviewNode, "check_out")),
                readText(reviewNode, "review_title"),
                readText(reviewNode, "review_description"),
                readText(reviewNode, "review_negative_description"),
                readText(reviewNode, "private_feedback"),
                overallScore,
                scoreNode,
                readText(reviewNode, "review_reply"),
                parseDateTime(readText(reviewNode, "reply_time")),
                readText(reviewNode, "review_status"),
                readBoolean(reviewNode, "is_eligible_to_respond"),
                null,
                parseDateTime(readText(reviewNode, "review_date")),
                firstText(reviewNode, "Ruid", "ruid"),
                hash(reviewNode),
                syncedAt
        );
    }

    public static List<JsonNode> extractPushReviewNodes(JsonNode root) {
        if (root == null || root.isNull()) {
            return List.of();
        }
        if (root.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            root.forEach(nodes::add);
            return nodes;
        }
        for (String field : List.of("reviews", "Reviews")) {
            JsonNode reviews = root.get(field);
            if (reviews != null && reviews.isArray()) {
                List<JsonNode> nodes = new ArrayList<>();
                reviews.forEach(nodes::add);
                return nodes;
            }
        }
        return root.isObject() ? List.of(root) : List.of();
    }

    private String hash(JsonNode node) {
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(node);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (Exception e) {
            throw new IllegalArgumentException("无法计算 Review 事件摘要", e);
        }
    }

    private static JsonNode objectOrEmpty(JsonNode node) {
        return node != null && node.isObject() ? node : com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }

    private static Integer requireChannelId(Integer channelId) {
        if (channelId == null) {
            throw new IllegalArgumentException("评价缺少 channel_id");
        }
        return channelId;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("评价缺少 " + field);
        }
        return value.trim();
    }

    private static String defaultReviewType(String reviewType) {
        return reviewType == null || reviewType.isBlank()
                ? "guest_to_host"
                : reviewType.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstText(JsonNode node, String... fields) {
        if (fields == null) {
            return null;
        }
        for (String field : fields) {
            String value = readText(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String readText(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || field == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isContainerNode()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.trim().isBlank() ? null : text.trim();
    }

    private static Integer readInteger(JsonNode node, String field) {
        String text = readText(node, field);
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("评价字段 " + field + " 不是有效整数");
        }
    }

    private static BigDecimal readDecimal(JsonNode node, String field) {
        String text = readText(node, field);
        return parseDecimal(text);
    }

    private static BigDecimal parseDecimal(String text) {
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean readBoolean(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText("").trim().toLowerCase(Locale.ROOT);
        if ("true".equals(text) || "y".equals(text) || "yes".equals(text) || "1".equals(text)) {
            return true;
        }
        if ("false".equals(text) || "n".equals(text) || "no".equals(text) || "0".equals(text)) {
            return false;
        }
        return null;
    }

    private static LocalDate parseLocalDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            if (value.length() == 10) {
                return LocalDate.parse(value).atStartOfDay();
            }
            if (value.contains("T")) {
                return LocalDateTime.parse(value);
            }
            return LocalDateTime.parse(value, SU_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public record NormalizedReview(
            Integer channelId,
            String channelReviewId,
            String reviewType,
            String channelPropertyId,
            String listingId,
            String channelBookingId,
            String hotelId,
            String guestName,
            String propertyName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String reviewTitle,
            String reviewText,
            String negativeReviewText,
            String privateFeedback,
            BigDecimal overallScore,
            JsonNode categoryRatings,
            String replyText,
            LocalDateTime replyAt,
            String reviewStatus,
            Boolean canReply,
            Boolean canReviewGuest,
            LocalDateTime receivedAt,
            String suRuid,
            String sourceHash,
            LocalDateTime syncedAt
    ) {
    }
}
