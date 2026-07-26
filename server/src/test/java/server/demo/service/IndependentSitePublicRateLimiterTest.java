package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSitePublicRateLimiterTest {

    @Test
    void springShouldSelectPublicClockConstructor() throws Exception {
        Constructor<IndependentSitePublicRateLimiter> productionConstructor =
                IndependentSitePublicRateLimiter.class.getConstructor(Clock.class);
        Constructor<IndependentSitePublicRateLimiter> testConstructor =
                IndependentSitePublicRateLimiter.class.getDeclaredConstructor(
                        Clock.class,
                        int.class,
                        Duration.class,
                        int.class,
                        Duration.class
                );

        assertTrue(productionConstructor.isAnnotationPresent(Autowired.class));
        assertFalse(testConstructor.isAnnotationPresent(Autowired.class));
    }

    @Test
    void checkHold_shouldRejectAboveLowerPerSiteAndRemoteAddressThreshold() {
        IndependentSitePublicRateLimiter limiter = limiter();

        limiter.checkHold("alpha", "203.0.113.10");
        limiter.checkHold("alpha", "203.0.113.10");

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkHold("alpha", "203.0.113.10")
        );

        assertEquals(429, exception.getStatus().value());
        assertEquals("PUBLIC_RATE_LIMITED", exception.getCode());
    }

    @Test
    void quoteAndHoldBuckets_shouldBeIndependentAndScopedBySiteAndRemoteAddress() {
        IndependentSitePublicRateLimiter limiter = limiter();

        limiter.checkHold("alpha", "203.0.113.10");
        limiter.checkHold("alpha", "203.0.113.10");
        limiter.checkQuote("alpha", "203.0.113.10");
        limiter.checkQuote("alpha", "203.0.113.10");
        limiter.checkQuote("alpha", "203.0.113.10");
        limiter.checkHold("beta", "203.0.113.10");
        limiter.checkHold("alpha", "203.0.113.11");

        IndependentSiteServiceException quoteException = assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkQuote("alpha", "203.0.113.10")
        );
        assertEquals(429, quoteException.getStatus().value());
    }

    @Test
    void checkIntent_shouldRejectAboveThresholdAndUseIndependentBucket() {
        IndependentSitePublicRateLimiter limiter = new IndependentSitePublicRateLimiter(
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC),
                3,
                Duration.ofMinutes(1),
                2,
                Duration.ofMinutes(15),
                2,
                Duration.ofMinutes(1)
        );

        limiter.checkIntent("alpha", "203.0.113.10");
        limiter.checkIntent("alpha", "203.0.113.10");

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkIntent("alpha", "203.0.113.10")
        );
        assertEquals(429, exception.getStatus().value());
        assertEquals("PUBLIC_RATE_LIMITED", exception.getCode());

        // intent 桶独立：不影响 quote；其他 slug / IP 也不受影响
        limiter.checkQuote("alpha", "203.0.113.10");
        limiter.checkIntent("beta", "203.0.113.10");
        limiter.checkIntent("alpha", "203.0.113.11");
    }

    private static IndependentSitePublicRateLimiter limiter() {
        return new IndependentSitePublicRateLimiter(
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC),
                3,
                Duration.ofMinutes(1),
                2,
                Duration.ofMinutes(15)
        );
    }
}
