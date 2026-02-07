package server.demo.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SuAriAutoSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SuAriAutoSyncScheduler.class);

    private final SuAriAutoSyncService autoSyncService;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Value("${su.ari.autosync.enabled:true}")
    private boolean enabled;

    @Value("${su.ari.autosync.pollSeconds:10}")
    private int pollSeconds;

    @Value("${su.ari.autosync.batchSize:3}")
    private int batchSize;

    public SuAriAutoSyncScheduler(SuAriAutoSyncService autoSyncService) {
        this.autoSyncService = autoSyncService;
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("Su ARI auto sync scheduler disabled (su.ari.autosync.enabled=false)");
            return;
        }
        int interval = Math.max(1, pollSeconds);
        executor.scheduleWithFixedDelay(this::runOnce, 3, interval, TimeUnit.SECONDS);
        logger.info("Su ARI auto sync scheduler started. pollSeconds={}, batchSize={}", interval, batchSize);
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }

    private void runOnce() {
        try {
            autoSyncService.processDueEvents(batchSize);
        } catch (Exception e) {
            logger.error("Su ARI auto sync scheduler tick failed", e);
        }
    }
}

