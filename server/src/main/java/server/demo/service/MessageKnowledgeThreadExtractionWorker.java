package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import server.demo.repository.SuMessageThreadRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MessageKnowledgeThreadExtractionWorker {
    private static final Logger logger = LoggerFactory.getLogger(MessageKnowledgeThreadExtractionWorker.class);

    private static final int DEFAULT_CLAIM_SIZE = 3;
    private static final int DEFAULT_MAX_CONCURRENCY = 3;
    private static final int MAX_CONCURRENCY_LIMIT = 3;
    private static final int MAX_CLAIM_SIZE = 50;
    private static final long DEFAULT_LEASE_MS = 600_000L;
    private static final long DEFAULT_FAILURE_BACKOFF_MS = 900_000L;
    private static final int ERROR_MAX_LENGTH = 500;

    private final SuMessageThreadRepository threadRepository;
    private final MessageKnowledgeThreadExtractionService extractionService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final AtomicInteger runningCount = new AtomicInteger(0);
    private final AtomicBoolean dispatching = new AtomicBoolean(false);
    private final AtomicLong runSequence = new AtomicLong(0L);
    private final String leaseOwnerPrefix = "message-knowledge-thread-worker-" + UUID.randomUUID();

    @Value("${messaging.knowledge.thread-scheduler.enabled:${messaging.knowledge.thread-extractor.enabled:false}}")
    private boolean enabled;

    @Value("${messaging.knowledge.thread-scheduler.claim-size:3}")
    private int claimSize;

    @Value("${messaging.knowledge.thread-scheduler.max-concurrency:3}")
    private int maxConcurrency;

    @Value("${messaging.knowledge.thread-scheduler.lease-ms:600000}")
    private long leaseMs;

    @Value("${messaging.knowledge.thread-scheduler.failure-backoff-ms:900000}")
    private long failureBackoffMs;

    public MessageKnowledgeThreadExtractionWorker(
            SuMessageThreadRepository threadRepository,
            MessageKnowledgeThreadExtractionService extractionService,
            PlatformTransactionManager transactionManager,
            Clock clock
    ) {
        this.threadRepository = threadRepository;
        this.extractionService = extractionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public int dispatchDueThreads() {
        if (!isEnabled()) {
            return 0;
        }
        if (!dispatching.compareAndSet(false, true)) {
            logger.info("Thread message knowledge scheduler skipped because dispatch is still active");
            return 0;
        }

        try {
            return dispatchDueThreadsLocked();
        } finally {
            dispatching.set(false);
        }
    }

    int runningCount() {
        return runningCount.get();
    }

    void processClaimedThread(Long storeId, Long threadId, Long dirtyMessageId, String leaseOwner) {
        if (!extractionService.isEnabled()) {
            recordFailure(storeId, threadId, leaseOwner, "Thread extractor disabled");
            return;
        }

        try {
            MessageKnowledgeThreadExtractionSummary summary = extractionService.extractThread(
                    storeId,
                    threadId,
                    dirtyMessageId,
                    leaseOwner,
                    leaseOwner
            );
            if (!"COMPLETED".equalsIgnoreCase(summary.status())) {
                recordFailure(storeId, threadId, leaseOwner, "Thread extraction status: " + summary.status());
                return;
            }
            completeSuccessfulExtraction(summary, leaseOwner);
        } catch (Exception e) {
            logger.warn(
                    "Thread message knowledge extraction failed. storeId={}, threadId={}, dirtyMessageId={}, error={}",
                    storeId,
                    threadId,
                    dirtyMessageId,
                    e.getMessage(),
                    e
            );
            recordFailure(storeId, threadId, leaseOwner, e.getMessage());
        }
    }

    private int dispatchDueThreadsLocked() {
        int limit = resolveDispatchLimit();
        if (limit < 1) {
            return 0;
        }

        LocalDateTime now = nowUtc();
        List<SuMessageThreadRepository.KnowledgeDueThreadRow> dueThreads =
                threadRepository.findDueKnowledgeThreads(now, PageRequest.of(0, limit));
        if (dueThreads == null || dueThreads.isEmpty()) {
            return 0;
        }

        int launched = 0;
        for (SuMessageThreadRepository.KnowledgeDueThreadRow row : dueThreads) {
            if (row == null || runningCount.get() >= resolveMaxConcurrency()) {
                break;
            }
            Long storeId = row.getStoreId();
            Long threadId = row.getThreadId();
            Long dirtyMessageId = row.getDirtyMessageId();
            if (!hasPositiveId(storeId) || !hasPositiveId(threadId) || !hasPositiveId(dirtyMessageId)) {
                continue;
            }

            String leaseOwner = nextLeaseOwner();
            if (!claimThread(storeId, threadId, leaseOwner)) {
                continue;
            }

            runningCount.incrementAndGet();
            launched++;
            CompletableFuture.runAsync(() -> {
                try {
                    processClaimedThread(storeId, threadId, dirtyMessageId, leaseOwner);
                } finally {
                    runningCount.decrementAndGet();
                }
            });
        }
        return launched;
    }

    private boolean claimThread(Long storeId, Long threadId, String leaseOwner) {
        Boolean claimed = transactionTemplate.execute(status -> {
            LocalDateTime now = nowUtc();
            LocalDateTime leaseUntil = now.plus(Duration.ofMillis(resolveLeaseMs()));
            int updated = threadRepository.claimDueKnowledgeThread(
                    storeId,
                    threadId,
                    now,
                    leaseOwner,
                    leaseUntil
            );
            return updated == 1;
        });
        return Boolean.TRUE.equals(claimed);
    }

    private void completeSuccessfulExtraction(
            MessageKnowledgeThreadExtractionSummary summary,
            String leaseOwner
    ) {
        if (summary == null || !hasPositiveId(summary.coveredUntilMessageId())) {
            recordFailure(
                    summary != null ? summary.storeId() : null,
                    summary != null ? summary.threadId() : null,
                    leaseOwner,
                    "Missing covered message id"
            );
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime now = nowUtc();
            int completed = threadRepository.completeKnowledgeExtractionIfCurrent(
                    summary.storeId(),
                    summary.threadId(),
                    leaseOwner,
                    summary.coveredUntilMessageId(),
                    now,
                    MessageKnowledgeThreadKnowledgeWriter.EXTRACTOR_VERSION
            );
            if (completed == 1) {
                return;
            }
            threadRepository.releaseKnowledgeExtractionForStaleDirty(
                    summary.storeId(),
                    summary.threadId(),
                    leaseOwner,
                    summary.coveredUntilMessageId(),
                    now,
                    MessageKnowledgeThreadKnowledgeWriter.EXTRACTOR_VERSION
            );
        });
    }

    private void recordFailure(Long storeId, Long threadId, String leaseOwner, String error) {
        if (!hasPositiveId(storeId) || !hasPositiveId(threadId) || leaseOwner == null || leaseOwner.isBlank()) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime now = nowUtc();
            threadRepository.failKnowledgeExtractionForRetry(
                    storeId,
                    threadId,
                    leaseOwner,
                    now,
                    now.plus(Duration.ofMillis(resolveFailureBackoffMs())),
                    truncateError(error)
            );
        });
    }

    private int resolveDispatchLimit() {
        int available = resolveMaxConcurrency() - runningCount.get();
        if (available < 1) {
            return 0;
        }
        return Math.min(resolveClaimSize(), available);
    }

    private boolean isEnabled() {
        return enabled && extractionService.isEnabled();
    }

    private int resolveClaimSize() {
        if (claimSize < 1) {
            return DEFAULT_CLAIM_SIZE;
        }
        return Math.min(claimSize, MAX_CLAIM_SIZE);
    }

    private int resolveMaxConcurrency() {
        if (maxConcurrency < 1) {
            return DEFAULT_MAX_CONCURRENCY;
        }
        return Math.min(maxConcurrency, MAX_CONCURRENCY_LIMIT);
    }

    private long resolveLeaseMs() {
        if (leaseMs < 1L) {
            return DEFAULT_LEASE_MS;
        }
        return leaseMs;
    }

    private long resolveFailureBackoffMs() {
        if (failureBackoffMs < 1L) {
            return DEFAULT_FAILURE_BACKOFF_MS;
        }
        return failureBackoffMs;
    }

    private String nextLeaseOwner() {
        return leaseOwnerPrefix + "-" + runSequence.incrementAndGet();
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static boolean hasPositiveId(Long value) {
        return value != null && value > 0L;
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
}
