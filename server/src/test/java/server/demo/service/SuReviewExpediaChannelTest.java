package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.dto.ReviewDtos;
import server.demo.entity.Channel;
import server.demo.entity.ChannelMappingPriceSetting;
import server.demo.entity.ChannelReview;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.enums.ReviewActionType;
import server.demo.enums.ReviewAssociationStatus;
import server.demo.repository.ChannelMappingPriceSettingRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ChannelReviewActionRepository;
import server.demo.repository.ChannelReviewRepository;
import server.demo.repository.ReservationRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P4 聚焦测试：评论链路双门控 + 映射校验 + 动作资格全部按目录 review 能力集放行 EXPEDIA(9)，
 * TRIP(339)/AGODA(189) 继续拒绝。Airbnb 专属 listing 校验保持不变。
 */
class SuReviewExpediaChannelTest {

    private static final Long STORE_ID = 10L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalizeChannelFilter_acceptsExpediaRejectsTripAgoda() {
        assertEquals(9, SuReviewService.normalizeChannelFilter("EXPEDIA"));
        assertEquals(9, SuReviewService.normalizeChannelFilter("9"));
        assertEquals(19, SuReviewService.normalizeChannelFilter("BOOKING"));
        assertEquals(244, SuReviewService.normalizeChannelFilter("AIRBNB"));
        assertNull(SuReviewService.normalizeChannelFilter(null));

        assertThrows(IllegalArgumentException.class, () -> SuReviewService.normalizeChannelFilter("TRIP"));
        assertThrows(IllegalArgumentException.class, () -> SuReviewService.normalizeChannelFilter("AGODA"));
        assertThrows(IllegalArgumentException.class, () -> SuReviewService.normalizeChannelFilter("339"));
        assertThrows(IllegalArgumentException.class, () -> SuReviewService.normalizeChannelFilter("189"));
        assertThrows(IllegalArgumentException.class, () -> SuReviewService.normalizeChannelFilter("253"));
    }

    @Test
    void channelCode_mapsExpediaAndKeepsUnsupportedUnknown() {
        assertEquals("BOOKING", SuReviewService.channelCode(19));
        assertEquals("AIRBNB", SuReviewService.channelCode(244));
        assertEquals("EXPEDIA", SuReviewService.channelCode(9));
        assertEquals("UNKNOWN", SuReviewService.channelCode(339));
        assertEquals("UNKNOWN", SuReviewService.channelCode(189));
        assertEquals("UNKNOWN", SuReviewService.channelCode(253));
        assertEquals("UNKNOWN", SuReviewService.channelCode(null));
    }

    @Test
    void eligibility_allowsExpediaReplyRejectsTripAgoda() {
        ReviewEligibilityService service = new ReviewEligibilityService();

        ChannelReview expediaReview = eligibleReview(9);
        expediaReview.setCanReply(true);
        assertNull(service.unavailableReason(expediaReview, ReviewActionType.REPLY));

        ChannelReview tripReview = eligibleReview(339);
        tripReview.setCanReply(true);
        assertTrue(service.unavailableReason(tripReview, ReviewActionType.REPLY) != null);

        ChannelReview agodaReview = eligibleReview(189);
        agodaReview.setCanReply(true);
        assertTrue(service.unavailableReason(agodaReview, ReviewActionType.REPLY) != null);

        // Airbnb 专属 guest-review 规则不变：Expedia 不允许 guest_review 动作
        ChannelReview expediaGuestReview = eligibleReview(9);
        expediaGuestReview.setCanReviewGuest(true);
        assertTrue(service.unavailableReason(expediaGuestReview, ReviewActionType.GUEST_REVIEW) != null);
    }

    @Test
    void mappingValidator_loadsExpediaMappingsAndRejectsTrip() {
        ChannelRepository channelRepository = Mockito.mock(ChannelRepository.class);
        ChannelMappingPriceSettingRepository mappingRepository =
                Mockito.mock(ChannelMappingPriceSettingRepository.class);
        when(channelRepository.findByStoreId(STORE_ID)).thenReturn(List.of(
                channel(102L, "EXPEDIA")
        ));
        when(mappingRepository.findByStoreIdAndSuPropertyIdAndSuChannelId(STORE_ID, "HOTEL1", "9"))
                .thenReturn(List.of(mapping(102L, "9", "EXP-PROPERTY", null)));

        SuReviewWebhookMappingValidator validator =
                new SuReviewWebhookMappingValidator(channelRepository, mappingRepository);

        SuReviewWebhookMappingValidator.CurrentMappingSnapshot snapshot =
                validator.loadCurrentMappings(STORE_ID, "HOTEL1");
        // EXPEDIA 映射进入快照，assertMapped 通过（不抛异常即通过）
        snapshot.assertMapped(9, "EXP-PROPERTY", null);

        // TRIP(339) 官方不支持评论：canonicalCode 为空，直接拒绝
        assertThrows(
                SuReviewWebhookMappingValidator.MappingRejectedException.class,
                () -> snapshot.assertMapped(339, "TRIP-PROPERTY", null)
        );
        // EXPEDIA 但未映射的物业同样拒绝
        assertThrows(
                SuReviewWebhookMappingValidator.MappingRejectedException.class,
                () -> snapshot.assertMapped(9, "REMOVED-PROPERTY", null)
        );
    }

