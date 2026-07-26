package server.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentSiteManagementRateLimiterTest {

    @Test
    void springShouldSelectPublicClockConstructor() throws Exception {
        Constructor<IndependentSiteManagementRateLimiter> productionConstructor =
                IndependentSiteManagementRateLimiter.class.getConstructor(Clock.class);
        Constructor<IndependentSiteManagementRateLimiter> testConstructor =
                IndependentSiteManagementRateLimiter.class.getDeclaredConstructor(
                        Clock.class,
                        int.class,
                        java.time.Duration.class
                );

        assertTrue(productionConstructor.isAnnotationPresent(Autowired.class));
        assertFalse(testConstructor.isAnnotationPresent(Autowired.class));
    }

    @Test
    void checkAiEdit_shouldRejectThe31stCallWithinAnHourPerStore() {
        IndependentSiteManagementRateLimiter limiter = new IndependentSiteManagementRateLimiter(
                Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC)
        );

        for (int i = 0; i < 30; i++) {
            limiter.checkAiEdit(1L);
        }

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkAiEdit(1L)
        );

        assertEquals(429, exception.getStatus().value());
        assertEquals("AI_EDIT_RATE_LIMITED", exception.getCode());
    }

    @Test
    void checkUrlImport_shouldRejectThe11thCallWithinAnHourPerStore() {
        IndependentSiteManagementRateLimiter limiter = new IndependentSiteManagementRateLimiter(
                Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC)
        );

        for (int i = 0; i < 10; i++) {
            limiter.checkUrlImport(1L);
        }

        IndependentSiteServiceException exception = assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkUrlImport(1L)
        );

        assertEquals(429, exception.getStatus().value());
        assertEquals("URL_IMPORT_RATE_LIMITED", exception.getCode());
    }

    @Test
    void urlImportAndAiEditBuckets_shouldBeIndependent() {
        IndependentSiteManagementRateLimiter limiter = new IndependentSiteManagementRateLimiter(
                Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC),
                1,
                java.time.Duration.ofHours(1),
                1,
                java.time.Duration.ofHours(1)
        );

        limiter.checkAiEdit(1L);
        limiter.checkUrlImport(1L);
        limiter.checkUrlImport(2L);

        assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkAiEdit(1L)
        );
        assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkUrlImport(1L)
        );
    }

    @Test
    void checkAiEditBuckets_shouldBeIndependentPerStore() {
        IndependentSiteManagementRateLimiter limiter = new IndependentSiteManagementRateLimiter(
                Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC),
                2,
                java.time.Duration.ofHours(1)
        );

        limiter.checkAiEdit(1L);
        limiter.checkAiEdit(1L);
        limiter.checkAiEdit(2L);
        limiter.checkAiEdit(2L);

        assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkAiEdit(1L)
        );
        assertThrows(
                IndependentSiteServiceException.class,
                () -> limiter.checkAiEdit(2L)
        );
    }
}
