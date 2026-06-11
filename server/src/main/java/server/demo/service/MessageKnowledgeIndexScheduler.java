package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.demo.repository.SuMessageRepository;
import server.demo.util.UtcTimeUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MessageKnowledgeIndexScheduler {
    private static final Logger logger = LoggerFactory.getLogger(MessageKnowledgeIndexScheduler.class);

    private static final int MIN_STORES_PER_RUN = 1;
    private static final int MAX_STORES_PER_RUN = 100;

    private final MessageKnowledgeIndexService indexService;
    private final SuMessageRepository messageRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${messaging.knowledge.indexer.enabled:false}")
    private boolean enabled;

    @Value("${messaging.knowledge.indexer.lookback-days:14}")
    private int lookbackDays;

    @Value("${messaging.knowledge.indexer.limit:100}")
    private int messageLimit;

    @Value("${messaging.knowledge.indexer.stores-per-run:10}")
    private int storesPerRun;

    public MessageKnowledgeIndexScheduler(
            MessageKnowledgeIndexService indexService,
            SuMessageRepository messageRepository
    ) {
        this.indexService = indexService;
        this.messageRepository = messageRepository;
    }

    @Scheduled(
            initialDelayString = "${messaging.knowledge.indexer.initial-delay-ms:60000}",
            fixedDelayString = "${messaging.knowledge.indexer.fixed-delay-ms:300000}"
    )
    public void runScheduledIndex() {
        if (!enabled) {
            return;
        }

        if (!running.compareAndSet(false, true)) {
            logger.info("Message knowledge index scheduler skipped because previous run is still active");
            return;
        }

        try {
            runOnce();
        } finally {
            running.set(false);
        }
    }

    void runOnce() {
        int effectiveLookbackDays = MessageKnowledgeIndexService.normalizeLookbackDays(lookbackDays);
        int effectiveMessageLimit = MessageKnowledgeIndexService.normalizeMessageLimit(messageLimit);
        int effectiveStoresPerRun = normalizeStoresPerRun(storesPerRun);
        LocalDateTime since = UtcTimeUtil.nowLocalDateTime().minusDays(effectiveLookbackDays);
        List<Long> storeIds = messageRepository.findDistinctStoreIdsWithMessagesSince(
                since,
                PageRequest.of(0, effectiveStoresPerRun)
        );
        if (storeIds == null || storeIds.isEmpty()) {
            logger.debug("Message knowledge index scheduler found no recent stores");
            return;
        }

        int attemptedCount = 0;
        for (Long storeId : storeIds) {
            if (storeId == null) {
                continue;
            }

            try {
                attemptedCount += indexService.indexRecentStoreMessages(
                        storeId,
                        effectiveLookbackDays,
                        effectiveMessageLimit
                );
            } catch (Exception e) {
                logger.warn("Message knowledge index scheduler failed for storeId={}: {}",
                        storeId, e.getMessage(), e);
            }
        }

        logger.info("Message knowledge index scheduler finished. stores={}, attempted={}",
                storeIds.size(), attemptedCount);
    }

    private static int normalizeStoresPerRun(int value) {
        return Math.max(MIN_STORES_PER_RUN, Math.min(value, MAX_STORES_PER_RUN));
    }
}
