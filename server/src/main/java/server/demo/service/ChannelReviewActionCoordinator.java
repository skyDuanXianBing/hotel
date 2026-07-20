package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.ChannelReview;
import server.demo.entity.ChannelReviewAction;
import server.demo.enums.ReviewActionStatus;
import server.demo.enums.ReviewActionType;
import server.demo.repository.ChannelReviewActionRepository;
import server.demo.repository.ChannelReviewRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChannelReviewActionCoordinator {

    private static final List<ReviewActionStatus> ACTIVE_STATUSES =
            List.of(ReviewActionStatus.PENDING, ReviewActionStatus.SUBMITTED);

    private final ChannelReviewRepository reviewRepository;
    private final ChannelReviewActionRepository actionRepository;
    private final ReviewEligibilityService eligibilityService;
    private final SuReviewWebhookMappingValidator mappingValidator;

    public ChannelReviewActionCoordinator(
            ChannelReviewRepository reviewRepository,
            ChannelReviewActionRepository actionRepository,
            ReviewEligibilityService eligibilityService,
            SuReviewWebhookMappingValidator mappingValidator
    ) {
        this.reviewRepository = reviewRepository;
        this.actionRepository = actionRepository;
        this.eligibilityService = eligibilityService;
        this.mappingValidator = mappingValidator;
    }

    @Transactional
    public PreparedAction prepare(
            Long storeId,
            Long reviewId,
            Long userId,
            String expectedHotelId,
            ReviewActionType actionType,
            String idempotencyKey,
            String requestHash
    ) {
        ChannelReview review = reviewRepository.findForUpdateByStoreIdAndId(storeId, reviewId)
                .orElseThrow(() -> new SuReviewService.ReviewNotFoundException("评价不存在"));
        if (expectedHotelId == null
                || review.getHotelId() == null
                || !expectedHotelId.equals(review.getHotelId().trim())) {
            throw new SuReviewService.ReviewConflictException(
                    "评价记录不属于当前门店唯一的 Su hotel_id"
            );
        }
        try {
            mappingValidator.assertCurrentMapping(
                    storeId,
                    expectedHotelId,
                    review.getSuChannelId(),
                    review.getChannelPropertyId(),
                    review.getListingId()
            );
        } catch (SuReviewWebhookMappingValidator.MappingRejectedException e) {
            throw new SuReviewService.ReviewConflictException(
                    "当前 Review 渠道映射已失效：" + e.getMessage()
            );
        }

        ChannelReviewAction existing =
                actionRepository.findByStoreIdAndIdempotencyKey(storeId, idempotencyKey).orElse(null);
        if (existing != null) {
            if (!reviewId.equals(existing.getReviewId())
                    || existing.getActionType() != actionType
                    || !requestHash.equals(existing.getRequestHash())) {
                throw new SuReviewService.ReviewConflictException("幂等键已用于其他评价请求");
            }
            return new PreparedAction(review, existing, true);
        }

        String unavailableReason = eligibilityService.unavailableReason(review, actionType);
        if (unavailableReason != null) {
            throw new SuReviewService.ReviewConflictException(unavailableReason);
        }
        if (actionRepository.existsByStoreIdAndReviewIdAndActionTypeAndStatusIn(
                storeId, reviewId, actionType, ACTIVE_STATUSES)) {
            throw new SuReviewService.ReviewConflictException("相同评价动作已在处理中");
        }

        ChannelReviewAction action = new ChannelReviewAction();
        action.setStoreId(storeId);
        action.setReviewId(reviewId);
        action.setActionType(actionType);
        action.setStatus(ReviewActionStatus.PENDING);
        action.setIdempotencyKey(idempotencyKey);
        action.setRequestHash(requestHash);
        action.setOperatorUserId(userId);
        action = actionRepository.save(action);

        review.setLastActionType(actionType);
        review.setLastActionStatus(ReviewActionStatus.PENDING);
        reviewRepository.save(review);
        return new PreparedAction(review, action, false);
    }

    @Transactional
    public ChannelReviewAction markSubmitted(
            Long storeId,
            Long reviewId,
            Long actionId,
            String message,
            String ruid
    ) {
        ChannelReview review = reviewRepository.findForUpdateByStoreIdAndId(storeId, reviewId)
                .orElseThrow(() -> new SuReviewService.ReviewNotFoundException("评价不存在"));
        ChannelReviewAction action = actionRepository.findById(actionId)
                .filter(item -> storeId.equals(item.getStoreId()) && reviewId.equals(item.getReviewId()))
                .orElseThrow(() -> new SuReviewService.ReviewNotFoundException("评价动作不存在"));

        LocalDateTime now = LocalDateTime.now();
        action.setStatus(ReviewActionStatus.SUBMITTED);
        action.setResponseMessage(trim(message, 1000));
        action.setSuRuid(trim(ruid, 100));
        action.setSubmittedAt(now);
        action = actionRepository.save(action);

        review.setLastActionType(action.getActionType());
        review.setLastActionStatus(ReviewActionStatus.SUBMITTED);
        review.setSuRuid(trim(ruid, 100));
        if (action.getActionType() == ReviewActionType.REPLY) {
            review.setCanReply(false);
        } else if (action.getActionType() == ReviewActionType.GUEST_REVIEW) {
            review.setCanReviewGuest(false);
        }
        reviewRepository.save(review);
        return action;
    }

    @Transactional
    public ChannelReviewAction markFailed(
            Long storeId,
            Long reviewId,
            Long actionId,
            String message,
            String errorCode,
            String ruid
    ) {
        ChannelReview review = reviewRepository.findForUpdateByStoreIdAndId(storeId, reviewId)
                .orElseThrow(() -> new SuReviewService.ReviewNotFoundException("评价不存在"));
        ChannelReviewAction action = actionRepository.findById(actionId)
                .filter(item -> storeId.equals(item.getStoreId()) && reviewId.equals(item.getReviewId()))
                .orElseThrow(() -> new SuReviewService.ReviewNotFoundException("评价动作不存在"));

        action.setStatus(ReviewActionStatus.FAILED);
        action.setResponseMessage(trim(message, 1000));
        action.setSuErrorCode(trim(errorCode, 50));
        action.setSuRuid(trim(ruid, 100));
        action = actionRepository.save(action);

        review.setLastActionType(action.getActionType());
        review.setLastActionStatus(ReviewActionStatus.FAILED);
        reviewRepository.save(review);
        return action;
    }

    public record PreparedAction(ChannelReview review, ChannelReviewAction action, boolean replay) {
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
