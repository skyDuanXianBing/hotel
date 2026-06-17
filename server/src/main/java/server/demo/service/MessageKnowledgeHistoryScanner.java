package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import server.demo.entity.MessageKnowledgeScanState;
import server.demo.entity.SuMessage;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.MessageKnowledgeScanStateRepository;
import server.demo.repository.SuMessageRepository;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MessageKnowledgeHistoryScanner {
    private static final Logger logger = LoggerFactory.getLogger(MessageKnowledgeHistoryScanner.class);

    public static final int DEFAULT_BATCH_SIZE = 1;
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 3;
    public static final int DEFAULT_STORES_PER_RUN = 1;
    public static final int MIN_STORES_PER_RUN = 1;
    public static final int MAX_STORES_PER_RUN = 1;
    private static final long DEFAULT_LEASE_MS = 300_000L;
    private static final long DEFAULT_SUCCESS_DELAY_MS = 60_000L;
    private static final long DEFAULT_IDLE_DELAY_MS = 3_600_000L;
    private static final long DEFAULT_FAILURE_BACKOFF_MS = 900_000L;
    private static final int ERROR_MAX_LENGTH = 500;

    private final SuMessageRepository messageRepository;
    private final MessageKnowledgeScanStateRepository scanStateRepository;
    private final MessageKnowledgeRefinementService refinementService;
    private final TransactionTemplate transactionTemplate;
    private final String leaseOwner = "message-knowledge-scanner-" + UUID.randomUUID();

    @Value("${messaging.knowledge.refined-scanner.batch-size:1}")
    private int batchSize;

    @Value("${messaging.knowledge.refined-scanner.stores-per-run:1}")
    private int storesPerRun;

    @Value("${messaging.knowledge.refined-scanner.lease-ms:300000}")
    private long leaseMs;

    @Value("${messaging.knowledge.refined-scanner.success-delay-ms:60000}")
    private long successDelayMs;

    @Value("${messaging.knowledge.refined-scanner.idle-delay-ms:3600000}")
    private long idleDelayMs;

    @Value("${messaging.knowledge.refined-scanner.failure-backoff-ms:900000}")
    private long failureBackoffMs;

    public MessageKnowledgeHistoryScanner(
            SuMessageRepository messageRepository,
            MessageKnowledgeScanStateRepository scanStateRepository,
            MessageKnowledgeRefinementService refinementService,
            PlatformTransactionManager transactionManager
    ) {
        this.messageRepository = messageRepository;
        this.scanStateRepository = scanStateRepository;
        this.refinementService = refinementService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int scanOnce() {
        int effectiveStoresPerRun = normalizeStoresPerRun(storesPerRun);
        int extractedCount = 0;
        for (int index = 0; index < effectiveStoresPerRun; index++) {
            Optional<Long> stateId = transactionTemplate.execute(status -> claimNextState());
            if (stateId == null || stateId.isEmpty()) {
                return extractedCount;
            }
            extractedCount += processClaimedStateSafely(stateId.get());
        }
        return extractedCount;
    }

    public static int normalizeBatchSize(Integer value) {
        if (value == null) {
            return DEFAULT_BATCH_SIZE;
        }
        return Math.max(MIN_BATCH_SIZE, Math.min(value, MAX_BATCH_SIZE));
    }

    public static int normalizeStoresPerRun(Integer value) {
        if (value == null) {
            return DEFAULT_STORES_PER_RUN;
        }
        return Math.max(MIN_STORES_PER_RUN, Math.min(value, MAX_STORES_PER_RUN));
    }

    private Optional<Long> claimNextState() {
        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        Optional<MessageKnowledgeScanState> state = scanStateRepository.findNextDueStateForUpdate(now);
        if (state.isEmpty()) {
            initializeOneUntrackedStore(now);
            state = scanStateRepository.findNextDueStateForUpdate(now);
        }
        if (state.isEmpty()) {
            return Optional.empty();
        }

        MessageKnowledgeScanState scanState = state.get();
        scanState.setStatus(MessageKnowledgeScanState.STATUS_RUNNING);
        scanState.setLeaseOwner(leaseOwner);
        scanState.setLeaseUntil(now.plusNanos(resolveLeaseMs() * 1_000_000L));
        scanState.setLastStartedAt(now);
        scanState.setLastError(null);
        scanStateRepository.save(scanState);
        return Optional.of(scanState.getId());
    }

    private void initializeOneUntrackedStore(LocalDateTime now) {
        List<Long> storeIds = messageRepository.findStoreIdsWithoutKnowledgeScanState(PageRequest.of(0, 1));
        if (storeIds == null || storeIds.isEmpty()) {
            return;
        }
        Long storeId = storeIds.get(0);
        if (storeId == null) {
            return;
        }
        scanStateRepository.insertStateIfAbsent(storeId, now);
    }

    private int processClaimedStateSafely(Long stateId) {
        try {
            Integer extractedCount = transactionTemplate.execute(status -> processClaimedState(stateId));
            if (extractedCount == null) {
                return 0;
            }
            return extractedCount;
        } catch (Exception e) {
            logger.warn("Refined message knowledge scan failed for stateId={}: {}", stateId, e.getMessage(), e);
            transactionTemplate.executeWithoutResult(status -> recordFailure(stateId, e));
            return 0;
        }
    }

    private int processClaimedState(Long stateId) {
        Optional<MessageKnowledgeScanState> optionalState = scanStateRepository.findById(stateId);
        if (optionalState.isEmpty()) {
            return 0;
        }

        MessageKnowledgeScanState state = optionalState.get();
        if (!leaseOwner.equals(state.getLeaseOwner())) {
            return 0;
        }

        int effectiveBatchSize = normalizeBatchSize(batchSize);
        Long storeId = state.getStoreId();
        Long cursor = state.getLastScannedMessageId();
        if (cursor == null) {
            cursor = 0L;
        }
        List<SuMessage> messages = messageRepository.findHistoryBatchForKnowledgeScanner(
                storeId,
                cursor,
                PageRequest.of(0, effectiveBatchSize)
        );

        ScanResult result = processMessages(storeId, messages);
        markSuccess(state, result, effectiveBatchSize);
        scanStateRepository.save(state);
        return result.extractedCount;
    }

    private ScanResult processMessages(Long storeId, List<SuMessage> messages) {
        ScanResult result = new ScanResult();
        if (storeId == null || messages == null || messages.isEmpty()) {
            return result;
        }

        for (SuMessage message : messages) {
            if (message == null || message.getId() == null) {
                continue;
            }
            result.processedCount++;
            result.lastMessageId = Math.max(result.lastMessageId, message.getId());
            if (!isCandidateStaffReply(storeId, message)) {
                continue;
            }

            SuMessageThread thread = message.getThread();
            Long threadId = thread.getId();
            List<SuMessage> guests = messageRepository.findPreviousMessageBySenderForKnowledgeScanner(
                    storeId,
                    threadId,
                    SuMessagingSenderType.GUEST,
                    message.getId(),
                    PageRequest.of(0, 1)
            );
            if (guests == null || guests.isEmpty()) {
                continue;
            }

            SuMessage guestMessage = guests.get(0);
            boolean hasEarlierSuccessfulStaffReply = messageRepository.existsSuccessfulStaffReplyBetween(
                    storeId,
                    threadId,
                    SuMessagingSenderType.STAFF,
                    guestMessage.getId(),
                    message.getId()
            );
            if (hasEarlierSuccessfulStaffReply) {
                continue;
            }

            boolean extracted = refinementService.refineSourcePair(storeId, thread, guestMessage, message);
            if (extracted) {
                result.extractedCount++;
            }
        }

        return result;
    }

    private static boolean isCandidateStaffReply(Long storeId, SuMessage message) {
        if (storeId == null || message == null || message.getThread() == null) {
            return false;
        }
        if (!storeId.equals(message.getStoreId())) {
            return false;
        }
        if (message.getSenderType() != SuMessagingSenderType.STAFF) {
            return false;
        }
        if (message.getContent() == null || message.getContent().isBlank()) {
            return false;
        }
        String deliveryStatus = message.getDeliveryStatus();
        if (deliveryStatus == null || deliveryStatus.isBlank()) {
            return true;
        }
        return !"FAILED".equalsIgnoreCase(deliveryStatus)
                && !"SENDING".equalsIgnoreCase(deliveryStatus);
    }

    private void markSuccess(
            MessageKnowledgeScanState state,
            ScanResult result,
            int effectiveBatchSize
    ) {
        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        if (result.lastMessageId > 0) {
            state.setLastScannedMessageId(result.lastMessageId);
        }
        state.setStatus(MessageKnowledgeScanState.STATUS_IDLE);
        state.setLeaseOwner(null);
        state.setLeaseUntil(null);
        state.setBackoffUntil(null);
        state.setFailureCount(0);
        state.setLastError(null);
        state.setLastFinishedAt(now);
        state.setProcessedMessageCount(addLong(state.getProcessedMessageCount(), result.processedCount));
        state.setExtractedPairCount(addLong(state.getExtractedPairCount(), result.extractedCount));

        long delayMs = resolveSuccessDelayMs();
        if (result.processedCount < effectiveBatchSize) {
            delayMs = resolveIdleDelayMs();
        }
        state.setNextScanAfter(now.plusNanos(delayMs * 1_000_000L));
    }

    private void recordFailure(Long stateId, Exception e) {
        Optional<MessageKnowledgeScanState> optionalState = scanStateRepository.findById(stateId);
        if (optionalState.isEmpty()) {
            return;
        }

        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        MessageKnowledgeScanState state = optionalState.get();
        int failureCount = state.getFailureCount() == null ? 0 : state.getFailureCount();
        state.setStatus(MessageKnowledgeScanState.STATUS_FAILED);
        state.setLeaseOwner(null);
        state.setLeaseUntil(null);
        state.setFailureCount(failureCount + 1);
        state.setLastError(truncateError(e.getMessage()));
        state.setBackoffUntil(now.plusNanos(resolveFailureBackoffMs() * 1_000_000L));
        state.setNextScanAfter(now.plusNanos(resolveFailureBackoffMs() * 1_000_000L));
        state.setLastFinishedAt(now);
        scanStateRepository.save(state);
    }

    private long resolveLeaseMs() {
        return Math.max(1L, valueOrDefault(leaseMs, DEFAULT_LEASE_MS));
    }

    private long resolveSuccessDelayMs() {
        return Math.max(1L, valueOrDefault(successDelayMs, DEFAULT_SUCCESS_DELAY_MS));
    }

    private long resolveIdleDelayMs() {
        return Math.max(1L, valueOrDefault(idleDelayMs, DEFAULT_IDLE_DELAY_MS));
    }

    private long resolveFailureBackoffMs() {
        return Math.max(1L, valueOrDefault(failureBackoffMs, DEFAULT_FAILURE_BACKOFF_MS));
    }

    private static long valueOrDefault(long value, long defaultValue) {
        if (value <= 0L) {
            return defaultValue;
        }
        return value;
    }

    private static Long addLong(Long current, long delta) {
        long base = 0L;
        if (current != null) {
            base = current;
        }
        return base + delta;
    }

    private static String truncateError(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= ERROR_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, ERROR_MAX_LENGTH);
    }

    private static final class ScanResult {
        private int processedCount;
        private int extractedCount;
        private long lastMessageId;
    }
}
