package server.demo.service;

import org.springframework.stereotype.Component;
import server.demo.entity.ChannelReview;
import server.demo.enums.ReviewActionStatus;
import server.demo.enums.ReviewActionType;
import server.demo.enums.ReviewAssociationStatus;

@Component
public class ReviewEligibilityService {

    public String unavailableReason(ChannelReview review, ReviewActionType actionType) {
        if (review == null) {
            return "评价不存在";
        }
        if (review.getAssociationStatus() != ReviewAssociationStatus.LINKED || review.getReservationId() == null) {
            return firstNonBlank(review.getAssociationReason(), "评价尚未与本地订单建立确定关联");
        }
        if (review.getSuChannelId() == null
                || (review.getSuChannelId() != SuReviewService.CHANNEL_AIRBNB
                && review.getSuChannelId() != SuReviewService.CHANNEL_BOOKING)) {
            return "当前渠道不在本期评价能力范围内";
        }
        if (!"guest_to_host".equalsIgnoreCase(review.getReviewType())) {
            return "只有住客对房源的评价可执行此操作";
        }
        if (isActive(review.getLastActionStatus())) {
            return "该评价已有渠道写操作处理中，请等待同步结果";
        }
        if (isBlank(review.getHotelId())
                || isBlank(review.getChannelPropertyId())
                || isBlank(review.getChannelReviewId())) {
            return "评价缺少 Su 渠道写入标识";
        }
        if (review.getSuChannelId() == SuReviewService.CHANNEL_AIRBNB && isBlank(review.getListingId())) {
            return "Airbnb 评价缺少 listing_id";
        }

        if (actionType == ReviewActionType.REPLY) {
            if (!Boolean.TRUE.equals(review.getCanReply())) {
                return "Su 未授予当前评价回复资格";
            }
            if (!isBlank(review.getReplyText())) {
                return "该评价已有回复";
            }
            if (review.getSuChannelId() == SuReviewService.CHANNEL_BOOKING
                    && isBlank(review.getReviewText())
                    && isBlank(review.getNegativeReviewText())
                    && isBlank(review.getReviewTitle())) {
                return "Booking.com 无正文评价不可回复";
            }
            return null;
        }

        if (actionType == ReviewActionType.GUEST_REVIEW) {
            if (review.getSuChannelId() != SuReviewService.CHANNEL_AIRBNB) {
                return "只有 Airbnb 支持评价住客";
            }
            if (!Boolean.TRUE.equals(review.getCanReviewGuest())) {
                return "Su 未授予当前订单评价住客资格";
            }
            return null;
        }

        return "不支持的评价动作";
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
