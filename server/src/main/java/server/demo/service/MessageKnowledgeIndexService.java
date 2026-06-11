package server.demo.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeEntry;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.MessageKnowledgeEntryRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MessageKnowledgeIndexService {
    public static final int DEFAULT_RECENT_INDEX_DAYS = 365;
    public static final int DEFAULT_MAX_MESSAGES_PER_RUN = 500;
    private static final int MIN_RECENT_INDEX_DAYS = 1;
    private static final int MIN_MESSAGES_PER_RUN = 1;
    private static final String DELIVERY_STATUS_FAILED = "FAILED";
    private static final String DELIVERY_STATUS_SENDING = "SENDING";

    private final SuMessageRepository messageRepository;
    private final MessageKnowledgeEntryRepository knowledgeEntryRepository;
    private final SuMessagingThreadContextResolver contextResolver;
    private final SuMessagingAiRedactor redactor;
    private final SuMessagingAiTextService textService;

    public MessageKnowledgeIndexService(
            SuMessageRepository messageRepository,
            MessageKnowledgeEntryRepository knowledgeEntryRepository,
            SuMessagingThreadContextResolver contextResolver,
            SuMessagingAiRedactor redactor,
            SuMessagingAiTextService textService
    ) {
        this.messageRepository = messageRepository;
        this.knowledgeEntryRepository = knowledgeEntryRepository;
        this.contextResolver = contextResolver;
        this.redactor = redactor;
        this.textService = textService;
    }

    @Transactional
    public int indexRecentStoreMessages(Long storeId) {
        return indexRecentStoreMessages(
                storeId,
                DEFAULT_RECENT_INDEX_DAYS,
                DEFAULT_MAX_MESSAGES_PER_RUN
        );
    }

    @Transactional
    public int indexRecentStoreMessages(Long storeId, Integer lookbackDays, Integer messageLimit) {
        if (storeId == null) {
            return 0;
        }

        int effectiveLookbackDays = normalizeLookbackDays(lookbackDays);
        int effectiveMessageLimit = normalizeMessageLimit(messageLimit);
        LocalDateTime since = UtcTimeUtil.nowLocalDateTime().minusDays(effectiveLookbackDays);
        List<SuMessage> messages = messageRepository.findRecentMessagesForKnowledgeIndex(
                storeId,
                since,
                PageRequest.of(0, effectiveMessageLimit)
        );
        return indexMessages(storeId, messages);
    }

    public static int normalizeLookbackDays(Integer lookbackDays) {
        if (lookbackDays == null) {
            return DEFAULT_RECENT_INDEX_DAYS;
        }
        return Math.max(MIN_RECENT_INDEX_DAYS, Math.min(lookbackDays, DEFAULT_RECENT_INDEX_DAYS));
    }

    public static int normalizeMessageLimit(Integer messageLimit) {
        if (messageLimit == null) {
            return DEFAULT_MAX_MESSAGES_PER_RUN;
        }
        return Math.max(MIN_MESSAGES_PER_RUN, Math.min(messageLimit, DEFAULT_MAX_MESSAGES_PER_RUN));
    }

    int indexMessages(Long storeId, List<SuMessage> messages) {
        if (storeId == null || messages == null || messages.isEmpty()) {
            return 0;
        }

        List<SuMessage> sortedMessages = new ArrayList<>();
        for (SuMessage message : messages) {
            if (isIndexableMessage(storeId, message) && message.getSentAt() != null) {
                sortedMessages.add(message);
            }
        }
        sortedMessages.sort(Comparator
                .comparing((SuMessage message) -> message.getThread().getId())
                .thenComparing(SuMessage::getSentAt)
                .thenComparing(SuMessage::getId));

        int indexedCount = 0;
        Long currentThreadId = null;
        SuMessage latestGuestMessage = null;
        SuMessagingThreadContext currentContext = null;

        for (SuMessage message : sortedMessages) {
            SuMessageThread thread = message.getThread();
            Long threadId = thread == null ? null : thread.getId();
            if (threadId == null) {
                continue;
            }

            if (!threadId.equals(currentThreadId)) {
                currentThreadId = threadId;
                latestGuestMessage = null;
                currentContext = contextResolver.resolveForIndex(storeId, thread);
            }

            if (message.getSenderType() == SuMessagingSenderType.GUEST) {
                latestGuestMessage = message;
                continue;
            }

            if (message.getSenderType() == SuMessagingSenderType.STAFF
                    && latestGuestMessage != null
                    && isSuccessfulStaffReply(message)) {
                upsertEntry(storeId, currentContext, thread, latestGuestMessage, message);
                indexedCount++;
                latestGuestMessage = null;
            }
        }

        return indexedCount;
    }

    private void upsertEntry(
            Long storeId,
            SuMessagingThreadContext context,
            SuMessageThread thread,
            SuMessage guestMessage,
            SuMessage staffMessage
    ) {
        String question = redactor.redact(guestMessage.getContent());
        String answer = redactor.redact(staffMessage.getContent());
        if (question.isBlank() || answer.isBlank()) {
            return;
        }

        String normalizedText = textService.normalize(question + " " + answer);
        String normalizedHash = textService.sha256(normalizedText);
        MessageKnowledgeEntry entry = knowledgeEntryRepository
                .findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageIdAndNormalizedHash(
                        storeId,
                        guestMessage.getId(),
                        staffMessage.getId(),
                        normalizedHash
                )
                .orElseGet(MessageKnowledgeEntry::new);

        entry.setStoreId(storeId);
        entry.setScopeType(context.getBestScopeType());
        entry.setScopeId(context.getBestScopeId());
        entry.setThreadId(thread.getId());
        entry.setReservationId(context.getReservationId());
        entry.setRoomId(context.getRoomId());
        entry.setRoomNumber(context.getRoomNumber());
        entry.setRoomTypeId(context.getRoomTypeId());
        entry.setRoomTypeName(context.getRoomTypeName());
        entry.setChannelId(context.getChannelId());
        entry.setBookingKey(context.getBookingKey());
        entry.setSourceGuestMessageId(guestMessage.getId());
        entry.setSourceStaffMessageId(staffMessage.getId());
        entry.setSourceTimestamp(staffMessage.getSentAt());
        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setNormalizedText(normalizedText);
        entry.setNormalizedHash(normalizedHash);
        entry.setLanguage(detectLanguage(question));
        entry.setStatus(MessageKnowledgeEntry.STATUS_ACTIVE);
        entry.setPiiRedactionStatus(MessageKnowledgeEntry.REDACTION_STATUS_REDACTED);
        entry.setIndexedAt(UtcTimeUtil.nowLocalDateTime());
        knowledgeEntryRepository.save(entry);
    }

    private static boolean isIndexableMessage(Long storeId, SuMessage message) {
        return message != null
                && message.getId() != null
                && message.getThread() != null
                && storeId.equals(message.getStoreId())
                && message.getContent() != null
                && !message.getContent().isBlank();
    }

    private static boolean isSuccessfulStaffReply(SuMessage message) {
        String deliveryStatus = message.getDeliveryStatus();
        if (deliveryStatus == null || deliveryStatus.isBlank()) {
            return true;
        }
        return !DELIVERY_STATUS_FAILED.equalsIgnoreCase(deliveryStatus)
                && !DELIVERY_STATUS_SENDING.equalsIgnoreCase(deliveryStatus);
    }

    private static String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (text.matches(".*[\\u3040-\\u30FF].*")) {
            return "ja";
        }
        if (text.matches(".*[\\uAC00-\\uD7AF].*")) {
            return "ko";
        }
        if (text.matches(".*[\\u4E00-\\u9FFF].*")) {
            return "zh";
        }
        return "en";
    }
}
