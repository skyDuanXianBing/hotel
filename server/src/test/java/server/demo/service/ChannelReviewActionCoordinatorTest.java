package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.entity.ChannelReview;
import server.demo.entity.ChannelReviewAction;
import server.demo.enums.ReviewActionStatus;
import server.demo.enums.ReviewActionType;
import server.demo.enums.ReviewAssociationStatus;
import server.demo.repository.ChannelReviewActionRepository;
import server.demo.repository.ChannelReviewRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChannelReviewActionCoordinatorTest {

    private ChannelReviewRepository reviewRepository;
    private ChannelReviewActionRepository actionRepository;
    private SuReviewWebhookMappingValidator mappingValidator;
    private ChannelReviewActionCoordinator coordinator;

    @BeforeEach
    void setUp() {
        reviewRepository = Mockito.mock(ChannelReviewRepository.class);
        actionRepository = Mockito.mock(ChannelReviewActionRepository.class);
        mappingValidator = Mockito.mock(SuReviewWebhookMappingValidator.class);
        coordinator = new ChannelReviewActionCoordinator(
                reviewRepository,
                actionRepository,
                new ReviewEligibilityService(),
                mappingValidator
        );
    }

    @Test
    void sameIdempotencyKeyReturnsExistingActionWithoutSecondWrite() {
        ChannelReview review = eligibleAirbnbReview();
        ChannelReviewAction existing = new ChannelReviewAction();
        existing.setId(90L);
        existing.setStoreId(10L);
        existing.setReviewId(1L);
        existing.setActionType(ReviewActionType.REPLY);
        existing.setStatus(ReviewActionStatus.SUBMITTED);
        existing.setIdempotencyKey("same-key-123");
        existing.setRequestHash("hash-1");

        when(reviewRepository.findForUpdateByStoreIdAndId(10L, 1L)).thenReturn(Optional.of(review));
        when(actionRepository.findByStoreIdAndIdempotencyKey(10L, "same-key-123"))
                .thenReturn(Optional.of(existing));

        ChannelReviewActionCoordinator.PreparedAction prepared = coordinator.prepare(
                10L, 1L, 7L, "HOTEL1", ReviewActionType.REPLY, "same-key-123", "hash-1"
        );

        assertTrue(prepared.replay());
        assertSame(existing, prepared.action());
        verify(actionRepository, never()).save(Mockito.any());
    }

    @Test
    void bookingGuestReviewIsRejectedBeforeAuditOrRemoteWrite() {
        ChannelReview booking = eligibleAirbnbReview();
        booking.setSuChannelId(SuReviewService.CHANNEL_BOOKING);
        booking.setPmsChannelCode("BOOKING");
        booking.setListingId(null);
        booking.setCanReviewGuest(true);

        when(reviewRepository.findForUpdateByStoreIdAndId(10L, 1L)).thenReturn(Optional.of(booking));
        when(actionRepository.findByStoreIdAndIdempotencyKey(10L, "booking-key-123"))
                .thenReturn(Optional.empty());

        SuReviewService.ReviewConflictException error = assertThrows(
                SuReviewService.ReviewConflictException.class,
                () -> coordinator.prepare(
                        10L,
                        1L,
                        7L,
                        "HOTEL1",
                        ReviewActionType.GUEST_REVIEW,
                        "booking-key-123",
                        "hash-1"
                )
        );

        assertTrue(error.getMessage().contains("只有 Airbnb"));
        verify(actionRepository, never()).save(Mockito.any());
    }

    @Test
    void reviewLookupAlwaysIncludesCurrentStoreId() {
        when(reviewRepository.findForUpdateByStoreIdAndId(22L, 1L)).thenReturn(Optional.empty());

        assertThrows(
                SuReviewService.ReviewNotFoundException.class,
                () -> coordinator.prepare(
                        22L, 1L, 7L, "HOTEL1", ReviewActionType.REPLY, "store-key-123", "hash-1"
                )
        );

        verify(reviewRepository).findForUpdateByStoreIdAndId(22L, 1L);
    }

    @Test
    void mismatchedHotelOwnershipIsRejectedBeforeIdempotencyOrAuditLookup() {
        ChannelReview review = eligibleAirbnbReview();
        when(reviewRepository.findForUpdateByStoreIdAndId(10L, 1L)).thenReturn(Optional.of(review));

        assertThrows(
                SuReviewService.ReviewConflictException.class,
                () -> coordinator.prepare(
                        10L,
                        1L,
                        7L,
                        "OTHER-HOTEL",
                        ReviewActionType.REPLY,
                        "store-key-123",
                        "hash-1"
                )
        );

        verify(actionRepository, never()).findByStoreIdAndIdempotencyKey(
                Mockito.anyLong(),
                Mockito.anyString()
        );
        verify(actionRepository, never()).save(Mockito.any());
    }

    @Test
    void deletedCurrentMappingBlocksReplyBeforeIdempotencyOrAuditLookup() {
        assertDeletedMappingBlocksAction(ReviewActionType.REPLY);
    }

    @Test
    void deletedCurrentMappingBlocksGuestReviewBeforeIdempotencyOrAuditLookup() {
        assertDeletedMappingBlocksAction(ReviewActionType.GUEST_REVIEW);
    }

    private void assertDeletedMappingBlocksAction(ReviewActionType actionType) {
        ChannelReview review = eligibleAirbnbReview();
        when(reviewRepository.findForUpdateByStoreIdAndId(10L, 1L)).thenReturn(Optional.of(review));
        Mockito.doThrow(new SuReviewWebhookMappingValidator.MappingRejectedException(
                "找不到当前门店的 Review 渠道物业映射"
        )).when(mappingValidator).assertCurrentMapping(
                10L,
                "HOTEL1",
                SuReviewService.CHANNEL_AIRBNB,
                "PROPERTY1",
                "LISTING1"
        );

        SuReviewService.ReviewConflictException error = assertThrows(
                SuReviewService.ReviewConflictException.class,
                () -> coordinator.prepare(
                        10L,
                        1L,
                        7L,
                        "HOTEL1",
                        actionType,
                        "mapping-key-123",
                        "hash-1"
                )
        );

        assertTrue(error.getMessage().contains("当前 Review 渠道映射已失效"));
        verify(actionRepository, never()).findByStoreIdAndIdempotencyKey(
                Mockito.anyLong(),
                Mockito.anyString()
        );
        verify(actionRepository, never()).save(Mockito.any());
    }

    private static ChannelReview eligibleAirbnbReview() {
        ChannelReview review = new ChannelReview();
        review.setId(1L);
        review.setStoreId(10L);
        review.setReservationId(20L);
        review.setAssociationStatus(ReviewAssociationStatus.LINKED);
        review.setPmsChannelCode("AIRBNB");
        review.setSuChannelId(SuReviewService.CHANNEL_AIRBNB);
        review.setReviewType("guest_to_host");
        review.setHotelId("HOTEL1");
        review.setChannelPropertyId("PROPERTY1");
        review.setChannelReviewId("244_R1");
        review.setListingId("LISTING1");
        review.setCanReply(true);
        review.setCanReviewGuest(true);
        review.setReviewText("Guest review");
        return review;
    }
}
