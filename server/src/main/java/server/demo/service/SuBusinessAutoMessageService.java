package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;
import server.demo.dto.SuMessagingMessageDTO;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.entity.SuReservationWebhookEvent;
import server.demo.enums.ReservationStatus;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.RoomGroupMemberRepository;
import server.demo.repository.RoomRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.repository.SuReservationWebhookEventRepository;
import server.demo.util.AutoMessageTemplateRenderer;
import server.demo.util.StoreTimeZoneUtil;
import server.demo.util.SuReservationParser;
import server.demo.util.UtcTimeUtil;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SuBusinessAutoMessageService {

    private static final Logger logger = LoggerFactory.getLogger(SuBusinessAutoMessageService.class);
    private static final Logger autoMessageLogger = LoggerFactory.getLogger("SU_AUTO_MESSAGE");

    private static final String TARGET_TYPE_RESERVATION = "RESERVATION";
    private static final String SENDLOG_ACTION_PREFIX = "AM:";
    private static final int MAX_LOG_MESSAGE_LEN = 800;
    private static final int MAX_LOOKBACK_DAYS = 400;
    private static final String DEFAULT_TEMPLATE_SENDER_NAME = "Auto Message";
    private static final String DELIVERY_SENDING = "SENDING";
    private static final String DELIVERY_SENT = "SENT";
    private static final String DELIVERY_FAILED = "FAILED";
    private static final String WAITING_PROPERTY_ACCESS = "WAITING_PROPERTY_ACCESS";

    private final AutoMessageSendLogRepository sendLogRepository;
    private final StoreRepository storeRepository;
    private final ReservationRepository reservationRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomGroupMemberRepository roomGroupMemberRepository;
    private final SuMessageThreadRepository threadRepository;
    private final SuMessageRepository messageRepository;
    private final SuReservationWebhookEventRepository webhookEventRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;
    private final SuMessagingRealtimeGateway realtimeGateway;
    private final ObjectMapper objectMapper;
    private final RegistrationLinkService registrationLinkService;
    private final String frontendBaseUrl;
    private final String templateSenderName;

    public SuBusinessAutoMessageService(
            AutoMessageSendLogRepository sendLogRepository,
            StoreRepository storeRepository,
            ReservationRepository reservationRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository,
            RoomGroupMemberRepository roomGroupMemberRepository,
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            SuReservationWebhookEventRepository webhookEventRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService,
            SuMessagingRealtimeGateway realtimeGateway,
            ObjectMapper objectMapper,
            RegistrationLinkService registrationLinkService,
            @Value("${app.frontend.url}") String frontendBaseUrl,
            @Value("${su.messaging.auto-template.sender-name:Auto Message}") String templateSenderName
    ) {
        this.sendLogRepository = sendLogRepository;
        this.storeRepository = storeRepository;
        this.reservationRepository = reservationRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.roomGroupMemberRepository = roomGroupMemberRepository;
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
        this.realtimeGateway = realtimeGateway;
        this.objectMapper = objectMapper;
        this.registrationLinkService = registrationLinkService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.templateSenderName = templateSenderName;
    }

    public record DispatchDecision(boolean okToSend, String reason) {}

    public DispatchDecision shouldSend(Reservation reservation, AutoMessage template) {
        if (reservation == null || template == null) {
            return new DispatchDecision(false, "missing reservation/template");
        }

        if (reservation.getStoreId() == null || template.getStoreId() == null) {
            return new DispatchDecision(false, "missing storeId");
        }
        if (!reservation.getStoreId().equals(template.getStoreId())) {
            return new DispatchDecision(false, "store mismatch");
        }

        ReservationStatus status = reservation.getStatus();
        if (status == ReservationStatus.CANCELLED || status == ReservationStatus.NO_SHOW) {
            return new DispatchDecision(false, "reservation cancelled/no_show");
        }

        Channel reservationChannel = reservation.getChannel();
        if (reservationChannel == null) {
            return new DispatchDecision(false, "missing channel");
        }
        if (toSuChannelId(reservationChannel.getCode()) == null) {
            return new DispatchDecision(false, "unsupported channel (only 19/244)");
        }

        Long reservationChannelId = reservation.getChannel() != null ? reservation.getChannel().getId() : null;
        if (!matchChannels(template.getChannels(), reservationChannelId)) {
            return new DispatchDecision(false, "channel not matched");
        }

        if (!matchRoomSelection(template, reservation)) {
            return new DispatchDecision(false, "room selection not matched");
        }

        return new DispatchDecision(true, "ok");
    }

    @Transactional
    public void trySendForReservation(Long storeId, Reservation reservation, AutoMessage template, LocalDateTime now, Duration delay) {
        if (storeId == null || reservation == null || template == null || now == null || delay == null) {
            return;
        }

        String sendLogAction = SENDLOG_ACTION_PREFIX + template.getId();
        Optional<AutoMessageSendLog> existing = sendLogRepository.findByStoreIdAndActionAndTargetTypeAndTargetId(
                storeId, sendLogAction, TARGET_TYPE_RESERVATION, reservation.getId()
        );

        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getSuccess())) {
            return;
        }
        if (existing.isPresent() && Boolean.FALSE.equals(existing.get().getSuccess())) {
            String err = existing.get().getErrorMessage();
            if (err == null || !err.startsWith("WAITING_")) {
                return;
            }
        }

        DispatchDecision decision = shouldSend(reservation, template);
        if (!decision.okToSend()) {
            return;
        }

        AutoMessageSendLog log = existing.orElseGet(AutoMessageSendLog::new);
        log.setStoreId(storeId);
        log.setAction(sendLogAction);
        log.setTargetType(TARGET_TYPE_RESERVATION);
        log.setTargetId(reservation.getId());
        log.setAutoMessageId(template.getId());
        log.setSuccess(null);
        log.setErrorMessage(null);

        if (existing.isEmpty()) {
            try {
                sendLogRepository.save(log);
            } catch (DataIntegrityViolationException e) {
                return;
            }
        }

        Store store = storeRepository.findById(storeId).orElse(null);

        try {
            SuMessageThread thread = resolveThreadForReservation(storeId, reservation, store);
            if (thread == null) {
                markWaiting(log, "WAITING_THREAD", "thread not found; wait for webhook sync");
                autoMessageLogger.info("[AutoMessage] waiting thread. storeId={}, reservationId={}, autoMessageId={}, channelId={}",
                        storeId, reservation.getId(), template.getId(), reservation.getChannel() != null ? reservation.getChannel().getId() : null);
                return;
            }

            String rendered = renderTemplate(store, reservation, template.getMessage());
            if (rendered == null || rendered.isBlank()) {
                markFailed(log, "template rendered empty");
                return;
            }
            String content = rendered.trim();

            if (thread.getListingId() == null || thread.getListingId().isBlank()) {
                String recoveredListingId = tryHydrateThreadListingIdFromWebhook(storeId, reservation, store, thread);
                if (recoveredListingId != null && !recoveredListingId.isBlank()) {
                    thread.setListingId(recoveredListingId);
                }
            }

            if (thread.getListingId() == null || thread.getListingId().isBlank()) {
                markWaiting(log, "WAITING_LISTINGID", "thread missing listingid");
                autoMessageLogger.info("[AutoMessage] waiting listingid. storeId={}, reservationId={}, autoMessageId={}, threadId={}",
                        storeId, reservation.getId(), template.getId(), thread.getId());
                return;
            }

            if (thread.getChannelId() != null && thread.getChannelId() == SuMessagingService.CHANNEL_AIRBNB) {
                String threadId = thread.getThreadId();
                String guestId = thread.getGuestId();
                if (threadId == null || threadId.isBlank() || guestId == null || guestId.isBlank()) {
                    markWaiting(log, "WAITING_THREAD_FIELDS", "airbnb thread missing threadid/guestid");
                    autoMessageLogger.info("[AutoMessage] waiting airbnb thread fields. storeId={}, reservationId={}, autoMessageId={}, threadDbId={} ",
                            storeId, reservation.getId(), template.getId(), thread.getId());
                    return;
                }
            }

            if (Boolean.TRUE.equals(thread.getClosed())) {
                markFailed(log, "thread is closed");
                return;
            }

            if (thread.getChannelId() != null && thread.getChannelId() == SuMessagingService.CHANNEL_BOOKING) {
                String recoveredBookingId = tryHydrateThreadBookingIdFromWebhook(storeId, reservation, store, thread);
                if (recoveredBookingId != null && !recoveredBookingId.isBlank()) {
                    reservation.setChannelOrderNumber(recoveredBookingId);
                }
            }

            thread = ensureThreadBookingIdentity(thread, reservation);
            if (thread.getChannelId() != null && thread.getChannelId() == SuMessagingService.CHANNEL_BOOKING) {
                String bookingId = resolveBookingReplyBookingId(thread);
                if (bookingId == null || bookingId.isBlank()) {
                    markWaiting(log, "WAITING_THREAD_FIELDS", "booking thread missing bookingid");
                    autoMessageLogger.info("[AutoMessage] waiting bookingid. storeId={}, reservationId={}, autoMessageId={}, threadDbId={}",
                            storeId, reservation.getId(), template.getId(), thread.getId());
                    return;
                }
            }

            String sender = resolveTemplateSenderName(templateSenderName);
            Map<String, Object> payload = null;
            String sendError = null;
            try {
                String channelMessage = formatMessageForChannel(thread.getChannelId(), content);
                payload = buildReplyPayload(thread, channelMessage);
                final Map<String, Object> requestPayload = payload;
                JsonNode suResponse = suAccessTokenService.executeWithTokenRetry(
                        token -> suApiClient.postMessagingAB(token, requestPayload),
                        "messagingAB"
                );
                if (!suApiClient.isSuSuccess(suResponse)) {
                    String suErrorMessage = suApiClient.extractSuErrorMessage(suResponse);
                    String suErrorCode = suApiClient.extractSuErrorCode(suResponse);
                    sendError = formatSuErrorSummary(
                            suErrorCode,
                            suErrorMessage != null ? suErrorMessage : "Su message send failed",
                            thread
                    );
                }
            } catch (Exception sendEx) {
                String raw = sendEx.getMessage();
                sendError = appendThreadContext(
                        raw == null || raw.isBlank() ? "SEND_ERR: unknown" : "SEND_ERR: " + raw,
                        thread
                );
            }

            if (sendError == null || sendError.isBlank()) {
                SuMessage localMessage = createLocalSendingMessage(storeId, thread, content, sender);
                markLocalMessageSent(storeId, thread.getId(), localMessage, payload);
            } else {
                WaitState sendWaitState = classifyRecoverableError(sendError);
                if (sendWaitState != null) {
                    throw new RuntimeException(sendError);
                }
                SuMessage localMessage = createLocalSendingMessage(storeId, thread, content, sender);
                markLocalMessageFailed(storeId, thread.getId(), localMessage, sendError, payload);
                throw new RuntimeException(sendError);
            }

            log.setSuccess(true);
            log.setErrorMessage(null);
            sendLogRepository.save(log);

            autoMessageLogger.info("[AutoMessage] sent ok. storeId={}, reservationId={}, autoMessageId={}, action={}, sendTiming={}",
                    storeId, reservation.getId(), template.getId(), template.getAction(), template.getSendTiming());
        } catch (Exception e) {
            String err = e.getMessage();
            WaitState waitState = classifyRecoverableError(err);
            logger.warn("[AutoMessage] send failed. storeId={}, reservationId={}, autoMessageId={}, err={}",
                    storeId, reservation.getId(), template.getId(), err, e);
            autoMessageLogger.error("[AutoMessage] send failed. storeId={}, reservationId={}, autoMessageId={}, err={}",
                    storeId, reservation.getId(), template.getId(), err);
            if (waitState != null) {
                markWaiting(log, waitState.code(), waitState.detail());
            } else {
                markFailed(log, err);
            }
        }
    }

    private String tryHydrateThreadListingIdFromWebhook(Long storeId, Reservation reservation, Store store, SuMessageThread thread) {
        if (storeId == null || reservation == null || thread == null) {
            return null;
        }

        String suHotelId = resolveSuHotelId(reservation, store);
        String reservationNotifId = normalizeIdentifier(reservation.getReservationNotifId());

        if (suHotelId != null && reservationNotifId != null) {
            Optional<SuReservationWebhookEvent> exactEvent =
                    webhookEventRepository.findByHotelIdAndReservationNotifId(suHotelId, reservationNotifId);
            if (exactEvent.isPresent()) {
                String recovered = resolveListingIdFromWebhookEventPayload(exactEvent.get(), reservation);
                if (recovered != null) {
                    return saveRecoveredThreadListingId(storeId, reservation, thread, recovered, "webhook_exact_notif");
                }
            }
        }

        List<SuReservationWebhookEvent> recentEvents = webhookEventRepository.findTop200ByStoreIdOrderByCreatedAtDesc(storeId);
        for (SuReservationWebhookEvent event : recentEvents) {
            String recovered = resolveListingIdFromWebhookEventPayload(event, reservation);
            if (recovered != null) {
                return saveRecoveredThreadListingId(storeId, reservation, thread, recovered, "webhook_recent_scan");
            }
        }
        return null;
    }

    private String tryHydrateThreadBookingIdFromWebhook(Long storeId, Reservation reservation, Store store, SuMessageThread thread) {
        if (storeId == null || reservation == null || thread == null) {
            return null;
        }

        String existingBookingId = resolveBookingReplyBookingId(thread);
        if (existingBookingId != null && !existingBookingId.isBlank()) {
            return existingBookingId;
        }

        String recovered = resolveBookingIdFromWebhook(storeId, reservation, store);
        if (recovered == null || recovered.isBlank()) {
            return null;
        }
        return saveRecoveredThreadBookingIdentity(storeId, reservation, thread, recovered, "webhook_bookingid");
    }

    private String saveRecoveredThreadListingId(
            Long storeId,
            Reservation reservation,
            SuMessageThread thread,
            String listingId,
            String source
    ) {
        String normalized = resolveTrustedListingId(listingId);
        if (normalized == null) {
            return null;
        }

        if (normalized.equals(resolveTrustedListingId(thread.getListingId()))) {
            return normalized;
        }

        thread.setListingId(normalized);
        thread.setLastActivity(UtcTimeUtil.nowLocalDateTime());
        try {
            threadRepository.save(thread);
            autoMessageLogger.info(
                    "[AutoMessage] recovered listingid from webhook. storeId={}, reservationId={}, threadId={}, source={}, listingId={}",
                    storeId,
                    reservation.getId(),
                    thread.getId(),
                    source,
                    normalized
            );
        } catch (DataIntegrityViolationException ex) {
            logger.warn(
                    "[AutoMessage] recover listingid save race. storeId={}, reservationId={}, threadId={}, source={}, err={}",
                    storeId,
                    reservation.getId(),
                    thread.getId(),
                    source,
                    ex.getMessage()
            );
        }
        return normalized;
    }

    private String saveRecoveredThreadBookingIdentity(
            Long storeId,
            Reservation reservation,
            SuMessageThread thread,
            String bookingId,
            String source
    ) {
        String normalized = SuReservationParser.normalizeBookingReservationId(bookingId);
        if (normalized == null) {
            return null;
        }

        boolean reservationChanged = !normalized.equals(SuReservationParser.normalizeBookingReservationId(reservation.getChannelOrderNumber()));
        if (reservationChanged) {
            reservation.setChannelOrderNumber(normalized);
            try {
                reservationRepository.save(reservation);
            } catch (Exception ex) {
                logger.warn(
                        "[AutoMessage] recover bookingid save reservation failed. storeId={}, reservationId={}, source={}, err={}",
                        storeId,
                        reservation.getId(),
                        source,
                        ex.getMessage()
                );
            }
        }

        String currentBookingId = SuReservationParser.normalizeBookingReservationId(thread.getBookingId());
        String currentThreadKey = SuReservationParser.normalizeBookingReservationId(thread.getThreadKey());
        if (normalized.equals(currentBookingId) && normalized.equals(currentThreadKey)) {
            return normalized;
        }

        thread.setBookingId(normalized);
        thread.setThreadKey(normalized);
        thread.setLastActivity(UtcTimeUtil.nowLocalDateTime());
        try {
            threadRepository.save(thread);
            autoMessageLogger.info(
                    "[AutoMessage] recovered bookingid from webhook. storeId={}, reservationId={}, threadId={}, source={}, bookingId={}",
                    storeId,
                    reservation.getId(),
                    thread.getId(),
                    source,
                    normalized
            );
        } catch (DataIntegrityViolationException ex) {
            logger.warn(
                    "[AutoMessage] recover bookingid save race. storeId={}, reservationId={}, threadId={}, source={}, err={}",
                    storeId,
                    reservation.getId(),
                    thread.getId(),
                    source,
                    ex.getMessage()
            );
        }
        return normalized;
    }

    private String resolveBookingIdFromWebhook(Long storeId, Reservation reservation, Store store) {
        if (storeId == null || reservation == null) {
            return null;
        }

        String suHotelId = resolveSuHotelId(reservation, store);
        String reservationNotifId = normalizeIdentifier(reservation.getReservationNotifId());
        if (suHotelId != null && reservationNotifId != null) {
            Optional<SuReservationWebhookEvent> exactEvent =
                    webhookEventRepository.findByHotelIdAndReservationNotifId(suHotelId, reservationNotifId);
            if (exactEvent.isPresent()) {
                String recovered = resolveBookingIdFromWebhookEventPayload(exactEvent.get(), reservation);
                if (recovered != null) {
                    return recovered;
                }
            }
        }

        List<SuReservationWebhookEvent> recentEvents = webhookEventRepository.findTop200ByStoreIdOrderByCreatedAtDesc(storeId);
        for (SuReservationWebhookEvent event : recentEvents) {
            String recovered = resolveBookingIdFromWebhookEventPayload(event, reservation);
            if (recovered != null) {
                return recovered;
            }
        }
        return null;
    }

    private String resolveListingIdFromWebhookEventPayload(SuReservationWebhookEvent event, Reservation reservation) {
        if (event == null || reservation == null) {
            return null;
        }
        String payload = event.getPayloadJson();
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            List<JsonNode> reservations = extractReservationNodesFromEventPayload(root);
            for (JsonNode reservationNode : reservations) {
                if (!matchesReservationNode(reservationNode, reservation)) {
                    continue;
                }

                SuReservationParser.MessagingListingResolution resolved =
                    SuReservationParser.extractMessagingListingIdWithSource(
                        reservation.getChannel() != null ? reservation.getChannel().getCode() : null,
                        reservationNode,
                        null
                    );
                if (resolved != null) {
                    String listingId = resolveTrustedListingId(resolved.listingId());
                    if (listingId != null) {
                        return listingId;
                    }
                }
            }
        } catch (Exception ex) {
            logger.debug(
                    "[AutoMessage] parse webhook payload for listingid failed. eventId={}, err={}",
                    event.getId(),
                    ex.getMessage()
            );
        }
        return null;
    }

    private String resolveBookingIdFromWebhookEventPayload(SuReservationWebhookEvent event, Reservation reservation) {
        if (event == null || reservation == null) {
            return null;
        }
        String payload = event.getPayloadJson();
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            List<JsonNode> reservations = extractReservationNodesFromEventPayload(root);
            for (JsonNode reservationNode : reservations) {
                if (!matchesReservationNode(reservationNode, reservation)) {
                    continue;
                }

                String recovered = SuReservationParser.extractBookingReservationId(
                        reservationNode,
                        reservation.getOrderNumber()
                );
                if (recovered != null && !recovered.isBlank()) {
                    return recovered;
                }
            }
        } catch (Exception ex) {
            logger.debug(
                    "[AutoMessage] parse webhook payload for bookingid failed. eventId={}, err={}",
                    event.getId(),
                    ex.getMessage()
            );
        }
        return null;
    }

    private static List<JsonNode> extractReservationNodesFromEventPayload(JsonNode root) {
        if (root == null || root.isNull()) {
            return List.of();
        }

        List<JsonNode> reservations = SuReservationParser.extractReservationNodes(root);
        if (!reservations.isEmpty()) {
            return reservations;
        }

        if (root.isArray()) {
            List<JsonNode> list = new ArrayList<>();
            for (JsonNode item : root) {
                if (item == null || item.isNull()) {
                    continue;
                }
                JsonNode reservationNode = item.get("reservation");
                if (reservationNode != null && reservationNode.isObject()) {
                    list.add(reservationNode);
                } else if (item.isObject()) {
                    list.add(item);
                }
            }
            return list;
        }

        JsonNode reservationNode = root.get("reservation");
        if (reservationNode != null && reservationNode.isObject()) {
            return List.of(reservationNode);
        }
        if (root.isObject()) {
            return List.of(root);
        }
        return List.of();
    }

    private static boolean matchesReservationNode(JsonNode reservationNode, Reservation reservation) {
        if (reservationNode == null || reservationNode.isNull() || reservation == null) {
            return false;
        }

        if (sameIdentifier(SuReservationParser.extractReservationNotifId(reservationNode), reservation.getReservationNotifId())) {
            return true;
        }
        if (sameIdentifier(SuReservationParser.extractReservationId(reservationNode), reservation.getSuReservationId())) {
            return true;
        }

        String channelBookingId = SuReservationParser.extractBookingReservationId(reservationNode);
        if (sameIdentifier(channelBookingId, reservation.getChannelOrderNumber())) {
            return true;
        }
        String reservationBookingId = resolveReservationBookingId(reservation);
        return sameIdentifier(channelBookingId, reservationBookingId);
    }

    private static boolean sameIdentifier(String left, String right) {
        String normalizedLeft = normalizeIdentifier(left);
        String normalizedRight = normalizeIdentifier(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }

    private static String normalizeIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String resolveReservationBookingId(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        String fromChannelOrder = SuReservationParser.normalizeBookingReservationId(reservation.getChannelOrderNumber());
        if (fromChannelOrder != null) {
            return fromChannelOrder;
        }
        return SuReservationParser.extractBookingReservationIdFromOrderNumber(reservation.getOrderNumber());
    }

    static String resolveThreadBookingIdForAutoMessage(Reservation reservation, Integer suChannelId) {
        if (reservation == null) {
            return null;
        }
        if (suChannelId != null && suChannelId == SuMessagingService.CHANNEL_BOOKING) {
            return resolveReservationBookingId(reservation);
        }
        String bookingId = normalizeIdentifier(reservation.getChannelOrderNumber());
        if (bookingId != null) {
            return bookingId;
        }
        return normalizeIdentifier(reservation.getOrderNumber());
    }

    private SuMessageThread ensureThreadBookingIdentity(SuMessageThread thread, Reservation reservation) {
        if (thread == null || thread.getChannelId() == null
                || thread.getChannelId() != SuMessagingService.CHANNEL_BOOKING) {
            return thread;
        }

        String normalizedBookingId = resolveThreadBookingIdForAutoMessage(reservation, thread.getChannelId());
        if (normalizedBookingId == null || normalizedBookingId.isBlank()) {
            normalizedBookingId = resolveBookingReplyBookingId(thread);
        }
        if (normalizedBookingId == null || normalizedBookingId.isBlank()) {
            return thread;
        }

        Optional<SuMessageThread> canonicalExisting = threadRepository
                .findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(
                        thread.getStoreId(),
                        thread.getChannelId(),
                        normalizedBookingId
                );
        if (canonicalExisting.isPresent() && !canonicalExisting.get().getId().equals(thread.getId())) {
            return mergeCanonicalBookingThread(canonicalExisting.get(), thread, normalizedBookingId);
        }

        boolean changed = false;
        String currentBookingId = SuReservationParser.normalizeBookingReservationId(thread.getBookingId());
        if (!normalizedBookingId.equals(currentBookingId)) {
            thread.setBookingId(normalizedBookingId);
            changed = true;
        }
        String currentThreadKey = SuReservationParser.normalizeBookingReservationId(thread.getThreadKey());
        if (!normalizedBookingId.equals(currentThreadKey)) {
            thread.setThreadKey(normalizedBookingId);
            changed = true;
        }
        if (!changed) {
            return thread;
        }

        try {
            return threadRepository.save(thread);
        } catch (DataIntegrityViolationException e) {
            return threadRepository.findByStoreIdAndChannelIdAndThreadKey(
                    thread.getStoreId(),
                    thread.getChannelId(),
                    normalizedBookingId
            ).orElse(thread);
        }
    }

    private SuMessageThread mergeCanonicalBookingThread(
            SuMessageThread canonical,
            SuMessageThread duplicate,
            String normalizedBookingId
    ) {
        boolean changed = false;
        if (canonical.getSuHotelId() == null || canonical.getSuHotelId().isBlank()) {
            canonical.setSuHotelId(duplicate.getSuHotelId());
            changed = true;
        }
        if (canonical.getGuestName() == null || canonical.getGuestName().isBlank()) {
            canonical.setGuestName(duplicate.getGuestName());
            changed = true;
        }
        if (canonical.getListingId() == null || canonical.getListingId().isBlank()) {
            canonical.setListingId(duplicate.getListingId());
            changed = true;
        }
        if (canonical.getListingName() == null || canonical.getListingName().isBlank()) {
            canonical.setListingName(duplicate.getListingName());
            changed = true;
        }
        if (!normalizedBookingId.equals(SuReservationParser.normalizeBookingReservationId(canonical.getBookingId()))) {
            canonical.setBookingId(normalizedBookingId);
            changed = true;
        }
        if (!normalizedBookingId.equals(SuReservationParser.normalizeBookingReservationId(canonical.getThreadKey()))) {
            canonical.setThreadKey(normalizedBookingId);
            changed = true;
        }
        if (changed) {
            canonical.setLastActivity(UtcTimeUtil.nowLocalDateTime());
            try {
                canonical = threadRepository.save(canonical);
            } catch (DataIntegrityViolationException ignore) {
                // keep best-effort behavior and continue with canonical thread
            }
        }
        return canonical;
    }

    static boolean canCreateBookingFallbackThread(Reservation reservation) {
        if (reservation == null) {
            return false;
        }
        return normalizeIdentifier(reservation.getSuReservationId()) != null
                || normalizeIdentifier(reservation.getReservationNotifId()) != null;
    }

    public LocalDateTime computeEarliestEventTime(AutoMessage template, LocalDateTime now) {
        LocalDateTime hardCap = now.minusDays(MAX_LOOKBACK_DAYS);
        if (Boolean.TRUE.equals(template.getResendOnExpire())) {
            return hardCap;
        }
        LocalDateTime createdAt = template.getCreatedAt();
        if (createdAt == null) {
            return hardCap;
        }
        LocalDateTime createdAtUtc = StoreTimeZoneUtil.reservationTimestampStorageToUtcLocalDateTime(createdAt);
        return createdAtUtc.isAfter(hardCap) ? createdAtUtc : hardCap;
    }

    public LocalDateTime resolveBaseTime(Reservation reservation, String action) {
        if (reservation == null || action == null) {
            return null;
        }
        return switch (action.trim().toUpperCase()) {
            case "BOOKING_CONFIRM" -> reservation.getCreatedAt();
            case "CHECK_IN" -> reservation.getActualCheckIn();
            case "CHECK_OUT" -> reservation.getActualCheckOut();
            default -> null;
        };
    }

    private SuMessageThread resolveThreadForReservation(Long storeId, Reservation reservation, Store store) {
        Channel channel = reservation.getChannel();
        if (channel == null) {
            return null;
        }

        Integer suChannelId = toSuChannelId(channel.getCode());
        if (suChannelId == null) {
            return null;
        }

        String bookingId = resolveThreadBookingIdForAutoMessage(reservation, suChannelId);
        if (bookingId == null || bookingId.isBlank()) {
            return null;
        }

        String normalizedBookingId = bookingId.trim();
        Optional<SuMessageThread> existing = threadRepository
                .findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(storeId, suChannelId, normalizedBookingId);
        if (existing.isPresent()) {
            SuMessageThread thread = existing.get();
            boolean changed = false;
            String resolvedSuHotelId = resolveSuHotelId(reservation, store);
            if ((thread.getSuHotelId() == null || thread.getSuHotelId().isBlank())
                    && resolvedSuHotelId != null && !resolvedSuHotelId.isBlank()) {
                thread.setSuHotelId(resolvedSuHotelId);
                changed = true;
            }
            if (thread.getBookingId() == null || thread.getBookingId().isBlank()) {
                thread.setBookingId(normalizedBookingId);
                changed = true;
            }
            if (thread.getThreadKey() == null || thread.getThreadKey().isBlank()) {
                thread.setThreadKey(normalizedBookingId);
                changed = true;
            }
            String normalizedThreadListingId = resolveTrustedListingId(thread.getListingId());
            if (normalizedThreadListingId == null && thread.getListingId() != null && !thread.getListingId().isBlank()) {
                thread.setListingId(null);
                changed = true;
            }
            String listingId = resolveThreadListingIdForReservationRecord(reservation);
            if (listingId != null && !listingId.isBlank()
                    && !listingId.equals(normalizedThreadListingId)) {
                thread.setListingId(listingId);
                changed = true;
            }
            if ((thread.getGuestName() == null || thread.getGuestName().isBlank())
                    && reservation.getGuestName() != null && !reservation.getGuestName().isBlank()) {
                thread.setGuestName(reservation.getGuestName());
                changed = true;
            }
            if (changed) {
                thread.setLastActivity(UtcTimeUtil.nowLocalDateTime());
                try {
                    thread = threadRepository.save(thread);
                } catch (DataIntegrityViolationException ignore) {
                    // keep best-effort behavior
                }
            }
            return thread;
        }

        // For Booking channel, create a best-effort thread fallback so event-driven auto messages
        // can be sent even before inbound messaging webhook creates the thread.
        if (suChannelId != SuMessagingService.CHANNEL_BOOKING) {
            return null;
        }
        if (!canCreateBookingFallbackThread(reservation)) {
            return null;
        }

        String threadKey = normalizedBookingId;
        String suHotelId = resolveSuHotelId(reservation, store);
        if (suHotelId == null || suHotelId.isBlank()) {
            return null;
        }

        String listingId = resolveThreadListingIdForReservationRecord(reservation);
        String listingName = reservation.getRoom() != null && reservation.getRoom().getRoomType() != null
                ? reservation.getRoom().getRoomType().getName()
                : null;

        try {
            SuMessageThread thread = threadRepository.findByStoreIdAndChannelIdAndThreadKey(storeId, suChannelId, threadKey)
                    .orElseGet(SuMessageThread::new);
            thread.setStoreId(storeId);
            thread.setSuHotelId(suHotelId);
            thread.setChannelId(suChannelId);
            thread.setThreadKey(threadKey);
            thread.setBookingId(normalizedBookingId);
            thread.setThreadId(normalizedBookingId);
            thread.setGuestName(reservation.getGuestName());
            if (listingName != null && !listingName.isBlank()) {
                thread.setListingName(listingName);
            }
            if (listingId != null && !listingId.isBlank()) {
                thread.setListingId(listingId);
            }
            thread.setLastActivity(UtcTimeUtil.nowLocalDateTime());
            return threadRepository.save(thread);
        } catch (DataIntegrityViolationException e) {
            return threadRepository.findByStoreIdAndChannelIdAndThreadKey(storeId, suChannelId, threadKey)
                    .orElse(null);
        }
    }

    private static String resolveSuHotelId(Reservation reservation, Store store) {
        String reservationSuHotelId = reservation != null ? reservation.getSuHotelId() : null;
        if (reservationSuHotelId != null && !reservationSuHotelId.isBlank()) {
            return reservationSuHotelId.trim();
        }
        String storeSuHotelId = store != null ? store.getSuHotelId() : null;
        if (storeSuHotelId != null && !storeSuHotelId.isBlank()) {
            return storeSuHotelId.trim();
        }
        return null;
    }

    static String resolveTrustedListingId(String rawListingId) {
        return SuReservationParser.normalizeMessagingListingId(rawListingId);
    }

    private static String resolveThreadListingIdForReservationRecord(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        String channelCode = reservation.getChannel() != null ? reservation.getChannel().getCode() : null;
        if (channelCode != null) {
            String normalizedChannel = channelCode.trim().toUpperCase(Locale.ROOT);
            if ("BOOKING".equals(normalizedChannel) || "BOOKING.COM".equals(normalizedChannel)) {
                // Booking listingid should come from webhook parsing strategy, not ota_room_id fallback.
                return null;
            }
        }
        return resolveTrustedListingId(reservation.getOtaRoomId());
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

    private boolean matchChannels(String channelsJson, Long reservationChannelId) {
        if (reservationChannelId == null) {
            return false;
        }
        if (channelsJson == null || channelsJson.isBlank()) {
            return false;
        }
        try {
            Set<Long> ids = parseLongJsonArray(channelsJson);
            return ids.contains(reservationChannelId);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchRoomSelection(AutoMessage template, Reservation reservation) {
        String type = template.getRoomSelectionType();
        if (type == null || type.isBlank() || "ALL_LOCAL".equalsIgnoreCase(type)) {
            return true;
        }

        String selection = template.getRoomSelection();
        if (selection == null || selection.isBlank()) {
            return false;
        }
        Set<Long> selectedIds;
        try {
            selectedIds = parseLongJsonArray(selection);
        } catch (Exception e) {
            return false;
        }
        if (selectedIds.isEmpty()) {
            return false;
        }

        return switch (type.trim().toUpperCase()) {
            case "BY_ROOM" -> {
                Room room = reservation.getRoom();
                yield room != null && room.getId() != null && selectedIds.contains(room.getId());
            }
            case "BY_ROOM_TYPE" -> {
                Long roomTypeId = resolveRoomTypeId(reservation);
                yield roomTypeId != null && selectedIds.contains(roomTypeId);
            }
            case "BY_GROUP" -> {
                Room room = reservation.getRoom();
                if (room == null || room.getId() == null) {
                    yield false;
                }
                boolean any = false;
                for (Long groupId : selectedIds) {
                    if (groupId != null && roomGroupMemberRepository.existsByStoreIdAndGroupIdAndRoomId(reservation.getStoreId(), groupId, room.getId())) {
                        any = true;
                        break;
                    }
                }
                yield any;
            }
            default -> false;
        };
    }

    private Long resolveRoomTypeId(Reservation reservation) {
        Room room = reservation.getRoom();
        if (room != null && room.getRoomType() != null && room.getRoomType().getId() != null) {
            return room.getRoomType().getId();
        }
        return reservation.getOtaRoomTypeId();
    }

    private String renderTemplate(Store store, Reservation reservation, String template) {
        Map<String, String> vars = buildVariables(store, reservation);
        return AutoMessageTemplateRenderer.render(template, vars);
    }

    private Map<String, String> buildVariables(Store store, Reservation reservation) {
        Map<String, String> vars = new HashMap<>();
        vars.put("property_name", store != null ? nullToEmpty(store.getName()) : "");
        vars.put("property_address", store != null ? nullToEmpty(store.getAddress()) : "");
        vars.put("property_city", store != null ? nullToEmpty(store.getCity()) : "");
        vars.put("property_phone", store != null ? nullToEmpty(store.getPhone()) : "");
        vars.put("property_email", store != null ? nullToEmpty(store.getEmail()) : "");

        vars.put("guest_name", reservation != null ? nullToEmpty(reservation.getGuestName()) : "");
        vars.put("guest_phone", reservation != null ? nullToEmpty(reservation.getGuestPhone()) : "");

        vars.put("checkin_date", formatDate(reservation != null ? reservation.getCheckInDate() : null));
        vars.put("checkout_date", formatDate(reservation != null ? reservation.getCheckOutDate() : null));

        vars.put("room_type_name", resolveRoomTypeName(reservation));
        vars.put("room_type_address", resolveRoomTypeAddress(reservation));
        vars.put("nearby_station", resolveNearbyStation(reservation));
        vars.put("rate_plan_name", "");
        vars.put("confirmation_code", reservation != null ? nullToEmpty(resolveRegistrationBookingKey(reservation)) : "");

        vars.put("order_number", reservation != null ? nullToEmpty(reservation.getOrderNumber()) : "");
        String registrationLink = buildRegistrationLink(reservation);
        vars.put("registration_link", registrationLink);
        vars.put("checkin_form_link", registrationLink);

        vars.put("number_of_nights", resolveNights(reservation));
        vars.put("checkin_code", "");
        vars.put("smartlock_passcode", resolveSmartlockPasscode(reservation));
        vars.put("room_number", resolveRoomNumber(reservation));

        return vars;
    }

    private String resolveSmartlockPasscode(Reservation reservation) {
        if (reservation == null) {
            return "";
        }

        Room room = reservation.getRoom();
        if (room != null) {
            return nullToEmpty(room.getSmartlockPasscode());
        }

        Long storeId = reservation.getStoreId();
        if (storeId == null) {
            return "";
        }
        String roomNumber = reservation.getOtaRoomNumber();
        if (roomNumber == null || roomNumber.isBlank()) {
            return "";
        }

        return roomRepository
                .findByStoreIdAndRoomNumber(storeId, roomNumber.trim())
                .map(Room::getSmartlockPasscode)
                .map(SuBusinessAutoMessageService::nullToEmpty)
                .orElse("");
    }

    private String buildRegistrationLink(Reservation reservation) {
        if (reservation == null || reservation.getStoreId() == null) {
            return "";
        }

        String bookingKey = resolveRegistrationBookingKey(reservation);
        if (bookingKey == null || bookingKey.isBlank()) {
            return "";
        }

        String token = registrationLinkService.generateToken(reservation.getStoreId(), bookingKey);
        String base = frontendBaseUrl != null ? frontendBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String encodedKey = UriUtils.encodePathSegment(bookingKey, StandardCharsets.UTF_8);
        return base + "/rb/" + encodedKey + "?t=" + token;
    }

    private String resolveRegistrationBookingKey(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        String channelOrderNumber = normalizeIdentifier(reservation.getChannelOrderNumber());
        if (channelOrderNumber != null) {
            return channelOrderNumber;
        }

        String externalBookingKey = normalizeIdentifier(reservation.getExternalBookingKey());
        if (externalBookingKey != null) {
            return externalBookingKey;
        }

        String extractedFromOrder = extractBookingKeyFromOrderLikeValue(reservation.getOrderNumber());
        if (extractedFromOrder != null) {
            return extractedFromOrder;
        }

        return normalizeIdentifier(reservation.getOrderNumber());
    }

    private static String extractBookingKeyFromOrderLikeValue(String rawValue) {
        String normalized = normalizeIdentifier(rawValue);
        if (normalized == null) {
            return null;
        }

        if (normalized.regionMatches(true, 0, "SU", 0, 2)) {
            int dashIndex = normalized.indexOf('-');
            if (dashIndex >= 0 && dashIndex + 1 < normalized.length()) {
                String suffix = normalized.substring(dashIndex + 1);
                int end = suffix.length();
                int suffixUnderscore = suffix.indexOf('_');
                if (suffixUnderscore >= 0) {
                    end = Math.min(end, suffixUnderscore);
                }
                int suffixDash = suffix.indexOf('-');
                if (suffixDash >= 0) {
                    end = Math.min(end, suffixDash);
                }

                String extracted = suffix.substring(0, end).trim();
                if (!extracted.isBlank()) {
                    return extracted;
                }
            }
        }

        int underscoreIndex = normalized.indexOf('_');
        if (underscoreIndex > 0) {
            String prefix = normalized.substring(0, underscoreIndex).trim();
            return prefix.isBlank() ? null : prefix;
        }

        return null;
    }

    private String resolveRoomTypeName(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null && room.getRoomType() != null) {
            return nullToEmpty(room.getRoomType().getName());
        }
        Long otaRoomTypeId = reservation.getOtaRoomTypeId();
        if (otaRoomTypeId == null) {
            return "";
        }
        Optional<RoomType> rt = roomTypeRepository.findById(otaRoomTypeId);
        return rt.map(RoomType::getName).orElse("");
    }

    private String resolveRoomTypeAddress(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null && room.getRoomType() != null) {
            return nullToEmpty(room.getRoomType().getRoomTypeAddress());
        }
        Long otaRoomTypeId = reservation.getOtaRoomTypeId();
        if (otaRoomTypeId == null) {
            return "";
        }
        Optional<RoomType> rt = roomTypeRepository.findById(otaRoomTypeId);
        return rt.map(RoomType::getRoomTypeAddress).map(SuBusinessAutoMessageService::nullToEmpty).orElse("");
    }

    private String resolveNearbyStation(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null && room.getRoomType() != null) {
            return nullToEmpty(room.getRoomType().getNearbyStation());
        }
        Long otaRoomTypeId = reservation.getOtaRoomTypeId();
        if (otaRoomTypeId == null) {
            return "";
        }
        Optional<RoomType> rt = roomTypeRepository.findById(otaRoomTypeId);
        return rt.map(RoomType::getNearbyStation).map(SuBusinessAutoMessageService::nullToEmpty).orElse("");
    }

    private String resolveRoomNumber(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        Room room = reservation.getRoom();
        if (room != null) {
            return nullToEmpty(room.getRoomNumber());
        }
        return nullToEmpty(reservation.getOtaRoomNumber());
    }

    private static String resolveNights(Reservation reservation) {
        if (reservation == null) {
            return "";
        }
        LocalDate in = reservation.getCheckInDate();
        LocalDate out = reservation.getCheckOutDate();
        if (in == null || out == null) {
            return "";
        }
        long nights = ChronoUnit.DAYS.between(in, out);
        return String.valueOf(Math.max(0, nights));
    }

    private static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static Set<Long> parseLongJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Set.of();
        }
        String trimmed = json.trim();
        if (trimmed.equals("[]")) {
            return Set.of();
        }
        // very small JSON array parser to avoid pulling ObjectMapper here.
        // expects format like: [1,2,3] or ["1","2"]
        trimmed = trimmed.replace("[", "").replace("]", "").trim();
        if (trimmed.isBlank()) {
            return Set.of();
        }
        String[] parts = trimmed.split(",");
        Set<Long> out = new HashSet<>();
        for (String part : parts) {
            if (part == null) continue;
            String p = part.trim();
            if (p.startsWith("\"") && p.endsWith("\"") && p.length() >= 2) {
                p = p.substring(1, p.length() - 1);
            }
            if (p.isBlank()) continue;
            try {
                out.add(Long.parseLong(p));
            } catch (NumberFormatException ignored) {
                // ignore invalid entries
            }
        }
        return out;
    }

    private SuMessage createLocalSendingMessage(Long storeId, SuMessageThread thread, String content, String senderName) {
        SuMessage message = new SuMessage();
        message.setStoreId(storeId);
        message.setThread(thread);
        message.setSenderType(SuMessagingSenderType.STAFF);
        message.setSenderName(senderName);
        message.setContent(content);
        message.setSentAt(UtcTimeUtil.nowLocalDateTime());
        message.setIsRead(true);
        message.setDeliveryStatus(DELIVERY_SENDING);
        message.setRawJson(null);
        message = messageRepository.saveAndFlush(message);

        thread.setLastMessage(trimToMax(content, 500));
        thread.setLastActivity(UtcTimeUtil.nowLocalDateTime());
        threadRepository.save(thread);

        realtimeGateway.broadcastMessageCreated(storeId, thread.getId(), toMessageDTO(message));
        return message;
    }

    private void markLocalMessageSent(Long storeId, Long threadId, SuMessage message, Map<String, Object> payload) {
        if (message == null) {
            return;
        }
        message.setDeliveryStatus(DELIVERY_SENT);
        message.setRawJson(payload != null ? writeJsonSafely(payload) : null);
        SuMessage saved = messageRepository.save(message);
        realtimeGateway.broadcastMessageUpdated(storeId, threadId, toMessageDTO(saved));
    }

    private void markLocalMessageFailed(Long storeId, Long threadId, SuMessage message, String err) {
        markLocalMessageFailed(storeId, threadId, message, err, null);
    }

    private void markLocalMessageFailed(Long storeId, Long threadId, SuMessage message, String err, Map<String, Object> payload) {
        if (message == null) {
            return;
        }
        message.setDeliveryStatus(DELIVERY_FAILED);
        String details = err == null ? "" : err.trim();
        if (details.length() > 400) {
            details = details.substring(0, 400);
        }
        message.setRawJson(payload != null ? writeJsonSafely(payload) : details);
        SuMessage saved = messageRepository.save(message);
        realtimeGateway.broadcastMessageUpdated(storeId, threadId, toMessageDTO(saved));
    }

    private static String formatMessageForChannel(Integer channelId, String message) {
        if (message == null) {
            return null;
        }
        return message.replace("\r\n", "\n").replace("\r", "\n");
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

        if (channelId == SuMessagingService.CHANNEL_AIRBNB) {
            String threadId = thread.getThreadId();
            String guestId = thread.getGuestId();
            if (threadId == null || threadId.isBlank() || guestId == null || guestId.isBlank()) {
                throw new IllegalStateException("Airbnb reply requires threadid + guestid");
            }
            payload.put("threadid", threadId);
            payload.put("guestid", guestId);
            payload.put("bookingid", thread.getBookingId() != null ? thread.getBookingId() : threadId);
            return payload;
        }

        if (channelId == SuMessagingService.CHANNEL_BOOKING) {
            String bookingId = resolveBookingReplyBookingId(thread);
            if (bookingId == null || bookingId.isBlank()) {
                throw new IllegalStateException("Booking.com reply requires bookingid");
            }
            payload.put("bookingid", bookingId);
            return payload;
        }

        throw new IllegalStateException("Unsupported channel: " + channelId);
    }

    private static String resolveBookingReplyBookingId(SuMessageThread thread) {
        if (thread == null) {
            return null;
        }
        String fromBookingId = SuReservationParser.normalizeBookingReservationId(thread.getBookingId());
        if (fromBookingId != null) {
            return fromBookingId;
        }
        String fromThreadKey = SuReservationParser.normalizeBookingReservationId(thread.getThreadKey());
        if (fromThreadKey != null) {
            return fromThreadKey;
        }
        return SuReservationParser.normalizeBookingReservationId(thread.getThreadId());
    }

    private String writeJsonSafely(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private static SuMessagingMessageDTO toMessageDTO(SuMessage message) {
        SuMessagingMessageDTO dto = new SuMessagingMessageDTO();
        dto.setId(message.getId());
        dto.setThreadId(message.getThread().getId());
        dto.setSenderType(message.getSenderType());
        dto.setSenderName(message.getSenderName());
        dto.setContent(message.getContent());
        dto.setDeliveryStatus(message.getDeliveryStatus());
        dto.setTimestamp(toUtcOffset(message.getSentAt()));
        return dto;
    }

    private static OffsetDateTime toUtcOffset(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return UtcTimeUtil.toUtcOffset(localDateTime);
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

    private static String resolveTemplateSenderName(String configuredSenderName) {
        if (configuredSenderName == null || configuredSenderName.isBlank()) {
            return DEFAULT_TEMPLATE_SENDER_NAME;
        }
        return configuredSenderName.trim();
    }

    private String formatSuErrorSummary(String suErrorCode, String suErrorMessage, SuMessageThread thread) {
        String normalizedCode = suErrorCode == null || suErrorCode.isBlank() ? "UNKNOWN" : suErrorCode.trim();
        String normalizedMessage = suErrorMessage == null || suErrorMessage.isBlank() ? "Unknown error" : suErrorMessage.trim();
        return appendThreadContext("SU_ERR[code=" + normalizedCode + "]: " + normalizedMessage, thread);
    }

    private String appendThreadContext(String message, SuMessageThread thread) {
        if (thread == null) {
            return message;
        }
        StringBuilder builder = new StringBuilder(message == null ? "" : message);
        builder.append(" {channelId=").append(thread.getChannelId() == null ? "-" : thread.getChannelId());
        builder.append(", threadId=").append(safeThreadField(thread.getThreadId()));
        builder.append(", bookingId=").append(safeThreadField(thread.getBookingId()));
        builder.append(", listingId=").append(safeThreadField(thread.getListingId()));
        builder.append('}');
        return builder.toString();
    }

    private static String safeThreadField(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private WaitState classifyRecoverableError(String errorMessage) {
        String code = classifyRecoverableWaitCode(errorMessage);
        if (code == null) {
            return null;
        }
        return new WaitState(code, trimErr(errorMessage));
    }

    static String classifyRecoverableWaitCode(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        String normalized = errorMessage.toLowerCase(Locale.ROOT);
        if (containsAny(
                normalized,
                "access to this property",
                "doesn't have access to this property",
                "does not have access to this property"
        )) {
            return WAITING_PROPERTY_ACCESS;
        }

        boolean hasReadinessHint = containsAny(normalized,
                "missing",
                "require",
                "required",
                "blank",
                "empty",
                "not found",
                "not ready",
                "cannot send",
                "invalid"
        );
        if (!hasReadinessHint) {
            return null;
        }

        if (containsAny(normalized,
                "invalid reservationid",
                "invalid reservation id",
                "invalid bookingid",
                "invalid booking id"
        )) {
            return "WAITING_THREAD_FIELDS";
        }

        if (containsAny(normalized,
                "threadid",
                "thread id",
                "thread_key",
                "thread key",
                "guestid",
                "guest id",
                "bookingid",
                "booking id",
                "reservationid",
                "reservation id",
                "hotelid",
                "hotel id",
                "channelid",
                "channel id"
        )) {
            return "WAITING_THREAD_FIELDS";
        }

        if (containsAny(normalized, "listingid", "listing id", "listing_id")) {
            return "WAITING_LISTINGID";
        }
        return null;
    }

    private static boolean containsAny(String source, String... keywords) {
        if (source == null || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record WaitState(String code, String detail) {}

    private void markWaiting(AutoMessageSendLog log, String code, String msg) {
        log.setSuccess(false);
        log.setErrorMessage(trimErr(code + ": " + msg));
        sendLogRepository.save(log);
    }

    private void markFailed(AutoMessageSendLog log, String err) {
        log.setSuccess(false);
        log.setErrorMessage(trimErr(err));
        sendLogRepository.save(log);
    }

    private static String trimErr(String err) {
        if (err == null) {
            return null;
        }
        String t = err.trim();
        if (t.length() <= MAX_LOG_MESSAGE_LEN) {
            return t;
        }
        return t.substring(0, MAX_LOG_MESSAGE_LEN);
    }
}
