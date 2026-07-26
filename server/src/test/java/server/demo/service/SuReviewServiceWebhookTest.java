package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.ChannelReview;
import server.demo.entity.Reservation;
import server.demo.enums.ReviewAssociationStatus;
import server.demo.repository.ChannelReviewActionRepository;
import server.demo.repository.ChannelReviewRepository;
import server.demo.repository.ReservationRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuReviewServiceWebhookTest {

    private ObjectMapper objectMapper;
    private ChannelReviewRepository reviewRepository;
    private ReservationRepository reservationRepository;
    private SuReviewService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reviewRepository = Mockito.mock(ChannelReviewRepository.class);
        ChannelReviewActionRepository actionRepository =
                Mockito.mock(ChannelReviewActionRepository.class);
        reservationRepository = Mockito.mock(ReservationRepository.class);
        service = new SuReviewService(
                reviewRepository,
                actionRepository,
                reservationRepository,
                Mockito.mock(SuReviewHotelOwnershipValidator.class),
                Mockito.mock(PermissionService.class),
                new ReviewEligibilityService(),
                new AirbnbGuestReviewValidator(),
                Mockito.mock(ChannelReviewActionCoordinator.class),
                Mockito.mock(SuReviewWebhookMappingValidator.class),
                new SuReviewPayloadMapper(objectMapper),
                Mockito.mock(SuReviewClient.class),
                Mockito.mock(SuApiClient.class),
                objectMapper
        );
    }

    @Test
    void pushAssociatesReservationOnlyThroughExactStoreChannelAndBookingQuery() throws Exception {
        Reservation reservation = new Reservation();
        ReflectionTestUtils.setField(reservation, "id", 77L);
        when(reservationRepository.findReviewAssociationCandidates(
                10L,
                List.of("BOOKING", "BOOKING_10"),
                "BKG-1"
        ))
                .thenReturn(List.of(reservation));
        when(reviewRepository
                .findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                        10L, 19, "19_R1", "guest_to_host"
                ))
                .thenReturn(Optional.empty());
        when(reviewRepository.saveAndFlush(any(ChannelReview.class))).thenAnswer(invocation -> {
            ChannelReview review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });

        SuReviewService.WebhookResult result = service.handleWebhook(
                10L,
                "HOTEL1",
                pushPayload()
        );

        assertEquals(1, result.created());
        ArgumentCaptor<ChannelReview> captor = ArgumentCaptor.forClass(ChannelReview.class);
        verify(reviewRepository).saveAndFlush(captor.capture());
        ChannelReview saved = captor.getValue();
        assertEquals(10L, saved.getStoreId());
        assertEquals("BOOKING", saved.getPmsChannelCode());
        assertEquals(77L, saved.getReservationId());
        assertEquals(ReviewAssociationStatus.LINKED, saved.getAssociationStatus());
        verify(reservationRepository).findReviewAssociationCandidates(
                10L,
                List.of("BOOKING", "BOOKING_10"),
                "BKG-1"
        );
    }

    @Test
    void repeatedPushUsesExternalUniqueKeyAndIsReportedAsDuplicate() throws Exception {
        AtomicReference<ChannelReview> stored = new AtomicReference<>();
        when(reviewRepository
                .findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                        eq(10L), eq(19), eq("19_R1"), eq("guest_to_host")
                ))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(reservationRepository.findReviewAssociationCandidates(
                10L,
                List.of("BOOKING", "BOOKING_10"),
                "BKG-1"
        ))
                .thenReturn(List.of());
        when(reviewRepository.saveAndFlush(any(ChannelReview.class))).thenAnswer(invocation -> {
            ChannelReview review = invocation.getArgument(0);
            if (review.getId() == null) {
                review.setId(1L);
            }
            stored.set(review);
            return review;
        });

        SuReviewService.WebhookResult first = service.handleWebhook(10L, "HOTEL1", pushPayload());
        SuReviewService.WebhookResult second = service.handleWebhook(10L, "HOTEL1", pushPayload());

        assertEquals(1, first.created());
        assertEquals(1, second.duplicates());
        assertNotNull(stored.get().getSourceEventHash());
        assertEquals(ReviewAssociationStatus.UNLINKED, stored.get().getAssociationStatus());
        verify(
                reviewRepository,
                atLeastOnce()
        ).findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                10L, 19, "19_R1", "guest_to_host"
        );
    }

    private JsonNode pushPayload() throws Exception {
        return objectMapper.readTree("""
                {
                  "hotel_id": "HOTEL1",
                  "channel_id": "19",
                  "channel_property_id": "PROPERTY1",
                  "channel_review_id": "19_R1",
                  "channel_booking_id": "BKG-1",
                  "review_title": "Good stay",
                  "review_description": "Clean and central",
                  "review_score": {"review_score": 8.5, "clean": 9},
                  "review_type": "guest_to_host",
                  "review_status": "published",
                  "review_date": "2026-07-18",
                  "is_eligible_to_respond": "Y"
                }
                """);
    }
}
