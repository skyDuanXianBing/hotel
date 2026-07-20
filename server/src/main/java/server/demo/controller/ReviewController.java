package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.ReviewDtos;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.SuReviewService;
import server.demo.util.StoreContextUtils;

@RestController
@RequestMapping("/api/v1/reviews")
@StoreScoped(warmupChannelPrices = false)
public class ReviewController {

    private final SuReviewService reviewService;

    public ReviewController(SuReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @RequirePermission(module = PermissionModule.REVIEW, action = PermissionAction.VIEW)
    public ResponseEntity<ApiResponse<ReviewDtos.PageResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false) String search
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success(reviewService.listReviews(
                    StoreContextUtils.requireStoreId(),
                    StoreContextUtils.requireUserId(),
                    page,
                    size,
                    tab,
                    channel,
                    reservationId,
                    search
            )));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (SuReviewService.ReviewForbiddenException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @RequirePermission(module = PermissionModule.REVIEW, action = PermissionAction.VIEW)
    public ResponseEntity<ApiResponse<ReviewDtos.Review>> detail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(reviewService.getReview(
                    StoreContextUtils.requireStoreId(),
                    StoreContextUtils.requireUserId(),
                    id
            )));
        } catch (SuReviewService.ReviewNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (SuReviewService.ReviewForbiddenException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/sync")
    @RequirePermission(module = PermissionModule.REVIEW, action = PermissionAction.SYNC)
    public ResponseEntity<ApiResponse<ReviewDtos.SyncResult>> sync() {
        try {
            ReviewDtos.SyncResult result = reviewService.syncReviews(
                    StoreContextUtils.requireStoreId(),
                    StoreContextUtils.requireUserId()
            );
            if (!result.success()) {
                return ResponseEntity.status(502).body(ApiResponse.error(result.message(), result));
            }
            return ResponseEntity.ok(ApiResponse.success(result.message(), result));
        } catch (SuReviewService.ReviewForbiddenException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (SuReviewService.ReviewNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reply")
    @RequirePermission(module = PermissionModule.REVIEW, action = PermissionAction.REPLY)
    public ResponseEntity<ApiResponse<ReviewDtos.ActionResult>> reply(
            @PathVariable Long id,
            @Valid @RequestBody ReviewDtos.ReplyRequest request
    ) {
        return executeAction(() -> reviewService.reply(
                StoreContextUtils.requireStoreId(),
                StoreContextUtils.requireUserId(),
                id,
                request
        ));
    }

    @PostMapping("/{id}/guest-review")
    @RequirePermission(module = PermissionModule.REVIEW, action = PermissionAction.REVIEW_GUEST)
    public ResponseEntity<ApiResponse<ReviewDtos.ActionResult>> reviewGuest(
            @PathVariable Long id,
            @Valid @RequestBody ReviewDtos.GuestReviewRequest request
    ) {
        return executeAction(() -> reviewService.reviewGuest(
                StoreContextUtils.requireStoreId(),
                StoreContextUtils.requireUserId(),
                id,
                request
        ));
    }

    private ResponseEntity<ApiResponse<ReviewDtos.ActionResult>> executeAction(ActionCall call) {
        try {
            ReviewDtos.ActionResult result = call.execute();
            return ResponseEntity.ok(ApiResponse.success(result.message(), result));
        } catch (SuReviewService.ReviewNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (SuReviewService.ReviewForbiddenException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (SuReviewService.ReviewConflictException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (SuReviewService.SuWriteFailedException e) {
            return ResponseEntity.status(502).body(ApiResponse.error(e.getMessage(), e.getResult()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @FunctionalInterface
    private interface ActionCall {
        ReviewDtos.ActionResult execute();
    }
}
