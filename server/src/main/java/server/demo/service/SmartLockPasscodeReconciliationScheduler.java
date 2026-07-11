package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.demo.config.SmartLockConfig;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.enums.SmartLockPasscodeStatus;
import server.demo.enums.SmartLockProvider;
import server.demo.repository.SmartLockPasscodeRecordRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SmartLockPasscodeReconciliationScheduler {
    private static final Logger logger =
            LoggerFactory.getLogger(SmartLockPasscodeReconciliationScheduler.class);
    private static final long SYSTEM_USER_ID = 0L;

    private final SmartLockConfig config;
    private final SmartLockPasscodeRecordRepository passcodeRepository;
    private final SmartLockPasscodeReconciliationService reconciliationService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<Long, AtomicInteger> storeCycleCounts = new ConcurrentHashMap<>();

    public SmartLockPasscodeReconciliationScheduler(
            SmartLockConfig config,
            SmartLockPasscodeRecordRepository passcodeRepository,
            SmartLockPasscodeReconciliationService reconciliationService
    ) {
        this.config = config;
        this.passcodeRepository = passcodeRepository;
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(
            initialDelayString = "${smart-lock.passcode-reconcile.initial-delay-ms:5000}",
            fixedDelayString = "${smart-lock.passcode-reconcile.fixed-delay-ms:10000}"
    )
    public void runScheduledReconciliation() {
        if (!config.isPasscodeReconcileEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            logger.info("SmartLock passcode reconciliation skipped because previous run is still active");
            return;
        }

        try {
            List<Long> storeIds = passcodeRepository.findDistinctStoreIdsForSwitchBotReconciliation(
                    SmartLockProvider.SWITCHBOT,
                    SmartLockPasscodeStatus.PENDING,
                    SmartLockPasscodeStatus.DELETE_PENDING
            );
            for (Long storeId : storeIds) {
                reconcileStore(storeId);
            }
        } finally {
            StoreContextHolder.clear();
            running.set(false);
        }
    }

    private void reconcileStore(Long storeId) {
        if (storeId == null) {
            return;
        }
        StoreContextHolder.setContext(new StoreContext(SYSTEM_USER_ID, storeId, "system"));
        try {
            SmartLockPasscodeReconciliationService.ReconciliationSummary summary =
                    reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(
                            config.getPasscodeReconcileBatchSize(),
                            config.getPasscodeReconcileTimeoutMinutes()
                    );
            int cycle = storeCycleCounts.computeIfAbsent(storeId, ignored -> new AtomicInteger()).incrementAndGet();
            boolean periodicSummary = cycle == 1 || cycle % 6 == 0;
            if (summary.candidates() > 0
                    && (periodicSummary || summary.changed() > 0 || summary.timedOut() > 0 || summary.failed() > 0)) {
                logger.info(
                        "switchbot_passcode_scheduler_cycle_summary storeId={} cycle={} candidates={} changed={} "
                                + "timedOut={} failed={}",
                        storeId,
                        cycle,
                        summary.candidates(),
                        summary.changed(),
                        summary.timedOut(),
                        summary.failed()
                );
            }
        } catch (RuntimeException ex) {
            logger.warn(
                    "SmartLock passcode reconciliation failed storeId={} errorClass={}",
                    storeId,
                    ex.getClass().getSimpleName()
            );
        } finally {
            StoreContextHolder.clear();
        }
    }
}
