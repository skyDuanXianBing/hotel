package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Store;
import server.demo.entity.User;
import server.demo.entity.SuMessageThread;
import server.demo.enums.ReservationStatus;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.UserRepository;
import server.demo.util.SuHotelIdUtil;
import server.demo.util.SuReservationParser;
import server.demo.util.SuRoomIdParser;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.UnexpectedRollbackException;

import java.time.LocalDateTime;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class OtaReservationSyncService {

    private static final Logger logger = LoggerFactory.getLogger(OtaReservationSyncService.class);
    private static final Logger reservationLogger = LoggerFactory.getLogger("SU_RESERVATION");

    private static final String OTA_CHANNEL_CODE_AIRBNB = "AIRBNB";
    private static final String OTA_CHANNEL_CODE_BOOKING = "BOOKING";

    private static final List<String> SUPPORTED_CHANNEL_CODES = List.of(
            OTA_CHANNEL_CODE_AIRBNB,
            OTA_CHANNEL_CODE_BOOKING
    );

    private final SuApiClient suApiClient;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ReservationRepository reservationRepository;
    private final SuMessageThreadRepository threadRepository;
    private final OtaReservationRoomAssignmentService roomAssignmentService;
    private final TransactionTemplate transactionTemplate;
    private final SuAccessTokenService suAccessTokenService;
    private final AutoMessageTriggerService autoMessageTriggerService;
    private final CleaningTaskAutoService cleaningTaskAutoService;
    private final PriceLabsCalendarSyncDebouncer priceLabsCalendarSyncDebouncer;
    private final RegistrationLinkInboxService registrationLinkInboxService;

    public OtaReservationSyncService(
            SuApiClient suApiClient,
            StoreRepository storeRepository,
            UserRepository userRepository,
            ChannelRepository channelRepository,
            ReservationRepository reservationRepository,
            SuMessageThreadRepository threadRepository,
            OtaReservationRoomAssignmentService roomAssignmentService,
            PlatformTransactionManager transactionManager,
            SuAccessTokenService suAccessTokenService,
            AutoMessageTriggerService autoMessageTriggerService,
            CleaningTaskAutoService cleaningTaskAutoService,
            PriceLabsCalendarSyncDebouncer priceLabsCalendarSyncDebouncer,
            RegistrationLinkInboxService registrationLinkInboxService
    ) {
        this.suApiClient = suApiClient;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.reservationRepository = reservationRepository;
        this.threadRepository = threadRepository;
        this.roomAssignmentService = roomAssignmentService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.suAccessTokenService = suAccessTokenService;
        this.autoMessageTriggerService = autoMessageTriggerService;
        this.cleaningTaskAutoService = cleaningTaskAutoService;
        this.priceLabsCalendarSyncDebouncer = priceLabsCalendarSyncDebouncer;
        this.registrationLinkInboxService = registrationLinkInboxService;
    }

    public List<String> getSupportedChannelCodes() {
        return SUPPORTED_CHANNEL_CODES;
    }

    public record ReservationSyncResult(
            Long storeId,
            String hotelId,
            int pulledReservations,
            int processedRoomStays,
            int skippedUnsupportedOta,
            int createdCount,
            int updatedCount,
            int failedCount,
            int ackRequested,
            int ackSuccess,
            String ackErrorMessage,
            List<String> ackNotifIds,
            List<String> errors
    ) {}

    public record PullUpsertResult(
            Long storeId,
            String hotelId,
            int pulledReservations,
            int processedRoomStays,
            int skippedUnsupportedOta,
            int createdCount,
            int updatedCount,
            int failedCount,
            List<String> errors
    ) {}

    public record UpsertOnlyResult(
            int processedRoomStays,
            int skippedUnsupportedOta,
            int createdCount,
            int updatedCount,
            int failedCount,
            List<String> errors
    ) {}

    record CalendarSyncRequest(Long roomTypeId, LocalDate startDate, LocalDate endDate) {}

    static List<CalendarSyncRequest> buildCalendarSyncRequests(
            Long oldRoomTypeId,
            LocalDate oldCheckIn,
            LocalDate oldCheckOut,
            Long newRoomTypeId,
            LocalDate newCheckIn,
            LocalDate newCheckOut
    ) {
        List<CalendarSyncRequest> out = new ArrayList<>();

        CalendarSyncRequest oldReq = toCalendarSyncRequest(oldRoomTypeId, oldCheckIn, oldCheckOut);
        if (oldReq != null) {
            out.add(oldReq);
        }

        CalendarSyncRequest newReq = toCalendarSyncRequest(newRoomTypeId, newCheckIn, newCheckOut);
        if (newReq != null) {
            if (out.isEmpty()) {
                out.add(newReq);
            } else {
                CalendarSyncRequest first = out.get(0);
                boolean same = Objects.equals(first.roomTypeId, newReq.roomTypeId)
                        && Objects.equals(first.startDate, newReq.startDate)
                        && Objects.equals(first.endDate, newReq.endDate);
                if (!same) {
                    out.add(newReq);
                }
            }
        }

        return out;
    }

    private static CalendarSyncRequest toCalendarSyncRequest(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        if (roomTypeId == null || checkIn == null || checkOut == null) {
            return null;
        }

        // Reservation occupancy is [checkIn, checkOut) (checkOut is exclusive).
        // PriceLabs /calendar uses inclusive [start_date, end_date], so we push until checkOut-1.
        LocalDate start = checkIn;
        LocalDate end = checkOut.minusDays(1);
        if (end.isBefore(start)) {
            end = start;
        }
        return new CalendarSyncRequest(roomTypeId, start, end);
    }

    public ReservationSyncResult syncStoreReservations(Long storeId) {
        return syncStoreReservations(storeId, null);
    }

    /**
     * Compatibility: some Su flows deliver reservation details directly to webhook without reservation_notif_id.
     * In that case, we can upsert the given reservations locally without pulling from Su.
     */
    public UpsertOnlyResult upsertReservationsFromWebhook(Long storeId, List<JsonNode> reservations) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("闂ㄥ簵涓嶅瓨鍦? " + storeId));

        List<JsonNode> reservationNodes = reservations != null ? reservations : List.of();
        if (reservationNodes.isEmpty()) {
            return new UpsertOnlyResult(0, 0, 0, 0, 0, List.of());
        }

        UpsertResult result;
        try {
            result = transactionTemplate.execute(status -> upsertReservationsInTx(store, reservationNodes));
        } catch (UnexpectedRollbackException e) {
            reservationLogger.error(
                    "[ReservationWebhook] upsert tx rolled back (rollback-only). storeId={}, hotelId={}, reservations={}, sample={}",
                    storeId,
                    resolveHotelId(store),
                    reservationNodes.size(),
                    summarizeReservationNodes(reservationNodes, 3),
                    e
            );
            throw e;
        }
        if (result == null) {
            return new UpsertOnlyResult(0, 0, 0, 0, 0, List.of("upsert tx failed"));
        }

        // Best-effort dispatch auto messages after new/updated reservations.
        if (result.createdCount() > 0 || result.updatedCount() > 0) {
            try {
                autoMessageTriggerService.dispatchStoreOnce(storeId);
            } catch (Exception e) {
                logger.warn("Dispatch auto messages after reservation webhook upsert failed. storeId={}, err={}", storeId, e.getMessage(), e);
                reservationLogger.error("[ReservationWebhook] dispatch auto messages failed. storeId={}, err={}", storeId, e.getMessage());
            }
        }

        return new UpsertOnlyResult(
                result.processedRoomStays(),
                result.skippedUnsupportedOta(),
                result.createdCount(),
                result.updatedCount(),
                result.failedCount(),
                result.errors()
        );
    }

    public ReservationSyncResult syncStoreReservations(Long storeId, Set<String> onlyAckNotifIds) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        String hotelId = resolveHotelId(store);
        reservationLogger.info("[ReservationSync] start. storeId={}, hotelId={}, onlyAckNotifIds={}",
                storeId, hotelId, onlyAckNotifIds != null ? onlyAckNotifIds.size() : null);

        JsonNode raw = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.pullReservations(token, hotelId),
                "Reservation"
        );
        List<JsonNode> reservations = SuReservationParser.extractReservationNodes(raw);
        reservationLogger.info("[ReservationSync] pulled reservations. storeId={}, hotelId={}, count={}",
                storeId, hotelId, reservations.size());

        UpsertResult upsert = transactionTemplate.execute(status -> upsertReservationsInTx(store, reservations));
        if (upsert == null) {
            upsert = new UpsertResult(0, 0, 0, 0, 0, Set.of(), List.of("事务执行失败：upsert结果为空"));
        }

        reservationLogger.info("[ReservationSync] upsert done. storeId={}, hotelId={}, processed={}, created={}, updated={}, failed={}, notifIds={}",
                storeId, hotelId, upsert.processedRoomStays(), upsert.createdCount(), upsert.updatedCount(), upsert.failedCount(), upsert.notifIds().size());

        List<String> ackNotifIds = buildAckNotifIds(upsert.notifIds(), onlyAckNotifIds);
        int ackRequested = ackNotifIds.size();
        int ackSuccess = 0;
        String ackErrorMessage = null;

        if (ackRequested > 0) {
            try {
                reservationLogger.info("[ReservationSync] ack start. storeId={}, hotelId={}, ackRequested={}", storeId, hotelId, ackRequested);
                JsonNode ackResponse = suAccessTokenService.executeWithTokenRetry(
                        token -> suApiClient.ackReservationNotifs(token, hotelId, ackNotifIds),
                        "Reservation_notif"
                );
                if (!suApiClient.isSuSuccess(ackResponse)) {
                    ackErrorMessage = suApiClient.extractSuErrorMessage(ackResponse);
                    logger.warn("Su Reservation_notif returned non-success. storeId={}, hotelId={}, body={}", storeId, hotelId, ackResponse);
                    reservationLogger.error("[ReservationSync] ack failed. storeId={}, hotelId={}, err={}, body={}",
                            storeId, hotelId, ackErrorMessage, ackResponse);
                } else {
                    ackSuccess = ackRequested;
                    reservationLogger.info("[ReservationSync] ack success. storeId={}, hotelId={}, ackSuccess={}", storeId, hotelId, ackSuccess);
                }
            } catch (Exception e) {
                ackErrorMessage = e.getMessage();
                logger.error("Ack reservation notif failed. storeId={}, hotelId={}", storeId, hotelId, e);
                reservationLogger.error("[ReservationSync] ack exception. storeId={}, hotelId={}, err={}", storeId, hotelId, ackErrorMessage, e);
            }
        } else {
            reservationLogger.info("[ReservationSync] ack skipped. storeId={}, hotelId={}, reason=no notif ids", storeId, hotelId);
        }

        // 尽快处理 IMMEDIATELY 的业务自动消息（例如 BOOKING_CONFIRM）
        if (upsert.createdCount() > 0 || upsert.updatedCount() > 0) {
            try {
                autoMessageTriggerService.dispatchStoreOnce(storeId);
            } catch (Exception e) {
                logger.warn("Dispatch auto messages after reservation sync failed. storeId={}, err={}", storeId, e.getMessage(), e);
                reservationLogger.error("[ReservationSync] dispatch auto messages failed. storeId={}, err={}", storeId, e.getMessage());
            }
        }

        return new ReservationSyncResult(
                storeId,
                hotelId,
                reservations.size(),
                upsert.processedRoomStays(),
                upsert.skippedUnsupportedOta(),
                upsert.createdCount(),
                upsert.updatedCount(),
                upsert.failedCount(),
                ackRequested,
                ackSuccess,
                ackErrorMessage,
                ackNotifIds,
                upsert.errors()
        );
    }

    /**
     * PUSH-mode: pull reservations and upsert locally, but DO NOT call Reservation_notif ack endpoint.
     * Confirmation is done by replying to the webhook with reservation_notif_id list.
     */
    public PullUpsertResult pullAndUpsertReservationsWithoutAck(Long storeId, Set<String> onlyProcessNotifIds) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("闂ㄥ簵涓嶅瓨鍦? " + storeId));

        String hotelId = resolveHotelId(store);
        reservationLogger.info("[ReservationSyncPush] start. storeId={}, hotelId={}, onlyProcessNotifIds={}",
                storeId, hotelId, onlyProcessNotifIds != null ? onlyProcessNotifIds.size() : null);

        JsonNode raw = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.pullReservations(token, hotelId),
                "Reservation"
        );
        List<JsonNode> reservations = SuReservationParser.extractReservationNodes(raw);
        reservationLogger.info("[ReservationSyncPush] pulled reservations. storeId={}, hotelId={}, count={}",
                storeId, hotelId, reservations.size());

        List<JsonNode> filtered = filterReservationsByNotifIds(reservations, onlyProcessNotifIds);

        UpsertResult upsert = transactionTemplate.execute(status -> upsertReservationsInTx(store, filtered));
        if (upsert == null) {
            upsert = new UpsertResult(0, 0, 0, 0, 0, Set.of(), List.of("浜嬪姟鎵ц澶辫触锛歶psert缁撴灉涓虹┖"));
        }

        reservationLogger.info("[ReservationSyncPush] upsert done. storeId={}, hotelId={}, processed={}, created={}, updated={}, failed={}, notifIds={}",
                storeId, hotelId, upsert.processedRoomStays(), upsert.createdCount(), upsert.updatedCount(), upsert.failedCount(), upsert.notifIds().size());

        if (upsert.createdCount() > 0 || upsert.updatedCount() > 0) {
            try {
                autoMessageTriggerService.dispatchStoreOnce(storeId);
            } catch (Exception e) {
                logger.warn("Dispatch auto messages after reservation push sync failed. storeId={}, err={}", storeId, e.getMessage(), e);
                reservationLogger.error("[ReservationSyncPush] dispatch auto messages failed. storeId={}, err={}", storeId, e.getMessage());
            }
        }

        return new PullUpsertResult(
                storeId,
                hotelId,
                reservations.size(),
                upsert.processedRoomStays(),
                upsert.skippedUnsupportedOta(),
                upsert.createdCount(),
                upsert.updatedCount(),
                upsert.failedCount(),
                upsert.errors()
        );
    }

    private static List<JsonNode> filterReservationsByNotifIds(List<JsonNode> reservations, Set<String> onlyProcessNotifIds) {
        if (reservations == null || reservations.isEmpty()) {
            return List.of();
        }
        if (onlyProcessNotifIds == null || onlyProcessNotifIds.isEmpty()) {
            return reservations;
        }

        List<JsonNode> out = new ArrayList<>();
        for (JsonNode reservationNode : reservations) {
            if (reservationNode == null || reservationNode.isNull()) {
                continue;
            }
            String notifId = SuReservationParser.extractReservationNotifId(reservationNode);
            if (notifId == null || notifId.isBlank()) {
                continue;
            }
            if (onlyProcessNotifIds.contains(notifId.trim())) {
                out.add(reservationNode);
            }
        }
        return out;
    }

    private static List<String> buildAckNotifIds(Set<String> extracted, Set<String> onlyAck) {
        if (extracted == null || extracted.isEmpty()) {
            return List.of();
        }
        if (onlyAck == null || onlyAck.isEmpty()) {
            return extracted.stream().toList();
        }
        List<String> list = new ArrayList<>();
        for (String id : extracted) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (onlyAck.contains(id)) {
                list.add(id);
            }
        }
        return list;
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

    private record UpsertResult(
            int processedRoomStays,
            int skippedUnsupportedOta,
            int createdCount,
            int updatedCount,
            int failedCount,
            Set<String> notifIds,
            List<String> errors
    ) {}

    private UpsertResult upsertReservationsInTx(Store store, List<JsonNode> reservations) {
        List<String> errors = new ArrayList<>();
        Set<String> notifIds = new LinkedHashSet<>();

        String suHotelId = resolveHotelId(store);

        int processedRoomStays = 0;
        int skippedUnsupported = 0;
        int created = 0;
        int updated = 0;
        int failed = 0;

        User user = userRepository.findById(store.getUserId())
                .orElseThrow(() -> new IllegalStateException("门店关联的用户不存在: " + store.getUserId()));

        for (JsonNode reservationNode : reservations) {
            if (reservationNode == null || reservationNode.isNull()) {
                continue;
            }

            String notifId = SuReservationParser.extractReservationNotifId(reservationNode);
            String reservationId = SuReservationParser.extractReservationId(reservationNode);
            String suStatus = SuReservationParser.extractSuStatus(reservationNode);
            String channelBookingId = SuReservationParser.extractChannelBookingId(reservationNode);
            String otaCode = SuReservationParser.extractOtaCode(reservationNode);
            String channelCode = SuReservationParser.mapOtaChannelCode(otaCode);

            if (channelCode == null) {
                skippedUnsupported++;
                reservationLogger.info(
                        "[ReservationUpsert] skip unsupported ota. storeId={}, hotelId={}, reservationId={}, notifId={}, otaCode={}, status={}",
                        store.getId(),
                        suHotelId,
                        reservationId,
                        notifId,
                        otaCode,
                        suStatus
                );
                continue;
            }

            List<JsonNode> roomStays = SuReservationParser.extractRoomStays(reservationNode);
            if (roomStays.isEmpty()) {
                roomStays = List.of((JsonNode) null);
            }
            int roomStayCount = roomStays.size();

            for (JsonNode roomStay : roomStays) {
                processedRoomStays++;
                try {
                    LocalDate checkIn = SuReservationParser.extractArrivalDate(reservationNode, roomStay);
                    LocalDate checkOut = SuReservationParser.extractDepartureDate(reservationNode, roomStay);
                    if (checkIn == null || checkOut == null) {
                        throw new IllegalArgumentException("缺少入住/退房日期");
                    }

                    String roomReservationId = roomStay != null ? SuReservationParser.extractRoomReservationId(roomStay) : null;
                    String orderNumber = SuReservationParser.buildOrderNumber(store.getId(), reservationId, roomReservationId);

                    reservationLogger.info(
                            "[ReservationUpsert] begin. storeId={}, hotelId={}, channel={}, reservationId={}, notifId={}, channelBookingId={}, suStatus={}, orderNumber={}, checkIn={}, checkOut={}, roomReservationId={}",
                            store.getId(),
                            suHotelId,
                            channelCode,
                            reservationId,
                            notifId,
                            channelBookingId,
                            suStatus,
                            orderNumber,
                            checkIn,
                            checkOut,
                            roomReservationId
                    );

                    Channel channel = resolveChannel(store.getId(), channelCode)
                            .orElseThrow(() -> new IllegalStateException("渠道不存在: " + channelCode));

                    Reservation reservation = reservationRepository.findByStoreIdAndOrderNumber(store.getId(), orderNumber)
                            .orElseGet(Reservation::new);

                    boolean isNew = reservation.getId() == null;
                    LocalDate oldCheckIn = isNew ? null : reservation.getCheckInDate();
                    LocalDate oldCheckOut = isNew ? null : reservation.getCheckOutDate();
                    Long oldRoomTypeId = isNew ? null : reservation.getOtaRoomTypeId();

                    reservationLogger.info(
                            "[ReservationUpsert] resolved target. storeId={}, hotelId={}, orderNumber={}, isNew={}, existingDbId={}",
                            store.getId(),
                            suHotelId,
                            orderNumber,
                            isNew,
                            reservation.getId()
                    );

                    reservation.setStoreId(store.getId());
                    reservation.setUser(user);
                    reservation.setChannel(channel);

                    if (reservation.getRoom() == null) {
                        reservation.setRoom(null);
                    }

                    reservation.setGuestName(SuReservationParser.extractGuestName(reservationNode, roomStay));
                    reservation.setGuestPhone(SuReservationParser.extractGuestPhone(reservationNode, roomStay));
                    reservation.setCheckInDate(checkIn);
                    reservation.setCheckOutDate(checkOut);
                    reservation.setAdults(SuReservationParser.extractAdults(reservationNode, roomStay));
                    reservation.setChildren(SuReservationParser.extractChildren(reservationNode, roomStay));
                    reservation.setTotalAmount(SuReservationParser.extractTotalAmount(reservationNode, roomStay));
                    reservation.setChannelOrderNumber(channelBookingId);

                    // Channel / Su fields (for channel-info UI and audit)
                    reservation.setSuHotelId(suHotelId);
                    reservation.setSuReservationId(reservationId);
                    reservation.setReservationNotifId(notifId);
                    reservation.setRoomReservationId(roomReservationId);
                    reservation.setPaymentMethod(SuReservationParser.extractPaymentType(reservationNode));
                    reservation.setCurrencyCode(SuReservationParser.extractCurrencyCode(reservationNode));
                    reservation.setCommission(SuReservationParser.extractCommissionAmount(reservationNode));

                    LocalDate bookedAt = SuReservationParser.extractBookedAt(reservationNode);
                    reservation.setBookingDate(bookedAt != null ? bookedAt.atStartOfDay() : null);

                    LocalDate modifiedAt = SuReservationParser.extractModifiedAt(reservationNode);
                    reservation.setModifiedAt(modifiedAt != null ? modifiedAt.atStartOfDay() : null);

                    String roomSpecialRequest = roomStay != null ? SuReservationParser.extractRoomSpecialRequest(roomStay) : null;
                    String customerRemarks = SuReservationParser.extractCustomerRemarks(reservationNode);
                    reservation.setSpecialRequests(combineSpecialRequests(roomSpecialRequest, customerRemarks));

                    reservation.setStatus(mapReservationStatus(suStatus));
                    reservation.setOrderNumber(orderNumber);

                    // 记录 Su rooms[].id（我们推送的 roomid={roomTypeId}-{roomNumber}），用于未排房也能按房型统计占用/回传 PriceLabs booked_units
                    String itProviderRoomId = roomStay != null ? SuReservationParser.extractRoomTypeId(roomStay) : null;
                    reservation.setOtaRoomId(itProviderRoomId);
                    SuRoomIdParser.ParsedRoomId parsedRoomId = SuRoomIdParser.parse(itProviderRoomId);
                    Long newRoomTypeId = parsedRoomId != null ? parsedRoomId.roomTypeId() : null;
                    reservation.setOtaRoomTypeId(newRoomTypeId);
                    reservation.setOtaRoomNumber(parsedRoomId != null ? parsedRoomId.roomNumber() : null);

                    // 自动排房（仅当当前预订未手动排房时尝试）
                    if (reservation.getRoom() == null) {
                        try {
                            roomAssignmentService.tryAutoAssignRoom(store.getId(), reservation, itProviderRoomId, checkIn, checkOut);
                        } catch (Exception ex) {
                            logger.warn("Auto-assign room skipped due to error. storeId={}, orderNumber={}, err={}",
                                    store.getId(), orderNumber, ex.getMessage(), ex);
                            reservationLogger.error(
                                    "[ReservationUpsert] auto-assign exception. storeId={}, hotelId={}, orderNumber={}, roomid={}, checkIn={}, checkOut={}, errType={}, err={}",
                                    store.getId(),
                                    suHotelId,
                                    orderNumber,
                                    itProviderRoomId,
                                    checkIn,
                                    checkOut,
                                    ex.getClass().getSimpleName(),
                                    ex.getMessage(),
                                    ex
                            );
                        }
                    }

                    reservationRepository.save(reservation);
                    reservationLogger.info(
                            "[ReservationUpsert] saved. storeId={}, hotelId={}, orderNumber={}, reservationDbId={}, assignedRoomId={}, otaRoomId={}, otaRoomTypeId={}, otaRoomNumber={}, status={}",
                            store.getId(),
                            suHotelId,
                            orderNumber,
                            reservation.getId(),
                            reservation.getRoom() != null ? reservation.getRoom().getId() : null,
                            reservation.getOtaRoomId(),
                            reservation.getOtaRoomTypeId(),
                            reservation.getOtaRoomNumber(),
                            reservation.getStatus() != null ? reservation.getStatus().name() : null
                    );

                    if (isNew) {
                        String bookingKey = (channelBookingId != null && !channelBookingId.isBlank()) ? channelBookingId : orderNumber;
                        registrationLinkInboxService.recordIfAbsent(
                                store.getId(),
                                bookingKey,
                                reservation.getGuestName(),
                                reservation.getCheckInDate(),
                                reservation.getCheckOutDate(),
                                roomStayCount
                        );
                    }

                    // Ensure SuMessageThread exists so auto-messages (BOOKING_CONFIRM/IMMEDIATELY etc.) can send
                    // without waiting for inbound messaging webhook.
                    tryUpsertMessageThreadFromReservation(store.getId(), suHotelId, channelCode, reservationNode, reservation);

                    cleaningTaskAutoService.syncTaskForReservation(reservation);

                    if (priceLabsCalendarSyncDebouncer != null) {
                        List<CalendarSyncRequest> reqs = buildCalendarSyncRequests(
                                oldRoomTypeId,
                                oldCheckIn,
                                oldCheckOut,
                                newRoomTypeId,
                                checkIn,
                                checkOut
                        );
                        for (CalendarSyncRequest req : reqs) {
                            priceLabsCalendarSyncDebouncer.requestSyncAfterCommit(store.getId(), req.roomTypeId(), req.startDate(), req.endDate());
                        }
                    }

                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }

                    if (notifId != null && !notifId.isBlank()) {
                        notifIds.add(notifId.trim());
                    }
                } catch (Exception e) {
                    failed++;
                    String rid = Objects.toString(reservationId, "unknown");
                    errors.add("reservationId=" + rid + ", channel=" + channelCode + ", error=" + e.getMessage());
                    logger.warn("Upsert Su reservation failed. storeId={}, reservationId={}, channelCode={}",
                            store.getId(), reservationId, channelCode, e);
                    reservationLogger.error(
                            "[ReservationUpsert] failed. storeId={}, hotelId={}, reservationId={}, notifId={}, channel={}, channelBookingId={}, suStatus={}, errType={}, err={}",
                            store.getId(),
                            suHotelId,
                            reservationId,
                            notifId,
                            channelCode,
                            channelBookingId,
                            suStatus,
                            e.getClass().getSimpleName(),
                            e.getMessage(),
                            e
                    );
                }
            }
        }

        return new UpsertResult(
                processedRoomStays,
                skippedUnsupported,
                created,
                updated,
                failed,
                notifIds,
                errors
        );
    }

    private Optional<Channel> resolveChannel(Long storeId, String channelCode) {
        if (storeId == null || channelCode == null || channelCode.isBlank()) {
            return Optional.empty();
        }
        Optional<Channel> storeScoped = channelRepository.findByStoreIdAndCode(storeId, channelCode);
        if (storeScoped.isPresent()) {
            return storeScoped;
        }
        Optional<Channel> global = channelRepository.findByCode(channelCode);
        global.ifPresent(c -> logger.warn("Using global Channel by code (store-scoped record not found). storeId={}, code={}", storeId, channelCode));
        return global;
    }

    private static ReservationStatus mapReservationStatus(String suStatus) {
        if (suStatus == null || suStatus.isBlank()) {
            return ReservationStatus.CONFIRMED;
        }
        String normalized = suStatus.trim().toLowerCase();
        if (normalized.contains("request")) {
            return ReservationStatus.REQUESTED;
        }
        if (normalized.contains("cancel")) {
            return ReservationStatus.CANCELLED;
        }
        return ReservationStatus.CONFIRMED;
    }

    private void tryUpsertMessageThreadFromReservation(
            Long storeId,
            String suHotelId,
            String channelCode,
            JsonNode reservationNode,
            Reservation reservation
    ) {
        if (storeId == null || reservation == null || channelCode == null || channelCode.isBlank()) {
            return;
        }

        Integer suChannelId = toSuChannelId(channelCode);
        if (suChannelId == null) {
            return;
        }

        String bookingId = reservation.getChannelOrderNumber();
        if (bookingId == null || bookingId.isBlank()) {
            bookingId = reservation.getOrderNumber();
        }
        if (bookingId == null || bookingId.isBlank()) {
            return;
        }

        String threadId = SuReservationParser.extractThreadId(reservationNode);
        String guestId = SuReservationParser.extractGuestId(reservationNode);

        String threadKey;
        if (suChannelId == SuMessagingService.CHANNEL_BOOKING) {
            threadKey = bookingId.trim();
        } else if (suChannelId == SuMessagingService.CHANNEL_AIRBNB) {
            // For Airbnb, threadKey must be threadId (same behavior as inbound messaging webhook).
            if (threadId == null || threadId.isBlank()) {
                return;
            }
            threadKey = threadId.trim();
        } else {
            return;
        }

        String listingId = reservation.getOtaRoomId();
        LocalDateTime now = LocalDateTime.now();

        try {
            SuMessageThread thread = threadRepository.findByStoreIdAndChannelIdAndThreadKey(storeId, suChannelId, threadKey)
                    .orElseGet(() -> {
                        SuMessageThread t = new SuMessageThread();
                        t.setStoreId(storeId);
                        t.setSuHotelId(suHotelId != null ? suHotelId : "UNKNOWN");
                        t.setChannelId(suChannelId);
                        t.setThreadKey(threadKey);
                        return t;
                    });

            if (suHotelId != null && !suHotelId.isBlank()) {
                thread.setSuHotelId(suHotelId);
            }
            thread.setChannelId(suChannelId);
            thread.setThreadKey(threadKey);
            thread.setBookingId(bookingId);

            if (threadId != null && !threadId.isBlank()) {
                thread.setThreadId(threadId);
            }
            if (guestId != null && !guestId.isBlank()) {
                thread.setGuestId(guestId);
            }
            if (listingId != null && !listingId.isBlank()) {
                thread.setListingId(listingId);
            }
            if (reservation.getGuestName() != null && !reservation.getGuestName().isBlank()) {
                thread.setGuestName(reservation.getGuestName());
            }
            thread.setLastActivity(now);

            threadRepository.save(thread);
        } catch (DataIntegrityViolationException e) {
            // Best-effort: ignore unique key races.
            reservationLogger.warn(
                    "[ReservationUpsert] thread upsert ignored (unique race). storeId={}, hotelId={}, channelCode={}, threadKey={}, bookingId={}, err={}",
                    storeId,
                    suHotelId,
                    channelCode,
                    threadKey,
                    bookingId,
                    e.getMessage()
            );
        }
    }

    private static Integer toSuChannelId(String channelCode) {
        if (channelCode == null) {
            return null;
        }
        String normalized = channelCode.trim().toUpperCase();
        if ("BOOKING".equals(normalized) || "BOOKING.COM".equals(normalized)) {
            return SuMessagingService.CHANNEL_BOOKING;
        }
        if ("AIRBNB".equals(normalized)) {
            return SuMessagingService.CHANNEL_AIRBNB;
        }
        return null;
    }

    private static String summarizeReservationNodes(List<JsonNode> nodes, int maxItems) {
        if (nodes == null || nodes.isEmpty()) {
            return "[]";
        }
        int limit = Math.max(1, maxItems);
        List<String> parts = new ArrayList<>();
        for (JsonNode node : nodes) {
            if (node == null || node.isNull()) {
                continue;
            }
            String rid = SuReservationParser.extractReservationId(node);
            String notifId = SuReservationParser.extractReservationNotifId(node);
            String bookingId = SuReservationParser.extractChannelBookingId(node);
            String status = SuReservationParser.extractSuStatus(node);
            parts.add("{reservationId=" + rid + ", notifId=" + notifId + ", bookingId=" + bookingId + ", status=" + status + "}");
            if (parts.size() >= limit) {
                break;
            }
        }
        return parts.toString() + (nodes.size() > parts.size() ? ("(+ " + (nodes.size() - parts.size()) + " more)") : "");
    }

    private static String combineSpecialRequests(String roomSpecialRequest, String customerRemarks) {
        String a = roomSpecialRequest != null ? roomSpecialRequest.trim() : null;
        String b = customerRemarks != null ? customerRemarks.trim() : null;

        if (a == null || a.isBlank()) {
            return (b == null || b.isBlank()) ? null : b;
        }
        if (b == null || b.isBlank()) {
            return a;
        }
        if (a.equalsIgnoreCase(b)) {
            return a;
        }
        return a + "\n" + b;
    }
}
