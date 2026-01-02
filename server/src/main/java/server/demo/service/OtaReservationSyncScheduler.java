package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.repository.OtaIntegrationRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * OTA 预订同步调度器（拉取 Su Reservation，并写入 PMS）
 */
@Service
public class OtaReservationSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OtaReservationSyncScheduler.class);

    private final OtaReservationSyncService otaReservationSyncService;
    private final OtaIntegrationRepository otaIntegrationRepository;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Value("${ota.reservation-sync.enabled:true}")
    private boolean enabled;

    @Value("${ota.reservation-sync.interval-seconds:300}")
    private long intervalSeconds;

    @Value("${ota.reservation-sync.initial-delay-seconds:30}")
    private long initialDelaySeconds;

    public OtaReservationSyncScheduler(
            OtaReservationSyncService otaReservationSyncService,
            OtaIntegrationRepository otaIntegrationRepository
    ) {
        this.otaReservationSyncService = otaReservationSyncService;
        this.otaIntegrationRepository = otaIntegrationRepository;
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("OTA reservation sync scheduler disabled (ota.reservation-sync.enabled=false)");
            return;
        }
        long effectiveInterval = Math.max(30, intervalSeconds);
        long effectiveInitialDelay = Math.max(1, initialDelaySeconds);
        executor.scheduleWithFixedDelay(this::runOnceSafely, effectiveInitialDelay, effectiveInterval, TimeUnit.SECONDS);
        logger.info("OTA reservation sync scheduler started. intervalSeconds={}, initialDelaySeconds={}", effectiveInterval, effectiveInitialDelay);
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }

    private void runOnceSafely() {
        try {
            runOnce();
        } catch (Exception e) {
            logger.error("OTA reservation scheduled sync failed", e);
        }
    }

    private void runOnce() {
        List<Long> storeIds = otaIntegrationRepository.findDistinctConnectedStoreIdsByCodes(
                otaReservationSyncService.getSupportedChannelCodes()
        );

        if (storeIds == null || storeIds.isEmpty()) {
            logger.debug("No connected OTA integrations found for reservation sync");
            return;
        }

        logger.info("Scheduled OTA reservation sync start. stores={}", storeIds.size());

        for (Long storeId : storeIds) {
            try {
                OtaReservationSyncService.ReservationSyncResult result = otaReservationSyncService.syncStoreReservations(storeId);
                logger.info("Scheduled OTA reservation sync done. storeId={}, pulled={}, processed={}, created={}, updated={}, failed={}, ack={}/{}",
                        storeId,
                        result.pulledReservations(),
                        result.processedRoomStays(),
                        result.createdCount(),
                        result.updatedCount(),
                        result.failedCount(),
                        result.ackSuccess(),
                        result.ackRequested()
                );
            } catch (Exception e) {
                logger.error("Scheduled OTA reservation sync failed. storeId={}", storeId, e);
            }
        }
    }
}

