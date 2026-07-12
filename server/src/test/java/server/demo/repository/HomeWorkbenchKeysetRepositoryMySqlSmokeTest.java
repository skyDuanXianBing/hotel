package server.demo.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true",
        disabledReason = "Set RUN_DB_INTEGRATION_TESTS=true to query an existing local MySQL schema")
@EnabledIf(value = "isLocalDbUrl", disabledReason = "DB_URL must use localhost or 127.0.0.1")
@DataJpaTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/booking_system_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:123456}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HomeWorkbenchKeysetRepositoryMySqlSmokeTest {
    private static final long ABSENT_STORE = Long.MAX_VALUE - 100;
    private static final LocalDate DATE = LocalDate.of(2026, 7, 12);
    private static final LocalDateTime DAY_START = DATE.atStartOfDay();

    @Autowired CleaningTaskRepository cleaning;
    @Autowired ReservationRepository orders;
    @Autowired RegistrationFormRepository reviews;
    @Autowired InternalTaskRepository other;
    @Value("${spring.datasource.url}") String datasourceUrl;

    @Test
    void mysqlParsesEveryKeysetQueryAndReturnsZeroForAbsentStoreAt49_50_51() {
        assumeTrue(datasourceUrl.contains("//localhost:") || datasourceUrl.contains("//127.0.0.1:"),
                "Workbench keyset repository smoke test only runs against local MySQL");
        for (int size : new int[]{49, 50, 51}) {
            assertTrue(cleaning.findHomeSlice(ABSENT_STORE, DATE, null, null, DAY_START,
                    false, 0, DAY_START, 0L, PageRequest.of(0, size)).isEmpty());
            assertTrue(orders.findHomeOrderSlice(ABSENT_STORE, DATE, false, 0, 0, DATE,
                    0L, PageRequest.of(0, size)).isEmpty());
            assertTrue(reviews.findHomeSlice(ABSENT_STORE, DAY_START.minusDays(7), null,
                    false, 0, DAY_START, 0L, PageRequest.of(0, size)).isEmpty());
            assertTrue(other.findHomeSlice(ABSENT_STORE, 1L, true, null, DAY_START.minusDays(7),
                    false, 0, DAY_START, 0L, PageRequest.of(0, size)).isEmpty());
        }
    }

    static boolean isLocalDbUrl() {
        String configured = System.getenv("DB_URL");
        if (configured == null || configured.isBlank()) {
            return true;
        }
        return configured.contains("//localhost:") || configured.contains("//127.0.0.1:");
    }
}
