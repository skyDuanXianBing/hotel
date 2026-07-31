package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import server.demo.i18n.ApiMessages;
@Service
public class IndependentSitePublicRateLimiter {

    private static final int DEFAULT_QUOTE_LIMIT = 60;
    private static final Duration DEFAULT_QUOTE_WINDOW = Duration.ofMinutes(1);
    private static final int DEFAULT_HOLD_LIMIT = 5;
    private static final Duration DEFAULT_HOLD_WINDOW = Duration.ofMinutes(15);
    private static final int DEFAULT_INTENT_LIMIT = 10;
    private static final Duration DEFAULT_INTENT_WINDOW = Duration.ofMinutes(1);
    private static final long CLEANUP_INTERVAL = 256;

    private final Clock clock;
    private final int quoteLimit;
    private final long quoteWindowMillis;
    private final int holdLimit;
    private final long holdWindowMillis;
    private final int intentLimit;
    private final long intentWindowMillis;
    private final ConcurrentHashMap<RateLimitKey, WindowCounter> counters =
            new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();

    @Autowired
    public IndependentSitePublicRateLimiter(Clock clock) {
        this(
                clock,
                DEFAULT_QUOTE_LIMIT,
                DEFAULT_QUOTE_WINDOW,
                DEFAULT_HOLD_LIMIT,
                DEFAULT_HOLD_WINDOW,
                DEFAULT_INTENT_LIMIT,
                DEFAULT_INTENT_WINDOW
        );
    }

    IndependentSitePublicRateLimiter(
            Clock clock,
            int quoteLimit,
            Duration quoteWindow,
            int holdLimit,
            Duration holdWindow
    ) {
        this(
                clock,
                quoteLimit,
                quoteWindow,
                holdLimit,
                holdWindow,
                DEFAULT_INTENT_LIMIT,
                DEFAULT_INTENT_WINDOW
        );
    }

    IndependentSitePublicRateLimiter(
            Clock clock,
            int quoteLimit,
            Duration quoteWindow,
            int holdLimit,
            Duration holdWindow,
            int intentLimit,
            Duration intentWindow
    ) {
        if (clock == null || quoteLimit < 1 || holdLimit < 1 || intentLimit < 1
                || quoteWindow == null || quoteWindow.isZero() || quoteWindow.isNegative()
                || holdWindow == null || holdWindow.isZero() || holdWindow.isNegative()
                || intentWindow == null || intentWindow.isZero() || intentWindow.isNegative()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.b552361a35e3"));
        }
        this.clock = clock;
        this.quoteLimit = quoteLimit;
        this.quoteWindowMillis = quoteWindow.toMillis();
        this.holdLimit = holdLimit;
        this.holdWindowMillis = holdWindow.toMillis();
        this.intentLimit = intentLimit;
        this.intentWindowMillis = intentWindow.toMillis();
    }

    public void checkQuote(String slug, String remoteAddress) {
        check(Action.QUOTE, slug, remoteAddress, quoteLimit, quoteWindowMillis);
    }

    public void checkHold(String slug, String remoteAddress) {
        check(Action.HOLD, slug, remoteAddress, holdLimit, holdWindowMillis);
    }

    /** Stripe intent 会真实打 Stripe API，单独限流：每 slug+IP 10 次/分钟。 */
    public void checkIntent(String slug, String remoteAddress) {
        check(Action.INTENT, slug, remoteAddress, intentLimit, intentWindowMillis);
    }

    private void check(
            Action action,
            String slug,
            String remoteAddress,
            int limit,
            long windowMillis
    ) {
        long now = clock.millis();
        RateLimitKey key = new RateLimitKey(
                action,
                normalizeSlug(slug),
                normalizeRemoteAddress(remoteAddress)
        );
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
                    "PUBLIC_RATE_LIMITED",
                    switch (action) {
                        case HOLD -> ApiMessages.get("api.t.f1263a0bb6f8");
                        case INTENT -> ApiMessages.get("api.t.cf408bd15e36");
                        default -> ApiMessages.get("api.t.1d4992b559c9");
                    }
            );
        }
    }

    private void cleanupOccasionally(long now) {
        if (operations.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        counters.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
    }

    private static String normalizeSlug(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRemoteAddress(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private enum Action {
        QUOTE,
        HOLD,
        INTENT
    }

    private record RateLimitKey(Action action, String slug, String remoteAddress) {
    }

    private record WindowCounter(int count, long expiresAtMillis) {
    }
}
