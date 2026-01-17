package server.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.demo.dto.PriceLabsWebhookRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class PriceLabsWebhookPayloadNormalizer {

    private PriceLabsWebhookPayloadNormalizer() {}

    public record NormalizedPayload(PriceLabsWebhookRequest request, String format) {}

    public static NormalizedPayload normalizeSyncPayload(ObjectMapper objectMapper, String rawBody) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper is required");
        }
        if (rawBody == null || rawBody.isBlank()) {
            throw new IllegalArgumentException("rawBody is empty");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (root != null && root.isObject()) {
                String listingId = readString(root, "listing_id", "listingId");
                String ratePlanId = readString(root, "rate_plan_id", "ratePlanId");
                String timestamp = readString(root, "timestamp");
                JsonNode data = root.get("data");

                boolean looksLikeCompact = listingId != null
                        && data != null
                        && data.isArray()
                        && data.size() > 0
                        && data.get(0) != null
                        && data.get(0).isObject()
                        && (data.get(0).hasNonNull("date") || data.get(0).hasNonNull("start_date"))
                        && data.get(0).hasNonNull("price");

                if (looksLikeCompact) {
                    PriceLabsWebhookRequest req = new PriceLabsWebhookRequest();
                    req.setType("price_update");
                    req.setTimestamp(timestamp);

                    PriceLabsWebhookRequest.ListingData listing = new PriceLabsWebhookRequest.ListingData();
                    listing.setListingId(listingId);
                    if (ratePlanId != null && !ratePlanId.isBlank()) {
                        listing.setRatePlanId(ratePlanId);
                    }

                    List<PriceLabsWebhookRequest.CalendarData> calendar = new ArrayList<>();
                    for (JsonNode item : data) {
                        if (item == null || !item.isObject()) {
                            continue;
                        }
                        String date = readString(item, "date", "start_date", "startDate");
                        if (date == null || date.isBlank()) {
                            continue;
                        }
                        JsonNode priceNode = item.get("price");
                        if (priceNode == null || priceNode.isNull()) {
                            continue;
                        }

                        PriceLabsWebhookRequest.CalendarData cal = new PriceLabsWebhookRequest.CalendarData();
                        cal.setDate(date.length() >= 10 ? date.substring(0, 10) : date);
                        cal.setPrice(parseBigDecimal(priceNode));

                        Integer minStay = readInt(item, "min_stay", "minStay");
                        Integer maxStay = readInt(item, "max_stay", "maxStay");
                        if (minStay != null) cal.setMinStay(minStay);
                        if (maxStay != null) cal.setMaxStay(maxStay);

                        calendar.add(cal);
                    }
                    listing.setCalendar(calendar);
                    req.setData(List.of(listing));
                    return new NormalizedPayload(req, "compact");
                }
            }

            PriceLabsWebhookRequest req = objectMapper.readValue(rawBody, PriceLabsWebhookRequest.class);
            return new NormalizedPayload(req, "standard");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IllegalArgumentException("Normalize PriceLabs /sync payload failed: " + msg, e);
        }
    }

    private static String readString(JsonNode node, String... keys) {
        if (node == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) continue;
            JsonNode v = node.get(key);
            if (v != null && v.isTextual()) {
                String s = v.asText().trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }

    private static Integer readInt(JsonNode node, String... keys) {
        if (node == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) continue;
            JsonNode v = node.get(key);
            if (v == null || v.isNull()) continue;
            if (v.isInt() || v.isLong()) return v.asInt();
            if (v.isTextual()) {
                String s = v.asText().trim();
                if (s.isEmpty()) continue;
                try {
                    return Integer.parseInt(s);
                } catch (Exception ignored) {
                    // noop
                }
            }
        }
        return null;
    }

    private static BigDecimal parseBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return new BigDecimal(node.asText());
        }
        if (node.isTextual()) {
            String s = node.asText().trim();
            if (s.isEmpty()) return null;
            return new BigDecimal(s);
        }
        return null;
    }
}

