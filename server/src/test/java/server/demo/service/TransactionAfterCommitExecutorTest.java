package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionAfterCommitExecutorTest {

    @AfterEach
    void cleanupSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void execute_defersActionUntilCommitWhenTransactionSynchronizationIsActive() {
        AtomicInteger calls = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();

        TransactionAfterCommitExecutor.execute(calls::incrementAndGet);

        assertEquals(0, calls.get());
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        assertEquals(1, calls.get());
    }

    @Test
    void execute_doesNotRunDeferredActionWithoutCommitCallback() {
        AtomicInteger calls = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();

        TransactionAfterCommitExecutor.execute(calls::incrementAndGet);
        TransactionSynchronizationManager.clearSynchronization();

        assertEquals(0, calls.get());
    }
}
