package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MessageKnowledgeScannerScheduler {
    private static final Logger logger = LoggerFactory.getLogger(MessageKnowledgeScannerScheduler.class);

    private final MessageKnowledgeHistoryScanner historyScanner;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${messaging.knowledge.refined-scanner.enabled:false}")
    private boolean enabled;

    public MessageKnowledgeScannerScheduler(MessageKnowledgeHistoryScanner historyScanner) {
        this.historyScanner = historyScanner;
    }

    @Scheduled(
            initialDelayString = "${messaging.knowledge.refined-scanner.initial-delay-ms:120000}",
            fixedDelayString = "${messaging.knowledge.refined-scanner.fixed-delay-ms:900000}"
    )
    public void runScheduledScan() {
        if (!enabled) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            logger.info("Refined message knowledge scanner skipped because previous run is still active");
            return;
        }

        try {
            int extractedCount = historyScanner.scanOnce();
            logger.info("Refined message knowledge scanner finished. extracted={}", extractedCount);
        } finally {
            running.set(false);
        }
    }
}
