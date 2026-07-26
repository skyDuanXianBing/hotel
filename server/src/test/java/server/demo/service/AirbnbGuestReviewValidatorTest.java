package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.dto.ReviewDtos;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirbnbGuestReviewValidatorTest {

    private final AirbnbGuestReviewValidator validator = new AirbnbGuestReviewValidator();

    @Test
    void validatesAllCategoriesAndAddsOfficialOtherTagsAtNegativeThresholds() {
        ReviewDtos.GuestReviewRequest request = new ReviewDtos.GuestReviewRequest(
                "review-key-123",
                true,
                false,
                "Public review",
                "Private feedback",
                List.of(
                        new ReviewDtos.CategoryRating("cleanliness", 3, "Needs cleaning", List.of()),
                        new ReviewDtos.CategoryRating("communication", 4, "Could reply faster", null),
                        new ReviewDtos.CategoryRating("respect_house_rules", 4, "Late checkout", null)
                )
        );

        List<Map<String, Object>> result = validator.validateAndBuild(request);

        assertEquals(3, result.size());
        assertEquals(List.of("cleanliness_other"), result.get(0).get("review_category_tags"));
        assertTrue(!result.get(1).containsKey("review_category_tags"));
        assertEquals(List.of("respect_house_rules_other"), result.get(2).get("review_category_tags"));
    }

    @Test
    void rejectsMissingLowScoreComment() {
        ReviewDtos.GuestReviewRequest request = new ReviewDtos.GuestReviewRequest(
                "review-key-123",
                true,
                true,
                "Public review",
                null,
                List.of(
                        new ReviewDtos.CategoryRating("cleanliness", 5, null, null),
                        new ReviewDtos.CategoryRating("communication", 4, null, null),
                        new ReviewDtos.CategoryRating("respect_house_rules", 5, null, null)
                )
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateAndBuild(request)
        );
        assertTrue(error.getMessage().contains("低于5分"));
    }

    @Test
    void rejectsTagFromWrongCategory() {
        ReviewDtos.GuestReviewRequest request = new ReviewDtos.GuestReviewRequest(
                "review-key-123",
                true,
                true,
                "Public review",
                null,
                List.of(
                        new ReviewDtos.CategoryRating(
                                "cleanliness",
                                5,
                                null,
                                List.of("host_review_guest_positive_respectful")
                        ),
                        new ReviewDtos.CategoryRating("communication", 5, null, null),
                        new ReviewDtos.CategoryRating("respect_house_rules", 5, null, null)
                )
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validateAndBuild(request));
    }
}
