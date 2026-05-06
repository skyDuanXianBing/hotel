package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.PriceLabsConnection;
import server.demo.entity.PriceLabsIntegration;
import server.demo.entity.PriceLabsAccount;
import server.demo.entity.Reservation;
import server.demo.entity.RoomBlockout;
import server.demo.entity.Store;
import server.demo.enums.ReservationStatus;
import server.demo.repository.PriceLabsConnectionRepository;
import server.demo.repository.PriceLabsIntegrationRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomBlockoutRepository;
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
    @Autowired private RoomBlockoutRepository roomBlockoutRepo;
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

        List<RoomBlockout> blockouts = roomBlockoutRepo.findByStoreIdAndBlockDateBetween(storeId, from, to);
        for (RoomBlockout blockout : blockouts) {
            if (blockout == null || blockout.getBlockDate() == null || blockout.getRoom() == null || blockout.getRoom().getId() == null || blockout.getRoom().getRoomType() == null) {
                continue;
            }
            Long roomTypeId = blockout.getRoom().getRoomType().getId();
            if (roomTypeId == null) {
                continue;
            }
            String listingId = listingIdByRoomTypeId.get(roomTypeId);
            if (listingId == null || listingId.isBlank()) {
                continue;
            }
            PriceLabsApiClient.ReservationData data = toBlockedReservationData(storeId, store, blockout);
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

    /**
     * PriceLabs 要求 reservation_id 在 PMS 全平台唯一，并且当预留(maintenance/owner blocks)被取消时，
     * 需要通过 /reservations 推送 status=canceled（并带 cancel_time）。
     *
     * <p>注意：RoomBlockout 删除后不再会出现在 pushReservationsForDateRange 的 blocked 列表中，因此这里需要显式推送取消。</p>
     */
    @Transactional(readOnly = true)
    public void pushCancelledBlockouts(
            Long storeId,
            List<RoomBlockout> blockouts,
            Map<Long, Long> roomTypeIdByRoomId,
            LocalDate cancelDate
    ) {
        if (storeId == null || blockouts == null || blockouts.isEmpty()) {
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

        LocalDate effectiveCancelDate = cancelDate != null ? cancelDate : LocalDate.now();

        Map<String, List<PriceLabsApiClient.ReservationData>> byListing = new HashMap<>();
        int reservationCount = 0;

        for (RoomBlockout blockout : blockouts) {
            if (blockout == null || blockout.getBlockDate() == null || blockout.getRoom() == null || blockout.getRoom().getId() == null) {
                continue;
            }
            Long roomId = blockout.getRoom().getId();

            Long roomTypeId = null;
            if (roomTypeIdByRoomId != null) {
                roomTypeId = roomTypeIdByRoomId.get(roomId);
            }
            if (roomTypeId == null && blockout.getRoom().getRoomType() != null) {
                roomTypeId = blockout.getRoom().getRoomType().getId();
            }
            if (roomTypeId == null) {
                continue;
            }

            String listingId = listingIdByRoomTypeId.get(roomTypeId);
            if (listingId == null || listingId.isBlank()) {
                continue;
            }

            PriceLabsApiClient.ReservationData data = toCancelledBlockedReservationData(storeId, store, blockout, effectiveCancelDate);
            byListing.computeIfAbsent(listingId, k -> new ArrayList<>()).add(data);
            reservationCount++;
        }

        if (byListing.isEmpty()) {
            return;
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

        if (!failureMessages.isEmpty()) {
            logger.warn("[PriceLabsReservations] push cancelled blockouts failed. storeId={}, blocks={}, success={}, fail={}, details={}",
                    storeId, reservationCount, successCount, failureCount, String.join("; ", failureMessages));
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
            PriceLabsAccount account = conn.getAccount();
            if (account != null && !Boolean.TRUE.equals(account.getIsEnabled())) {
                continue;
            }
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

        BigDecimal totalCost = nonNegative(reservation.getTotalAmount());
        BigDecimal totalFees = nonNegative(reservation.getOtherFees());
        BigDecimal totalTaxes = BigDecimal.ZERO;
        BigDecimal otaCommission = nonNegative(reservation.getCommission());
        BigDecimal hostPayout = clampNonNegative(totalCost.subtract(otaCommission));
        BigDecimal rentalRevenue = clampNonNegative(totalCost.subtract(totalFees).subtract(totalTaxes));

        d.setReservationId(reservationId);
        d.setStartDate(checkIn != null ? checkIn.toString() : null);
        d.setEndDate(checkOut != null ? checkOut.toString() : null);
        d.setBookedTime(bookedTime.toString());
        d.setTotalDays(totalDays);
        d.setTotalCost(totalCost);
        d.setTotalFees(totalFees);
        d.setTotalTaxes(totalTaxes);
        d.setHostPayout(hostPayout);
        d.setOtaCommission(otaCommission);
        d.setRentalRevenue(rentalRevenue);
        d.setCurrency(resolveCurrency(store));
        d.setStatus(mapStatus(reservation.getStatus()));

        if (ReservationStatus.CANCELLED.equals(reservation.getStatus()) || ReservationStatus.NO_SHOW.equals(reservation.getStatus())) {
            LocalDate cancelTime = reservation.getUpdatedAt() != null ? reservation.getUpdatedAt().toLocalDate() : LocalDate.now();
            d.setCancelTime(cancelTime.toString());
        }

        return d;
    }

    private static PriceLabsApiClient.ReservationData toBlockedReservationData(Long storeId, Store store, RoomBlockout blockout) {
        PriceLabsApiClient.ReservationData d = new PriceLabsApiClient.ReservationData();

        LocalDate blockDate = blockout.getBlockDate();
        LocalDate endDateExclusive = blockDate != null ? blockDate.plusDays(1) : null;
        String blockType = blockout.getBlockType() != null ? blockout.getBlockType().name().toLowerCase() : "blocked";
        Long roomId = blockout.getRoom() != null ? blockout.getRoom().getId() : null;
        String reservationId = "blk_" + storeId + "_" + roomId + "_" + blockDate + "_" + blockType;

        LocalDate bookedTime = blockout.getCreatedAt() != null ? blockout.getCreatedAt().toLocalDate() : LocalDate.now();
        BigDecimal zero = BigDecimal.ZERO;

        d.setReservationId(reservationId);
        d.setStartDate(blockDate != null ? blockDate.toString() : null);
        d.setEndDate(endDateExclusive != null ? endDateExclusive.toString() : null);
        d.setBookedTime(bookedTime.toString());
        d.setTotalDays(1);
        d.setTotalCost(zero);
        d.setTotalFees(zero);
        d.setTotalTaxes(zero);
        d.setHostPayout(zero);
        d.setOtaCommission(zero);
        d.setRentalRevenue(zero);
        d.setCurrency(resolveCurrency(store));
        d.setStatus("blocked");
        return d;
    }

    private static PriceLabsApiClient.ReservationData toCancelledBlockedReservationData(
            Long storeId,
            Store store,
            RoomBlockout blockout,
            LocalDate cancelDate
    ) {
        PriceLabsApiClient.ReservationData d = new PriceLabsApiClient.ReservationData();

        LocalDate blockDate = blockout != null ? blockout.getBlockDate() : null;
        LocalDate endDateExclusive = blockDate != null ? blockDate.plusDays(1) : null;
        String blockType = blockout != null && blockout.getBlockType() != null ? blockout.getBlockType().name().toLowerCase() : "blocked";
        Long roomId = blockout != null && blockout.getRoom() != null ? blockout.getRoom().getId() : null;
        String reservationId = "blk_" + storeId + "_" + roomId + "_" + blockDate + "_" + blockType;

        LocalDate bookedTime = (blockout != null && blockout.getCreatedAt() != null) ? blockout.getCreatedAt().toLocalDate() : cancelDate;
        BigDecimal zero = BigDecimal.ZERO;

        d.setReservationId(reservationId);
        d.setStartDate(blockDate != null ? blockDate.toString() : null);
        d.setEndDate(endDateExclusive != null ? endDateExclusive.toString() : null);
        d.setBookedTime(bookedTime != null ? bookedTime.toString() : null);
        d.setTotalDays(1);
        d.setTotalCost(zero);
        d.setTotalFees(zero);
        d.setTotalTaxes(zero);
        d.setHostPayout(zero);
        d.setOtaCommission(zero);
        d.setRentalRevenue(zero);
        d.setCurrency(resolveCurrency(store));
        d.setStatus("canceled");
        d.setCancelTime(cancelDate != null ? cancelDate.toString() : LocalDate.now().toString());
        return d;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return clampNonNegative(value);
    }

    private static BigDecimal clampNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }
}

