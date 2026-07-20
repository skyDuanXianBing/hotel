package server.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import server.demo.config.SuReviewWebhookAuthConfig;
import server.demo.entity.Channel;
import server.demo.entity.ChannelMappingPriceSetting;
import server.demo.entity.Store;
import server.demo.repository.ChannelMappingPriceSettingRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;
import server.demo.service.SuReviewHotelOwnershipValidator;
import server.demo.service.SuReviewService;
import server.demo.service.SuReviewWebhookMappingValidator;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SuReviewWebhookControllerTest {

    private static final String REVIEW_AUTHORIZATION = "SuReview test-credential";

    private StoreRepository storeRepository;
    private ChannelRepository channelRepository;
    private ChannelMappingPriceSettingRepository mappingRepository;
    private SuReviewService reviewService;
    private SuReviewWebhookAuthConfig authConfig;
    private SuReviewWebhookController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        storeRepository = Mockito.mock(StoreRepository.class);
        channelRepository = Mockito.mock(ChannelRepository.class);
        mappingRepository = Mockito.mock(ChannelMappingPriceSettingRepository.class);
        reviewService = Mockito.mock(SuReviewService.class);
        authConfig = new SuReviewWebhookAuthConfig();
        controller = new SuReviewWebhookController(
                new ObjectMapper(),
                reviewService,
                authConfig,
                new SuReviewHotelOwnershipValidator(storeRepository),
                new SuReviewWebhookMappingValidator(channelRepository, mappingRepository)
        );
        request = Mockito.mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void unconfiguredReviewAuthorizationFailsClosedBeforeParsingOrPersistence() {
        when(request.getHeader("Authorization")).thenReturn(REVIEW_AUTHORIZATION);

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        verify(storeRepository, never()).findAllBySuHotelIdOrderByIdAsc(any());
        verifyNoInteractions(reviewService);
    }

    @Test
    void missingAuthorizationIsRejectedBeforeStoreLookup() {
        authConfig.setAuthorization(REVIEW_AUTHORIZATION);

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(storeRepository, never()).findAllBySuHotelIdOrderByIdAsc(any());
        verifyNoInteractions(reviewService);
    }

    @Test
    void wrongAuthorizationIncludingUnrelatedMessagingCredentialIsRejected() {
        authConfig.setAuthorization(REVIEW_AUTHORIZATION);
        when(request.getHeader("Authorization")).thenReturn("Basic unrelated-messaging-credential");

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(storeRepository, never()).findAllBySuHotelIdOrderByIdAsc(any());
        verifyNoInteractions(reviewService);
    }

    @Test
    void correctReviewAuthorizationAndStandardChannelMappingAreAccepted() {
        configureAuthorization();
        configureBookingMapping("BOOKING", "PROPERTY1");
        when(reviewService.handleWebhook(eq(10L), eq("HOTEL1"), any()))
                .thenReturn(new SuReviewService.WebhookResult(1, 1, 0, 0));

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody().get("status"));
        verify(reviewService).handleWebhook(eq(10L), eq("HOTEL1"), any());
    }

    @Test
    void currentStoreLegacyChannelSuffixMappingIsAccepted() {
        configureAuthorization();
        configureBookingMapping("booking_10", "PROPERTY1");
        when(reviewService.handleWebhook(eq(10L), eq("HOTEL1"), any()))
                .thenReturn(new SuReviewService.WebhookResult(1, 1, 0, 0));

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reviewService).handleWebhook(eq(10L), eq("HOTEL1"), any());
    }

    @Test
    void unknownHotelMappingIsRejectedBeforePersistence() {
        configureAuthorization();
        when(storeRepository.findAllBySuHotelIdOrderByIdAsc("HOTEL1")).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void wrongPropertyMappingIsRejectedBeforePersistence() {
        configureAuthorization();
        configureBookingMapping("BOOKING", "OTHER-PROPERTY");

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void wrongChannelMappingIsRejectedBeforePersistence() {
        configureAuthorization();
        configureBookingMapping("AIRBNB", "PROPERTY1");

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void approximateChannelCodeIsRejectedBeforePersistence() {
        configureAuthorization();
        configureBookingMapping("BOOKING_COM", "PROPERTY1");

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void otherStoreLegacyChannelSuffixIsRejectedBeforePersistence() {
        configureAuthorization();
        configureBookingMapping("BOOKING_11", "PROPERTY1");

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void airbnbMissingListingIdIsRejectedBeforePersistence() {
        configureAuthorization();
        configureAirbnbMapping("LISTING-1");

        ResponseEntity<Map<String, Object>> response = controller.handle(
                request,
                airbnbPayload(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void airbnbMismatchedListingIdIsRejectedBeforePersistence() {
        configureAuthorization();
        configureAirbnbMapping("LISTING-1");

        ResponseEntity<Map<String, Object>> response = controller.handle(
                request,
                airbnbPayload("LISTING-2")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verifyNoInteractions(reviewService);
    }

    @Test
    void airbnbExactListingIdMappingIsAccepted() {
        configureAuthorization();
        configureAirbnbMapping("LISTING-1");
        when(reviewService.handleWebhook(eq(10L), eq("HOTEL1"), any()))
                .thenReturn(new SuReviewService.WebhookResult(1, 1, 0, 0));

        ResponseEntity<Map<String, Object>> response = controller.handle(
                request,
                airbnbPayload("LISTING-1")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reviewService).handleWebhook(eq(10L), eq("HOTEL1"), any());
    }

    @Test
    void persistenceFailureReturnsNon2xxSoSuCanRetry() {
        configureAuthorization();
        configureBookingMapping("BOOKING", "PROPERTY1");
        when(reviewService.handleWebhook(eq(10L), eq("HOTEL1"), any()))
                .thenThrow(new RuntimeException("database unavailable"));

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Fail", response.getBody().get("Status"));
    }

    @Test
    void persistedReplayIsAcknowledgedOnlyAfterIdempotentServiceReturns() {
        configureAuthorization();
        configureBookingMapping("BOOKING", "PROPERTY1");
        when(reviewService.handleWebhook(eq(10L), eq("HOTEL1"), any()))
                .thenReturn(new SuReviewService.WebhookResult(1, 0, 0, 1));

        ResponseEntity<Map<String, Object>> response = controller.handle(request, validBookingPayload());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody().get("status"));
        assertEquals(1, response.getBody().get("duplicates"));
    }

    private void configureAuthorization() {
        authConfig.setAuthorization(REVIEW_AUTHORIZATION);
        when(request.getHeader("Authorization")).thenReturn(REVIEW_AUTHORIZATION);
    }

    private void configureBookingMapping(String channelCode, String mappedPropertyId) {
        Store store = store(10L, "HOTEL1");
        Channel channel = channel(100L, 10L, channelCode);
        ChannelMappingPriceSetting mapping = mapping(
                10L,
                100L,
                "HOTEL1",
                "19",
                mappedPropertyId,
                null
        );
        when(storeRepository.findAllBySuHotelIdOrderByIdAsc("HOTEL1")).thenReturn(List.of(store));
        when(channelRepository.findByStoreId(10L)).thenReturn(List.of(channel));
        when(mappingRepository.findByStoreIdAndSuPropertyIdAndSuChannelId(
                10L,
                "HOTEL1",
                "19"
        )).thenReturn(List.of(mapping));
    }

    private void configureAirbnbMapping(String listingId) {
        Store store = store(10L, "HOTEL1");
        Channel channel = channel(101L, 10L, "AIRBNB");
        ChannelMappingPriceSetting mapping = mapping(
                10L,
                101L,
                "HOTEL1",
                "244",
                "PROPERTY1",
                listingId
        );
        when(storeRepository.findAllBySuHotelIdOrderByIdAsc("HOTEL1")).thenReturn(List.of(store));
        when(channelRepository.findByStoreId(10L)).thenReturn(List.of(channel));
        when(mappingRepository.findByStoreIdAndSuPropertyIdAndSuChannelId(
                10L,
                "HOTEL1",
                "244"
        )).thenReturn(List.of(mapping));
    }

    private static Store store(Long id, String hotelId) {
        Store store = new Store();
        store.setId(id);
        store.setSuHotelId(hotelId);
        return store;
    }

    private static Channel channel(Long id, Long storeId, String code) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setStoreId(storeId);
        channel.setCode(code);
        return channel;
    }

    private static ChannelMappingPriceSetting mapping(
            Long storeId,
            Long channelId,
            String hotelId,
            String suChannelId,
            String channelPropertyId,
            String listingId
    ) {
        ChannelMappingPriceSetting mapping = new ChannelMappingPriceSetting();
        mapping.setStoreId(storeId);
        mapping.setChannelId(channelId);
        mapping.setSuPropertyId(hotelId);
        mapping.setSuChannelId(suChannelId);
        mapping.setChannelHotelId(channelPropertyId);
        mapping.setListingId(listingId);
        return mapping;
    }

    private static String validBookingPayload() {
        return """
                {
                  "hotel_id": "HOTEL1",
                  "channel_id": "19",
                  "channel_property_id": "PROPERTY1",
                  "channel_review_id": "19_R1",
                  "channel_booking_id": "BKG-1",
                  "review_type": "guest_to_host",
                  "review_status": "published",
                  "review_date": "2026-07-18",
                  "review_score": {"review_score": 8},
                  "is_eligible_to_respond": "Y"
                }
                """;
    }

    private static String airbnbPayload(String listingId) {
        String listingField = listingId == null
                ? ""
                : "\"listing_id\": \"" + listingId + "\",";
        return """
                {
                  "hotel_id": "HOTEL1",
                  "channel_id": "244",
                  "channel_property_id": "PROPERTY1",
                  %s
                  "channel_review_id": "244_R1",
                  "channel_booking_id": "AIR-1",
                  "review_type": "guest_to_host",
                  "review_status": "published",
                  "review_date": "2026-07-18",
                  "review_score": {"review_score": 5},
                  "is_eligible_to_respond": "Y"
                }
                """.formatted(listingField);
    }
}
