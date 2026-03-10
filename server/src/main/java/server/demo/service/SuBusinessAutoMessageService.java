package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;
import server.demo.dto.SuMessagingSendRequest;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.Channel;
import server.demo.entity.Reservation;
import server.demo.entity.Room;
import server.demo.entity.RoomType;
import server.demo.entity.Store;
import server.demo.entity.SuMessageThread;
import server.demo.enums.ReservationStatus;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.RoomGroupMemberRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.util.AutoMessageTemplateRenderer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
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

    private final AutoMessageSendLogRepository sendLogRepository;
    private final StoreRepository storeRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomGroupMemberRepository roomGroupMemberRepository;
    private final SuMessageThreadRepository threadRepository;
    private final SuMessagingService suMessagingService;
    private final RegistrationLinkService registrationLinkService;
    private final String serverBaseUrl;

    public SuBusinessAutoMessageService(
            AutoMessageSendLogRepository sendLogRepository,
            StoreRepository storeRepository,
            RoomTypeRepository roomTypeRepository,
            RoomGroupMemberRepository roomGroupMemberRepository,
            SuMessageThreadRepository threadRepository,
            SuMessagingService suMessagingService,
            RegistrationLinkService registrationLinkService,
            @Value("${server.base-url}") String serverBaseUrl
    ) {
        this.sendLogRepository = sendLogRepository;
        this.storeRepository = storeRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomGroupMemberRepository = roomGroupMemberRepository;
        this.threadRepository = threadRepository;
        this.suMessagingService = suMessagingService;
        this.registrationLinkService = registrationLinkService;
        this.serverBaseUrl = serverBaseUrl;
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
                markWaiting(log, "WAITING_THREAD", "未找到会话(thread)，暂不发送；等待 Su messaging webhook 入库后自动重试");
                autoMessageLogger.info("[AutoMessage] waiting thread. storeId={}, reservationId={}, autoMessageId={}, channelId={}",
                        storeId, reservation.getId(), template.getId(), reservation.getChannel() != null ? reservation.getChannel().getId() : null);
                return;
            }

            if (thread.getListingId() == null || thread.getListingId().isBlank()) {
                markWaiting(log, "WAITING_LISTINGID", "会话缺少 listingid，暂不发送；等待下一次 webhook 补齐");
                autoMessageLogger.info("[AutoMessage] waiting listingid. storeId={}, reservationId={}, autoMessageId={}, threadId={}",
                        storeId, reservation.getId(), template.getId(), thread.getId());
                return;
            }
            
            if (thread.getChannelId() != null && thread.getChannelId() == SuMessagingService.CHANNEL_AIRBNB) {
                String threadId = thread.getThreadId();
                String guestId = thread.getGuestId();
                if (threadId == null || threadId.isBlank() || guestId == null || guestId.isBlank()) {
                    markWaiting(log, "WAITING_THREAD_FIELDS", "Airbnb 会话缺少 threadid/guestid，暂不发送；等待下一次 webhook 补齐");
                    autoMessageLogger.info("[AutoMessage] waiting airbnb thread fields. storeId={}, reservationId={}, autoMessageId={}, threadDbId={} ",
                            storeId, reservation.getId(), template.getId(), thread.getId());
                    return;
                }
            }

            String rendered = renderTemplate(store, reservation, template.getMessage());
            if (rendered == null || rendered.isBlank()) {
                markFailed(log, "template rendered empty");
                return;
            }

            SuMessagingSendRequest req = new SuMessagingSendRequest();
            req.setContent(rendered.trim());
            req.setSenderName("系统自动消息");

            suMessagingService.sendMessage(storeId, thread.getId(), req);

            log.setSuccess(true);
            log.setErrorMessage(null);
            sendLogRepository.save(log);

                autoMessageLogger.info("[AutoMessage] sent ok. storeId={}, reservationId={}, autoMessageId={}, action={}, sendTiming={}",
                    storeId, reservation.getId(), template.getId(), template.getAction(), template.getSendTiming());
        } catch (Exception e) {
            logger.warn("[AutoMessage] send failed. storeId={}, reservationId={}, autoMessageId={}, err={}",
                    storeId, reservation.getId(), template.getId(), e.getMessage(), e);
                autoMessageLogger.error("[AutoMessage] send failed. storeId={}, reservationId={}, autoMessageId={}, err={}",
                    storeId, reservation.getId(), template.getId(), e.getMessage());
            markFailed(log, e.getMessage());
        }
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
        return createdAt.isAfter(hardCap) ? createdAt : hardCap;
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

        String bookingId = reservation.getChannelOrderNumber();
        if (bookingId == null || bookingId.isBlank()) {
            bookingId = reservation.getOrderNumber();
        }
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
            String listingId = reservation.getOtaRoomId();
            if ((thread.getListingId() == null || thread.getListingId().isBlank())
                    && listingId != null && !listingId.isBlank()) {
                thread.setListingId(listingId);
                changed = true;
            }
            if ((thread.getGuestName() == null || thread.getGuestName().isBlank())
                    && reservation.getGuestName() != null && !reservation.getGuestName().isBlank()) {
                thread.setGuestName(reservation.getGuestName());
                changed = true;
            }
            if (changed) {
                thread.setLastActivity(LocalDateTime.now());
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

        String threadKey = normalizedBookingId;
        String suHotelId = resolveSuHotelId(reservation, store);
        if (suHotelId == null || suHotelId.isBlank()) {
            return null;
        }

        String listingId = reservation.getOtaRoomId();
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
            thread.setLastActivity(LocalDateTime.now());
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
        vars.put("rate_plan_name", "");
        vars.put("confirmation_code", reservation != null ? nullToEmpty(reservation.getChannelOrderNumber()) : "");

        vars.put("order_number", reservation != null ? nullToEmpty(reservation.getOrderNumber()) : "");
        String registrationLink = buildRegistrationLink(reservation);
        vars.put("registration_link", registrationLink);
        vars.put("checkin_form_link", registrationLink);

        vars.put("number_of_nights", resolveNights(reservation));
        vars.put("checkin_code", "");
        vars.put("smartlock_passcode", "");
        vars.put("room_number", resolveRoomNumber(reservation));

        return vars;
    }

    private String buildRegistrationLink(Reservation reservation) {
        if (reservation == null || reservation.getStoreId() == null) {
            return "";
        }

        String bookingKey = reservation.getChannelOrderNumber();
        if (bookingKey == null || bookingKey.isBlank()) {
            bookingKey = reservation.getOrderNumber();
        }
        if (bookingKey == null || bookingKey.isBlank()) {
            return "";
        }

        String token = registrationLinkService.generateToken(reservation.getStoreId(), bookingKey);
        String base = serverBaseUrl != null ? serverBaseUrl.trim() : "";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String encodedKey = UriUtils.encodePathSegment(bookingKey, StandardCharsets.UTF_8);
        return base + "/rb/" + encodedKey + "?t=" + token;
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
