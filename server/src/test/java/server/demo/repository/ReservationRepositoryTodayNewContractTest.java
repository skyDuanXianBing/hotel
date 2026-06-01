package server.demo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationRepositoryTodayNewContractTest {

    @Test
    void storeScopedTodayNewCount_shouldIgnoreActualCheckInOnlyRecords() throws NoSuchMethodException {
        Query query = getQuery(
                "countTodayNewOrdersByStoreId",
                Long.class,
                LocalDateTime.class,
                LocalDateTime.class
        );

        assertCreatedAtOnlyStoreScopedWindow(query.value(), "startOfDay", "endOfDay");
    }

    @Test
    void storeScopedTodayNewList_shouldIgnoreActualCheckInOnlyRecords() throws NoSuchMethodException {
        Query query = getQuery(
                "findTodayNewOrdersByStoreId",
                Long.class,
                LocalDateTime.class,
                LocalDateTime.class
        );

        assertCreatedAtOnlyStoreScopedWindow(query.value(), "startOfDay", "endOfDay");
        assertTrue(query.value().contains("ORDER BY r.createdAt DESC"));
    }

    @Test
    void storeScopedCreatedAtBetweenList_shouldIgnoreActualCheckInOnlyRecords() throws NoSuchMethodException {
        Query query = getQuery(
                "findByStoreIdAndCreatedAtBetween",
                Long.class,
                LocalDateTime.class,
                LocalDateTime.class
        );

        assertCreatedAtOnlyStoreScopedWindow(query.value(), "start", "end");
        assertTrue(query.value().contains("ORDER BY r.createdAt DESC"));
    }

    private static Query getQuery(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = ReservationRepository.class.getMethod(methodName, parameterTypes);
        Query query = method.getAnnotation(Query.class);

        assertNotNull(query, methodName + " must keep an explicit store-scoped today-new query");
        return query;
    }

    private static void assertCreatedAtOnlyStoreScopedWindow(
            String query,
            String startParameter,
            String endParameter
    ) {
        assertTrue(query.contains("r.storeId = :storeId"));
        assertTrue(query.contains("r.createdAt >= :" + startParameter));
        assertTrue(query.contains("r.createdAt < :" + endParameter));
        assertFalse(
                query.contains("actualCheckIn"),
                "actualCheckIn-only reservations must not be counted as store-scoped today-new"
        );
    }
}
