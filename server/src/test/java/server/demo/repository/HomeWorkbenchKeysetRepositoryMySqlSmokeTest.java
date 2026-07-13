package server.demo.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import server.demo.entity.InternalTask;
import server.demo.enums.InternalTaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Autowired JdbcTemplate jdbc;
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

    @Test
    void mysqlCreatorOrAssigneeVisibilityIsDedupedAndKeysetStable() {
        assumeTrue(datasourceUrl.contains("//localhost:") || datasourceUrl.contains("//127.0.0.1:"),
                "Workbench keyset repository smoke test only runs against local MySQL");
        List<Map<String, Object>> stores = jdbc.queryForList("""
                select store_id
                from store_users
                where is_active = true
                group by store_id
                having count(distinct user_id) >= 3
                order by store_id
                limit 1
                """);
        assumeTrue(!stores.isEmpty(), "Need one local store with at least three active members");
        long storeId = ((Number) stores.get(0).get("store_id")).longValue();
        List<Long> users = jdbc.queryForList("""
                select distinct user_id
                from store_users
                where store_id = ? and is_active = true
                order by user_id
                limit 3
                """, Long.class, storeId);
        assumeTrue(users.size() == 3, "Need three active users for visibility smoke data");
        long creator = users.get(0);
        long assignee = users.get(1);
        long unrelated = users.get(2);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        // Isolate the fixture inside this rollback-only test transaction without deleting production-like rows.
        jdbc.update("update internal_tasks set archived_at = ? where store_id = ? and archived_at is null",
                now, storeId);

        long creatorOnlyId = insertTask(storeId, "creator-only", creator, assignee, now.minusSeconds(3));
        long creatorAndAssigneeId = insertTask(storeId, "creator-assignee", creator, creator, now.minusSeconds(2));
        insertTask(storeId, "unrelated", assignee, unrelated, now.minusSeconds(1));

        var mine = other.findVisibleToUser(storeId, creator, InternalTaskStatus.ASSIGNED,
                PageRequest.of(0, 20));
        assertEquals(List.of(creatorOnlyId, creatorAndAssigneeId), mine.getContent().stream()
                .map(InternalTask::getId).sorted().toList());
        assertEquals(2L, other.countVisibleToUser(storeId, creator, InternalTaskStatus.ASSIGNED));
        assertEquals(2L, other.countHome(storeId, creator, false, InternalTaskStatus.ASSIGNED, null));

        List<InternalTask> first = other.findHomeSlice(storeId, creator, false,
                InternalTaskStatus.ASSIGNED, now.minusDays(7), false, 0, now.minusDays(7), 0L,
                PageRequest.of(0, 1));
        assertEquals(1, first.size());
        InternalTask cursor = first.get(0);
        List<InternalTask> second = other.findHomeSlice(storeId, creator, false,
                InternalTaskStatus.ASSIGNED, now.minusDays(7), true, 1, cursor.getUpdatedAt(), cursor.getId(),
                PageRequest.of(0, 2));
        assertEquals(1, second.size());
        assertEquals(List.of(creatorOnlyId, creatorAndAssigneeId),
                java.util.stream.Stream.concat(first.stream(), second.stream())
                        .map(InternalTask::getId).sorted().toList());
    }

    private long insertTask(long storeId, String suffix, long creator, long assignee, LocalDateTime timestamp) {
        jdbc.update("""
                insert into internal_tasks (
                    store_id, title, status, assignee_user_id, assignee_name_snapshot,
                    created_by_user_id, created_by_name_snapshot, version, created_at, updated_at
                ) values (?, ?, 'ASSIGNED', ?, 'smoke-assignee', ?, 'smoke-creator', 0, ?, ?)
                """, storeId, "codex-creator-visibility-" + suffix + "-" + System.nanoTime(),
                assignee, creator, timestamp, timestamp);
        return jdbc.queryForObject("select last_insert_id()", Long.class);
    }

    static boolean isLocalDbUrl() {
        String configured = System.getenv("DB_URL");
        if (configured == null || configured.isBlank()) {
            return true;
        }
        return configured.contains("//localhost:") || configured.contains("//127.0.0.1:");
    }
}
