package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.ReviewDtos;
import server.demo.entity.ChannelReview;
import server.demo.entity.ChannelReviewAction;
import server.demo.entity.Reservation;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.enums.ReviewActionStatus;
import server.demo.enums.ReviewActionType;
import server.demo.enums.ReviewAssociationStatus;
import server.demo.repository.ChannelReviewActionRepository;
import server.demo.repository.ChannelReviewRepository;
import server.demo.repository.ReservationRepository;
import server.demo.service.ChannelReviewActionCoordinator.PreparedAction;
import server.demo.service.SuReviewPayloadMapper.NormalizedReview;
import server.demo.service.SuReviewWebhookMappingValidator.CurrentMappingSnapshot;
import server.demo.service.SuReviewWebhookMappingValidator.MappingRejectedException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SuReviewService {

    private static final Logger logger = LoggerFactory.getLogger(SuReviewService.class);

    public static final int CHANNEL_BOOKING = 19;
    public static final int CHANNEL_AIRBNB = 244;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int PULL_PAGE_SIZE = 100;
    private static final int MAX_PULL_PAGES = 50;
    private static final List<ReviewActionStatus> SUBMITTED_STATUSES =
            List.of(ReviewActionStatus.SUBMITTED);

    private static final String ACTION_SYNC = "SYNC";
    private static final String ACTION_REPLY = "REPLY";
    private static final String ACTION_GUEST_REVIEW = "GUEST_REVIEW";

    private final ChannelReviewRepository reviewRepository;
    private final ChannelReviewActionRepository actionRepository;
    private final ReservationRepository reservationRepository;
    private final SuReviewHotelOwnershipValidator hotelOwnershipValidator;
    private final PermissionService permissionService;
    private final ReviewEligibilityService eligibilityService;
    private final AirbnbGuestReviewValidator guestReviewValidator;
    private final ChannelReviewActionCoordinator actionCoordinator;
    private final SuReviewWebhookMappingValidator mappingValidator;
    private final SuReviewPayloadMapper payloadMapper;
    private final SuReviewClient reviewClient;
    private final SuApiClient suApiClient;
    private final ObjectMapper objectMapper;

    public SuReviewService(
            ChannelReviewRepository reviewRepository,
            ChannelReviewActionRepository actionRepository,
            ReservationRepository reservationRepository,
            SuReviewHotelOwnershipValidator hotelOwnershipValidator,
            PermissionService permissionService,
            ReviewEligibilityService eligibilityService,
            AirbnbGuestReviewValidator guestReviewValidator,
            ChannelReviewActionCoordinator actionCoordinator,
            SuReviewWebhookMappingValidator mappingValidator,
            SuReviewPayloadMapper payloadMapper,
            SuReviewClient reviewClient,
            SuApiClient suApiClient,
            ObjectMapper objectMapper
    ) {
        this.reviewRepository = reviewRepository;
        this.actionRepository = actionRepository;
        this.reservationRepository = reservationRepository;
        this.hotelOwnershipValidator = hotelOwnershipValidator;
        this.permissionService = permissionService;
        this.eligibilityService = eligibilityService;
        this.guestReviewValidator = guestReviewValidator;
        this.actionCoordinator = actionCoordinator;
        this.mappingValidator = mappingValidator;
        this.payloadMapper = payloadMapper;
        this.reviewClient = reviewClient;
        this.suApiClient = suApiClient;
        this.objectMapper = objectMapper;
    }

    public ReviewDtos.PageResponse listReviews(
            Long storeId,
            Long userId,
            Integer page,
            Integer size,
            String tab,
            String channel,
            Long reservationId,
            String search
    ) {
        ensurePermission(storeId, userId, PermissionAction.VIEW);
        int normalizedPage = page == null ? 0 : page;
        int normalizedSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (normalizedPage < 0) {
            throw new IllegalArgumentException("page 不能小于0");
        }
        if (normalizedSize < 1 || normalizedSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size 必须在1到100之间");
        }

        String normalizedTab = normalizeTab(tab);
        Integer channelId = normalizeChannelFilter(channel);
        String normalizedSearch = trimToNull(search);

        Specification<ChannelReview> specification = storeSpecification(storeId)
                .and(channelSpecification(channelId))
                .and(reservationSpecification(reservationId))
                .and(searchSpecification(normalizedSearch))
                .and(tabSpecification(normalizedTab));

        Sort sort = Sort.by(
                Sort.Order.desc("receivedAt").nullsLast(),
                Sort.Order.desc("id")
        );
        Page<ChannelReview> result = reviewRepository.findAll(
                specification,
                PageRequest.of(normalizedPage, normalizedSize, sort)
        );

        List<ReviewDtos.Review> items = result.getContent().stream()
                .map(review -> toDto(review, userId, false))
                .toList();
        ActionCapabilities syncCapabilities = syncCapabilities(storeId, userId);
        return new ReviewDtos.PageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                syncCapabilities.allowedActions(),
                syncCapabilities.reasons()
        );
    }

    public ReviewDtos.Review getReview(Long storeId, Long userId, Long reviewId) {
        ensurePermission(storeId, userId, PermissionAction.VIEW);
        ChannelReview review = reviewRepository.findByStoreIdAndId(storeId, reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("评价不存在"));
        return toDto(review, userId, true);
    }

    public ReviewDtos.SyncResult syncReviews(Long storeId, Long userId) {
        ensurePermission(storeId, userId, PermissionAction.SYNC);
        String hotelId = hotelOwnershipValidator.requireUniqueOwnership(storeId);
        LocalDateTime syncedAt = LocalDateTime.now();
        int fetched = 0;
        int created = 0;
        int updated = 0;
        int unlinked = 0;
        int skipped = 0;
        Map<String, Integer> skippedReasons = new LinkedHashMap<>();
        String nextPage = "";
        Set<String> seenPageTokens = new HashSet<>();

        try {
            for (int pageIndex = 0; pageIndex < MAX_PULL_PAGES; pageIndex++) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("hotel_id", hotelId);
                payload.put("page_size", PULL_PAGE_SIZE);
                payload.put("next_page", nextPage);

                JsonNode response = reviewClient.pullReviews(payload);
                if (!suApiClient.isSuSuccess(response)) {
                    String message = firstNonBlank(
                            suApiClient.extractSuErrorMessage(response),
                            "Su Review Pull 返回失败"
                    );
                    return new ReviewDtos.SyncResult(
                            false,
                            fetched,
                            created,
                            updated,
                            unlinked,
                            withSkippedSummary(message, skipped, skippedReasons),
                            syncedAt
                    );
                }

                JsonNode data = firstObject(response, "Data", "data");
                JsonNode reviews = data != null ? firstArray(data, "reviews", "Reviews") : null;
                if (reviews != null && !reviews.isEmpty()) {
                    CurrentMappingSnapshot currentMappings =
                            mappingValidator.loadCurrentMappings(storeId, hotelId);
                    for (JsonNode node : reviews) {
                        NormalizedReview normalized = payloadMapper.fromPull(node, hotelId, syncedAt);
                        fetched++;
                        try {
                            currentMappings.assertMapped(
                                    normalized.channelId(),
                                    normalized.channelPropertyId(),
                                    normalized.listingId()
                            );
                        } catch (MappingRejectedException e) {
                            skipped++;
                            String reason = safeExceptionMessage(e);
                            skippedReasons.merge(reason, 1, Integer::sum);
                            logger.warn(
                                    "[SuReviewSync][SECURITY] skipped unmapped review. storeId={}, hotelId={}, channelId={}, channelReviewId={}, reason={}",
                                    storeId,
                                    hotelId,
                                    normalized.channelId(),
                                    normalized.channelReviewId(),
                                    reason
                            );
                            continue;
                        }

                        UpsertOutcome outcome = upsertNormalized(storeId, normalized, true);
                        if (outcome.created()) {
                            created++;
                        } else {
                            updated++;
                        }
                        if (outcome.review().getAssociationStatus() != ReviewAssociationStatus.LINKED) {
                            unlinked++;
                        }
                    }
                }

                String returnedNextPage = data != null ? firstText(data, "next_page", "nextPage") : null;
                if (returnedNextPage == null || returnedNextPage.isBlank()) {
                    break;
                }
                if (!seenPageTokens.add(returnedNextPage)) {
                    return new ReviewDtos.SyncResult(
                            false,
                            fetched,
                            created,
                            updated,
                            unlinked,
                            withSkippedSummary(
                                    "Su Review Pull 返回了重复分页游标，已停止同步",
                                    skipped,
                                    skippedReasons
                            ),
                            syncedAt
                    );
                }
                nextPage = returnedNextPage;
                if (pageIndex == MAX_PULL_PAGES - 1) {
                    return new ReviewDtos.SyncResult(
                            false,
                            fetched,
                            created,
                            updated,
                            unlinked,
                            withSkippedSummary(
                                    "评价数量超过单次同步上限，请稍后继续同步",
                                    skipped,
                                    skippedReasons
                            ),
                            syncedAt
                    );
                }
            }
            return new ReviewDtos.SyncResult(
                    true,
                    fetched,
                    created,
                    updated,
                    unlinked,
                    withSkippedSummary("评价同步完成", skipped, skippedReasons),
                    syncedAt
            );
        } catch (RuntimeException e) {
            return new ReviewDtos.SyncResult(
                    false,
                    fetched,
                    created,
                    updated,
                    unlinked,
                    withSkippedSummary(
                            "评价同步失败: " + safeExceptionMessage(e),
                            skipped,
                            skippedReasons
                    ),
                    syncedAt
            );
        }
    }

    public ReviewDtos.ActionResult reply(
            Long storeId,
            Long userId,
            Long reviewId,
            ReviewDtos.ReplyRequest request
    ) {
        ensurePermission(storeId, userId, PermissionAction.REPLY);
        String hotelId = hotelOwnershipValidator.requireUniqueOwnership(storeId);
        if (request == null) {
            throw new IllegalArgumentException("回复请求不能为空");
        }
        String reply = trimToNull(request.reviewReply());
        if (reply == null) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        String requestHash = hashRequest(Map.of(
                "reviewId", reviewId,
                "action", ReviewActionType.REPLY.name(),
                "reviewReply", reply
        ));

        PreparedAction prepared = actionCoordinator.prepare(
                storeId,
                reviewId,
                userId,
                hotelId,
                ReviewActionType.REPLY,
                idempotencyKey,
                requestHash
        );
        if (prepared.replay()) {
            return replayResult(storeId, userId, prepared);
        }

        Map<String, Object> payload = baseWritePayload(prepared.review());
        payload.put("review_reply", reply);
        return submitAction(storeId, userId, prepared, payload);
    }

    public ReviewDtos.ActionResult reviewGuest(
            Long storeId,
            Long userId,
            Long reviewId,
            ReviewDtos.GuestReviewRequest request
    ) {
        ensurePermission(storeId, userId, PermissionAction.REVIEW_GUEST);
        String hotelId = hotelOwnershipValidator.requireUniqueOwnership(storeId);
        if (request == null) {
            throw new IllegalArgumentException("评价住客请求不能为空");
        }
        List<Map<String, Object>> categoryRatings = guestReviewValidator.validateAndBuild(request);
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());

        Map<String, Object> normalizedReview = new LinkedHashMap<>();
        normalizedReview.put("is_reviewee_recommended", request.isRevieweeRecommended());
        normalizedReview.put("category_ratings", categoryRatings);
        normalizedReview.put("public_review", request.publicReview().trim());
        String privateFeedback = trimToNull(request.privateFeedback());
        if (privateFeedback != null) {
            normalizedReview.put("private_feedback", privateFeedback);
        }
        String requestHash = hashRequest(Map.of(
                "reviewId", reviewId,
                "action", ReviewActionType.GUEST_REVIEW.name(),
                "review", normalizedReview
        ));

        PreparedAction prepared = actionCoordinator.prepare(
                storeId,
                reviewId,
                userId,
                hotelId,
                ReviewActionType.GUEST_REVIEW,
                idempotencyKey,
                requestHash
        );
        if (prepared.replay()) {
            return replayResult(storeId, userId, prepared);
        }

        Map<String, Object> payload = baseWritePayload(prepared.review());
        payload.put("review", normalizedReview);
        return submitAction(storeId, userId, prepared, payload);
    }

    @Transactional
    public WebhookResult handleWebhook(Long storeId, String hotelId, JsonNode root) {
        if (storeId == null) {
            throw new IllegalArgumentException("Review Push 缺少门店");
        }
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("Review Push 缺少 hotel_id");
        }
        List<JsonNode> reviewNodes = SuReviewPayloadMapper.extractPushReviewNodes(root);
        if (reviewNodes.isEmpty()) {
            throw new IllegalArgumentException("Review Push 缺少评价数据");
        }

        int created = 0;
        int updated = 0;
        int duplicates = 0;
        LocalDateTime syncedAt = LocalDateTime.now();
        for (JsonNode node : reviewNodes) {
            NormalizedReview normalized = payloadMapper.fromPush(node, hotelId, syncedAt);
            UpsertOutcome outcome = upsertNormalized(storeId, normalized, false);
            if (outcome.duplicate()) {
                duplicates++;
            } else if (outcome.created()) {
                created++;
            } else {
                updated++;
            }
        }
        return new WebhookResult(reviewNodes.size(), created, updated, duplicates);
    }

    private ReviewDtos.ActionResult submitAction(
            Long storeId,
            Long userId,
            PreparedAction prepared,
            Map<String, Object> payload
    ) {
        ChannelReviewAction action = prepared.action();
        JsonNode response;
        try {
            response = reviewClient.submitReply(payload);
        } catch (RuntimeException e) {
            ChannelReviewAction failed = actionCoordinator.markFailed(
                    storeId,
                    prepared.review().getId(),
                    action.getId(),
                    safeExceptionMessage(e),
                    null,
                    null
            );
            ReviewDtos.ActionResult result = actionResult(
                    failed,
                    storeId,
                    userId,
                    "提交 Su 失败: " + safeExceptionMessage(e)
            );
            throw new SuWriteFailedException(result.message(), result);
        }

        if (!suApiClient.isSuSuccess(response)) {
            String message = firstNonBlank(
                    suApiClient.extractSuErrorMessage(response),
                    "Su Review Reply 返回失败"
            );
            String errorCode = suApiClient.extractSuErrorCode(response);
            String ruid = firstText(response, "Ruid", "ruid");
            ChannelReviewAction failed = actionCoordinator.markFailed(
                    storeId,
                    prepared.review().getId(),
                    action.getId(),
                    message,
                    errorCode,
                    ruid
            );
            ReviewDtos.ActionResult result = actionResult(failed, storeId, userId, message);
            throw new SuWriteFailedException(message, result);
        }

        String message = firstNonBlank(firstText(response, "Message", "message"), "已提交 Su，等待渠道确认");
        String ruid = firstText(response, "Ruid", "ruid");
        ChannelReviewAction submitted = actionCoordinator.markSubmitted(
                storeId,
                prepared.review().getId(),
                action.getId(),
                message,
                ruid
        );
        return actionResult(submitted, storeId, userId, message);
    }

    private ReviewDtos.ActionResult replayResult(Long storeId, Long userId, PreparedAction prepared) {
        String message = switch (prepared.action().getStatus()) {
            case PENDING -> "相同请求已记录，等待提交结果";
            case SUBMITTED -> "相同请求已提交 Su，等待渠道确认";
            case CONFIRMED -> "相同请求已由渠道确认";
            case FAILED -> firstNonBlank(prepared.action().getResponseMessage(), "相同请求此前提交失败");
        };
        ReviewDtos.ActionResult result = actionResult(prepared.action(), storeId, userId, message);
        if (prepared.action().getStatus() == ReviewActionStatus.FAILED) {
            throw new SuWriteFailedException(message, result);
        }
        return result;
    }

    private ReviewDtos.ActionResult actionResult(
            ChannelReviewAction action,
            Long storeId,
            Long userId,
            String message
    ) {
        ChannelReview review = reviewRepository.findByStoreIdAndId(storeId, action.getReviewId())
                .orElseThrow(() -> new ReviewNotFoundException("评价不存在"));
        return new ReviewDtos.ActionResult(
                action.getId(),
                action.getActionType().name(),
                action.getStatus().name(),
                message,
                action.getSubmittedAt(),
                toDto(review, userId, true)
        );
    }

    private Map<String, Object> baseWritePayload(ChannelReview review) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotel_id", review.getHotelId());
        payload.put("channel_id", String.valueOf(review.getSuChannelId()));
        payload.put("channel_property_id", review.getChannelPropertyId());
        payload.put("channel_review_id", review.getChannelReviewId());
        if (review.getSuChannelId() == CHANNEL_AIRBNB) {
            payload.put("listing_id", review.getListingId());
        }
        return payload;
    }

    private UpsertOutcome upsertNormalized(
            Long storeId,
            NormalizedReview normalized,
            boolean pullIsAuthoritative
    ) {
        ChannelReview existing = reviewRepository
                .findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                        storeId,
                        normalized.channelId(),
                        normalized.channelReviewId(),
                        normalized.reviewType()
                )
                .orElse(null);
        boolean created = existing == null;
        boolean duplicate = existing != null
                && normalized.sourceHash() != null
                && normalized.sourceHash().equals(existing.getSourceEventHash());
        ChannelReview review = existing != null ? existing : new ChannelReview();
        review.setStoreId(storeId);

        applyNormalized(review, normalized, pullIsAuthoritative);
        resolveAssociation(review);
        try {
            review = reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException race) {
            if (!created) {
                throw race;
            }
            ChannelReview winner = reviewRepository
                    .findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                            storeId,
                            normalized.channelId(),
                            normalized.channelReviewId(),
                            normalized.reviewType()
                    )
                    .orElseThrow(() -> race);
            duplicate = normalized.sourceHash() != null
                    && normalized.sourceHash().equals(winner.getSourceEventHash());
            applyNormalized(winner, normalized, pullIsAuthoritative);
            resolveAssociation(winner);
            review = reviewRepository.save(winner);
            created = false;
        }

        if (pullIsAuthoritative) {
            confirmSubmittedActionIfObserved(review, normalized);
        }
        return new UpsertOutcome(review, created, duplicate);
    }

    private void applyNormalized(
            ChannelReview review,
            NormalizedReview normalized,
            boolean pullIsAuthoritative
    ) {
        review.setSuChannelId(normalized.channelId());
        review.setPmsChannelCode(channelCode(normalized.channelId()));
        review.setHotelId(normalized.hotelId());
        review.setChannelPropertyId(normalized.channelPropertyId());
        review.setChannelReviewId(normalized.channelReviewId());
        review.setReviewType(normalized.reviewType());
        review.setListingId(preferIncoming(normalized.listingId(), review.getListingId()));
        review.setChannelBookingId(preferIncoming(normalized.channelBookingId(), review.getChannelBookingId()));
        review.setGuestName(preferIncoming(normalized.guestName(), review.getGuestName()));
        review.setPropertyName(preferIncoming(normalized.propertyName(), review.getPropertyName()));
        review.setCheckInDate(normalized.checkInDate() != null ? normalized.checkInDate() : review.getCheckInDate());
        review.setCheckOutDate(normalized.checkOutDate() != null ? normalized.checkOutDate() : review.getCheckOutDate());
        review.setReviewTitle(preferIncoming(normalized.reviewTitle(), review.getReviewTitle()));
        review.setReviewText(preferIncoming(normalized.reviewText(), review.getReviewText()));
        review.setNegativeReviewText(preferIncoming(
                normalized.negativeReviewText(),
                review.getNegativeReviewText()
        ));
        review.setPrivateFeedback(preferIncoming(normalized.privateFeedback(), review.getPrivateFeedback()));
        review.setOverallScore(normalized.overallScore() != null
                ? normalized.overallScore()
                : review.getOverallScore());
        if (normalized.categoryRatings() != null
                && normalized.categoryRatings().isObject()
                && normalized.categoryRatings().size() > 0) {
            try {
                review.setCategoryRatingsJson(objectMapper.writeValueAsString(normalized.categoryRatings()));
            } catch (Exception e) {
                throw new IllegalArgumentException("评价分类评分无法序列化", e);
            }
        }
        review.setReplyText(preferIncoming(normalized.replyText(), review.getReplyText()));
        review.setReplyAt(normalized.replyAt() != null ? normalized.replyAt() : review.getReplyAt());
        review.setReviewStatus(preferIncoming(normalized.reviewStatus(), review.getReviewStatus()));
        if (normalized.canReply() != null) {
            review.setCanReply(normalized.canReply());
        }
        if (pullIsAuthoritative && normalized.canReviewGuest() != null) {
            review.setCanReviewGuest(normalized.canReviewGuest());
        } else if (review.getCanReviewGuest() == null) {
            review.setCanReviewGuest(false);
        }
        review.setReceivedAt(normalized.receivedAt() != null
                ? normalized.receivedAt()
                : review.getReceivedAt());
        review.setSuRuid(preferIncoming(normalized.suRuid(), review.getSuRuid()));
        review.setSourceEventHash(normalized.sourceHash());
        review.setLastSyncedAt(normalized.syncedAt());
    }

    private void resolveAssociation(ChannelReview review) {
        String channelCode = review.getPmsChannelCode();
        if (!"AIRBNB".equals(channelCode) && !"BOOKING".equals(channelCode)) {
            unlink(review, ReviewAssociationStatus.UNLINKED, "Su channel_id 不在 Airbnb/Booking.com 范围内");
            return;
        }
        String bookingKey = trimToNull(review.getChannelBookingId());
        if (bookingKey == null) {
            unlink(review, ReviewAssociationStatus.UNLINKED, "Su 评价未提供 channel_booking_id/booking_id");
            return;
        }

        List<Reservation> candidates = reservationRepository.findReviewAssociationCandidates(
                review.getStoreId(),
                ReviewChannelCodePolicy.acceptedStoreCodes(review.getStoreId(), channelCode),
                bookingKey
        );
        Map<Long, Reservation> distinct = new LinkedHashMap<>();
        if (candidates != null) {
            for (Reservation candidate : candidates) {
                if (candidate != null && candidate.getId() != null) {
                    distinct.put(candidate.getId(), candidate);
                }
            }
        }
        if (distinct.size() == 1) {
            review.setReservationId(distinct.values().iterator().next().getId());
            review.setAssociationStatus(ReviewAssociationStatus.LINKED);
            review.setAssociationReason(null);
            return;
        }
        if (distinct.isEmpty()) {
            unlink(
                    review,
                    ReviewAssociationStatus.UNLINKED,
                    "未找到渠道代码与预订号均精确匹配的本地订单"
            );
            return;
        }
        unlink(
                review,
                ReviewAssociationStatus.AMBIGUOUS,
                "存在多个渠道代码与预订号相同的本地订单，禁止自动关联"
        );
    }

    private void confirmSubmittedActionIfObserved(ChannelReview review, NormalizedReview normalized) {
        ReviewActionType actionType = review.getLastActionType();
        if (review.getLastActionStatus() != ReviewActionStatus.SUBMITTED || actionType == null) {
            return;
        }
        boolean confirmed = actionType == ReviewActionType.REPLY
                ? trimToNull(normalized.replyText()) != null
                : Boolean.FALSE.equals(normalized.canReviewGuest());
        if (!confirmed) {
            return;
        }

        ChannelReviewAction action = actionRepository
                .findFirstByStoreIdAndReviewIdAndActionTypeAndStatusInOrderByCreatedAtDesc(
                        review.getStoreId(),
                        review.getId(),
                        actionType,
                        SUBMITTED_STATUSES
                )
                .orElse(null);
        if (action == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        action.setStatus(ReviewActionStatus.CONFIRMED);
        action.setConfirmedAt(now);
        action.setResponseMessage("已通过 Su Pull 确认渠道状态");
        actionRepository.save(action);
        review.setLastActionStatus(ReviewActionStatus.CONFIRMED);
        reviewRepository.save(review);
    }

    private ReviewDtos.Review toDto(ChannelReview review, Long userId, boolean includeDetail) {
        Reservation reservation = review.getReservationId() == null
                ? null
                : reservationRepository.findByStoreIdAndIdWithRoomType(
                        review.getStoreId(),
                        review.getReservationId()
                ).orElse(null);
        ActionCapabilities capabilities = itemCapabilities(review, userId);
        boolean canSeeSensitive = permissionService.hasPermission(
                review.getStoreId(),
                userId,
                PermissionModule.REVIEW,
                PermissionAction.REVIEW_GUEST
        ) || permissionService.hasPermission(
                review.getStoreId(),
                userId,
                PermissionModule.REVIEW,
                PermissionAction.REPLY
        );
        List<ReviewDtos.ActionAudit> actions = includeDetail
                ? actionRepository.findByStoreIdAndReviewIdOrderByCreatedAtDesc(
                        review.getStoreId(),
                        review.getId()
                ).stream().map(this::toActionAudit).toList()
                : List.of();

        return new ReviewDtos.Review(
                review.getId(),
                review.getReservationId(),
                reservation != null ? reservation.getOrderNumber() : null,
                review.getChannelBookingId(),
                firstNonBlank(review.getGuestName(), reservation != null ? reservation.getGuestName() : null),
                review.getCheckInDate() != null
                        ? review.getCheckInDate()
                        : reservation != null ? reservation.getCheckInDate() : null,
                review.getCheckOutDate() != null
                        ? review.getCheckOutDate()
                        : reservation != null ? reservation.getCheckOutDate() : null,
                reservation != null && reservation.getStatus() != null
                        ? reservation.getStatus().name()
                        : null,
                review.getPmsChannelCode(),
                review.getSuChannelId(),
                review.getPropertyName(),
                review.getReviewType(),
                review.getReviewStatus(),
                review.getReviewTitle(),
                review.getReviewText(),
                review.getNegativeReviewText(),
                review.getOverallScore(),
                review.getReplyText(),
                review.getAssociationStatus() != null ? review.getAssociationStatus().name() : null,
                review.getAssociationReason(),
                review.getLastActionStatus() != null ? review.getLastActionStatus().name() : null,
                review.getReceivedAt(),
                review.getLastSyncedAt(),
                readCategoryRatings(review.getCategoryRatingsJson()),
                includeDetail && canSeeSensitive ? review.getPrivateFeedback() : null,
                capabilities.allowedActions(),
                capabilities.reasons(),
                actions
        );
    }

    private ReviewDtos.ActionAudit toActionAudit(ChannelReviewAction action) {
        return new ReviewDtos.ActionAudit(
                action.getId(),
                action.getActionType() != null ? action.getActionType().name() : null,
                action.getStatus() != null ? action.getStatus().name() : null,
                action.getOperatorUserId(),
                action.getResponseMessage(),
                action.getSuErrorCode(),
                action.getSubmittedAt(),
                action.getConfirmedAt(),
                action.getCreatedAt()
        );
    }

    private ActionCapabilities itemCapabilities(ChannelReview review, Long userId) {
        List<String> allowed = new ArrayList<>();
        Map<String, String> reasons = new LinkedHashMap<>();

        String replyReason = permissionReason(review.getStoreId(), userId, PermissionAction.REPLY);
        if (replyReason == null) {
            replyReason = eligibilityService.unavailableReason(review, ReviewActionType.REPLY);
        }
        if (replyReason == null) {
            allowed.add(ACTION_REPLY);
        } else {
            reasons.put(ACTION_REPLY, replyReason);
        }

        String guestReviewReason =
                permissionReason(review.getStoreId(), userId, PermissionAction.REVIEW_GUEST);
        if (guestReviewReason == null) {
            guestReviewReason =
                    eligibilityService.unavailableReason(review, ReviewActionType.GUEST_REVIEW);
        }
        if (guestReviewReason == null) {
            allowed.add(ACTION_GUEST_REVIEW);
        } else {
            reasons.put(ACTION_GUEST_REVIEW, guestReviewReason);
        }
        return new ActionCapabilities(List.copyOf(allowed), Map.copyOf(reasons));
    }

    private ActionCapabilities syncCapabilities(Long storeId, Long userId) {
        String reason = permissionReason(storeId, userId, PermissionAction.SYNC);
        if (reason == null) {
            try {
                hotelOwnershipValidator.requireUniqueOwnership(storeId);
            } catch (RuntimeException e) {
                reason = safeExceptionMessage(e);
            }
        }
        return reason == null
                ? new ActionCapabilities(List.of(ACTION_SYNC), Map.of())
                : new ActionCapabilities(List.of(), Map.of(ACTION_SYNC, reason));
    }

    private String permissionReason(Long storeId, Long userId, PermissionAction action) {
        return permissionService.hasPermission(
                storeId,
                userId,
                PermissionModule.REVIEW,
                action
        ) ? null : "当前账号没有此评价操作权限";
    }

    private void ensurePermission(Long storeId, Long userId, PermissionAction action) {
        if (!permissionService.hasPermission(storeId, userId, PermissionModule.REVIEW, action)) {
            throw new ReviewForbiddenException("当前账号没有此评价操作权限");
        }
    }

    private Specification<ChannelReview> storeSpecification(Long storeId) {
        return (root, query, cb) -> cb.equal(root.get("storeId"), storeId);
    }

    private Specification<ChannelReview> channelSpecification(Integer channelId) {
        return channelId == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("suChannelId"), channelId);
    }

    private Specification<ChannelReview> reservationSpecification(Long reservationId) {
        return reservationId == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("reservationId"), reservationId);
    }

    private Specification<ChannelReview> searchSpecification(String search) {
        if (search == null) {
            return null;
        }
        String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("guestName")), pattern),
                cb.like(cb.lower(root.get("channelBookingId")), pattern),
                cb.like(cb.lower(root.get("reviewTitle")), pattern),
                cb.like(cb.lower(root.get("reviewText")), pattern),
                cb.like(cb.lower(root.get("propertyName")), pattern)
        );
    }

    private Specification<ChannelReview> tabSpecification(String tab) {
        return switch (tab) {
            case "ALL" -> null;
            case "PENDING_REPLY" -> (root, query, cb) -> cb.isTrue(root.get("canReply"));
            case "PENDING_GUEST_REVIEW" -> (root, query, cb) -> cb.and(
                    cb.equal(root.get("suChannelId"), CHANNEL_AIRBNB),
                    cb.isTrue(root.get("canReviewGuest"))
            );
            case "PROCESSING" -> (root, query, cb) -> root.get("lastActionStatus").in(
                    ReviewActionStatus.PENDING,
                    ReviewActionStatus.SUBMITTED
            );
            case "COMPLETED" -> (root, query, cb) -> cb.or(
                    cb.equal(root.get("lastActionStatus"), ReviewActionStatus.CONFIRMED),
                    cb.and(
                            cb.isNotNull(root.get("replyText")),
                            cb.notEqual(root.get("replyText"), "")
                    )
            );
            case "UNLINKED" -> (root, query, cb) -> cb.notEqual(
                    root.get("associationStatus"),
                    ReviewAssociationStatus.LINKED
            );
            default -> throw new IllegalArgumentException("不支持的 tab: " + tab);
        };
    }

    private static String normalizeTab(String tab) {
        String normalized = trimToNull(tab);
        return normalized == null ? "ALL" : normalized.toUpperCase(Locale.ROOT);
    }

    private static Integer normalizeChannelFilter(String channel) {
        String normalized = trimToNull(channel);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "AIRBNB", "244" -> CHANNEL_AIRBNB;
            case "BOOKING", "BOOKING.COM", "19" -> CHANNEL_BOOKING;
            default -> throw new IllegalArgumentException("不支持的评价渠道: " + channel);
        };
    }

    private static String channelCode(Integer channelId) {
        if (channelId != null && channelId == CHANNEL_AIRBNB) {
            return "AIRBNB";
        }
        if (channelId != null && channelId == CHANNEL_BOOKING) {
            return "BOOKING";
        }
        return "UNKNOWN";
    }

    private Map<String, BigDecimal> readCategoryRatings(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return Map.of();
            }
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                BigDecimal score = null;
                if (value != null && value.isNumber()) {
                    score = value.decimalValue();
                } else if (value != null && value.isObject()) {
                    JsonNode scoreNode = value.get("score");
                    if (scoreNode != null && scoreNode.isNumber()) {
                        score = scoreNode.decimalValue();
                    }
                }
                if (score != null && !"review_score".equals(entry.getKey())) {
                    result.put(entry.getKey(), score);
                }
            });
            return Map.copyOf(result);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String hashRequest(Object request) {
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法计算评价请求摘要", e);
        }
    }

    private static JsonNode firstObject(JsonNode root, String... fields) {
        if (root == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isObject()) {
                return value;
            }
        }
        return null;
    }

    private static JsonNode firstArray(JsonNode root, String... fields) {
        if (root == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private static String firstText(JsonNode root, String... fields) {
        if (root == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = root.get(field);
            if (value == null || value.isNull() || value.isContainerNode()) {
                continue;
            }
            String text = trimToNull(value.asText(null));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private static String normalizeIdempotencyKey(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() < 8 || normalized.length() > 120) {
            throw new IllegalArgumentException("幂等键长度必须为8到120个字符");
        }
        return normalized;
    }

    private static String safeExceptionMessage(Throwable error) {
        if (error == null || trimToNull(error.getMessage()) == null) {
            return "未知错误";
        }
        String message = error.getMessage().trim();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private static String withSkippedSummary(
            String baseMessage,
            int skipped,
            Map<String, Integer> skippedReasons
    ) {
        if (skipped <= 0) {
            return baseMessage;
        }
        StringBuilder summary = new StringBuilder(baseMessage)
                .append("；安全跳过 ")
                .append(skipped)
                .append(" 条当前映射无效的评价");
        if (skippedReasons != null && !skippedReasons.isEmpty()) {
            summary.append("（");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : skippedReasons.entrySet()) {
                if (!first) {
                    summary.append("；");
                }
                summary.append(entry.getKey()).append(" × ").append(entry.getValue());
                first = false;
            }
            summary.append("）");
        }
        return summary.toString();
    }

    private static void unlink(
            ChannelReview review,
            ReviewAssociationStatus status,
            String reason
    ) {
        review.setReservationId(null);
        review.setAssociationStatus(status);
        review.setAssociationReason(reason);
    }

    private static String preferIncoming(String incoming, String existing) {
        String normalized = trimToNull(incoming);
        return normalized != null ? normalized : existing;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String firstNonBlank(String first, String second) {
        String normalized = trimToNull(first);
        return normalized != null ? normalized : trimToNull(second);
    }

    private record ActionCapabilities(List<String> allowedActions, Map<String, String> reasons) {
    }

    private record UpsertOutcome(ChannelReview review, boolean created, boolean duplicate) {
    }

    public record WebhookResult(int processed, int created, int updated, int duplicates) {
    }

    public static class ReviewNotFoundException extends RuntimeException {
        public ReviewNotFoundException(String message) {
            super(message);
        }
    }

    public static class ReviewForbiddenException extends RuntimeException {
        public ReviewForbiddenException(String message) {
            super(message);
        }
    }

    public static class ReviewConflictException extends RuntimeException {
        public ReviewConflictException(String message) {
            super(message);
        }
    }

    public static class SuWriteFailedException extends RuntimeException {
        private final ReviewDtos.ActionResult result;

        public SuWriteFailedException(String message, ReviewDtos.ActionResult result) {
            super(message);
            this.result = result;
        }

        public ReviewDtos.ActionResult getResult() {
            return result;
        }
    }
}
