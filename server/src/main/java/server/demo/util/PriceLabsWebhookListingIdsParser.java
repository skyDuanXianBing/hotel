package server.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * PriceLabs /sync webhook listing_ids parser.
 */
public final class PriceLabsWebhookListingIdsParser {

    private PriceLabsWebhookListingIdsParser() {}

    public record ListingIdsPayload(List<String> listingIds, boolean hasPriceData) {}

    /**
     * Parse raw body, collect listing_ids and detect if price data exists.
     *
     * @param objectMapper Jackson ObjectMapper
     * @param rawBody webhook raw body
     * @return listing ids + has price data
     */
    public static ListingIdsPayload parse(ObjectMapper objectMapper, String rawBody) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper is required");
        }
        if (rawBody == null || rawBody.isBlank()) {
            return new ListingIdsPayload(List.of(), false);
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            Set<String> listingIds = new LinkedHashSet<>();
            collectListingIds(root, listingIds);
            boolean hasPriceData = containsPriceData(root);
            return new ListingIdsPayload(new ArrayList<>(listingIds), hasPriceData);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new IllegalArgumentException("Parse PriceLabs sync payload failed: " + msg, e);
        }
    }

    private static void collectListingIds(JsonNode node, Set<String> out) {
        if (node == null || out == null) {
            return;
        }

        addListingId(node, out, "listing_id");
        addListingId(node, out, "listingId");

        JsonNode listingIdsNode = node.get("listing_ids");
        if (listingIdsNode != null && listingIdsNode.isArray()) {
            for (JsonNode item : listingIdsNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                String val = item.asText(null);
                if (val != null) {
                    String trimmed = val.trim();
                    if (!trimmed.isEmpty()) {
                        out.add(trimmed);
                    }
                }
            }
        }

        JsonNode data = node.get("data");
        if (data == null || data.isNull()) {
            return;
        }
        if (data.isArray()) {
            for (JsonNode item : data) {
                collectListingIds(item, out);
            }
        } else if (data.isObject()) {
            collectListingIds(data, out);
        }
    }

    private static void addListingId(JsonNode node, Set<String> out, String key) {
        if (node == null || out == null || key == null) {
            return;
        }
        JsonNode v = node.get(key);
        if (v != null && v.isTextual()) {
            String trimmed = v.asText().trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
    }

    private static boolean containsPriceData(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.has("calendar")) {
            return true;
        }
        JsonNode data = node.get("data");
        if (data == null || data.isNull()) {
            return false;
        }
        if (data.isArray()) {
            for (JsonNode item : data) {
                if (item == null || item.isNull()) {
                    continue;
                }
                if (item.has("calendar")) {
                    return true;
                }
                if (item.has("price") || item.has("date") || item.has("start_date") || item.has("startDate")) {
                    return true;
                }
            }
            return false;
        }
        if (data.isObject()) {
            return data.has("calendar")
                    || data.has("price")
                    || data.has("date")
                    || data.has("start_date")
                    || data.has("startDate");
        }
        return false;
    }
}
