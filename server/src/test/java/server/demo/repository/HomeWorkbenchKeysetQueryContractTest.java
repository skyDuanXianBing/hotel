package server.demo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeWorkbenchKeysetQueryContractTest {
    @Test
    void allWorkbenchSourcesExposeListSlicesWithStableCaseCoalesceAndIdTieBreak() {
        assertQuery(CleaningTaskRepository.class, "findHomeSlice",
                "case when ct.status", "coalesce(ct.estimatedtime", "ct.id>:cursorid", "order by");
        assertQuery(ReservationRepository.class, "findHomeOrderSlice",
                "case when r.room is null", "r.checkindate=:cursordate", "r.id>:cursorid", "order by");
        assertQuery(RegistrationFormRepository.class, "findHomeSlice",
                "case when f.status", "coalesce(", "f.id > :cursorid", "order by");
        assertQuery(InternalTaskRepository.class, "findHomeSlice",
                "case when t.status", "coalesce(t.completedat, t.updatedat)", "t.id > :cursorid", "order by");
    }

    @Test
    void internalTaskQueriesUseOneCreatorOrAssigneeVisibilityPredicate() {
        assertQueryContains(InternalTaskRepository.class, "findVisibleToUser",
                "t.createdbyuserid=:userid or t.assigneeuserid=:userid");
        assertQueryContains(InternalTaskRepository.class, "countVisibleToUser",
                "t.createdbyuserid=:userid or t.assigneeuserid=:userid");
        assertQueryContains(InternalTaskRepository.class, "findHomeSlice",
                ":manager = true or t.createdbyuserid = :userid or t.assigneeuserid = :userid");
        assertQueryContains(InternalTaskRepository.class, "countHome",
                ":manager=true or t.createdbyuserid=:userid or t.assigneeuserid=:userid");
    }

    private static void assertQueryContains(Class<?> repository, String methodName, String fragment) {
        Method method = Arrays.stream(repository.getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
        Query query = method.getAnnotation(Query.class);
        String sql = query.value().toLowerCase().replaceAll("\\s+", " ");
        assertTrue(sql.contains(fragment), repository.getSimpleName() + " missing " + fragment);
    }

    private static void assertQuery(Class<?> repository, String methodName, String... fragments) {
        Method method = Arrays.stream(repository.getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
        Query query = method.getAnnotation(Query.class);
        String sql = query.value().toLowerCase().replaceAll("\\s+", " ");
        assertEquals(List.class, method.getReturnType());
        assertTrue(Arrays.stream(method.getParameterTypes()).anyMatch(Pageable.class::equals));
        for (String fragment : fragments) {
            assertTrue(sql.contains(fragment), repository.getSimpleName() + " missing " + fragment);
        }
    }
}
