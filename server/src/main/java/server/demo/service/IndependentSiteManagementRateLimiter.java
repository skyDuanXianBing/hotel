package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import server.demo.i18n.ApiMessages;
/**
 * 独立站管理端限流器。按 (action, storeId) 维度的内存固定窗口计数，
 * 与 IndependentSitePublicRateLimiter 同一模式，仅作用于管理端高成本操作。
 */
@Service
public class IndependentSiteManagementRateLimiter {

    private static final int DEFAULT_AI_EDIT_LIMIT = 30;
    private static final Duration DEFAULT_AI_EDIT_WINDOW = Duration.ofHours(1);
    private static final int DEFAULT_URL_IMPORT_LIMIT = 10;
    private static final Duration DEFAULT_URL_IMPORT_WINDOW = Duration.ofHours(1);
    private static final long CLEANUP_INTERVAL = 256;

    private final Clock clock;
    private final int aiEditLimit;
    private final long aiEditWindowMillis;
    private final int urlImportLimit;
    private final long urlImportWindowMillis;
    private final ConcurrentHashMap<RateLimitKey, WindowCounter> counters =
            new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();

    @Autowired
    public IndependentSiteManagementRateLimiter(Clock clock) {
        this(
                clock,
                DEFAULT_AI_EDIT_LIMIT,
                DEFAULT_AI_EDIT_WINDOW,
                DEFAULT_URL_IMPORT_LIMIT,
                DEFAULT_URL_IMPORT_WINDOW
        );
    }

    IndependentSiteManagementRateLimiter(Clock clock, int aiEditLimit, Duration aiEditWindow) {
        this(
                clock,
                aiEditLimit,
                aiEditWindow,
                DEFAULT_URL_IMPORT_LIMIT,
                DEFAULT_URL_IMPORT_WINDOW
        );
    }

    IndependentSiteManagementRateLimiter(
            Clock clock,
            int aiEditLimit,
            Duration aiEditWindow,
            int urlImportLimit,
            Duration urlImportWindow
    ) {
        if (clock == null || aiEditLimit < 1
                || aiEditWindow == null || aiEditWindow.isZero() || aiEditWindow.isNegative()
                || urlImportLimit < 1
                || urlImportWindow == null || urlImportWindow.isZero() || urlImportWindow.isNegative()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.7111e2220203"));
        }
        this.clock = clock;
        this.aiEditLimit = aiEditLimit;
        this.aiEditWindowMillis = aiEditWindow.toMillis();
        this.urlImportLimit = urlImportLimit;
        this.urlImportWindowMillis = urlImportWindow.toMillis();
    }

    public void checkAiEdit(Long storeId) {
        check(
                Action.AI_EDIT,
                storeId,
                aiEditLimit,
                aiEditWindowMillis,
                "AI_EDIT_RATE_LIMITED",
                ApiMessages.get("api.t.f2041011ea67")
        );
    }

    public void checkUrlImport(Long storeId) {
        check(
                Action.URL_IMPORT,
                storeId,
                urlImportLimit,
                urlImportWindowMillis,
                "URL_IMPORT_RATE_LIMITED",
                ApiMessages.get("api.t.7f0537d0b1f4")
        );
    }

    private void check(
            Action action,
            Long storeId,
            int limit,
            long windowMillis,
            String code,
            String message
    ) {
        long now = clock.millis();
        RateLimitKey key = new RateLimitKey(action, storeId == null ? 0L : storeId);
        boolean[] rejected = new boolean[1];
        counters.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAtMillis() <= now) {
                return new WindowCounter(1, now + windowMillis);
            }
            if (current.count() >= limit) {
                rejected[0] = true;
                return current;
            }
            return new WindowCounter(current.count() + 1, current.expiresAtMillis());
        });
        cleanupOccasionally(now);
        if (rejected[0]) {
            throw new IndependentSiteServiceException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    code,
                    message
            );
        }
    }

    private void cleanupOccasionally(long now) {
        if (operations.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private enum Action {
        AI_EDIT,
        URL_IMPORT
    }

    private record RateLimitKey(Action action, Long storeId) {
    }

    private record WindowCounter(int count, long expiresAtMillis) {
    }
}
