package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
}
