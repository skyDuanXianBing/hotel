package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import server.demo.dto.SuMessagingMessagePageResponse;
import server.demo.dto.SuMessagingMessageDTO;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.dto.SuMessagingThreadPageResponse;
import server.demo.dto.SuMessagingUnreadSummaryDTO;
import server.demo.entity.Reservation;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.ReservationStatus;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.ReservationRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.util.StoreTimeZoneUtil;
import server.demo.util.StoreContextUtils;
import server.demo.util.SuReservationParser;
import server.demo.util.UtcTimeUtil;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SuMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(SuMessagingService.class);

    public static final int CHANNEL_BOOKING = 19;
    public static final int CHANNEL_AIRBNB = 244;
    private static final String OTA_CODE_AIRBNB = "AIRBNB";
    private static final int DEFAULT_THREAD_PAGE_SIZE = 20;
    private static final int MAX_THREAD_PAGE_SIZE = 100;
    private static final int DEFAULT_MESSAGE_PAGE_LIMIT = 50;
    private static final int MAX_MESSAGE_PAGE_LIMIT = 100;
    private static final String ORDER_KIND_INQUIRY = "INQUIRY";
    private static final String ORDER_KIND_REQUESTED = "REQUESTED";
    private static final String ORDER_KIND_CONFIRMED = "CONFIRMED";
    private static final String ORDER_KIND_CANCELLED = "CANCELLED";
    private static final String ORDER_KIND_COMPLETED = "COMPLETED";
    private static final String ORDER_KIND_UNMATCHED_ORDER = "UNMATCHED_ORDER";
    private static final String STATUS_INQUIRY = "INQUIRY";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final SuMessageThreadRepository threadRepository;
    private final SuMessageRepository messageRepository;
    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReservationBookingKeyResolver reservationBookingKeyResolver;
    private final Clock clock;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;
    private final ObjectMapper objectMapper;
    private final SuMessagingRealtimeGateway suMessagingRealtimeGateway;
    private final OtaIntegrationService otaIntegrationService;
    private MessageKnowledgeThreadDirtyMarker knowledgeThreadDirtyMarker;

    public SuMessagingService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            ReservationRepository reservationRepository,
            ReservationBookingKeyResolver reservationBookingKeyResolver,
            StoreRepository storeRepository,
            Clock clock,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService,
            ObjectMapper objectMapper,
            SuMessagingRealtimeGateway suMessagingRealtimeGateway,
            OtaIntegrationService otaIntegrationService,
            RoomTypeRepository roomTypeRepository
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.reservationRepository = reservationRepository;
        this.storeRepository = storeRepository;
        this.reservationBookingKeyResolver = reservationBookingKeyResolver;
        this.clock = clock;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
        this.objectMapper = objectMapper;
        this.suMessagingRealtimeGateway = suMessagingRealtimeGateway;
        this.otaIntegrationService = otaIntegrationService;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Autowired(required = false)
    public void setKnowledgeThreadDirtyMarker(MessageKnowledgeThreadDirtyMarker knowledgeThreadDirtyMarker) {
        this.knowledgeThreadDirtyMarker = knowledgeThreadDirtyMarker;
    }

    @Transactional
    public void handleInboundMessage(Long storeId, String suHotelId, JsonNode root, String rawJson) {
        Integer channelId = readInt(root, "channel_id");
        if (channelId == null) {
            logger.warn("[SuMessaging] inbound missing channel_id. storeId={}, hotelId={}", storeId, suHotelId);
            return;
        }
        if (channelId != CHANNEL_BOOKING && channelId != CHANNEL_AIRBNB) {
            logger.info("[SuMessaging] inbound channel not supported, ignored. storeId={}, channelId={}", storeId, channelId);
            return;
        }

        String messageId = readText(root, "messageid");
        if (messageId != null) {
            boolean exists = messageRepository.findByStoreIdAndExternalMessageId(storeId, messageId).isPresent();
            if (exists) {
                return;
            }
        }

        String threadId = readText(root, "threadid");
        String rawBookingId = readText(root, "bookingid");
        String bookingId = normalizeInboundBookingId(channelId, rawBookingId, threadId);
        String guestId = readText(root, "guestid");
        String listingId = readText(root, "listingid");
        String bookingFlag = readText(root, "bookingflag");
        String content = readText(root, "message");

        String threadKey = buildThreadKey(channelId, threadId, bookingId);
        if (threadKey == null) {
            logger.warn("[SuMessaging] inbound missing thread key fields. storeId={}, channelId={}, threadId={}, bookingId={}",
                    storeId, channelId, threadId, bookingId);
            return;
        }
        if (content == null) {
            logger.warn("[SuMessaging] inbound missing message content. storeId={}, channelId={}, threadKey={}", storeId, channelId, threadKey);
            return;
        }

        SuMessageThread thread = threadRepository.findByStoreIdAndChannelIdAndThreadKey(storeId, channelId, threadKey)
                .orElseGet(() -> {
                    SuMessageThread t = new SuMessageThread();
                    t.setStoreId(storeId);
                    t.setSuHotelId(suHotelId);
                    t.setChannelId(channelId);
                    t.setThreadKey(threadKey);
                    return t;
                });

        thread.setSuHotelId(suHotelId);
        thread.setChannelId(channelId);
        thread.setThreadKey(threadKey);
        thread.setThreadId(threadId);
        thread.setBookingId(bookingId);
        thread.setGuestId(guestId);
        thread.setListingId(listingId);
        thread.setBookingFlag(bookingFlag);
        thread.setListingName(readText(root.at("/booking_details"), "listing_name"));
        if (thread.getGuestName() == null || thread.getGuestName().isBlank()) {
            thread.setGuestName(readAirbnbGuestFirstName(root));
        }
        thread.setLastMessage(trimToMax(content, 500));
        thread.setLastActivity(UtcTimeUtil.nowLocalDateTime());

        thread = threadRepository.save(thread);

        SuMessage msg = new SuMessage();
        msg.setStoreId(storeId);
        msg.setThread(thread);
        msg.setExternalMessageId(messageId);
        msg.setSenderType(SuMessagingSenderType.GUEST);
        msg.setSenderName(thread.getGuestName());
        msg.setContent(content);
        msg.setSentAt(UtcTimeUtil.nowLocalDateTime());
        msg.setIsRead(false);
        msg.setDeliveryStatus("SENT");
        msg.setRawJson(rawJson);
        msg = messageRepository.save(msg);
        markKnowledgeThreadDirty(msg);

        Long savedThreadId = thread.getId();
        Long savedMessageId = msg.getId();
        if (savedThreadId != null && savedMessageId != null) {
            SuMessagingMessageDTO savedMessageDto = toMessageDTO(msg);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        suMessagingRealtimeGateway.broadcastMessageCreated(storeId, savedThreadId, savedMessageDto);
                        suMessagingRealtimeGateway.broadcastWorkbenchInvalidated(storeId, "message");
                        triggerAutoReplyAfterCommit(storeId, savedThreadId, savedMessageId);
                    }
                });
            } else {
                suMessagingRealtimeGateway.broadcastMessageCreated(storeId, savedThreadId, savedMessageDto);
                suMessagingRealtimeGateway.broadcastWorkbenchInvalidated(storeId, "message");
                triggerAutoReplyAfterCommit(storeId, savedThreadId, savedMessageId);
            }
        }
    }

    private void triggerAutoReplyAfterCommit(Long storeId, Long threadId, Long triggerMessageId) {
        logger.info("[SuMessaging] AI auto reply disabled. skip trigger. storeId={}, threadId={}, triggerMessageId={}",
                storeId, threadId, triggerMessageId);
    }

    public List<SuMessagingThreadDTO> listThreads(Long storeId) {
        LocalDate today = currentStoreDate(storeId);
        List<SuMessagingThreadDTO> items = threadRepository.findByStoreIdOrderByLastActivityDesc(storeId).stream()
                .map(thread -> toThreadDTO(storeId, thread, today))
                .toList();
        fillAirbnbInquiryRoomTypeNames(storeId, items);
        return items;
    }

    public SuMessagingThreadPageResponse listThreadPage(
            Long storeId,
            Integer page,
            Integer size,
            String channel,
            String orderKind,
            String reservationStatus,
            String orderStatuses,
            Boolean unread,
            Boolean closed,
            String search
    ) {
        int currentPage = normalizePage(page);
        int pageSize = normalizeThreadPageSize(size);
        Set<Integer> channelIds = parseChannelIds(channel);
        Set<String> orderKinds = parseUppercaseCsv(orderKind);
        Set<String> reservationStatuses = parseUppercaseCsv(reservationStatus);
        Set<String> messageOrderStatuses = parseUppercaseCsv(orderStatuses);
        String normalizedSearch = normalizeBlankToNull(search);
        LocalDate today = currentStoreDate(storeId);

        boolean requiresDynamicFilter = channelIds.size() > 1
                || !orderKinds.isEmpty()
                || !reservationStatuses.isEmpty()
                || !messageOrderStatuses.isEmpty();

        if (!requiresDynamicFilter) {
            Integer channelId = channelIds.isEmpty() ? null : channelIds.iterator().next();
            Page<SuMessageThread> threadPage = threadRepository.findPageByStoreIdAndFilters(
                    storeId,
                    channelId,
                    closed,
                    unread,
                    normalizedSearch,
                    SuMessagingSenderType.GUEST,
                    PageRequest.of(currentPage, pageSize)
            );
            List<SuMessagingThreadDTO> items = threadPage.getContent().stream()
                    .map(thread -> toThreadDTO(storeId, thread, today))
                    .toList();
            fillAirbnbInquiryRoomTypeNames(storeId, items);
            return new SuMessagingThreadPageResponse(
                    items,
                    currentPage,
                    pageSize,
                    threadPage.getTotalElements(),
                    threadPage.getTotalPages(),
                    threadPage.hasNext()
            );
        }

        Integer databaseChannelId = channelIds.size() == 1 ? channelIds.iterator().next() : null;
        List<SuMessagingThreadDTO> filtered = threadRepository.findByStoreIdAndFilters(
                        storeId,
                        databaseChannelId,
                        closed,
                        unread,
                        normalizedSearch,
                        SuMessagingSenderType.GUEST
                ).stream()
                .filter(thread -> channelIds.isEmpty() || channelIds.contains(thread.getChannelId()))
                .map(thread -> toThreadDTO(storeId, thread, today))
                .filter(dto -> matchesUppercaseFilter(orderKinds, dto.getOrderKind()))
                .filter(dto -> matchesUppercaseFilter(reservationStatuses, dto.getReservationStatus()))
                .filter(dto -> matchesMessageOrderStatusFilter(messageOrderStatuses, dto))
                .toList();

        long totalElements = filtered.size();
        int fromIndex = currentPage * pageSize;
        List<SuMessagingThreadDTO> items;
        if (fromIndex >= filtered.size()) {
            items = List.of();
        } else {
            int toIndex = Math.min(fromIndex + pageSize, filtered.size());
            items = filtered.subList(fromIndex, toIndex);
        }
        fillAirbnbInquiryRoomTypeNames(storeId, items);

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        return new SuMessagingThreadPageResponse(
                items,
                currentPage,
                pageSize,
                totalElements,
                totalPages,
                fromIndex + pageSize < totalElements
        );
    }

    /**
     * Returns the shared-inbox conversations whose latest effective message still comes from a guest.
     * A staff message is effective only after it has committed with delivery_status=SENT.
     */
    public SuMessagingThreadPageResponse listAwaitingReplyThreadPage(Long storeId, Integer page, Integer size) {
        requireMatchingStoreContext(storeId);

        int currentPage = normalizePage(page);
        int pageSize = normalizeThreadPageSize(size);
        LocalDate today = currentStoreDate(storeId);
        Page<SuMessageThread> threadPage = threadRepository.findAwaitingReplyPageByStoreId(
                storeId,
                PageRequest.of(currentPage, pageSize)
        );
        List<SuMessageThread> threads = threadPage.getContent();
        Map<Long, Reservation> reservationByThreadId =
                reservationBookingKeyResolver.findFirstReservationsForThreads(storeId, threads);
        Map<Long, Long> unreadByThreadId = loadUnreadCounts(storeId, threads);
        List<SuMessagingThreadDTO> items = threads.stream()
                .map(thread -> toThreadDTO(
                        thread,
                        reservationByThreadId.get(thread.getId()),
                        unreadByThreadId.getOrDefault(thread.getId(), 0L),
                        today
                ))
                .toList();
        fillAirbnbInquiryRoomTypeNames(storeId, items);
        return new SuMessagingThreadPageResponse(
                items,
                currentPage,
                pageSize,
                threadPage.getTotalElements(),
                threadPage.getTotalPages(),
                threadPage.hasNext()
        );
    }

    public long countAwaitingReplyThreads(Long storeId) {
        requireMatchingStoreContext(storeId);
        return threadRepository.countAwaitingReplyByStoreId(storeId);
    }

    /**
     * Workbench-only keyset slice. It deliberately does not execute the awaiting-reply count query.
     */
    public SuMessagingThreadPageResponse listAwaitingReplyThreadSlice(
            Long storeId,
            LocalDateTime cursorDueAt,
            Long cursorId,
            int size
    ) {
        requireMatchingStoreContext(storeId);
        int pageSize = normalizeThreadPageSize(size);
        boolean hasCursor = cursorId != null;
        List<SuMessageThread> candidates = threadRepository.findAwaitingReplySliceByStoreId(
                storeId,
                hasCursor,
                cursorDueAt,
                cursorId == null ? 0L : cursorId,
                PageRequest.of(0, Math.min(pageSize + 1, 101))
        );
        boolean hasNext = candidates.size() > pageSize;
        List<SuMessageThread> threads = candidates;
        LocalDate today = currentStoreDate(storeId);
        Map<Long, Reservation> reservationByThreadId =
                reservationBookingKeyResolver.findFirstReservationsForThreads(storeId, threads);
        Map<Long, Long> unreadByThreadId = loadUnreadCounts(storeId, threads);
        List<SuMessagingThreadDTO> items = threads.stream()
                .map(thread -> toThreadDTO(
                        thread,
                        reservationByThreadId.get(thread.getId()),
                        unreadByThreadId.getOrDefault(thread.getId(), 0L),
                        today
                ))
                .toList();
        fillAirbnbInquiryRoomTypeNames(storeId, items);
        return new SuMessagingThreadPageResponse(items, 0, pageSize, 0, 0, hasNext);
    }

    private void requireMatchingStoreContext(Long storeId) {
        Long contextStoreId = StoreContextUtils.requireStoreId();
        if (storeId == null || !storeId.equals(contextStoreId)) {
            throw new IllegalArgumentException("门店上下文不一致");
        }
    }

    private Map<Long, Long> loadUnreadCounts(Long storeId, List<SuMessageThread> threads) {
        if (threads == null || threads.isEmpty()) {
            return Map.of();
        }
        List<Long> threadIds = threads.stream()
                .map(SuMessageThread::getId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (threadIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (SuMessageRepository.ThreadUnreadCountRow row
                : messageRepository.countUnreadByStoreIdAndThreadIds(
                        storeId,
                        threadIds,
                        SuMessagingSenderType.GUEST
                )) {
            if (row != null && row.getThreadId() != null) {
                result.put(row.getThreadId(), row.getUnreadCount() != null ? row.getUnreadCount() : 0L);
            }
        }
        return result;
    }

    private SuMessagingThreadDTO toThreadDTO(Long storeId, SuMessageThread thread) {
        return toThreadDTO(storeId, thread, currentStoreDate(storeId));
    }

    private SuMessagingThreadDTO toThreadDTO(Long storeId, SuMessageThread thread, LocalDate today) {
        Reservation reservation = resolveReservationForThread(storeId, thread);
        long unreadCount = messageRepository.countByThread_IdAndSenderTypeAndIsReadFalse(
                thread.getId(),
                SuMessagingSenderType.GUEST
        );
        return toThreadDTO(thread, reservation, unreadCount, today);
    }

    private SuMessagingThreadDTO toThreadDTO(
            SuMessageThread thread,
            Reservation reservation,
            long unreadCount,
            LocalDate today
    ) {
        String effectiveReservationStatus = resolveEffectiveReservationStatus(reservation, today);
        SuMessagingThreadDTO dto = new SuMessagingThreadDTO();
        dto.setId(thread.getId());
        dto.setReservationId(reservation != null ? reservation.getId() : null);
        dto.setChannelId(thread.getChannelId());
        dto.setChannelName(resolveChannelName(thread.getChannelId()));
        dto.setGuestName(thread.getGuestName());
        dto.setBookingId(thread.getBookingId());
        dto.setBookingFlag(thread.getBookingFlag());
        dto.setThreadId(thread.getThreadId());
        dto.setListingId(thread.getListingId());
        dto.setListingName(thread.getListingName());
        dto.setCheckInDate(reservation != null ? reservation.getCheckInDate() : null);
        dto.setCheckOutDate(reservation != null ? reservation.getCheckOutDate() : null);
        dto.setReservationStatus(effectiveReservationStatus);
        dto.setOrderKind(resolveOrderKind(thread, reservation, today));
        dto.setRoomTypeName(resolveRoomTypeName(reservation));
        dto.setLastMessage(thread.getLastMessage());
        dto.setLastActivity(toUtcOffset(thread.getLastActivity()));
        dto.setClosed(Boolean.TRUE.equals(thread.getClosed()));
        dto.setUnreadCount(unreadCount);
        return dto;
    }

    static String resolveOrderKind(SuMessageThread thread, Reservation reservation) {
        return resolveOrderKind(thread, reservation, defaultBusinessDate());
    }

    private static String resolveOrderKind(SuMessageThread thread, Reservation reservation, LocalDate today) {
        if (reservation != null) {
            return resolveOrderKindFromReservation(reservation, today);
        }
        if (thread == null || thread.getChannelId() == null) {
            return null;
        }
        if (thread.getChannelId() == CHANNEL_AIRBNB) {
            String bookingFlag = thread.getBookingFlag();
            if (bookingFlag != null && "T".equalsIgnoreCase(bookingFlag.trim())) {
                return ORDER_KIND_INQUIRY;
            }
            return ORDER_KIND_UNMATCHED_ORDER;
        }
        if (thread.getChannelId() == CHANNEL_BOOKING) {
            return ORDER_KIND_UNMATCHED_ORDER;
        }
        return null;
    }

    private static String resolveOrderKindFromReservation(Reservation reservation, LocalDate today) {
        String effectiveStatus = resolveEffectiveReservationStatus(reservation, today);
        if (effectiveStatus == null) {
            return ORDER_KIND_UNMATCHED_ORDER;
        }
        if (ReservationStatus.REQUESTED.name().equals(effectiveStatus)) {
            return ORDER_KIND_REQUESTED;
        }
        if (STATUS_CONFIRMED.equals(effectiveStatus) || STATUS_CHECKED_IN.equals(effectiveStatus)) {
            return ORDER_KIND_CONFIRMED;
        }
        if (STATUS_CANCELLED.equals(effectiveStatus)) {
            return ORDER_KIND_CANCELLED;
        }
        if (STATUS_CHECKED_OUT.equals(effectiveStatus)) {
            return ORDER_KIND_COMPLETED;
        }
        return ORDER_KIND_UNMATCHED_ORDER;
    }

    static String resolveEffectiveReservationStatus(Reservation reservation, LocalDate today) {
        if (reservation == null) {
            return null;
        }

        ReservationStatus originalStatus = reservation.getStatus();
        if (originalStatus == ReservationStatus.CANCELLED || originalStatus == ReservationStatus.NO_SHOW) {
            return STATUS_CANCELLED;
        }
        if (originalStatus == ReservationStatus.REQUESTED) {
            return originalStatus.name();
        }
        if (reservation.getActualCheckOut() != null || originalStatus == ReservationStatus.CHECKED_OUT) {
            return STATUS_CHECKED_OUT;
        }

        LocalDate businessDate = today != null ? today : defaultBusinessDate();
        LocalDate checkOutDate = reservation.getCheckOutDate();
        if (checkOutDate != null && !checkOutDate.isAfter(businessDate)) {
            return STATUS_CHECKED_OUT;
        }

        LocalDate checkInDate = reservation.getCheckInDate();
        if (checkInDate != null && !checkInDate.isAfter(businessDate)) {
            if (checkOutDate == null || checkOutDate.isAfter(businessDate)) {
                return STATUS_CHECKED_IN;
            }
        }
        if (checkInDate != null && checkInDate.isAfter(businessDate)) {
            return STATUS_CONFIRMED;
        }
        if (originalStatus != null) {
            return originalStatus.name();
        }
        return null;
    }

    private LocalDate currentStoreDate(Long storeId) {
        Store store = null;
        if (storeId != null) {
            store = storeRepository.findById(storeId).orElse(null);
        }
        ZoneId zoneId = StoreTimeZoneUtil.resolveZoneId(store);
        return LocalDate.now(effectiveClock().withZone(zoneId));
    }

    private Clock effectiveClock() {
        return clock != null ? clock : Clock.systemUTC();
    }

    private static LocalDate defaultBusinessDate() {
        return LocalDate.now(StoreTimeZoneUtil.getBusinessDefaultZoneId());
    }

    private Reservation resolveReservationForThread(Long storeId, SuMessageThread thread) {
        return reservationBookingKeyResolver.findFirstReservationForThread(storeId, thread);
    }

    private static String resolveRoomTypeName(Reservation reservation) {
        if (reservation == null || reservation.getRoom() == null || reservation.getRoom().getRoomType() == null) {
            return null;
        }
        String roomTypeName = reservation.getRoom().getRoomType().getName();
        return roomTypeName == null || roomTypeName.isBlank() ? null : roomTypeName;
    }

    private void fillAirbnbInquiryRoomTypeNames(Long storeId, List<SuMessagingThreadDTO> items) {
        if (storeId == null || items == null || items.isEmpty() || otaIntegrationService == null || roomTypeRepository == null) {
            return;
        }

        Set<String> listingIds = new HashSet<>();
        for (SuMessagingThreadDTO item : items) {
            if (!shouldResolveAirbnbInquiryRoomType(item)) {
                continue;
            }
            String listingId = normalizeBlankToNull(item.getListingId());
            if (listingId != null) {
                listingIds.add(listingId);
            }
        }

        if (listingIds.isEmpty()) {
            return;
        }

        JsonNode mappings;
        try {
            mappings = otaIntegrationService.getSuMappingsByStoreAndCode(
                    storeId,
                    OTA_CODE_AIRBNB,
                    String.valueOf(CHANNEL_AIRBNB)
            );
        } catch (Exception e) {
            logger.warn("[SuMessaging] failed to load Airbnb mappings for inquiry room type names. storeId={}, err={}",
                    storeId, e.getMessage());
            return;
        }

        Map<String, String> pmsRoomIdByListingId = resolvePmsRoomIdsByListingId(mappings, listingIds);
        if (pmsRoomIdByListingId.isEmpty()) {
            return;
        }

        Map<String, String> roomTypeNameByPmsRoomId = resolveRoomTypeNamesByPmsRoomId(storeId, pmsRoomIdByListingId);
        if (roomTypeNameByPmsRoomId.isEmpty()) {
            return;
        }

        for (SuMessagingThreadDTO item : items) {
            if (!shouldResolveAirbnbInquiryRoomType(item)) {
                continue;
            }
            String listingId = normalizeBlankToNull(item.getListingId());
            String pmsRoomId = listingId != null ? pmsRoomIdByListingId.get(listingId) : null;
            String roomTypeName = pmsRoomId != null ? roomTypeNameByPmsRoomId.get(pmsRoomId) : null;
            if (roomTypeName != null) {
                item.setRoomTypeName(roomTypeName);
            }
        }
    }

    private static boolean shouldResolveAirbnbInquiryRoomType(SuMessagingThreadDTO item) {
        if (item == null || item.getChannelId() == null || item.getChannelId() != CHANNEL_AIRBNB) {
            return false;
        }
        if (!ORDER_KIND_INQUIRY.equalsIgnoreCase(item.getOrderKind())) {
            return false;
        }
        String currentRoomTypeName = item.getRoomTypeName();
        return currentRoomTypeName == null || currentRoomTypeName.isBlank();
    }

    private Map<String, String> resolvePmsRoomIdsByListingId(JsonNode mappings, Set<String> listingIds) {
        Map<String, String> result = new HashMap<>();
        JsonNode channelArray = findAirbnbMappingsArray(mappings);
        if (channelArray == null || !channelArray.isArray()) {
            return result;
        }

        for (JsonNode mappingItem : channelArray) {
            if (mappingItem == null || mappingItem.isNull() || !mappingItem.isObject()) {
                continue;
            }
            JsonNode ratePlans = mappingItem.get("Rateplans");
            if (ratePlans == null || !ratePlans.isArray()) {
                continue;
            }

            for (JsonNode ratePlan : ratePlans) {
                String channelRoomId = readText(ratePlan, "ChannelRoomID");
                if (channelRoomId == null || !listingIds.contains(channelRoomId) || result.containsKey(channelRoomId)) {
                    continue;
                }

                String pmsRoomId = readText(ratePlan, "PMSRoomID");
                if (pmsRoomId == null) {
                    pmsRoomId = resolveSingleRoomIdFallback(mappingItem);
                }
                if (pmsRoomId != null) {
                    result.put(channelRoomId, pmsRoomId);
                }
            }
        }

        return result;
    }

    private Map<String, String> resolveRoomTypeNamesByPmsRoomId(
            Long storeId,
            Map<String, String> pmsRoomIdByListingId
    ) {
        Set<Long> roomTypeIds = new HashSet<>();
        for (String pmsRoomId : pmsRoomIdByListingId.values()) {
            Long roomTypeId = parsePositiveLong(pmsRoomId);
            if (roomTypeId != null) {
                roomTypeIds.add(roomTypeId);
            }
        }

        if (roomTypeIds.isEmpty()) {
            return Map.of();
        }

        List<RoomType> roomTypes = roomTypeRepository.findByStoreIdAndIdIn(storeId, new ArrayList<>(roomTypeIds));
        Map<String, String> result = new HashMap<>();
        for (RoomType roomType : roomTypes) {
            if (roomType == null || roomType.getId() == null) {
                continue;
            }
            String name = roomType.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            result.put(String.valueOf(roomType.getId()), name.trim());
        }
        return result;
    }

    private static JsonNode findAirbnbMappingsArray(JsonNode root) {
        JsonNode direct = findAirbnbMappingsArrayInNode(root);
        if (direct != null) {
            return direct;
        }
        JsonNode data = root != null ? root.get("data") : null;
        if (data == null && root != null) {
            data = root.get("Data");
        }
        return findAirbnbMappingsArrayInNode(data);
    }

    private static JsonNode findAirbnbMappingsArrayInNode(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return null;
        }
        JsonNode airbnbArray = node.get(String.valueOf(CHANNEL_AIRBNB));
        if (airbnbArray != null && airbnbArray.isArray()) {
            return airbnbArray;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            JsonNode value = fields.next().getValue();
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private static String resolveSingleRoomIdFallback(JsonNode mappingItem) {
        JsonNode roomIds = mappingItem != null ? mappingItem.get("RoomIDs") : null;
        if (roomIds == null || !roomIds.isArray()) {
            return null;
        }

        String singleRoomId = null;
        int validRoomIdCount = 0;
        for (JsonNode roomIdNode : roomIds) {
            String roomId = roomIdNode != null ? roomIdNode.asText(null) : null;
            if (roomId == null || roomId.isBlank()) {
                continue;
            }
            validRoomIdCount++;
            if (validRoomIdCount > 1) {
                // Multiple RoomIDs are ambiguous for a listing-level inquiry, so keep the listing fallback.
                return null;
            }
            singleRoomId = roomId.trim();
        }

        return validRoomIdCount == 1 ? singleRoomId : null;
    }

    private static Long parsePositiveLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    public void markThreadAsRead(Long storeId, Long threadId) {
        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found or no permission"));

        messageRepository.markThreadMessagesAsRead(thread.getId(), SuMessagingSenderType.GUEST);
    }

    @Transactional
    public List<SuMessagingMessageDTO> getThreadMessages(Long storeId, Long threadId) {
        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found or no permission"));

        List<SuMessagingMessageDTO> dtos = messageRepository.findByThread_IdOrderBySentAtAsc(thread.getId()).stream()
                .map(SuMessagingService::toMessageDTO)
                .toList();

        messageRepository.markThreadMessagesAsRead(thread.getId(), SuMessagingSenderType.GUEST);
        return dtos;
    }

    @Transactional
    public SuMessagingMessagePageResponse getThreadMessagePage(
            Long storeId,
            Long threadId,
            Integer limit,
            Long beforeMessageId,
            Long afterMessageId,
            Boolean markRead
    ) {
        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found or no permission"));

        int pageLimit = normalizeMessagePageLimit(limit);
        List<SuMessage> rows = messageRepository.findMessagePageByCursorDesc(
                storeId,
                thread.getId(),
                beforeMessageId,
                afterMessageId,
                PageRequest.of(0, pageLimit + 1)
        );

        List<SuMessage> selectedRows = rows;
        if (rows.size() > pageLimit) {
            selectedRows = rows.subList(0, pageLimit);
        }

        List<SuMessage> orderedRows = new ArrayList<>(selectedRows);
        Collections.reverse(orderedRows);
        List<SuMessagingMessageDTO> items = orderedRows.stream()
                .map(SuMessagingService::toMessageDTO)
                .toList();

        Long nextBeforeMessageId = null;
        boolean hasMoreBefore = false;
        boolean hasMoreAfter = false;
        if (!orderedRows.isEmpty()) {
            Long oldestMessageId = orderedRows.get(0).getId();
            Long newestMessageId = orderedRows.get(orderedRows.size() - 1).getId();
            hasMoreBefore = messageRepository.existsByStoreIdAndThread_IdAndIdLessThan(
                    storeId,
                    thread.getId(),
                    oldestMessageId
            );
            hasMoreAfter = messageRepository.existsByStoreIdAndThread_IdAndIdGreaterThan(
                    storeId,
                    thread.getId(),
                    newestMessageId
            );
            if (hasMoreBefore) {
                nextBeforeMessageId = oldestMessageId;
            }
        }

        if (markRead == null || Boolean.TRUE.equals(markRead)) {
            messageRepository.markThreadMessagesAsRead(thread.getId(), SuMessagingSenderType.GUEST);
        }

        return new SuMessagingMessagePageResponse(
                items,
                pageLimit,
                hasMoreBefore,
                nextBeforeMessageId,
                hasMoreAfter
        );
    }

    public SuMessagingUnreadSummaryDTO getUnreadSummary(Long storeId) {
        long totalUnread = messageRepository.countUnreadMessagesByStoreId(storeId, SuMessagingSenderType.GUEST);
        long unreadThreadCount = messageRepository.countUnreadThreadsByStoreId(storeId, SuMessagingSenderType.GUEST);
        Map<String, Long> byChannel = new HashMap<>();
        for (SuMessageRepository.ChannelUnreadSummaryRow row :
                messageRepository.summarizeUnreadByChannel(storeId, SuMessagingSenderType.GUEST)) {
            byChannel.put(
                    resolveChannelCode(row.getChannelId()),
                    row.getUnreadMessageCount() == null ? 0L : row.getUnreadMessageCount()
            );
        }
        return new SuMessagingUnreadSummaryDTO(totalUnread, unreadThreadCount, byChannel);
    }

    @Transactional
    public List<SuMessagingMessageDTO> pollThreadMessages(Long storeId, Long threadId, String since) {
        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found or no permission"));

        LocalDateTime sinceTime = parseSince(since);
        List<SuMessagingMessageDTO> dtos = messageRepository.findByThread_IdAndSentAtAfterOrderBySentAtAsc(thread.getId(), sinceTime).stream()
                .map(SuMessagingService::toMessageDTO)
                .toList();

        if (!dtos.isEmpty()) {
            messageRepository.markThreadMessagesAsRead(thread.getId(), SuMessagingSenderType.GUEST);
        }
        return dtos;
    }

    @Transactional
    public SuMessagingMessageDTO sendMessage(Long storeId, Long threadId, SuMessagingSendRequest request) {
        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found or no permission"));

        if (Boolean.TRUE.equals(thread.getClosed())) {
            throw new IllegalStateException("Thread is closed, cannot send message");
        }

        String message = request.getContent() != null ? request.getContent().trim() : null;
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }

        Map<String, Object> payload = buildReplyPayload(thread, message);

        JsonNode response = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.postMessagingAB(token, payload),
                "messagingAB"
        );

        if (!suApiClient.isSuSuccess(response)) {
            String err = suApiClient.extractSuErrorMessage(response);
            throw new RuntimeException(err != null ? err : "Su message send failed");
        }

        SuMessage msg = new SuMessage();
        msg.setStoreId(storeId);
        msg.setThread(thread);
        msg.setExternalMessageId(null);
        msg.setSenderType(SuMessagingSenderType.STAFF);
        String senderName = request.getSenderName() != null ? request.getSenderName().trim() : null;
        msg.setSenderName(senderName != null && !senderName.isBlank() ? senderName : null);
        msg.setContent(message);
        msg.setSentAt(UtcTimeUtil.nowLocalDateTime());
        msg.setIsRead(true);
        msg.setDeliveryStatus("SENT");
        msg.setRawJson(writeJsonSafely(payload));
        msg = messageRepository.save(msg);

        thread.setLastMessage(trimToMax(message, 500));
        thread.setLastActivity(UtcTimeUtil.nowLocalDateTime());
        threadRepository.save(thread);
        markKnowledgeThreadDirty(msg);

        SuMessagingMessageDTO dto = toMessageDTO(msg);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    suMessagingRealtimeGateway.broadcastMessageCreated(storeId, threadId, dto);
                    suMessagingRealtimeGateway.broadcastWorkbenchInvalidated(storeId, "message");
                }
            });
        } else {
            suMessagingRealtimeGateway.broadcastMessageCreated(storeId, threadId, dto);
            suMessagingRealtimeGateway.broadcastWorkbenchInvalidated(storeId, "message");
        }

        return dto;
    }

    private void markKnowledgeThreadDirty(SuMessage message) {
        if (knowledgeThreadDirtyMarker == null) {
            return;
        }
        knowledgeThreadDirtyMarker.markDirty(message);
    }

    private Map<String, Object> buildReplyPayload(SuMessageThread thread, String message) {
        String hotelId = thread.getSuHotelId();
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalStateException("Missing hotelid, cannot send");
        }
        Integer channelId = thread.getChannelId();
        if (channelId == null) {
            throw new IllegalStateException("Missing channelId, cannot send");
        }
        String listingId = thread.getListingId();
        if (listingId == null || listingId.isBlank()) {
            throw new IllegalStateException("Missing listingid, cannot send");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("hotelid", hotelId);
        payload.put("channelid", String.valueOf(channelId));
        payload.put("message", message);
        payload.put("listingid", listingId);

        if (channelId == CHANNEL_AIRBNB) {
            String threadId = thread.getThreadId();
            String guestId = thread.getGuestId();
            if (threadId == null || threadId.isBlank() || guestId == null || guestId.isBlank()) {
                throw new IllegalStateException("Airbnb reply requires threadid + guestid");
            }
            payload.put("threadid", threadId);
            payload.put("guestid", guestId);
            payload.put("bookingid", thread.getBookingId() != null ? thread.getBookingId() : threadId);
        } else if (channelId == CHANNEL_BOOKING) {
            String bookingId = thread.getBookingId();
            if (bookingId == null || bookingId.isBlank()) {
                throw new IllegalStateException("Booking.com reply requires bookingid");
            }
            payload.put("bookingid", bookingId);
        } else {
            throw new IllegalStateException("Missing channelId, cannot send");
        }

        return payload;
    }

    private String writeJsonSafely(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private static SuMessagingMessageDTO toMessageDTO(SuMessage msg) {
        SuMessagingMessageDTO dto = new SuMessagingMessageDTO();
        dto.setId(msg.getId());
        dto.setThreadId(msg.getThread().getId());
        dto.setSenderType(msg.getSenderType());
        dto.setSenderName(msg.getSenderName());
        dto.setContent(msg.getContent());
        dto.setDeliveryStatus(msg.getDeliveryStatus());
        dto.setTimestamp(toUtcOffset(msg.getSentAt()));
        return dto;
    }

    private static OffsetDateTime toUtcOffset(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return UtcTimeUtil.toUtcOffset(localDateTime);
    }

    private static String buildThreadKey(Integer channelId, String threadId, String bookingId) {
        if (channelId == null) {
            return null;
        }
        if (channelId == CHANNEL_AIRBNB) {
            return threadId != null && !threadId.isBlank() ? threadId.trim() : null;
        }
        if (channelId == CHANNEL_BOOKING) {
            if (bookingId != null && !bookingId.isBlank()) {
                return bookingId.trim();
            }
            return threadId != null && !threadId.isBlank() ? threadId.trim() : null;
        }
        return null;
    }

    static String normalizeInboundBookingId(Integer channelId, String bookingId, String threadId) {
        if (channelId == null || channelId != CHANNEL_BOOKING) {
            return bookingId != null && !bookingId.isBlank() ? bookingId.trim() : null;
        }
        String normalizedBookingId = SuReservationParser.normalizeBookingReservationId(bookingId);
        if (normalizedBookingId != null) {
            return normalizedBookingId;
        }
        return SuReservationParser.normalizeBookingReservationId(threadId);
    }

    private static String resolveChannelName(Integer channelId) {
        if (channelId == null) {
            return "UNKNOWN";
        }
        if (channelId == CHANNEL_AIRBNB) {
            return "Airbnb";
        }
        if (channelId == CHANNEL_BOOKING) {
            return "Booking.com";
        }
        return "CHANNEL_" + channelId;
    }

    private static String resolveChannelCode(Integer channelId) {
        if (channelId != null && channelId == CHANNEL_AIRBNB) {
            return "AIRBNB";
        }
        if (channelId != null && channelId == CHANNEL_BOOKING) {
            return "BOOKING";
        }
        return channelId == null ? "UNKNOWN" : String.valueOf(channelId);
    }

    private static int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private static int normalizeThreadPageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_THREAD_PAGE_SIZE;
        }
        return Math.min(size, MAX_THREAD_PAGE_SIZE);
    }

    private static int normalizeMessagePageLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_MESSAGE_PAGE_LIMIT;
        }
        return Math.min(limit, MAX_MESSAGE_PAGE_LIMIT);
    }

    private static Set<Integer> parseChannelIds(String channel) {
        Set<String> values = parseUppercaseCsv(channel);
        Set<Integer> channelIds = new HashSet<>();
        for (String value : values) {
            if ("AIRBNB".equals(value)) {
                channelIds.add(CHANNEL_AIRBNB);
            } else if ("BOOKING".equals(value) || "BOOKING_COM".equals(value) || "BOOKING.COM".equals(value)) {
                channelIds.add(CHANNEL_BOOKING);
            } else {
                try {
                    channelIds.add(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unsupported channel filter: " + value);
                }
            }
        }
        return channelIds;
    }

    private static Set<String> parseUppercaseCsv(String value) {
        Set<String> values = new HashSet<>();
        if (value == null || value.isBlank()) {
            return values;
        }

        String[] parts = value.split(",");
        for (String part : parts) {
            String normalized = part == null ? null : part.trim();
            if (normalized != null && !normalized.isBlank()) {
                values.add(normalized.toUpperCase(Locale.ROOT));
            }
        }
        return values;
    }

    private static boolean matchesUppercaseFilter(Set<String> allowedValues, String value) {
        if (allowedValues == null || allowedValues.isEmpty()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        return allowedValues.contains(value.trim().toUpperCase(Locale.ROOT));
    }

    private static boolean matchesMessageOrderStatusFilter(
            Set<String> allowedStatuses,
            SuMessagingThreadDTO dto
    ) {
        if (allowedStatuses == null || allowedStatuses.isEmpty()) {
            return true;
        }
        if (dto == null) {
            return false;
        }
        if (allowedStatuses.contains(STATUS_INQUIRY)
                && ORDER_KIND_INQUIRY.equalsIgnoreCase(dto.getOrderKind())) {
            return true;
        }
        String reservationStatus = dto.getReservationStatus();
        if (reservationStatus == null || reservationStatus.isBlank()) {
            return false;
        }
        return allowedStatuses.contains(reservationStatus.trim().toUpperCase(Locale.ROOT));
    }

    private static String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static Integer readInt(JsonNode root, String field) {
        String raw = readText(root, field);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String readText(JsonNode root, String field) {
        if (root == null || field == null) {
            return null;
        }
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private static String readAirbnbGuestFirstName(JsonNode root) {
        // Prefer Airbnb guest name from user_details.users[0].first_name
        JsonNode users = root.at("/user_details/users");
        if (users != null && users.isArray() && !users.isEmpty()) {
            String firstName = readText(users.get(0), "first_name");
            if (firstName != null) {
                return firstName;
            }
        }
        return null;
    }

    private static String trimToMax(String text, int max) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    private static LocalDateTime parseSince(String since) {
        if (since == null || since.isBlank()) {
            return LocalDateTime.MIN;
        }
        String s = since.trim();
        try {
            OffsetDateTime odt = OffsetDateTime.parse(s);
            return odt.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception ignore) {
            // continue
        }
        try {
            Instant instant = Instant.parse(s);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (Exception ignore) {
            // continue
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid since format: " + since);
        }
    }
}
