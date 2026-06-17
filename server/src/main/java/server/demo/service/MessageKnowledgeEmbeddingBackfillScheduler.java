package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MessageKnowledgeEmbeddingBackfillScheduler {
    private static final Logger logger =
            LoggerFactory.getLogger(MessageKnowledgeEmbeddingBackfillScheduler.class);

    private final MessageKnowledgeEmbeddingBackfillService backfillService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${messaging.knowledge.embedding.backfill.enabled:false}")
    private boolean enabled;

    public MessageKnowledgeEmbeddingBackfillScheduler(
            MessageKnowledgeEmbeddingBackfillService backfillService
    ) {
        this.backfillService = backfillService;
    }

    @Scheduled(
            initialDelayString = "${messaging.knowledge.embedding.backfill.initial-delay-ms:180000}",
            fixedDelayString = "${messaging.knowledge.embedding.backfill.fixed-delay-ms:900000}"
    )
    public void runScheduledBackfill() {
        if (!enabled) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            logger.info("Message knowledge embedding backfill skipped because previous run is still active");
            return;
        }

        try {
            MessageKnowledgeEmbeddingBackfillResult result = backfillService.runOnce();
            logger.info(
                    "Message knowledge embedding backfill finished. attempted={}, succeeded={}, failed={}, skippedReason={}",
                    result.attempted(),
                    result.succeeded(),
                    result.failed(),
                    result.skippedReason()
            );
        } finally {
            running.set(false);
        }
    }
}
