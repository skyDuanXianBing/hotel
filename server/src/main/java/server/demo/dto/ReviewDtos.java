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
            @NotBlank(message = "{api.t.f5da0888cd26}")
            @Size(max = 5000, message = "{api.t.53cb4eace618}")
            String reviewReply,
            @NotBlank(message = "{api.t.d5fb56f49c83}")
            @Size(min = 8, max = 120, message = "{api.t.5cd5160b6aac}")
            String idempotencyKey
    ) {
    }

    public record GuestReviewRequest(
            @NotBlank(message = "{api.t.d5fb56f49c83}")
            @Size(min = 8, max = 120, message = "{api.t.5cd5160b6aac}")
            String idempotencyKey,
            @AssertTrue(message = "{api.t.d6eed374b0e2}")
            boolean confirmed,
            @NotNull(message = "{api.t.24426efdd37c}")
            Boolean isRevieweeRecommended,
            @NotBlank(message = "{api.t.ffa489420b6e}")
            @Size(max = 999, message = "{api.t.933358840823}")
            String publicReview,
            @Size(max = 999, message = "{api.t.da00e45df564}")
            String privateFeedback,
            @NotNull(message = "{api.t.ac405167a599}")
            @Size(min = 3, max = 3, message = "{api.t.78ca42f7d2ed}")
            List<@Valid CategoryRating> categoryRatings
    ) {
    }

    public record CategoryRating(
            @NotBlank(message = "{api.t.0d1f57e61665}")
            String category,
            @Min(value = 1, message = "{api.t.8711bfeadf1a}")
            @Max(value = 5, message = "{api.t.9cfe491cb2b8}")
            int rating,
            @Size(max = 50, message = "{api.t.55670a33f2ea}")
            String comment,
            @Size(max = 20, message = "{api.t.95d5ee8282e5}")
            List<@NotBlank(message = "{api.t.e734811b2556}") String> reviewCategoryTags
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
