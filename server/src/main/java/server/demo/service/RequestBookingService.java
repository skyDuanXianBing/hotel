package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.util.SuHotelIdUtil;

@Service
public class RequestBookingService {

    private final SuApiClient suApiClient;
    private final StoreRepository storeRepository;
    private final OtaReservationSyncService otaReservationSyncService;
    private final SuAccessTokenService suAccessTokenService;

    public RequestBookingService(
            SuApiClient suApiClient,
            StoreRepository storeRepository,
            OtaReservationSyncService otaReservationSyncService,
            SuAccessTokenService suAccessTokenService
    ) {
        this.suApiClient = suApiClient;
        this.storeRepository = storeRepository;
        this.otaReservationSyncService = otaReservationSyncService;
        this.suAccessTokenService = suAccessTokenService;
    }

    public JsonNode confirm(Long storeId, String bookingId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        String hotelId = resolveHotelId(store);
        validateBookingIdBelongsToHotel(bookingId, hotelId);

        JsonNode response = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.confirmRequestBooking(token, bookingId),
                "requestbookings(confirm)"
        );

        if (suApiClient.isSuSuccess(response)) {
            otaReservationSyncService.syncStoreReservations(storeId);
        }
        return response;
    }

    public JsonNode deny(Long storeId, String bookingId, String declineReason, String messageGuest, String messageAirbnb) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        String hotelId = resolveHotelId(store);
        validateBookingIdBelongsToHotel(bookingId, hotelId);

        JsonNode response = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.denyRequestBooking(token, bookingId, declineReason, messageGuest, messageAirbnb),
                "requestbookings(deny)"
        );

        if (suApiClient.isSuSuccess(response)) {
            otaReservationSyncService.syncStoreReservations(storeId);
        }
        return response;
    }

    private static void validateBookingIdBelongsToHotel(String bookingId, String hotelId) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId 不能为空");
        }
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId 不能为空");
        }
        String trimmed = bookingId.trim();
        String suffix = "_" + hotelId.trim();
        if (!trimmed.endsWith(suffix)) {
            throw new IllegalArgumentException("bookingId 与当前门店不匹配（期望后缀 " + suffix + "）");
        }
    }

    private String resolveHotelId(Store store) {
        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null) {
            hotelId = SuHotelIdUtil.buildDefault(store.getId());
            store.setSuHotelId(hotelId);
            storeRepository.save(store);
        }
        return hotelId;
    }
}
