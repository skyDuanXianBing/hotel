package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.Channel;
import server.demo.entity.Store;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.util.AutoMessageTemplateRenderer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SuAutoReplyService {

    private static final Logger logger = LoggerFactory.getLogger(SuAutoReplyService.class);

    public static final String ACTION_GUEST_MESSAGE = "GUEST_MESSAGE";
    private static final String TARGET_TYPE_SU_THREAD = "SU_THREAD";

    private static final String CHANNEL_CODE_BOOKING = "BOOKING";
    private static final String CHANNEL_CODE_AIRBNB = "AIRBNB";

    private final SuMessageThreadRepository threadRepository;
    private final SuMessageRepository messageRepository;
    private final AutoMessageRepository autoMessageRepository;
    private final AutoMessageSendLogRepository sendLogRepository;
    private final ChannelRepository channelRepository;
    private final StoreRepository storeRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;
    private final ObjectMapper objectMapper;

    public SuAutoReplyService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            AutoMessageRepository autoMessageRepository,
            AutoMessageSendLogRepository sendLogRepository,
            ChannelRepository channelRepository,
            StoreRepository storeRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService,
            ObjectMapper objectMapper
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.autoMessageRepository = autoMessageRepository;
        this.sendLogRepository = sendLogRepository;
        this.channelRepository = channelRepository;
        this.storeRepository = storeRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void tryAutoReply(Long storeId, Long threadId) {
        if (storeId == null || threadId == null) {
            return;
        }

        Optional<SuMessageThread> threadOpt = threadRepository.findByStoreIdAndId(storeId, threadId);
        if (threadOpt.isEmpty()) {
            return;
        }
        SuMessageThread thread = threadOpt.get();

        Integer suChannelId = thread.getChannelId();
        if (suChannelId == null || (suChannelId != SuMessagingService.CHANNEL_BOOKING && suChannelId != SuMessagingService.CHANNEL_AIRBNB)) {
            return;
        }

        if (messageRepository.existsByThread_IdAndSenderType(thread.getId(), SuMessagingSenderType.STAFF)) {
            return;
        }

        if (sendLogRepository.existsByStoreIdAndActionAndTargetTypeAndTargetId(storeId, ACTION_GUEST_MESSAGE, TARGET_TYPE_SU_THREAD, thread.getId())) {
            return;
        }

        AutoMessage template = findMatchingTemplate(storeId, suChannelId).orElse(null);
        if (template == null) {
            return;
        }

        AutoMessageSendLog log = new AutoMessageSendLog();
        log.setStoreId(storeId);
        log.setAction(ACTION_GUEST_MESSAGE);
        log.setTargetType(TARGET_TYPE_SU_THREAD);
        log.setTargetId(thread.getId());
        log.setAutoMessageId(template.getId());
        log.setSuccess(null);

        try {
            sendLogRepository.save(log);
        } catch (DataIntegrityViolationException e) {
            return;
        }

        try {
            String rendered = renderTemplate(storeId, thread, template.getMessage());
            if (rendered == null || rendered.isBlank()) {
                log.setSuccess(false);
                log.setErrorMessage("template rendered empty");
                sendLogRepository.save(log);
                return;
            }

            String formattedMessage = formatMessageForChannel(suChannelId, rendered);
            Map<String, Object> payload = buildReplyPayload(thread, formattedMessage);

            JsonNode response = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.postMessagingAB(token, payload),
                    "messagingAB"
            );

            if (!suApiClient.isSuSuccess(response)) {
                String err = suApiClient.extractSuErrorMessage(response);
                log.setSuccess(false);
                log.setErrorMessage(err != null ? err : "Su auto reply failed");
                sendLogRepository.save(log);
                return;
            }

            SuMessage msg = new SuMessage();
            msg.setStoreId(storeId);
            msg.setThread(thread);
            msg.setExternalMessageId(null);
            msg.setSenderType(SuMessagingSenderType.STAFF);
            msg.setSenderName("系统自动回复");
            msg.setContent(rendered.trim());
            msg.setSentAt(LocalDateTime.now());
            msg.setIsRead(true);
            msg.setRawJson(writeJsonSafely(payload));
            messageRepository.save(msg);

            thread.setLastMessage(rendered.trim());
            thread.setLastActivity(LocalDateTime.now());
            threadRepository.save(thread);

            log.setSuccess(true);
            log.setErrorMessage(null);
            sendLogRepository.save(log);
        } catch (Exception ex) {
            logger.error("[SuAutoReply] failed. storeId={}, threadId={}, err={}", storeId, threadId, ex.getMessage(), ex);
            log.setSuccess(false);
            log.setErrorMessage(ex.getMessage());
            sendLogRepository.save(log);
        }
    }

    private Optional<AutoMessage> findMatchingTemplate(Long storeId, int suChannelId) {
        String channelCode = suChannelId == SuMessagingService.CHANNEL_BOOKING ? CHANNEL_CODE_BOOKING : CHANNEL_CODE_AIRBNB;
        Optional<Channel> channelOpt = channelRepository.findByStoreIdAndCode(storeId, channelCode);
        if (channelOpt.isEmpty()) {
            return Optional.empty();
        }
        Long channelId = channelOpt.get().getId();

        List<AutoMessage> all = autoMessageRepository.findByStoreIdAndEnabledTrue(storeId);
        return all.stream()
                .filter(m -> ACTION_GUEST_MESSAGE.equalsIgnoreCase(nullToEmpty(m.getAction())))
                .filter(m -> matchChannel(m, channelId))
                .filter(m -> "ALL_LOCAL".equalsIgnoreCase(nullToEmpty(m.getRoomSelectionType())) || m.getRoomSelectionType() == null)
                .findFirst();
    }

    private boolean matchChannel(AutoMessage message, Long channelId) {
        if (channelId == null) {
            return false;
        }
        String channelsJson = message.getChannels();
        if (channelsJson == null || channelsJson.isBlank()) {
            return false;
        }
        try {
            Long[] ids = objectMapper.readValue(channelsJson, Long[].class);
            for (Long id : ids) {
                if (channelId.equals(id)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String renderTemplate(Long storeId, SuMessageThread thread, String template) {
        Store store = storeRepository.findById(storeId).orElse(null);

        Map<String, String> vars = new HashMap<>();
        vars.put("property_name", store != null ? nullToEmpty(store.getName()) : "");
        vars.put("property_address", store != null ? nullToEmpty(store.getAddress()) : "");
        vars.put("property_city", store != null ? nullToEmpty(store.getCity()) : "");
        vars.put("property_phone", store != null ? nullToEmpty(store.getPhone()) : "");
        vars.put("property_email", store != null ? nullToEmpty(store.getEmail()) : "");

        vars.put("guest_name", nullToEmpty(thread.getGuestName()));
        vars.put("guest_phone", "");

        vars.put("checkin_date", "");
        vars.put("checkout_date", "");
        vars.put("room_type_name", "");
        vars.put("room_type_address", "");
        vars.put("rate_plan_name", "");
        vars.put("confirmation_code", nullToEmpty(thread.getBookingId()));
        vars.put("number_of_nights", "");
        vars.put("checkin_code", "");
        vars.put("smartlock_passcode", "");
        vars.put("room_number", "");

        return AutoMessageTemplateRenderer.render(template, vars);
    }

    private static String formatMessageForChannel(int suChannelId, String message) {
        if (message == null) {
            return null;
        }
        return message.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private Map<String, Object> buildReplyPayload(SuMessageThread thread, String message) {
        String hotelId = thread.getSuHotelId();
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalStateException("缺少 hotelid，无法发送");
        }
        Integer channelId = thread.getChannelId();
        if (channelId == null) {
            throw new IllegalStateException("缺少 channelId，无法发送");
        }
        String listingId = thread.getListingId();
        if (listingId == null || listingId.isBlank()) {
            throw new IllegalStateException("缺少 listingid（渠道房源ID），无法发送");
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
                throw new IllegalStateException("Airbnb 回复需要 threadid + guestid，但当前会话缺少必要字段");
            }
            payload.put("threadid", threadId);
            payload.put("guestid", guestId);
            payload.put("bookingid", thread.getBookingId() != null ? thread.getBookingId() : threadId);
        } else if (channelId == SuMessagingService.CHANNEL_BOOKING) {
            String bookingId = thread.getBookingId();
            if (bookingId == null || bookingId.isBlank()) {
                throw new IllegalStateException("Booking.com 回复需要 bookingid，但当前会话缺少必要字段");
            }
            payload.put("bookingid", bookingId);
        } else {
            throw new IllegalStateException("不支持的渠道: " + channelId);
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
}

