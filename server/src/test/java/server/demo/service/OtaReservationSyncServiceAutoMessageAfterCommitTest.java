package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * webhook 预订入库后的自动消息派发必须：
 * - 在事务提交后才提交异步任务（afterCommit），不能在事务内同步执行；
 * - 通过 SuWebhookAsyncProcessor 在 worker 线程上执行，不占用 webhook 请求线程。
 */
class OtaReservationSyncServiceAutoMessageAfterCommitTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void scheduleAutoMessageDispatchAfterCommit_defersDispatchUntilAfterCommitWhenTransactionIsActive() {
        AutoMessageTriggerService autoMessageTriggerService = mock(AutoMessageTriggerService.class);
        SuWebhookAsyncProcessor asyncProcessor = mockInlineAsyncProcessor();
        OtaReservationSyncService service = createService(autoMessageTriggerService, asyncProcessor);

        TransactionSynchronizationManager.initSynchronization();

        service.scheduleAutoMessageDispatchAfterCommit(26L, Set.of(1594L));

        verify(autoMessageTriggerService, times(0)).dispatchStoreOnce(eq(26L), any());
        verify(asyncProcessor, times(0)).submit(anyString(), any(Runnable.class));
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(asyncProcessor, times(1)).submit(anyString(), any(Runnable.class));
        verify(autoMessageTriggerService, times(1)).dispatchStoreOnce(eq(26L), eq(Set.of(1594L)));
    }

    @Test
    void scheduleAutoMessageDispatchAfterCommit_submitsImmediatelyWhenTransactionIsInactive() {
        AutoMessageTriggerService autoMessageTriggerService = mock(AutoMessageTriggerService.class);
        SuWebhookAsyncProcessor asyncProcessor = mockInlineAsyncProcessor();
        OtaReservationSyncService service = createService(autoMessageTriggerService, asyncProcessor);

        service.scheduleAutoMessageDispatchAfterCommit(26L, Set.of(1593L, 1594L));

        verify(asyncProcessor, times(1)).submit(anyString(), any(Runnable.class));
        verify(autoMessageTriggerService, times(1)).dispatchStoreOnce(eq(26L), eq(Set.of(1593L, 1594L)));
    }

    @Test
    void scheduleAutoMessageDispatchAfterCommit_nullScopeMeansFullStoreDispatch() {
        AutoMessageTriggerService autoMessageTriggerService = mock(AutoMessageTriggerService.class);
        SuWebhookAsyncProcessor asyncProcessor = mockInlineAsyncProcessor();
        OtaReservationSyncService service = createService(autoMessageTriggerService, asyncProcessor);

        service.scheduleAutoMessageDispatchAfterCommit(26L, null);

        verify(autoMessageTriggerService, times(1)).dispatchStoreOnce(eq(26L), eq(null));
    }

    /** submit 的任务直接内联执行，便于验证最终调用到 dispatchStoreOnce。 */
    private static SuWebhookAsyncProcessor mockInlineAsyncProcessor() {
        SuWebhookAsyncProcessor asyncProcessor = mock(SuWebhookAsyncProcessor.class);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        }).when(asyncProcessor).submit(anyString(), any(Runnable.class));
        return asyncProcessor;
    }

    private static OtaReservationSyncService createService(
            AutoMessageTriggerService autoMessageTriggerService,
            SuWebhookAsyncProcessor asyncProcessor
    ) {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        return new OtaReservationSyncService(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                transactionManager,
                null,
                autoMessageTriggerService,
                null,
                null,
                null,
                null,
                null,
                null,
                asyncProcessor
        );
    }
}
