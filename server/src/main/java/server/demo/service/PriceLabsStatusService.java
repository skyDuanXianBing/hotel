package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.Reservation;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.ReservationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import server.demo.i18n.ApiMessages;
@Service
public class PriceLabsStatusService {

    @Autowired private PriceLabsApiClient apiClient;
    @Autowired private PriceLabsIntegrationRepository integrationRepo;
    @Autowired private ReservationRepository reservationRepo;

    @Transactional(readOnly = true)
    public PriceLabsApiClient.PriceLabsResponse queryStatus(Long storeId, List<PriceLabsApiClient.StatusReq> statuses) {
        if (storeId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d7f09cedd0e8"));
        }
        if (statuses == null || statuses.isEmpty()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.300fe5295d2b"));
        }
        ensureIntegrationEnabled(storeId);
        return apiClient.queryStatus(statuses);
    }

    @Transactional(readOnly = true)
    public PriceLabsApiClient.PriceLabsResponse queryReservationStatusByRoomType(
            Long storeId,
            Long roomTypeId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (storeId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.d7f09cedd0e8"));
        }
        if (roomTypeId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.205932254d65"));
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.1308fe0b7e65"));
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.9fcad55933f6"));
        }

        ensureIntegrationEnabled(storeId);

        List<Reservation> reservations = reservationRepo.findByStoreIdAndRoomTypeIdOverlappingDateRangeWithRoomType(
                storeId, roomTypeId, startDate, endDate);
        if (reservations.isEmpty()) {
            return emptyResponse();
        }

        Set<String> orderNumbers = new LinkedHashSet<>();
        for (Reservation reservation : reservations) {
            if (reservation == null) {
                continue;
            }
            String orderNumber = reservation.getOrderNumber();
            if (orderNumber != null && !orderNumber.isBlank()) {
                orderNumbers.add(orderNumber.trim());
            }
        }

        if (orderNumbers.isEmpty()) {
            return emptyResponse();
        }

        List<PriceLabsApiClient.StatusReq> statuses = new ArrayList<>();
        for (String orderNumber : orderNumbers) {
            PriceLabsApiClient.StatusReq req = new PriceLabsApiClient.StatusReq();
            req.setId(orderNumber);
            req.setType("reservation");
            statuses.add(req);
        }

        return apiClient.queryStatus(statuses);
    }

    private PriceLabsIntegration ensureIntegrationEnabled(Long storeId) {
        PriceLabsIntegration integration = integrationRepo.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("PriceLabs integration not found for store: " + storeId));
        if (!Boolean.TRUE.equals(integration.getIsEnabled())) {
            throw new RuntimeException(ApiMessages.get("api.t.b4c8d68ce538"));
        }
        return integration;
    }

    private static PriceLabsApiClient.PriceLabsResponse emptyResponse() {
        PriceLabsApiClient.PriceLabsResponse res = new PriceLabsApiClient.PriceLabsResponse();
        res.setSuccess(List.of());
        res.setFailure(List.of());
        return res;
    }
}

