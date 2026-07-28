package server.demo.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import server.demo.entity.AutoMessageSendLog;
import server.demo.repository.AutoMessageSendLogRepository;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发回归：定时任务与 webhook 同时为同一 (store_id, action, target_type, target_id)
 * 抢占 auto_message_send_logs 唯一键时：
 * - 只落一行日志；
 * - 输掉的一方仅收到 DataIntegrityViolationException（在 REQUIRES_NEW 边界外），
 *   其外层事务不会被标记 rollback-only，仍可正常提交。
 *
 * 依赖真实数据源（MySQL 唯一键 + 锁等待语义，mock 无法复现），CI 无数据库时保持禁用；
 * 本地运行：临时注释掉 @Disabled 后执行
 *   ./mvnw -Dtest=AutoMessageSendLogClaimConcurrencyTest test
 */
@DataJpaTest(properties = {
        // 测试环境的 application.properties 全局排除了 DataSource/JPA 自动配置，这里恢复
        "spring.autoconfigure.exclude=",
        "spring.jpa.hibernate.ddl-auto=update",
        // 与主配置保持一致：不自动执行 Flyway 迁移
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AutoMessageSendLogClaimService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Disabled("需要本地 MySQL 数据源，CI 无数据库时跳过；本地手动执行验证（已于 2026-07-28 在本地 MySQL 跑通）")
class AutoMessageSendLogClaimConcurrencyTest {

    /**
     * 测试上下文不会加载主配置里的 spring.config.import，手动从本地 .env 读取数据库连接。
     */
    @DynamicPropertySource
    static void datasourceFromLocalEnv(DynamicPropertyRegistry registry) {
        Properties env = new Properties();
        for (String candidate : new String[]{".env", "../.env"}) {
            Path path = Path.of(candidate);
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    env.load(in);
                } catch (Exception ignored) {
                }
                break;
            }
        }
        registry.add("spring.datasource.url", () -> env.getProperty("DB_URL",
                "jdbc:mysql://localhost:3306/booking_system_db?useUnicode=true&characterEncoding=utf8"
                        + "&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"));
        registry.add("spring.datasource.username", () -> env.getProperty("DB_USERNAME", "root"));
        registry.add("spring.datasource.password", () -> env.getProperty("DB_PASSWORD", "123456"));
    }

    @Autowired
    private AutoMessageSendLogClaimService claimService;

    @Autowired
    private AutoMessageSendLogRepository sendLogRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentInsertClaim_onlyOneRowWinsAndLoserOuterTransactionStillCommits() throws Exception {
        Long storeId = 26L;
        String action = "AM:T" + (System.nanoTime() % 1_000_000_000L);
        String targetType = "RESERVATION";
        Long targetId = System.nanoTime() % 1_000_000_000L;
        Long autoMessageId = 999_999L;

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger claimed = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        Runnable worker = () -> {
            try {
                TransactionTemplate outerTx = new TransactionTemplate(transactionManager);
                // 外层事务模拟 webhook 的预订入库事务；若被冲突毒化，commit 会抛 UnexpectedRollbackException
                outerTx.executeWithoutResult(status -> {
                    try {
                        barrier.await();
                        claimService.insertClaim(storeId, action, targetType, targetId, autoMessageId);
                        claimed.incrementAndGet();
                    } catch (DataIntegrityViolationException e) {
                        conflicted.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Throwable t) {
                unexpected.set(t);
            } finally {
                done.countDown();
            }
        };

        Thread t1 = new Thread(worker, "claim-worker-1");
        Thread t2 = new Thread(worker, "claim-worker-2");
        t1.start();
        t2.start();
        assertTrue(done.await(30, TimeUnit.SECONDS), "workers did not finish in time");

        try {
            assertNull(unexpected.get(), "外层事务不应因唯一键冲突而回滚: " + unexpected.get());
            assertEquals(1, claimed.get(), "只应有一方抢占成功");
            assertEquals(1, conflicted.get(), "另一方应收到 DataIntegrityViolationException");

            Optional<AutoMessageSendLog> row = sendLogRepository
                    .findByStoreIdAndActionAndTargetTypeAndTargetId(storeId, action, targetType, targetId);
            assertTrue(row.isPresent(), "唯一键应只落一行");
        } finally {
            sendLogRepository.findByStoreIdAndActionAndTargetTypeAndTargetId(storeId, action, targetType, targetId)
                    .ifPresent(sendLogRepository::delete);
        }
    }
}
