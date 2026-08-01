package server.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 数据库死锁 / 锁等待 / 并发唯一键冲突的有限退避重试执行器。
 *
 * <p>重试异常族：</p>
 * <ul>
 *   <li>{@link ConcurrencyFailureException}：覆盖 InnoDB 死锁（errno 1213，
 *   DeadlockLoserDataAccessException）与锁等待超时（errno 1205，CannotAcquireLockException）</li>
 *   <li>{@link DataIntegrityViolationException}：并发撞唯一键；重试后由既有幂等检查/查询兜底，
 *   保证重复写入只入库一次</li>
 * </ul>
 *
 * <p><b>关键约束：必须在事务边界之外使用</b>（例如 controller 经 Spring 代理调用
 * {@code @Transactional} service 方法），保证每次尝试都是全新事务。禁止在
 * {@code @Transactional} 方法内部用本执行器重试——事务已被标记回滚时重试没有意义。</p>
 */
public class DeadlockRetryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DeadlockRetryExecutor.class);

    /** 默认最多 3 次（首次 + 2 次重试）。 */
    public static final int DEFAULT_MAX_ATTEMPTS = 3;

    /** 默认退避：第 1 次失败后等 100ms，第 2 次失败后等 300ms。 */
    public static final List<Long> DEFAULT_BACKOFF_MILLIS = List.of(100L, 300L);

    private final int maxAttempts;
    private final List<Long> backoffMillis;

    public DeadlockRetryExecutor() {
        this(DEFAULT_MAX_ATTEMPTS, DEFAULT_BACKOFF_MILLIS);
    }

    public DeadlockRetryExecutor(int maxAttempts, List<Long> backoffMillis) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
        this.backoffMillis = backoffMillis == null ? List.of() : List.copyOf(backoffMillis);
    }

    public void execute(String operation, Runnable action) {
        Objects.requireNonNull(action, "action");
        execute(operation, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 执行动作，遇到可重试并发异常时按退避策略重试，直到成功或达到次数上限。
     * 达到上限或遇到非可重试异常时，原样抛出最后一次异常，由调用方兜底。
     * 退避 sleep 被中断时恢复线程中断标记并停止重试，抛出触发本次退避的异常。
     */
    public <T> T execute(String operation, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return action.get();
            } catch (RuntimeException e) {
                if (!isRetryable(e) || attempt >= maxAttempts) {
                    throw e;
                }
                long backoff = backoffFor(attempt);
                logger.warn("[DeadlockRetry] {} 遇到可重试并发异常（第 {}/{} 次尝试失败），{}ms 后重试: {}",
                        operation, attempt, maxAttempts, backoff, e.toString());
                if (!sleep(backoff)) {
                    throw e;
                }
            }
        }
    }

    static boolean isRetryable(RuntimeException e) {
        return e instanceof ConcurrencyFailureException
                || e instanceof DataIntegrityViolationException;
    }

    private long backoffFor(int failedAttempt) {
        if (backoffMillis.isEmpty()) {
            return 0L;
        }
        int index = Math.min(failedAttempt - 1, backoffMillis.size() - 1);
        Long value = backoffMillis.get(index);
        return value != null && value > 0 ? value : 0L;
    }

    private static boolean sleep(long millis) {
        try {
            if (millis > 0) {
                Thread.sleep(millis);
            }
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
