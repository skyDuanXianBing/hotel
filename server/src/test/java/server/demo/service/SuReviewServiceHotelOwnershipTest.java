package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.dto.ReviewDtos;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.ChannelReviewActionRepository;
import server.demo.repository.ChannelReviewRepository;
import server.demo.repository.ReservationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SuReviewServiceHotelOwnershipTest {

    private static final String DUPLICATE_HOTEL_ERROR =
            "Su hotel_id 未唯一归属当前门店，已拒绝评价渠道操作";

    private SuReviewHotelOwnershipValidator hotelOwnershipValidator;
    private ChannelReviewActionCoordinator actionCoordinator;
    private SuReviewClient reviewClient;
    private SuReviewService service;

    @BeforeEach
    void setUp() {
        PermissionService permissionService = Mockito.mock(PermissionService.class);
        when(permissionService.hasPermission(
                Mockito.eq(10L),
                Mockito.eq(7L),
                Mockito.eq(PermissionModule.REVIEW),
                Mockito.any(PermissionAction.class)
        )).thenReturn(true);

        hotelOwnershipValidator = Mockito.mock(SuReviewHotelOwnershipValidator.class);
        when(hotelOwnershipValidator.requireUniqueOwnership(10L))
                .thenThrow(new IllegalStateException(DUPLICATE_HOTEL_ERROR));
        actionCoordinator = Mockito.mock(ChannelReviewActionCoordinator.class);
        reviewClient = Mockito.mock(SuReviewClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        service = new SuReviewService(
                Mockito.mock(ChannelReviewRepository.class),
                Mockito.mock(ChannelReviewActionRepository.class),
                Mockito.mock(ReservationRepository.class),
                hotelOwnershipValidator,
                permissionService,
                new ReviewEligibilityService(),
                new AirbnbGuestReviewValidator(),
                actionCoordinator,
                Mockito.mock(SuReviewWebhookMappingValidator.class),
                new SuReviewPayloadMapper(objectMapper),
                reviewClient,
                Mockito.mock(SuApiClient.class),
                objectMapper
        );
    }

    @Test
    void duplicateHotelOwnershipBlocksPullBeforeRemoteCall() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.syncReviews(10L, 7L)
        );

        assertEquals(DUPLICATE_HOTEL_ERROR, error.getMessage());
        verifyNoInteractions(reviewClient);
    }

    @Test
    void duplicateHotelOwnershipBlocksReplyBeforeAuditPreparation() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.reply(
                        10L,
                        7L,
                        1L,
                        new ReviewDtos.ReplyRequest("Reply", "reply-key-123")
                )
        );

        assertEquals(DUPLICATE_HOTEL_ERROR, error.getMessage());
        verifyNoInteractions(actionCoordinator, reviewClient);
    }

    @Test
    void duplicateHotelOwnershipBlocksGuestReviewBeforeAuditPreparation() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.reviewGuest(10L, 7L, 1L, null)
        );

        assertEquals(DUPLICATE_HOTEL_ERROR, error.getMessage());
        verifyNoInteractions(actionCoordinator, reviewClient);
    }
}
