package server.demo.service;

import org.springframework.stereotype.Component;
import server.demo.entity.ChannelReview;
import server.demo.enums.ReviewActionStatus;
import server.demo.enums.ReviewActionType;
import server.demo.enums.ReviewAssociationStatus;

import server.demo.i18n.ApiMessages;
@Component
public class ReviewEligibilityService {

    public String unavailableReason(ChannelReview review, ReviewActionType actionType) {
        if (review == null) {
            return ApiMessages.get("api.t.dd59c417cc66");
        }
        if (review.getAssociationStatus() != ReviewAssociationStatus.LINKED || review.getReservationId() == null) {
            return firstNonBlank(review.getAssociationReason(), ApiMessages.get("api.t.15a57c558b1b"));
        }
        if (review.getSuChannelId() == null
                || (review.getSuChannelId() != SuReviewService.CHANNEL_AIRBNB
                && review.getSuChannelId() != SuReviewService.CHANNEL_BOOKING)) {
            return ApiMessages.get("api.t.2cd143ef3fb3");
        }
        if (!"guest_to_host".equalsIgnoreCase(review.getReviewType())) {
            return ApiMessages.get("api.t.6576b5d65233");
        }
        if (isActive(review.getLastActionStatus())) {
            return ApiMessages.get("api.t.1571783d3660");
        }
        if (isBlank(review.getHotelId())
                || isBlank(review.getChannelPropertyId())
                || isBlank(review.getChannelReviewId())) {
            return ApiMessages.get("api.t.7ae744b9abf6");
        }
        if (review.getSuChannelId() == SuReviewService.CHANNEL_AIRBNB && isBlank(review.getListingId())) {
            return ApiMessages.get("api.t.f8369b63e5c8");
        }

        if (actionType == ReviewActionType.REPLY) {
            if (!Boolean.TRUE.equals(review.getCanReply())) {
                return ApiMessages.get("api.t.4929c26741aa");
            }
            if (!isBlank(review.getReplyText())) {
                return ApiMessages.get("api.t.f5cee0b15c13");
            }
            if (review.getSuChannelId() == SuReviewService.CHANNEL_BOOKING
                    && isBlank(review.getReviewText())
                    && isBlank(review.getNegativeReviewText())
                    && isBlank(review.getReviewTitle())) {
                return ApiMessages.get("api.t.8615d95b5e46");
            }
            return null;
        }

        if (actionType == ReviewActionType.GUEST_REVIEW) {
            if (review.getSuChannelId() != SuReviewService.CHANNEL_AIRBNB) {
                return ApiMessages.get("api.t.358aebb298fd");
            }
            if (!Boolean.TRUE.equals(review.getCanReviewGuest())) {
                return ApiMessages.get("api.t.8fea5f7bef2c");
            }
            return null;
        }

        return ApiMessages.get("api.t.95bddbb88a9a");
    }

    private static boolean isActive(ReviewActionStatus status) {
        return status == ReviewActionStatus.PENDING || status == ReviewActionStatus.SUBMITTED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }
}
