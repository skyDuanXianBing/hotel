package server.demo.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import server.demo.config.SmartLockConfig;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.enums.SmartLockPasscodeStatus;
import server.demo.enums.SmartLockProvider;
import server.demo.repository.SmartLockPasscodeRecordRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmartLockPasscodeReconciliationSchedulerTest {
    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void runScheduledReconciliation_shouldSetAndClearStoreContextForEachStore() {
        SmartLockConfig config = mock(SmartLockConfig.class);
        SmartLockPasscodeRecordRepository passcodeRepository =
                mock(SmartLockPasscodeRecordRepository.class);
        SmartLockPasscodeReconciliationService reconciliationService =
                mock(SmartLockPasscodeReconciliationService.class);
        SmartLockPasscodeReconciliationScheduler scheduler =
                new SmartLockPasscodeReconciliationScheduler(config, passcodeRepository, reconciliationService);
        List<Long> observedStoreIds = new ArrayList<>();

        when(config.isPasscodeReconcileEnabled()).thenReturn(true);
        when(config.getPasscodeReconcileBatchSize()).thenReturn(20);
        when(config.getPasscodeReconcileTimeoutMinutes()).thenReturn(5L);
        when(passcodeRepository.findDistinctStoreIdsForSwitchBotReconciliation(
                SmartLockProvider.SWITCHBOT,
                SmartLockPasscodeStatus.PENDING,
                SmartLockPasscodeStatus.DELETE_PENDING
        )).thenReturn(List.of(10L, 20L));
        when(reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5L))
                .thenAnswer(invocation -> {
                    StoreContext context = StoreContextHolder.getContext();
                    observedStoreIds.add(context.getStoreId());
                    return new SmartLockPasscodeReconciliationService.ReconciliationSummary(1, 1, 0, 0);
                });

        scheduler.runScheduledReconciliation();

        assertEquals(List.of(10L, 20L), observedStoreIds);
        assertNull(StoreContextHolder.getContext());
    }

    @Test
    void runScheduledReconciliation_shouldSampleUnchangedSummaryOnFirstAndSixthCycle() {
        SmartLockConfig config = mock(SmartLockConfig.class);
        SmartLockPasscodeRecordRepository passcodeRepository = mock(SmartLockPasscodeRecordRepository.class);
        SmartLockPasscodeReconciliationService reconciliationService =
                mock(SmartLockPasscodeReconciliationService.class);
        SmartLockPasscodeReconciliationScheduler scheduler =
                new SmartLockPasscodeReconciliationScheduler(config, passcodeRepository, reconciliationService);
        when(config.isPasscodeReconcileEnabled()).thenReturn(true);
        when(config.getPasscodeReconcileBatchSize()).thenReturn(20);
        when(config.getPasscodeReconcileTimeoutMinutes()).thenReturn(5L);
        when(passcodeRepository.findDistinctStoreIdsForSwitchBotReconciliation(
                SmartLockProvider.SWITCHBOT,
                SmartLockPasscodeStatus.PENDING,
                SmartLockPasscodeStatus.DELETE_PENDING
        )).thenReturn(List.of(10L));
        when(reconciliationService.reconcilePendingSwitchBotPasscodesForCurrentStore(20, 5L))
                .thenReturn(new SmartLockPasscodeReconciliationService.ReconciliationSummary(1, 0, 0, 0));
        Logger logger = (Logger) LoggerFactory.getLogger(SmartLockPasscodeReconciliationScheduler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            for (int cycle = 0; cycle < 6; cycle++) {
                scheduler.runScheduledReconciliation();
            }
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        List<String> summaries = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("switchbot_passcode_scheduler_cycle_summary"))
                .toList();
        assertEquals(2, summaries.size());
        assertEquals(true, summaries.get(0).contains("cycle=1"));
        assertEquals(true, summaries.get(1).contains("cycle=6"));
    }
}
