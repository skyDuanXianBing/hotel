package server.demo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuMessageThreadRepositoryQueryShapeTest {

    @Test
    void awaitingReplyQueryUsesStableLatestEffectiveMessageWindow() throws Exception {
        Method method = SuMessageThreadRepository.class.getMethod(
                "findAwaitingReplyPageByStoreId",
                Long.class,
                Pageable.class
        );
        Query query = method.getAnnotation(Query.class);
        String contentSql = normalize(query.value());
        String countSql = normalize(query.countQuery());

        assertTrue(contentSql.contains("ROW_NUMBER() OVER"));
        assertTrue(contentSql.contains("PARTITION BY m.thread_id"));
        assertTrue(contentSql.contains("ORDER BY m.sent_at DESC, m.id DESC"));
        assertTrue(contentSql.contains("m.sender_type = 'GUEST'"));
        assertTrue(contentSql.contains("m.sender_type = 'STAFF' AND m.delivery_status = 'SENT'"));
        assertTrue(contentSql.contains("ranked.sender_type = 'GUEST'"));
        assertTrue(contentSql.contains("t.closed = false"));
        assertFalse(contentSql.contains("NOT EXISTS"));

        assertTrue(countSql.contains("ROW_NUMBER() OVER"));
        assertTrue(countSql.contains("ranked.sender_type = 'GUEST'"));
        assertFalse(countSql.contains("NOT EXISTS"));
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
