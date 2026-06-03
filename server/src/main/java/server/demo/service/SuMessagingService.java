package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import server.demo.dto.SuMessagingMessageDTO;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.dto.SuMessagingThreadDTO;
import server.demo.entity.Reservation;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.ReservationRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.util.SuReservationParser;
import server.demo.util.UtcTimeUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SuMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(SuMessagingService.class);

    public static final int CHANNEL_BOOKING = 19;
    public static final int CHANNEL_AIRBNB = 244;

    private final SuMessageThreadRepository threadRepository;
    private final SuMessageRepository messageRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationBookingKeyResolver reservationBookingKeyResolver;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;
    private final ObjectMapper objectMapper;
    private final SuMessagingRealtimeGateway suMessagingRealtimeGateway;

    public SuMessagingService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            ReservationRepository reservationRepository,
            ReservationBookingKeyResolver reservationBookingKeyResolver,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService,
            ObjectMapper objectMapper,
            SuMessagingRealtimeGateway suMessagingRealtimeGateway
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.reservationRepository = reservationRepository;
        this.reservationBookingKeyResolver = reservationBookingKeyResolver;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
        this.objectMapper = objectMapper;
        this.suMessagingRealtimeGateway = suMessagingRealtimeGateway;
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
        messageRepository.save(msg);

        Long savedThreadId = thread.getId();
        Long savedMessageId = msg.getId();
        if (savedThreadId != null && savedMessageId != null) {
            SuMessagingMessageDTO savedMessageDto = toMessageDTO(msg);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        suMessagingRealtimeGateway.broadcastMessageCreated(storeId, savedThreadId, savedMessageDto);
                        triggerAutoReplyAfterCommit(storeId, savedThreadId, savedMessageId);
                    }
                });
            } else {
                suMessagingRealtimeGateway.broadcastMessageCreated(storeId, savedThreadId, savedMessageDto);
                triggerAutoReplyAfterCommit(storeId, savedThreadId, savedMessageId);
            }
        }
    }

    private void triggerAutoReplyAfterCommit(Long storeId, Long threadId, Long triggerMessageId) {
        logger.info("[SuMessaging] AI auto reply disabled. skip trigger. storeId={}, threadId={}, triggerMessageId={}",
                storeId, threadId, triggerMessageId);
    }

    public List<SuMessagingThreadDTO> listThreads(Long storeId) {
        return threadRepository.findByStoreIdOrderByLastActivityDesc(storeId).stream()
                .map(thread -> {
                    Reservation reservation = resolveReservationForThread(storeId, thread);
                    SuMessagingThreadDTO dto = new SuMessagingThreadDTO();
                    dto.setId(thread.getId());
                    dto.setReservationId(reservation != null ? reservation.getId() : null);
                    dto.setChannelId(thread.getChannelId());
                    dto.setChannelName(resolveChannelName(thread.getChannelId()));
                    dto.setGuestName(thread.getGuestName());
                    dto.setBookingId(thread.getBookingId());
                    dto.setThreadId(thread.getThreadId());
                    dto.setListingId(thread.getListingId());
                    dto.setListingName(thread.getListingName());
                    dto.setCheckInDate(reservation != null ? reservation.getCheckInDate() : null);
                    dto.setCheckOutDate(reservation != null ? reservation.getCheckOutDate() : null);
                    dto.setRoomTypeName(resolveRoomTypeName(reservation));
                    dto.setLastMessage(thread.getLastMessage());
                    dto.setLastActivity(toUtcOffset(thread.getLastActivity()));
                    dto.setClosed(Boolean.TRUE.equals(thread.getClosed()));
                    dto.setUnreadCount(messageRepository.countByThread_IdAndSenderTypeAndIsReadFalse(thread.getId(), SuMessagingSenderType.GUEST));
                    return dto;
                })
                .toList();
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

        SuMessagingMessageDTO dto = toMessageDTO(msg);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    suMessagingRealtimeGateway.broadcastMessageCreated(storeId, threadId, dto);
                }
            });
        } else {
            suMessagingRealtimeGateway.broadcastMessageCreated(storeId, threadId, dto);
        }

        return dto;
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
