package server.demo.service;

import org.springframework.stereotype.Component;
import server.demo.dto.ReviewDtos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import server.demo.i18n.ApiMessages;
@Component
public class AirbnbGuestReviewValidator {

    private static final List<String> REQUIRED_CATEGORIES =
            List.of("cleanliness", "communication", "respect_house_rules");

    private static final Map<String, Set<String>> TAGS_BY_CATEGORY = Map.of(
            "cleanliness", Set.of(
                    "host_review_guest_positive_neat_and_tidy",
                    "host_review_guest_positive_kept_in_good_condition",
                    "host_review_guest_positive_took_care_of_garbage",
                    "host_review_guest_negative_ignored_checkout_directions",
                    "host_review_guest_negative_garbage",
                    "host_review_guest_negative_messy_kitchen",
                    "host_review_guest_negative_damage",
                    "host_review_guest_negative_ruined_bed_linens",
                    "cleanliness_other"
            ),
            "communication", Set.of(
                    "host_review_guest_positive_helpful_messages",
                    "host_review_guest_positive_respectful",
                    "host_review_guest_negative_unhelpful_messages",
                    "host_review_guest_negative_disrespectful",
                    "host_review_guest_negative_unreachable",
                    "host_review_guest_negative_slow_responses",
                    "communication_other"
            ),
            "respect_house_rules", Set.of(
                    "host_review_guest_negative_arrived_early",
                    "host_review_guest_negative_stayed_past_checkout",
                    "host_review_guest_negative_unapproved_guests",
                    "host_review_guest_negative_unapproved_pet",
                    "host_review_guest_negative_did_not_respect_quiet_hours",
                    "host_review_guest_negative_unapproved_filming",
                    "host_review_guest_negative_unapproved_event",
                    "host_review_guest_negative_smoking",
                    "respect_house_rules_other"
            )
    );

    public List<Map<String, Object>> validateAndBuild(ReviewDtos.GuestReviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.bbcb32b8e386"));
        }
        if (!request.confirmed()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d6eed374b0e2"));
        }
        if (request.categoryRatings() == null || request.categoryRatings().size() != 3) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.78ca42f7d2ed"));
        }

        Map<String, ReviewDtos.CategoryRating> byCategory = new LinkedHashMap<>();
        for (ReviewDtos.CategoryRating rating : request.categoryRatings()) {
            if (rating == null || rating.category() == null) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.0d1f57e61665"));
            }
            String category = rating.category().trim().toLowerCase(Locale.ROOT);
            if (!REQUIRED_CATEGORIES.contains(category)) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.bf0cd9271eeb") + rating.category());
            }
            if (byCategory.putIfAbsent(category, rating) != null) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.a2a52c0828c5") + category);
            }
        }
        if (!byCategory.keySet().containsAll(REQUIRED_CATEGORIES)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.78ca42f7d2ed"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String category : REQUIRED_CATEGORIES) {
            ReviewDtos.CategoryRating rating = byCategory.get(category);
            if (rating.rating() < 1 || rating.rating() > 5) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.cf5d7b33acb4"));
            }
            String comment = trimToNull(rating.comment());
            if (rating.rating() < 5 && comment == null) {
                throw new IllegalArgumentException(category + ApiMessages.get("api.t.55517b0956c7"));
            }
            if (comment != null && comment.length() > 50) {
                throw new IllegalArgumentException(category + ApiMessages.get("api.t.75119af1bcb0"));
            }

            Set<String> normalizedTags = new LinkedHashSet<>();
            if (rating.reviewCategoryTags() != null) {
                for (String rawTag : rating.reviewCategoryTags()) {
                    String tag = trimToNull(rawTag);
                    if (tag == null || !TAGS_BY_CATEGORY.get(category).contains(tag)) {
                        throw new IllegalArgumentException(ApiMessages.get("api.t.d32ac3b3ef24") + category + ": " + rawTag);
                    }
                    normalizedTags.add(tag);
                }
            }

            if (requiresNegativeTag(category, rating.rating()) && normalizedTags.isEmpty()) {
                normalizedTags.add(category + "_other");
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", category);
            item.put("rating", rating.rating());
            if (comment != null) {
                item.put("comment", comment);
            }
            if (!normalizedTags.isEmpty()) {
                item.put("review_category_tags", List.copyOf(normalizedTags));
            }
            result.add(item);
        }
        return result;
    }

    private static boolean requiresNegativeTag(String category, int rating) {
        if ("respect_house_rules".equals(category)) {
            return rating < 5;
        }
        return rating < 4;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
