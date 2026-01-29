package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SuWebhookAsyncProcessor {

    private static final Logger reservationLogger = LoggerFactory.getLogger("SU_RESERVATION");

    private final ExecutorService executor;

    public SuWebhookAsyncProcessor() {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable);
            t.setName("su-webhook-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void submit(String jobName, Runnable task) {
        Objects.requireNonNull(task, "task");
        String safeJobName = jobName == null ? "unknown" : jobName;

        executor.submit(() -> {
            long startAt = System.currentTimeMillis();
            try {
                reservationLogger.info("[SuWebhookAsync] start. job={}", safeJobName);
                task.run();
                reservationLogger.info("[SuWebhookAsync] success. job={}, costMs={}", safeJobName, System.currentTimeMillis() - startAt);
            } catch (Exception e) {
                reservationLogger.error("[SuWebhookAsync] failed. job={}, costMs={}, err={}", safeJobName, System.currentTimeMillis() - startAt, e.getMessage(), e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}

