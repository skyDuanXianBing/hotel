package server.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ReviewDtos {
    private ReviewDtos() {
    }

    public record Review(
            Long id,
            Long reservationId,
            String orderNumber,
            String channelBookingId,
            String guestName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String reservationStatus,
            String channelCode,
            Integer suChannelId,
            String propertyName,
            String reviewType,
            String reviewStatus,
            String reviewTitle,
            String reviewText,
            String negativeReviewText,
            BigDecimal overallScore,
            String replyText,
            String associationStatus,
            String associationReason,
            String lastActionStatus,
            LocalDateTime receivedAt,
            LocalDateTime lastSyncedAt,
            Map<String, BigDecimal> categoryRatings,
            String privateFeedback,
            List<String> allowedActions,
            Map<String, String> actionReasons,
            List<ActionAudit> actions
    ) {
    }

    public record PageResponse(
            List<Review> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<String> allowedActions,
            Map<String, String> actionReasons
    ) {
    }

    public record ReplyRequest(
            @NotBlank(message = "回复内容不能为空")
            @Size(max = 5000, message = "回复内容不能超过5000个字符")
            String reviewReply,
            @NotBlank(message = "幂等键不能为空")
            @Size(min = 8, max = 120, message = "幂等键长度必须为8到120个字符")
            String idempotencyKey
    ) {
    }

    public record GuestReviewRequest(
            @NotBlank(message = "幂等键不能为空")
            @Size(min = 8, max = 120, message = "幂等键长度必须为8到120个字符")
            String idempotencyKey,
            @AssertTrue(message = "提交评价住客前必须二次确认")
            boolean confirmed,
            @NotNull(message = "必须明确是否推荐住客")
            Boolean isRevieweeRecommended,
            @NotBlank(message = "公开评价不能为空")
            @Size(max = 999, message = "公开评价必须少于1000个字符")
            String publicReview,
            @Size(max = 999, message = "私密反馈必须少于1000个字符")
            String privateFeedback,
            @NotNull(message = "分类评分不能为空")
            @Size(min = 3, max = 3, message = "必须提交清洁、沟通和遵守房屋规则三项评分")
            List<@Valid CategoryRating> categoryRatings
    ) {
    }

    public record CategoryRating(
            @NotBlank(message = "评分分类不能为空")
            String category,
            @Min(value = 1, message = "评分最低为1")
            @Max(value = 5, message = "评分最高为5")
            int rating,
            @Size(max = 50, message = "低分说明不能超过50个字符")
            String comment,
            @Size(max = 20, message = "每项评分最多提交20个标签")
            List<@NotBlank(message = "评分标签不能为空") String> reviewCategoryTags
    ) {
    }

    public record ActionAudit(
            Long id,
            String actionType,
            String status,
            Long operatorUserId,
            String responseMessage,
            String errorCode,
            LocalDateTime submittedAt,
            LocalDateTime confirmedAt,
            LocalDateTime createdAt
    ) {
    }

    public record ActionResult(
            Long actionId,
            String actionType,
            String status,
            String message,
            LocalDateTime submittedAt,
            Review review
    ) {
    }

    public record SyncResult(
            boolean success,
            int fetched,
            int created,
            int updated,
            int unlinked,
            String message,
            LocalDateTime syncedAt
    ) {
    }
}
