package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;
import server.demo.dto.SuMessagingMessageDTO;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.repository.SuMessageThreadRepository;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SuAiAutoReplyService {

    private static final Logger logger = LoggerFactory.getLogger(SuAiAutoReplyService.class);

    static final String ACTION_AI_GUEST_MESSAGE = "AI_GUEST_MESSAGE";
    static final String TARGET_TYPE_SU_MESSAGE = "SU_MESSAGE";

    private static final String DEFAULT_AI_SENDER_NAME = "AI客服";
    private static final String DELIVERY_SENDING = "SENDING";
    private static final String DELIVERY_SENT = "SENT";
    private static final String DELIVERY_FAILED = "FAILED";

    private final SuMessageThreadRepository threadRepository;
    private final SuMessageRepository messageRepository;
    private final AutoMessageSendLogRepository sendLogRepository;
    private final ChatService chatService;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;
    private final SuMessagingRealtimeGateway realtimeGateway;
    private final ObjectMapper objectMapper;

    @Value("${su.messaging.ai-auto-reply.sender-name:AI客服}")
    private String senderName;

    @Value("${su.messaging.ai-auto-reply.max-input-length:1200}")
    private int maxInputLength;

    public SuAiAutoReplyService(
            SuMessageThreadRepository threadRepository,
            SuMessageRepository messageRepository,
            AutoMessageSendLogRepository sendLogRepository,
            ChatService chatService,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService,
            SuMessagingRealtimeGateway realtimeGateway,
            ObjectMapper objectMapper
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.sendLogRepository = sendLogRepository;
        this.chatService = chatService;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
        this.realtimeGateway = realtimeGateway;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void tryAutoReply(Long storeId, Long threadId, Long triggerMessageId) {
        if (storeId == null || threadId == null || triggerMessageId == null) {
            return;
        }

        if (sendLogRepository.existsByStoreIdAndActionAndTargetTypeAndTargetId(
                storeId, ACTION_AI_GUEST_MESSAGE, TARGET_TYPE_SU_MESSAGE, triggerMessageId
        )) {
            return;
        }

        SuMessageThread thread = threadRepository.findByStoreIdAndId(storeId, threadId).orElse(null);
        if (thread == null || Boolean.TRUE.equals(thread.getClosed())) {
            return;
        }

        SuMessage triggerMessage = messageRepository.findById(triggerMessageId).orElse(null);
        if (triggerMessage == null || triggerMessage.getThread() == null) {
            return;
        }
        if (!storeId.equals(triggerMessage.getStoreId()) || !threadId.equals(triggerMessage.getThread().getId())) {
            return;
        }
        if (triggerMessage.getSenderType() != SuMessagingSenderType.GUEST) {
            return;
        }

        String guestText = trimToMax(triggerMessage.getContent(), maxInputLength);
        if (guestText == null || guestText.isBlank()) {
            return;
        }

        AutoMessageSendLog log = new AutoMessageSendLog();
        log.setStoreId(storeId);
        log.setAction(ACTION_AI_GUEST_MESSAGE);
        log.setTargetType(TARGET_TYPE_SU_MESSAGE);
        log.setTargetId(triggerMessageId);
        log.setSuccess(null);

        try {
            sendLogRepository.save(log);
        } catch (DataIntegrityViolationException e) {
            return;
        }

        try {
            ChatMessageRequest chatRequest = new ChatMessageRequest();
            chatRequest.setMessage(guestText);
            chatRequest.setSessionId(buildSessionId(storeId, threadId));
            chatRequest.setUserId("store:" + storeId);

            ChatMessageResponse chatResponse = chatService.processMessage(chatRequest);
            if (chatResponse == null || !"success".equals(chatResponse.getStatus())) {
                markFailed(log, chatResponse != null ? chatResponse.getErrorMessage() : "AI response is null");
                return;
            }

            String aiReply = trimToMax(chatResponse.getReply(), 2000);
            if (aiReply == null || aiReply.isBlank()) {
                markFailed(log, "AI reply is empty");
                return;
            }

            String aiReplyContent = aiReply.trim();
            String resolvedSenderName = resolveAiSenderName(senderName);

            // 先入库“发送中”并推送，前端显示转圈
            SuMessage aiMessage = new SuMessage();
            aiMessage.setStoreId(storeId);
            aiMessage.setThread(thread);
            aiMessage.setSenderType(SuMessagingSenderType.STAFF);
            aiMessage.setSenderName(resolvedSenderName);
            aiMessage.setContent(aiReplyContent);
            aiMessage.setSentAt(UtcTimeUtil.nowLocalDateTime());
            aiMessage.setIsRead(true);
            aiMessage.setDeliveryStatus(DELIVERY_SENDING);
            aiMessage.setRawJson(null);
            aiMessage = messageRepository.saveAndFlush(aiMessage);

            thread.setLastMessage(aiReplyContent);
            thread.setLastActivity(UtcTimeUtil.nowLocalDateTime());
            threadRepository.save(thread);

            realtimeGateway.broadcastMessageCreated(storeId, threadId, toMessageDTO(aiMessage));

            Map<String, Object> payload = null;
            String suDeliverError = null;
            try {
                String channelMessage = formatMessageForChannel(thread.getChannelId(), aiReplyContent);
                payload = buildReplyPayload(thread, channelMessage);
                final Map<String, Object> requestPayload = payload;
                JsonNode suResponse = suAccessTokenService.executeWithTokenRetry(
                        token -> suApiClient.postMessagingAB(token, requestPayload),
                        "messagingAB"
                );
                if (!suApiClient.isSuSuccess(suResponse)) {
                    String err = suApiClient.extractSuErrorMessage(suResponse);
                    suDeliverError = err != null ? err : "Su AI auto reply failed";
                }
            } catch (Exception suEx) {
                suDeliverError = suEx.getMessage();
                logger.warn("[SuAiAutoReply] SU delivery failed, keep local reply. storeId={}, threadId={}, err={}",
                        storeId, threadId, suDeliverError);
            }

            aiMessage.setDeliveryStatus((suDeliverError == null || suDeliverError.isBlank()) ? DELIVERY_SENT : DELIVERY_FAILED);
            aiMessage.setRawJson(payload != null ? writeJsonSafely(payload) : null);
            aiMessage = messageRepository.save(aiMessage);

            // 再推送一次更新事件，前端把转圈改为成功/失败
            realtimeGateway.broadcastMessageUpdated(storeId, threadId, toMessageDTO(aiMessage));

            if (suDeliverError == null || suDeliverError.isBlank()) {
                log.setSuccess(true);
                log.setErrorMessage(null);
            } else {
                log.setSuccess(false);
                log.setErrorMessage("SU delivery failed, local reply saved: " + suDeliverError);
            }
            sendLogRepository.save(log);
        } catch (Exception ex) {
            logger.error("[SuAiAutoReply] failed. storeId={}, threadId={}, triggerMessageId={}, err={}",
                    storeId, threadId, triggerMessageId, ex.getMessage(), ex);
            markFailed(log, ex.getMessage());
        }
    }

    private void markFailed(AutoMessageSendLog log, String message) {
        log.setSuccess(false);
        log.setErrorMessage(message);
        sendLogRepository.save(log);
    }

    private static String buildSessionId(Long storeId, Long threadId) {
        return "su_thread_" + storeId + "_" + threadId;
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
            throw new IllegalStateException("缺少 hotelid，无法回复");
        }

        Integer channelId = thread.getChannelId();
        if (channelId == null) {
            throw new IllegalStateException("缺少 channelId，无法回复");
        }

        String listingId = thread.getListingId();
        if (listingId == null || listingId.isBlank()) {
            throw new IllegalStateException("缺少 listingid，无法回复");
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
            return payload;
        }

        if (channelId == SuMessagingService.CHANNEL_BOOKING) {
            String bookingId = thread.getBookingId();
            if (bookingId == null || bookingId.isBlank()) {
                throw new IllegalStateException("Booking.com 回复需要 bookingid，但当前会话缺少必要字段");
            }
            payload.put("bookingid", bookingId);
            return payload;
        }

        throw new IllegalStateException("不支持的渠道: " + channelId);
    }

    private String writeJsonSafely(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
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

    private static String resolveAiSenderName(String configuredSenderName) {
        if (configuredSenderName == null || configuredSenderName.isBlank()) {
            return DEFAULT_AI_SENDER_NAME;
        }
        String trimmed = configuredSenderName.trim();
        if (trimmed.contains("锟") || trimmed.contains("å") || trimmed.contains("æ") || trimmed.contains("瀹")) {
            return DEFAULT_AI_SENDER_NAME;
        }
        return trimmed;
    }
}
