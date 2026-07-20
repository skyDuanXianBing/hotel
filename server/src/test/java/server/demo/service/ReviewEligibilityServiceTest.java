package server.demo.service;

import org.junit.jupiter.api.Test;
import server.demo.entity.ChannelReview;
import server.demo.enums.ReviewActionType;
import server.demo.enums.ReviewAssociationStatus;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewEligibilityServiceTest {

    private final ReviewEligibilityService service = new ReviewEligibilityService();

    @Test
    void bookingGuestReviewIsAlwaysRejectedByBackend() {
        ChannelReview review = eligibleReview(SuReviewService.CHANNEL_BOOKING);
        review.setCanReviewGuest(true);

        String reason = service.unavailableReason(review, ReviewActionType.GUEST_REVIEW);

        assertTrue(reason.contains("只有 Airbnb"));
    }

    @Test
    void bookingReplyRequiresReviewBody() {
        ChannelReview review = eligibleReview(SuReviewService.CHANNEL_BOOKING);
        review.setCanReply(true);

        String reason = service.unavailableReason(review, ReviewActionType.REPLY);

        assertTrue(reason.contains("无正文"));
        review.setReviewText("Guest feedback");
        assertNull(service.unavailableReason(review, ReviewActionType.REPLY));
    }

    @Test
    void unresolvedReviewNeverAllowsRemoteWrite() {
        ChannelReview review = eligibleReview(SuReviewService.CHANNEL_AIRBNB);
        review.setAssociationStatus(ReviewAssociationStatus.UNLINKED);
        review.setReservationId(null);
        review.setAssociationReason("预订号缺失");
        review.setCanReply(true);
        review.setCanReviewGuest(true);

        assertTrue(service.unavailableReason(review, ReviewActionType.REPLY).contains("预订号缺失"));
        assertTrue(service.unavailableReason(review, ReviewActionType.GUEST_REVIEW).contains("预订号缺失"));
    }

    private static ChannelReview eligibleReview(int channelId) {
        ChannelReview review = new ChannelReview();
        review.setId(1L);
        review.setStoreId(10L);
        review.setReservationId(20L);
        review.setAssociationStatus(ReviewAssociationStatus.LINKED);
        review.setSuChannelId(channelId);
        review.setReviewType("guest_to_host");
        review.setHotelId("HOTEL1");
        review.setChannelPropertyId("PROPERTY1");
        review.setChannelReviewId(channelId + "_review");
        review.setListingId(channelId == SuReviewService.CHANNEL_AIRBNB ? "listing-1" : null);
        return review;
    }
}
