package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.PriceLabsIdUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PriceLabsReservationSyncService {
    private static final Logger logger = LoggerFactory.getLogger(PriceLabsReservationSyncService.class);

    private static final int MAX_LISTINGS_PER_REQUEST = 20;
    private static final int MAX_RESERVATIONS_PER_LISTING = 100;

    @Autowired private PriceLabsApiClient apiClient;
    @Autowired private PriceLabsConnectionRepository connectionRepo;
    @Autowired private PriceLabsIntegrationRepository integrationRepo;
    @Autowired private ReservationRepository reservationRepo;
    @Autowired private StoreRepository storeRepo;

    public record PushSummary(
            int listingCount,
            int reservationCount,
            int successCount,
            int failureCount,
            List<String> failureMessages
    ) {}

    @Transactional(readOnly = true)
    public PushSummary pushReservationsForDateRange(Long storeId, LocalDate startDate, LocalDate endDate) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId 不能为空");
        }
        LocalDate from = startDate != null ? startDate : LocalDate.of(2020, 1, 1);
        LocalDate to = endDate != null ? endDate : LocalDate.now().plusDays(365);
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("endDate 不能早于 startDate");
        }

        PriceLabsIntegration integration = integrationRepo.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("PriceLabs integration not found for store: " + storeId));
        if (!Boolean.TRUE.equals(integration.getIsEnabled())) {
            throw new RuntimeException("PriceLabs 集成未启用");
        }

        Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        Map<Long, String> listingIdByRoomTypeId = enabledListingIdByRoomTypeId(storeId);
        if (listingIdByRoomTypeId.isEmpty()) {
            throw new RuntimeException("没有启用的 PriceLabs 连接，无法推送 reservations");
        }

        List<Reservation> reservations = reservationRepo.findByStoreIdOverlappingDateRangeWithRoomType(storeId, from, to);
        if (reservations.isEmpty()) {
            return new PushSummary(0, 0, 0, 0, List.of());
        }

        Map<String, List<PriceLabsApiClient.ReservationData>> byListing = new HashMap<>();
        int reservationCount = 0;

        for (Reservation reservation : reservations) {
            if (reservation == null) continue;
            Long roomTypeId = resolveRoomTypeId(reservation);
            if (roomTypeId == null) continue;

            String listingId = listingIdByRoomTypeId.get(roomTypeId);
            if (listingId == null || listingId.isBlank()) {
                continue;
            }

            PriceLabsApiClient.ReservationData data = toReservationData(reservation, store, listingId);
            byListing.computeIfAbsent(listingId, k -> new ArrayList<>()).add(data);
            reservationCount++;
        }

        if (byListing.isEmpty()) {
            return new PushSummary(0, 0, 0, 0, List.of());
        }

        List<String> failureMessages = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        List<PriceLabsApiClient.ReservationsByListing> batch = new ArrayList<>();
        for (Map.Entry<String, List<PriceLabsApiClient.ReservationData>> entry : byListing.entrySet()) {
            String listingId = entry.getKey();
            List<PriceLabsApiClient.ReservationData> all = entry.getValue();
            if (all == null || all.isEmpty()) continue;

            int index = 0;
            while (index < all.size()) {
                int next = Math.min(index + MAX_RESERVATIONS_PER_LISTING, all.size());

                PriceLabsApiClient.ReservationsByListing item = new PriceLabsApiClient.ReservationsByListing();
                item.setListingId(listingId);
                item.setData(new ArrayList<>(all.subList(index, next)));
                batch.add(item);

                if (batch.size() >= MAX_LISTINGS_PER_REQUEST) {
                    PushResult r = pushBatch(batch);
                    successCount += r.successCount;
                    failureCount += r.failureCount;
                    failureMessages.addAll(r.failureMessages);
                    batch.clear();
                }

                index = next;
            }
        }

        if (!batch.isEmpty()) {
            PushResult r = pushBatch(batch);
            successCount += r.successCount;
            failureCount += r.failureCount;
            failureMessages.addAll(r.failureMessages);
        }

        integration.setLastReservationSyncAt(java.time.LocalDateTime.now());
        integrationRepo.save(integration);

        return new PushSummary(byListing.size(), reservationCount, successCount, failureCount, failureMessages);
    }

    @Transactional(readOnly = true)
    public void pushReservationById(Long storeId, Long reservationId) {
        if (storeId == null || reservationId == null) {
            return;
        }

        Optional<PriceLabsIntegration> integrationOpt = integrationRepo.findByStoreId(storeId);
        if (integrationOpt.isEmpty() || !Boolean.TRUE.equals(integrationOpt.get().getIsEnabled())) {
            return;
        }

        Store store = storeRepo.findById(storeId).orElse(null);
        if (store == null) {
            return;
        }

        Map<Long, String> listingIdByRoomTypeId = enabledListingIdByRoomTypeId(storeId);
        if (listingIdByRoomTypeId.isEmpty()) {
            return;
        }

        Reservation reservation = reservationRepo.findByStoreIdAndIdWithRoomType(storeId, reservationId).orElse(null);
        if (reservation == null) {
            return;
        }

        Long roomTypeId = resolveRoomTypeId(reservation);
        if (roomTypeId == null) {
            return;
        }

        String listingId = listingIdByRoomTypeId.get(roomTypeId);
        if (listingId == null || listingId.isBlank()) {
            return;
        }

        PriceLabsApiClient.ReservationData data = toReservationData(reservation, store, listingId);

        PriceLabsApiClient.ReservationsByListing byListing = new PriceLabsApiClient.ReservationsByListing();
        byListing.setListingId(listingId);
        byListing.setData(List.of(data));

        PushResult r = pushBatch(List.of(byListing));
        if (!r.failureMessages.isEmpty()) {
            logger.warn("[PriceLabsReservations] push reservation failed. storeId={}, reservationId={}, details={}",
                    storeId, reservationId, String.join("; ", r.failureMessages));
        }

        PriceLabsIntegration integration = integrationOpt.get();
        integration.setLastReservationSyncAt(java.time.LocalDateTime.now());
        integrationRepo.save(integration);
    }

    private record PushResult(int successCount, int failureCount, List<String> failureMessages) {}

    private PushResult pushBatch(List<PriceLabsApiClient.ReservationsByListing> batch) {
        if (batch == null || batch.isEmpty()) {
            return new PushResult(0, 0, List.of());
        }
        PriceLabsApiClient.PriceLabsResponse response = apiClient.pushReservations(batch);
        int successCount = response != null && response.getSuccess() != null ? response.getSuccess().size() : 0;
        int failureCount = response != null && response.getFailure() != null ? response.getFailure().size() : 0;

        List<String> failures = List.of();
        if (response != null && response.getFailure() != null && !response.getFailure().isEmpty()) {
            failures = PriceLabsApiClient.extractFailureMessages(Map.of("failure", response.getFailure()));
        }
        return new PushResult(successCount, failureCount, failures);
    }

    private Map<Long, String> enabledListingIdByRoomTypeId(Long storeId) {
        List<PriceLabsConnection> enabled = connectionRepo.findByStoreIdAndIsEnabledTrue(storeId);
        Map<Long, String> map = new HashMap<>();
        for (PriceLabsConnection conn : enabled) {
            if (conn == null || conn.getRoomType() == null || conn.getRoomType().getId() == null) continue;
            Long roomTypeId = conn.getRoomType().getId();

            String listingId = conn.getPriceLabsListingId();
            if (listingId == null || listingId.isBlank()) {
                listingId = PriceLabsIdUtil.formatListingId(storeId, roomTypeId);
            }
            map.put(roomTypeId, listingId);
        }
        return map;
    }

    private static Long resolveRoomTypeId(Reservation reservation) {
        if (reservation.getRoom() != null && reservation.getRoom().getRoomType() != null) {
            return reservation.getRoom().getRoomType().getId();
        }
        return reservation.getOtaRoomTypeId();
    }

    private static String resolveCurrency(Store store) {
        if (store != null && store.getCurrency() != null && !store.getCurrency().trim().isEmpty()) {
            return store.getCurrency().trim();
        }
        return "JPY";
    }

    private static String mapStatus(ReservationStatus status) {
        if (status == null) return "unconfirmed";
        return switch (status) {
            case CONFIRMED, CHECKED_IN, CHECKED_OUT -> "booked";
            case CANCELLED, NO_SHOW -> "canceled";
            case REQUESTED -> "unconfirmed";
        };
    }

    private static PriceLabsApiClient.ReservationData toReservationData(Reservation reservation, Store store, String listingId) {
        PriceLabsApiClient.ReservationData d = new PriceLabsApiClient.ReservationData();

        String reservationId = reservation.getOrderNumber();
        if (reservationId == null || reservationId.isBlank()) {
            reservationId = "rsv_" + reservation.getId() + "_" + listingId;
        }

        LocalDate checkIn = reservation.getCheckInDate();
        LocalDate checkOut = reservation.getCheckOutDate();
        LocalDate bookedTime = reservation.getCreatedAt() != null ? reservation.getCreatedAt().toLocalDate() : LocalDate.now();

        int totalDays = 0;
        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            totalDays = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        }

        BigDecimal totalCost = reservation.getTotalAmount() != null ? reservation.getTotalAmount() : BigDecimal.ZERO;

        d.setReservationId(reservationId);
        d.setStartDate(checkIn != null ? checkIn.toString() : null);
        d.setEndDate(checkOut != null ? checkOut.toString() : null);
        d.setBookedTime(bookedTime.toString());
        d.setTotalDays(totalDays);
        d.setTotalCost(totalCost);
        d.setCurrency(resolveCurrency(store));
        d.setStatus(mapStatus(reservation.getStatus()));

        if (ReservationStatus.CANCELLED.equals(reservation.getStatus()) || ReservationStatus.NO_SHOW.equals(reservation.getStatus())) {
            LocalDate cancelTime = reservation.getUpdatedAt() != null ? reservation.getUpdatedAt().toLocalDate() : LocalDate.now();
            d.setCancelTime(cancelTime.toString());
        }

        return d;
    }
}

