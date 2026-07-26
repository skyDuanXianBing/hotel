package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import server.demo.service.SuReviewPayloadMapper.NormalizedReview;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuReviewPayloadMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SuReviewPayloadMapper mapper = new SuReviewPayloadMapper(objectMapper);

    @Test
    void mapsCurrentPullShapeAndReplyFlags() throws Exception {
        NormalizedReview review = mapper.fromPull(objectMapper.readTree("""
                {
                  "booking_id": "BKG-1",
                  "channel_id": "244",
                  "channel_hotel_id": "CP-1",
                  "listing_id": "LIST-1",
                  "guest_name": "Guest",
                  "property_name": "Property",
                  "guest_info": {
                    "channel_review_id": "244_R1",
                    "review": "Great stay",
                    "overall_score": 5,
                    "review_json": {"cleanliness": {"score": 5}},
                    "received_time": "2026-07-18",
                    "review_type": "guest_to_host"
                  },
                  "reply_flags": {
                    "reply_to_guest": true,
                    "review_the_guest": true
                  }
                }
                """), "HOTEL1", LocalDateTime.of(2026, 7, 19, 10, 0));

        assertEquals(244, review.channelId());
        assertEquals("244_R1", review.channelReviewId());
        assertEquals("BKG-1", review.channelBookingId());
        assertTrue(review.canReply());
        assertTrue(review.canReviewGuest());
        assertEquals("guest_to_host", review.reviewType());
    }

    @Test
    void mapsCurrentPushEligibilityDespiteDocumentBooleanStringConflict() throws Exception {
        NormalizedReview review = mapper.fromPush(objectMapper.readTree("""
                {
                  "hotel_id": "HOTEL1",
                  "channel_id": "19",
                  "channel_property_id": "CP-1",
                  "channel_review_id": "19_R1",
                  "channel_booking_id": "BKG-1",
                  "reviewee_name": "Current Reviewee",
                  "guest_name": "Legacy Guest",
                  "review_type": "guest_to_host",
                  "review_status": "published",
                  "review_date": "2026-07-18",
                  "review_description": "Good",
                  "review_score": {"review_score": 8.5, "clean": 9},
                  "is_eligible_to_respond": "Y"
                }
                """), "HOTEL1", LocalDateTime.of(2026, 7, 19, 10, 0));

        assertTrue(review.canReply());
        assertNull(review.canReviewGuest());
        assertEquals("8.5", review.overallScore().toPlainString());
        assertEquals("Current Reviewee", review.guestName());
    }

    @Test
    void fallsBackToLegacyGuestNameWhenRevieweeNameIsAbsent() throws Exception {
        NormalizedReview review = mapper.fromPush(objectMapper.readTree("""
                {
                  "hotel_id": "HOTEL1",
                  "channel_id": "19",
                  "channel_property_id": "CP-1",
                  "channel_review_id": "19_R2",
                  "guest_name": "Legacy Guest"
                }
                """), "HOTEL1", LocalDateTime.of(2026, 7, 19, 10, 0));

        assertEquals("Legacy Guest", review.guestName());
    }

    @Test
    void mapsScalarPushReviewScoreWithoutLosingOverallScore() throws Exception {
        NormalizedReview review = mapper.fromPush(objectMapper.readTree("""
                {
                  "hotel_id": "HOTEL1",
                  "channel_id": "19",
                  "channel_property_id": "CP-1",
                  "channel_review_id": "19_R3",
                  "review_score": 8.5
                }
                """), "HOTEL1", LocalDateTime.of(2026, 7, 19, 10, 0));

        assertEquals("8.5", review.overallScore().toPlainString());
        assertEquals(0, review.categoryRatings().size());
    }
}
