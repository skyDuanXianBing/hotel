package server.demo.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PriceLabs webhook 异步任务执行器。
 *
 * 目的：让 webhook 回调快速返回 200，避免 PriceLabs 因同步处理超时/异常判定失败。
 */
@Service
public class PriceLabsWebhookAsyncProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PriceLabsWebhookAsyncProcessor.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable);
        t.setName("pricelabs-webhook-worker");
        t.setDaemon(true);
        return t;
    });

    public void submit(String jobName, Runnable task) {
        Objects.requireNonNull(task, "task");
        String safeJobName = (jobName == null || jobName.isBlank()) ? "unknown" : jobName;

        executor.submit(() -> {
            long startedAt = System.currentTimeMillis();
            try {
                logger.info("[PriceLabsWebhookAsync] start. job={}", safeJobName);
                task.run();
                logger.info("[PriceLabsWebhookAsync] success. job={}, costMs={}", safeJobName, System.currentTimeMillis() - startedAt);
            } catch (Exception e) {
                logger.error(
                        "[PriceLabsWebhookAsync] failed. job={}, costMs={}, err={}",
                        safeJobName,
                        System.currentTimeMillis() - startedAt,
                        e.getMessage(),
                        e
                );
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}

