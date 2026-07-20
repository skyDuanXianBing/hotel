package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.dto.ReviewDtos;
import server.demo.entity.Channel;
import server.demo.entity.ChannelMappingPriceSetting;
import server.demo.entity.ChannelReview;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.ChannelMappingPriceSettingRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ChannelReviewActionRepository;
import server.demo.repository.ChannelReviewRepository;
import server.demo.repository.ReservationRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuReviewServiceSyncMappingTest {

    private ObjectMapper objectMapper;
    private ChannelReviewRepository reviewRepository;
    private ReservationRepository reservationRepository;
    private SuReviewClient reviewClient;
    private SuApiClient suApiClient;
    private SuReviewService service;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        reviewRepository = Mockito.mock(ChannelReviewRepository.class);
        reservationRepository = Mockito.mock(ReservationRepository.class);
        reviewClient = Mockito.mock(SuReviewClient.class);
        suApiClient = Mockito.mock(SuApiClient.class);

        PermissionService permissionService = Mockito.mock(PermissionService.class);
        when(permissionService.hasPermission(
                10L,
                7L,
                PermissionModule.REVIEW,
                PermissionAction.SYNC
        )).thenReturn(true);
        SuReviewHotelOwnershipValidator hotelOwnershipValidator =
                Mockito.mock(SuReviewHotelOwnershipValidator.class);
        when(hotelOwnershipValidator.requireUniqueOwnership(10L)).thenReturn("HOTEL1");

        ChannelRepository channelRepository = Mockito.mock(ChannelRepository.class);
        ChannelMappingPriceSettingRepository mappingRepository =
                Mockito.mock(ChannelMappingPriceSettingRepository.class);
        when(channelRepository.findByStoreId(10L)).thenReturn(List.of(
                channel(100L, "BOOKING"),
                channel(101L, "AIRBNB")
        ));
        when(mappingRepository.findByStoreIdAndSuPropertyIdAndSuChannelId(
                10L,
                "HOTEL1",
                "19"
        )).thenReturn(List.of(mapping(100L, "19", "PROPERTY1", null)));
        when(mappingRepository.findByStoreIdAndSuPropertyIdAndSuChannelId(
                10L,
                "HOTEL1",
                "244"
        )).thenReturn(List.of(mapping(101L, "244", "AIR-PROPERTY", "LISTING-1")));

        SuReviewWebhookMappingValidator mappingValidator =
                new SuReviewWebhookMappingValidator(channelRepository, mappingRepository);
        service = new SuReviewService(
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

        JsonNode response = pullResponse();
        when(reviewClient.pullReviews(any())).thenReturn(response);
        when(suApiClient.isSuSuccess(response)).thenReturn(true);
        when(reviewRepository.findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                10L,
                19,
                "19_R1",
                "guest_to_host"
        )).thenReturn(Optional.empty());
        when(reservationRepository.findReviewAssociationCandidates(
                10L,
                List.of("BOOKING", "BOOKING_10"),
                "BKG-1"
        )).thenReturn(List.of());
        when(reviewRepository.saveAndFlush(any(ChannelReview.class))).thenAnswer(invocation -> {
            ChannelReview review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });
    }

    @Test
    void syncPersistsOnlyReviewsMatchingCurrentPropertyAndAirbnbListing() {
        ReviewDtos.SyncResult result = service.syncReviews(10L, 7L);

        assertTrue(result.success());
        assertEquals(3, result.fetched());
        assertEquals(1, result.created());
        assertEquals(0, result.updated());
        assertEquals(1, result.unlinked());
        assertTrue(result.message().contains("安全跳过 2 条"));
        assertTrue(result.message().contains("找不到当前门店的 Review 渠道物业映射"));

        verify(reviewRepository, times(1)).saveAndFlush(any(ChannelReview.class));
        verify(reviewRepository, never())
                .findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                        10L,
                        19,
                        "19_R2",
                        "guest_to_host"
                );
        verify(reviewRepository, never())
                .findByStoreIdAndSuChannelIdAndChannelReviewIdAndReviewType(
                        10L,
                        244,
                        "244_R1",
                        "guest_to_host"
                );
    }

    private JsonNode pullResponse() throws Exception {
        return objectMapper.readTree("""
                {
                  "Status": "Success",
                  "Data": {
                    "reviews": [
                      {
                        "channel_id": "19",
                        "channel_hotel_id": "PROPERTY1",
                        "booking_id": "BKG-1",
                        "guest_info": {
                          "channel_review_id": "19_R1",
                          "review_type": "guest_to_host",
                          "review": "Mapped Booking review"
                        },
                        "reply_flags": {}
                      },
                      {
                        "channel_id": "19",
                        "channel_hotel_id": "REMOVED-PROPERTY",
                        "booking_id": "BKG-2",
                        "guest_info": {
                          "channel_review_id": "19_R2",
                          "review_type": "guest_to_host",
                          "review": "Removed Booking mapping"
                        },
                        "reply_flags": {}
                      },
                      {
                        "channel_id": "244",
                        "channel_hotel_id": "AIR-PROPERTY",
                        "listing_id": "LISTING-REMOVED",
                        "booking_id": "AIR-1",
                        "guest_info": {
                          "channel_review_id": "244_R1",
                          "review_type": "guest_to_host",
                          "review": "Removed Airbnb listing mapping"
                        },
                        "reply_flags": {}
                      }
                    ],
                    "next_page": ""
                  }
                }
                """);
    }

    private static Channel channel(Long id, String code) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setStoreId(10L);
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
        mapping.setStoreId(10L);
        mapping.setChannelId(channelId);
        mapping.setSuPropertyId("HOTEL1");
        mapping.setSuChannelId(suChannelId);
        mapping.setChannelHotelId(channelPropertyId);
        mapping.setListingId(listingId);
        return mapping;
    }
}
