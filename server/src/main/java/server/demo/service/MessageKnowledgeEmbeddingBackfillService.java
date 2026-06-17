package server.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import server.demo.entity.MessageKnowledgeItem;
import server.demo.repository.MessageKnowledgeItemRepository;
import server.demo.util.UtcTimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageKnowledgeEmbeddingBackfillService {
    private static final int DEFAULT_BATCH_SIZE = 1;
    private static final int MIN_BATCH_SIZE = 1;
    private static final int MAX_BATCH_SIZE = 3;
    private static final int DEFAULT_STORES_PER_RUN = 1;
    private static final int MAX_STORES_PER_RUN = 3;
    private static final long DEFAULT_LEASE_MS = 300_000L;
    private static final long DEFAULT_FAILURE_BACKOFF_MS = 900_000L;
    private static final int MAX_ERROR_LENGTH = 500;
    private static final List<String> DUE_EMBEDDING_STATUSES = List.of(
            MessageKnowledgeItem.EMBEDDING_STATUS_PENDING,
            MessageKnowledgeItem.EMBEDDING_STATUS_STALE,
            MessageKnowledgeItem.EMBEDDING_STATUS_FAILED
    );

    private final MessageKnowledgeItemRepository itemRepository;
    private final MessageKnowledgeEmbeddingProvider embeddingProvider;
    private final MessageKnowledgeEmbeddingTextService embeddingTextService;
    private final ObjectMapper objectMapper;
    private final String leaseOwner = "message-knowledge-embedding-" + UUID.randomUUID();

    @Value("${messaging.knowledge.embedding.backfill.enabled:false}")
    private boolean enabled;

    @Value("${messaging.knowledge.embedding.backfill.batch-size:1}")
    private int batchSize;

    @Value("${messaging.knowledge.embedding.backfill.stores-per-run:1}")
    private int storesPerRun;

    @Value("${messaging.knowledge.embedding.backfill.max-calls-per-run:0}")
    private int maxCallsPerRun;

    @Value("${messaging.knowledge.embedding.backfill.lease-ms:300000}")
    private long leaseMs;

    @Value("${messaging.knowledge.embedding.backfill.failure-backoff-ms:900000}")
    private long failureBackoffMs;

    public MessageKnowledgeEmbeddingBackfillService(
            MessageKnowledgeItemRepository itemRepository,
            MessageKnowledgeEmbeddingProvider embeddingProvider,
            MessageKnowledgeEmbeddingTextService embeddingTextService,
            ObjectMapper objectMapper
    ) {
        this.itemRepository = itemRepository;
        this.embeddingProvider = embeddingProvider;
        this.embeddingTextService = embeddingTextService;
        this.objectMapper = objectMapper;
    }

    public MessageKnowledgeEmbeddingBackfillResult runOnce() {
        BackfillAvailability availability = checkAvailability();
        if (availability.skippedReason() != null) {
            return MessageKnowledgeEmbeddingBackfillResult.skipped(availability.skippedReason());
        }

        LocalDateTime now = UtcTimeUtil.nowLocalDateTime();
        List<Long> storeIds = itemRepository.findDueEmbeddingStoreIds(
                MessageKnowledgeItem.STATUS_ACTIVE,
                DUE_EMBEDDING_STATUSES,
                now,
                PageRequest.of(0, normalizeStoresPerRun(storesPerRun))
        );
        if (storeIds.isEmpty()) {
            return MessageKnowledgeEmbeddingBackfillResult.skipped("no due embedding stores");
        }

        MessageKnowledgeEmbeddingBackfillResult result =
                new MessageKnowledgeEmbeddingBackfillResult(0, 0, 0, null);
        for (Long storeId : storeIds) {
            int remainingCalls = availability.maxCalls() - result.attempted();
            if (remainingCalls <= 0) {
                break;
            }
            result = result.plus(runOnceForStoreInternal(storeId, remainingCalls, now));
        }
        return result;
    }

    public MessageKnowledgeEmbeddingBackfillResult runOnceForStore(Long storeId) {
        if (storeId == null) {
            return MessageKnowledgeEmbeddingBackfillResult.skipped("store id is required");
        }
        BackfillAvailability availability = checkAvailability();
        if (availability.skippedReason() != null) {
            return MessageKnowledgeEmbeddingBackfillResult.skipped(availability.skippedReason());
        }
        return runOnceForStoreInternal(storeId, availability.maxCalls(), UtcTimeUtil.nowLocalDateTime());
    }

    public static int normalizeBatchSize(Integer value) {
        if (value == null) {
            return DEFAULT_BATCH_SIZE;
        }
        if (value < MIN_BATCH_SIZE) {
            return MIN_BATCH_SIZE;
        }
        return Math.min(value, MAX_BATCH_SIZE);
    }

    public static int normalizeStoresPerRun(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_STORES_PER_RUN;
        }
        return Math.min(value, MAX_STORES_PER_RUN);
    }

    public static int normalizeMaxCallsPerRun(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return value;
    }

    private MessageKnowledgeEmbeddingBackfillResult runOnceForStoreInternal(
            Long storeId,
            int remainingCalls,
            LocalDateTime now
    ) {
        int pageSize = Math.min(normalizeBatchSize(batchSize), remainingCalls);
        if (pageSize < 1) {
            return MessageKnowledgeEmbeddingBackfillResult.skipped("max embedding calls reached");
        }
        List<MessageKnowledgeItem> candidates = itemRepository.findEmbeddingBackfillCandidates(
                storeId,
                MessageKnowledgeItem.STATUS_ACTIVE,
                DUE_EMBEDDING_STATUSES,
                now,
                PageRequest.of(0, pageSize)
        );
        if (candidates.isEmpty()) {
            return MessageKnowledgeEmbeddingBackfillResult.skipped("no due embedding items");
        }

        int attempted = 0;
        int succeeded = 0;
        int failed = 0;
        for (MessageKnowledgeItem item : candidates) {
            if (attempted >= remainingCalls) {
                break;
            }
            if (item == null || isLeaseActive(item, now)) {
                continue;
            }
            attempted++;
            boolean embedded = processItem(item, now);
            if (embedded) {
                succeeded++;
            } else {
                failed++;
            }
        }
        return new MessageKnowledgeEmbeddingBackfillResult(attempted, succeeded, failed, null);
    }

    private boolean processItem(MessageKnowledgeItem item, LocalDateTime now) {
        String input = item.getSemanticText();
        if (!hasText(input)) {
            embeddingTextService.refreshSemanticFields(item, false);
            input = item.getSemanticText();
        }
        if (!hasText(input)) {
            markFailed(item, "semantic_text is empty", now);
            itemRepository.save(item);
            return false;
        }

        String inputHash = embeddingTextService.inputHash(input);
        if (MessageKnowledgeItem.EMBEDDING_STATUS_READY.equals(item.getEmbeddingStatus())
                && inputHash.equals(item.getEmbeddingInputHash())) {
            clearLease(item);
            itemRepository.save(item);
            return true;
        }

        claimLease(item, now);
        itemRepository.save(item);

        try {
            MessageKnowledgeEmbeddingResponse response = embeddingProvider.embed(input);
            markReady(item, response, inputHash, now);
            itemRepository.save(item);
            return true;
        } catch (RuntimeException e) {
            markFailed(item, e.getMessage(), now);
            itemRepository.save(item);
            return false;
        }
    }

    private BackfillAvailability checkAvailability() {
        if (!enabled) {
            return new BackfillAvailability(0, "embedding backfill disabled");
        }
        int maxCalls = normalizeMaxCallsPerRun(maxCallsPerRun);
        if (maxCalls < 1) {
            return new BackfillAvailability(0, "embedding max calls per run is zero");
        }
        if (!embeddingProvider.isEnabled()) {
            return new BackfillAvailability(0, "embedding provider disabled");
        }
        return new BackfillAvailability(maxCalls, null);
    }

    private void claimLease(MessageKnowledgeItem item, LocalDateTime now) {
        int attempts = item.getEmbeddingAttemptCount() == null ? 0 : item.getEmbeddingAttemptCount();
        item.setEmbeddingAttemptCount(attempts + 1);
        item.setEmbeddingLeaseOwner(leaseOwner);
        item.setEmbeddingLeaseUntil(now.plus(Duration.ofMillis(normalizePositiveMillis(leaseMs, DEFAULT_LEASE_MS))));
    }

    private void markReady(
            MessageKnowledgeItem item,
            MessageKnowledgeEmbeddingResponse response,
            String inputHash,
            LocalDateTime now
    ) {
        validateResponse(response);
        item.setEmbeddingVector(toVectorJson(response.vector()));
        item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_READY);
        item.setEmbeddingProvider(response.provider());
        item.setEmbeddingModel(response.model());
        item.setEmbeddingDimensions(response.dimensions());
        item.setEmbeddingInputHash(inputHash);
        item.setEmbeddingError(null);
        item.setEmbeddingUpdatedAt(now);
        item.setEmbeddingNextAttemptAt(null);
        clearLease(item);
    }

    private void markFailed(MessageKnowledgeItem item, String errorMessage, LocalDateTime now) {
        item.setEmbeddingStatus(MessageKnowledgeItem.EMBEDDING_STATUS_FAILED);
        item.setEmbeddingError(truncateError(errorMessage));
        item.setEmbeddingUpdatedAt(now);
        item.setEmbeddingNextAttemptAt(
                now.plus(Duration.ofMillis(normalizePositiveMillis(failureBackoffMs, DEFAULT_FAILURE_BACKOFF_MS)))
        );
        clearLease(item);
    }

    private void clearLease(MessageKnowledgeItem item) {
        item.setEmbeddingLeaseOwner(null);
        item.setEmbeddingLeaseUntil(null);
    }

    private boolean isLeaseActive(MessageKnowledgeItem item, LocalDateTime now) {
        LocalDateTime leaseUntil = item.getEmbeddingLeaseUntil();
        return leaseUntil != null && leaseUntil.isAfter(now);
    }

    private void validateResponse(MessageKnowledgeEmbeddingResponse response) {
        if (response == null || response.vector() == null || response.vector().isEmpty()) {
            throw new IllegalStateException("Embedding provider returned an empty vector");
        }
    }

    private String toVectorJson(List<Float> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize embedding vector", e);
        }
    }

    private static long normalizePositiveMillis(long value, long fallback) {
        if (value < 1) {
            return fallback;
        }
        return value;
    }

    private static String truncateError(String value) {
        if (!hasText(value)) {
            return "Embedding provider failed";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= MAX_ERROR_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_ERROR_LENGTH);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record BackfillAvailability(
            int maxCalls,
            String skippedReason
    ) {
    }
}