    @Test
    void syncReviews_persistsExpediaReviewAndSkipsTrip() throws Exception {
        ChannelReviewRepository reviewRepository = Mockito.mock(ChannelReviewRepository.class);
        ReservationRepository reservationRepository = Mockito.mock(ReservationRepository.class);
        SuReviewClient reviewClient = Mockito.mock(SuReviewClient.class);
        SuApiClient suApiClient = Mockito.mock(SuApiClient.class);

        PermissionService permissionService = Mockito.mock(PermissionService.class);
        when(permissionService.hasPermission(STORE_ID, 7L, PermissionModule.REVIEW, PermissionAction.SYNC))
                .thenReturn(true);
        SuReviewHotelOwnershipValidator hotelOwnershipValidator =
                Mockito.mock(SuReviewHotelOwnershipValidator.class);
        when(hotelOwnershipValidator.requireUniqueOwnership(STORE_ID)).thenReturn("HOTEL1");

        ChannelRepository channelRepository = Mockito.mock(ChannelRepository.class);
        ChannelMappingPriceSettingRepository mappingRepository =
                Mockito.mock(ChannelMappingPriceSettingRepository.class);
        when(channelRepository.findByStoreId(STORE_ID)).thenReturn(List.of(
                channel(102L, "EXPEDIA")
        ));
        when(mappingRepository.findByStoreIdAndSuPropertyIdAndSuChannelId(STORE_ID, "HOTEL1", "9"))
                .thenReturn(List.of(mapping(102L, "9", "EXP-PROPERTY", null)));

        SuReviewWebhookMappingValidator mappingValidator =
                new SuReviewWebhookMappingValidator(channelRepository, mappingRepository);
        SuReviewService service = new SuReviewService(
                reviewRepository,
                Mockito.mock(ChannelReviewActionRepository.class),
                reservationRepository,
                hotelOwnershipValidator,
                permissionService,
                new ReviewEligibilityService(),
                new AirbnbGuestReviewValidator(),
                Mockito.mock(ChannelReviewActionCoordinator.class),
                mappingValidator,
                new SuReviewPayloadMapper(objectMapper),
                reviewClient,
                suApiClient,
                objectMapper
        );

        JsonNode response = objectMapper.readTree("""
                {
                  "Status": "Success",
                  "Data": {
                    "reviews": [
                      {
                        "channel_id": "9",
                        "channel_hotel_id": "EXP-PROPERTY",
                        "booking_id": "EXP-1",
                        "guest_info": {
                          "channel_review_id": "9_R1",
                          "review_type": "guest_to_host",
                          "review": "Great stay"
                        },
                        "reply_flags": {}
                      },
                      {
                        "channel_id": "339",
                        "channel_hotel_id": "TRIP-PROPERTY",
                        "booking_id": "TRIP-1",
                        "guest_info": {
                          "channel_review_id": "339_R1",
                          "review_type": "guest_to_host",
                          "review": "Unsupported channel review"
                        },
                        "reply_flags": {}
                      }
                    ],
                    "next_page": ""
                  }
                }
                """);
        when(reviewClient.pullReviews(any())).thenReturn(response);
        when(suApiClient.isSuSuccess(response)).thenReturn(true);
        when(reviewRepository.findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                STORE_ID, 9, "9_R1", "guest_to_host"
        )).thenReturn(Optional.empty());
        when(reservationRepository.findReviewAssociationCandidates(
                STORE_ID,
                List.of("EXPEDIA", "EXPEDIA_10"),
                "EXP-1"
        )).thenReturn(List.of());
        when(reviewRepository.saveAndFlush(any(ChannelReview.class))).thenAnswer(invocation -> {
            ChannelReview review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });

        ReviewDtos.SyncResult result = service.syncReviews(STORE_ID, 7L);

        assertTrue(result.success());
        assertEquals(2, result.fetched());
        assertEquals(1, result.created());
        verify(reviewRepository, times(1)).saveAndFlush(any(ChannelReview.class));
        // TRIP 评论从未进入 upsert 查询
        verify(reviewRepository, never()).findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                STORE_ID, 339, "339_R1", "guest_to_host"
        );
    }

    private static ChannelReview eligibleReview(int channelId) {
        ChannelReview review = new ChannelReview();
        review.setId(1L);
        review.setStoreId(STORE_ID);
        review.setReservationId(20L);
        review.setAssociationStatus(ReviewAssociationStatus.LINKED);
        review.setSuChannelId(channelId);
        review.setReviewType("guest_to_host");
        review.setHotelId("HOTEL1");
        review.setChannelPropertyId("PROPERTY1");
        review.setChannelReviewId(channelId + "_review");
        return review;
    }

    private static Channel channel(Long id, String code) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setStoreId(STORE_ID);
        channel.setCode(code);
        return channel;
    }

    private static ChannelMappingPriceSetting mapping(
            Long channelId,
            String suChannelId,
            String channelPropertyId,
            String listingId
    ) {
        ChannelMappingPriceSetting mapping = new ChannelMappingPriceSetting();
        mapping.setStoreId(STORE_ID);
        mapping.setChannelId(channelId);
        mapping.setSuPropertyId("HOTEL1");
        mapping.setSuChannelId(suChannelId);
        mapping.setChannelHotelId(channelPropertyId);
        mapping.setListingId(listingId);
        return mapping;
    }
}
