package server.demo.util;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadlockRetryExecutorTest {

    private static DeadlockRetryExecutor noSleepExecutor() {
        return new DeadlockRetryExecutor(3, List.of(0L, 0L));
    }

    @Test
    void succeedsOnSecondAttemptAfterDeadlock() {
        DeadlockRetryExecutor executor = noSleepExecutor();
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.execute("test-op", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new DeadlockLoserDataAccessException("deadlock loser", null);
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void retriesLockWaitTimeoutAndUniqueKeyRace() {
        DeadlockRetryExecutor executor = noSleepExecutor();
        AtomicInteger lockWaitAttempts = new AtomicInteger();
        AtomicInteger uniqueKeyAttempts = new AtomicInteger();

        executor.execute("lock-wait", () -> {
            if (lockWaitAttempts.incrementAndGet() == 1) {
                throw new CannotAcquireLockException("lock wait timeout");
            }
        });
        executor.execute("unique-key", () -> {
            if (uniqueKeyAttempts.incrementAndGet() == 1) {
                throw new DataIntegrityViolationException("duplicate entry");
            }
        });

        assertEquals(2, lockWaitAttempts.get());
        assertEquals(2, uniqueKeyAttempts.get());
    }

    @Test
    void stopsAtMaxAttemptsAndRethrowsLastFailure() {
        DeadlockRetryExecutor executor = noSleepExecutor();
        AtomicInteger attempts = new AtomicInteger();

        DeadlockLoserDataAccessException thrown = assertThrows(
                DeadlockLoserDataAccessException.class,
                () -> executor.execute("test-op", () -> {
                    attempts.incrementAndGet();
                    throw new DeadlockLoserDataAccessException("always deadlocked", null);
                })
        );

        assertEquals("always deadlocked", thrown.getMessage());
        assertEquals(3, attempts.get());
    }

    @Test
    void doesNotRetryNonRetryableException() {
        DeadlockRetryExecutor executor = noSleepExecutor();
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(
                IllegalStateException.class,
                () -> executor.execute("test-op", () -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("not a concurrency failure");
                })
        );

        assertEquals(1, attempts.get());
    }

    @Test
    void successfulActionRunsExactlyOnce() {
        DeadlockRetryExecutor executor = noSleepExecutor();
        AtomicInteger attempts = new AtomicInteger();

        executor.execute("idempotent-early-return", attempts::incrementAndGet);

        assertEquals(1, attempts.get());
    }

    @Test
    void interruptedBackoffRestoresInterruptFlagAndStopsRetrying() {
        DeadlockRetryExecutor executor = new DeadlockRetryExecutor(3, List.of(10_000L));
        AtomicInteger attempts = new AtomicInteger();

        Thread.currentThread().interrupt();
        try {
            assertThrows(
                    DeadlockLoserDataAccessException.class,
                    () -> executor.execute("test-op", () -> {
                        attempts.incrementAndGet();
                        throw new DeadlockLoserDataAccessException("deadlock loser", null);
                    })
            );
            assertEquals(1, attempts.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            // 清掉中断标记，避免污染同线程后续测试
            Thread.interrupted();
        }
    }
}
