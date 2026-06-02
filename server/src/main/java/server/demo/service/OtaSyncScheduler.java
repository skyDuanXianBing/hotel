package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.StoreTimeZoneUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 独立的 OTA 同步调度器（不依赖 Spring @Scheduled，避免启用全局调度带来副作用）。
 */
@Service
public class OtaSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OtaSyncScheduler.class);

    private final OtaSyncService otaSyncService;
    private final ChannelPriceRepository channelPriceRepository;
    private final ChannelRepository channelRepository;
    private final StoreRepository storeRepository;
    private final Clock clock;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Value("${ota.sync.enabled:true}")
    private boolean enabled;

    @Value("${ota.sync.hour:12}")
    private int hour;

    @Value("${ota.sync.minute:0}")
    private int minute;

    @Value("${ota.sync.days:365}")
    private int days;

    public OtaSyncScheduler(
            OtaSyncService otaSyncService,
            ChannelPriceRepository channelPriceRepository,
            ChannelRepository channelRepository,
            StoreRepository storeRepository,
            Clock clock
    ) {
        this.otaSyncService = otaSyncService;
        this.channelPriceRepository = channelPriceRepository;
        this.channelRepository = channelRepository;
        this.storeRepository = storeRepository;
        this.clock = clock;
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("OTA sync scheduler disabled (ota.sync.enabled=false)");
            return;
        }
        scheduleNextRun();
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }

    private void scheduleNextRun() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute));
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }

        long delayMillis = Duration.between(now, next).toMillis();
        executor.schedule(this::runAndReschedule, delayMillis, TimeUnit.MILLISECONDS);
        logger.info("Next OTA sync scheduled at {}", next);
    }

    private void runAndReschedule() {
        try {
            runOnce();
        } catch (Exception e) {
            logger.error("OTA scheduled sync failed", e);
        } finally {
            scheduleNextRun();
        }
    }

    private void runOnce() {
        List<Long> storeIds = channelRepository.findDistinctStoreIdsByEnabledAutoSyncAndCodes(
                otaSyncService.getDefaultOtaChannelCodes()
        );
        if (storeIds.isEmpty()) {
            logger.info("No OTA candidate stores found for scheduled sync");
            return;
        }

        logger.info("Scheduled OTA sync start. stores={}", storeIds.size());

        for (Long storeId : storeIds) {
            try {
                LocalDate startDate = currentStoreDate(storeId);
                OtaSyncService.OtaSyncResult result = otaSyncService.syncStorePricesToSu(storeId, startDate, days);
                logger.info("Scheduled OTA sync done. storeId={}, pushed={}, marked={}",
                        storeId, result.pushedCountByChannel(), result.markedSyncedCountByChannel());
            } catch (Exception e) {
                logger.error("Scheduled OTA sync failed. storeId={}", storeId, e);
            }
        }
    }

    private LocalDate currentStoreDate(Long storeId) {
        ZoneId zoneId = StoreTimeZoneUtil.resolveZoneId(storeRepository.findById(storeId).orElse(null));
        return LocalDate.now(clock.withZone(zoneId));
    }
}
