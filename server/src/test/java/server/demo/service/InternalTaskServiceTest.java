package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import server.demo.context.*;
import server.demo.dto.internaltask.InternalTaskCreateRequest;
import server.demo.entity.*;
import server.demo.enums.InternalTaskStatus;
import server.demo.exception.InternalTaskNotFoundException;
import server.demo.repository.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class InternalTaskServiceTest {
    private final InternalTaskRepository tasks = Mockito.mock(InternalTaskRepository.class);
    private final StoreUserRepository memberships = Mockito.mock(StoreUserRepository.class);
    private final UserRepository users = Mockito.mock(UserRepository.class);
    private final CleanerRepository cleaners = Mockito.mock(CleanerRepository.class);
    private final InternalTaskService service = new InternalTaskService(tasks, memberships, users, cleaners);

    @AfterEach void clearContext() { StoreContextHolder.clear(); }

    @Test
    void create_shouldScopeTaskAndAssignOnlyActiveSameStoreUser() {
        User owner = user(1L, "店长"); User employee = user(2L, "员工");
        StoreContextHolder.setContext(new StoreContext(1L, 10L, "owner"));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(users.findById(2L)).thenReturn(Optional.of(employee));
        when(memberships.findByStoreIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership(owner, "owner")));
        when(memberships.findByStoreIdAndUserId(10L, 2L)).thenReturn(Optional.of(membership(employee, "member")));
        when(cleaners.findByUserIdAndStoreId(2L, 10L)).thenReturn(java.util.List.of());
        when(tasks.save(any(InternalTask.class))).thenAnswer(invocation -> { InternalTask t = invocation.getArgument(0); t.setId(99L); t.setVersion(0L); return t; });

        InternalTaskCreateRequest request = new InternalTaskCreateRequest(); request.setTitle("检查仓库"); request.setAssigneeUserId(2L);
        var result = service.create(request);

        assertEquals(99L, result.getId()); assertEquals(InternalTaskStatus.ASSIGNED, result.getStatus());
        assertEquals(2L, result.getAssigneeUserId());
    }

    @Test
    void complete_shouldReturnExistingCompletionWithoutChangingAuditAgain() {
        User employee = user(2L, "员工"); StoreContextHolder.setContext(new StoreContext(2L, 10L, "member"));
        when(users.findById(2L)).thenReturn(Optional.of(employee));
        when(memberships.findByStoreIdAndUserId(10L, 2L)).thenReturn(Optional.of(membership(employee, "member")));
        InternalTask completed = task(99L, 10L, 2L, InternalTaskStatus.COMPLETED); completed.setCompletedAt(LocalDateTime.of(2026, 7, 12, 9, 0));
        when(tasks.completeAssigned(eq(99L), eq(10L), eq(2L), anyString(), any(), eq(InternalTaskStatus.ASSIGNED), eq(InternalTaskStatus.COMPLETED))).thenReturn(0);
        when(tasks.findByIdAndStoreId(99L, 10L)).thenReturn(Optional.of(completed));

        var result = service.complete(99L);
        assertEquals(LocalDateTime.of(2026, 7, 12, 9, 0), result.getCompletedAt());
    }

    @Test
    void complete_shouldHideTaskAfterReassignment() {
        User employee = user(2L, "员工"); StoreContextHolder.setContext(new StoreContext(2L, 10L, "member"));
        when(users.findById(2L)).thenReturn(Optional.of(employee));
        when(memberships.findByStoreIdAndUserId(10L, 2L)).thenReturn(Optional.of(membership(employee, "member")));
        when(tasks.completeAssigned(anyLong(), anyLong(), anyLong(), anyString(), any(), any(), any())).thenReturn(0);
        when(tasks.findByIdAndStoreId(99L, 10L)).thenReturn(Optional.of(task(99L, 10L, 3L, InternalTaskStatus.ASSIGNED)));
        assertThrows(InternalTaskNotFoundException.class, () -> service.complete(99L));
    }

    @Test
    void getManaged_shouldReturnStoreScopedStatusCounts() {
        User owner = user(1L, "店长");
        StoreContextHolder.setContext(new StoreContext(1L, 10L, "owner"));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(memberships.findByStoreIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(membership(owner, "owner")));
        InternalTask assignedTask = task(99L, 10L, 2L, InternalTaskStatus.ASSIGNED);
        when(tasks.findByStoreIdAndArchivedAtIsNull(eq(10L), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(assignedTask)));
        when(tasks.countByStoreIdAndStatusAndArchivedAtIsNull(10L, InternalTaskStatus.ASSIGNED))
                .thenReturn(4L);
        when(tasks.countByStoreIdAndStatusAndArchivedAtIsNull(10L, InternalTaskStatus.COMPLETED))
                .thenReturn(7L);

        var result = service.getManaged(null, 0, 20);

        assertEquals(4L, result.getAssignedCount());
        assertEquals(7L, result.getCompletedCount());
        assertEquals(1, result.getItems().size());
        Mockito.verify(tasks).countByStoreIdAndStatusAndArchivedAtIsNull(
                10L, InternalTaskStatus.ASSIGNED);
        Mockito.verify(tasks).countByStoreIdAndStatusAndArchivedAtIsNull(
                10L, InternalTaskStatus.COMPLETED);
    }

    @Test
    void getManaged_shouldRejectInactiveStoreMembership() {
        User owner = user(1L, "店长");
        StoreContextHolder.setContext(new StoreContext(1L, 10L, "owner"));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        StoreUser inactive = membership(owner, "owner"); inactive.setIsActive(false);
        when(memberships.findByStoreIdAndUserId(10L, 1L)).thenReturn(Optional.of(inactive));

        assertThrows(server.demo.exception.PermissionDeniedException.class,
                () -> service.getManaged(null, 0, 20));
        Mockito.verifyNoInteractions(tasks);
    }

    private User user(Long id, String name) { User u = new User(); u.setId(id); u.setName(name); u.setEmail("u" + id + "@example.com"); u.setIsActive(true); return u; }
    private StoreUser membership(User user, String role) { Store store = new Store(); store.setId(10L); StoreUser su = new StoreUser(store, user, role); su.setIsActive(true); return su; }
    private InternalTask task(Long id, Long storeId, Long assignee, InternalTaskStatus status) { InternalTask t = new InternalTask(); t.setId(id); t.setStoreId(storeId); t.setAssigneeUserId(assignee); t.setStatus(status); t.setVersion(0L); return t; }
}
