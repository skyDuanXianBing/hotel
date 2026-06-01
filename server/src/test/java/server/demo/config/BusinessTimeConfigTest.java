package server.demo.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import server.demo.util.StoreTimeZoneUtil;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessTimeConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BusinessTimeConfig.class);

    @AfterEach
    void tearDown() {
        StoreTimeZoneUtil.setBusinessDefaultZoneId(ZoneId.of(StoreTimeZoneUtil.DEFAULT_TIMEZONE));
        StoreTimeZoneUtil.setReservationTimestampStorageZoneId(
                ZoneId.of(StoreTimeZoneUtil.DEFAULT_RESERVATION_TIMESTAMP_STORAGE_ZONE)
        );
    }

    @Test
    void businessDefaultZoneId_shouldFallbackToTokyoWhenPropertyMissing() {
        contextRunner.run(context -> {
            ZoneId zoneId = context.getBean("businessDefaultZoneId", ZoneId.class);
            ZoneId storageZoneId = context.getBean("reservationTimestampStorageZoneId", ZoneId.class);

            assertEquals(ZoneId.of("Asia/Tokyo"), zoneId);
            assertEquals(ZoneId.of("Asia/Tokyo"), StoreTimeZoneUtil.getBusinessDefaultZoneId());
            assertEquals(ZoneId.of("Asia/Shanghai"), storageZoneId);
            assertEquals(ZoneId.of("Asia/Shanghai"), StoreTimeZoneUtil.getReservationTimestampStorageZoneId());
        });
    }

    @Test
    void businessDefaultZoneId_shouldUseConfiguredPropertyWhenValid() {
        contextRunner
                .withPropertyValues("app.time.default-zone=Asia/Shanghai")
                .run(context -> {
                    ZoneId zoneId = context.getBean("businessDefaultZoneId", ZoneId.class);

                    assertEquals(ZoneId.of("Asia/Shanghai"), zoneId);
                    assertEquals(ZoneId.of("Asia/Shanghai"), StoreTimeZoneUtil.getBusinessDefaultZoneId());
                });
    }

    @Test
    void businessDefaultZoneId_shouldFallbackToTokyoWhenPropertyInvalid() {
        contextRunner
                .withPropertyValues("app.time.default-zone=bad/timezone")
                .run(context -> {
                    ZoneId zoneId = context.getBean("businessDefaultZoneId", ZoneId.class);

                    assertEquals(ZoneId.of("Asia/Tokyo"), zoneId);
                    assertEquals(ZoneId.of("Asia/Tokyo"), StoreTimeZoneUtil.getBusinessDefaultZoneId());
                });
    }

    @Test
    void businessDefaultZoneId_shouldFallbackToTokyoWhenPropertyBlank() {
        contextRunner
                .withPropertyValues("app.time.default-zone= ")
                .run(context -> {
                    ZoneId zoneId = context.getBean("businessDefaultZoneId", ZoneId.class);

                    assertEquals(ZoneId.of("Asia/Tokyo"), zoneId);
                    assertEquals(ZoneId.of("Asia/Tokyo"), StoreTimeZoneUtil.getBusinessDefaultZoneId());
                });
    }

    @Test
    void reservationTimestampStorageZoneId_shouldUseConfiguredUtcPropertyWhenValid() {
        contextRunner
                .withPropertyValues("app.time.reservation-timestamp-storage-zone=UTC")
                .run(context -> {
                    ZoneId storageZoneId = context.getBean("reservationTimestampStorageZoneId", ZoneId.class);

                    assertEquals(ZoneId.of("UTC"), storageZoneId);
                    assertEquals(ZoneId.of("UTC"), StoreTimeZoneUtil.getReservationTimestampStorageZoneId());
                });
    }

    @Test
    void reservationTimestampStorageZoneId_shouldFallbackToShanghaiWhenPropertyInvalid() {
        contextRunner
                .withPropertyValues("app.time.reservation-timestamp-storage-zone=bad/timezone")
                .run(context -> {
                    ZoneId storageZoneId = context.getBean("reservationTimestampStorageZoneId", ZoneId.class);

                    assertEquals(ZoneId.of("Asia/Shanghai"), storageZoneId);
                    assertEquals(ZoneId.of("Asia/Shanghai"), StoreTimeZoneUtil.getReservationTimestampStorageZoneId());
                });
    }
}
