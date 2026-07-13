package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import server.demo.context.*;
import server.demo.dto.internaltask.InternalTaskCreateRequest;
import server.demo.entity.*;
import server.demo.enums.InternalTaskStatus;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
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
    private final PermissionService permissions = Mockito.mock(PermissionService.class);
    private final InternalTaskService service = new InternalTaskService(tasks, memberships, users, cleaners, permissions);

    @AfterEach void clearContext() { StoreContextHolder.clear(); }

    @Test
    void create_shouldScopeTaskAndAssignOnlyActiveSameStoreUser() {
        User owner = user(1L, "店长"); User employee = user(2L, "员工");
        StoreContextHolder.setContext(new StoreContext(1L, 10L, "owner"));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(users.findById(2L)).thenReturn(Optional.of(employee));
        when(memberships.findByStoreIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership(owner, "owner")));
        when(permissions.hasPermission(10L, 1L, PermissionModule.ACCOMMODATION,
                PermissionAction.CREATE_INTERNAL_TASK)).thenReturn(true);
        when(memberships.findByStoreIdAndUserId(10L, 2L)).thenReturn(Optional.of(membership(employee, "member")));
        when(cleaners.findByUserIdAndStoreId(2L, 10L)).thenReturn(java.util.List.of());
        when(tasks.save(any(InternalTask.class))).thenAnswer(invocation -> { InternalTask t = invocation.getArgument(0); t.setId(99L); t.setVersion(0L); return t; });

        InternalTaskCreateRequest request = new InternalTaskCreateRequest(); request.setTitle("检查仓库"); request.setAssigneeUserId(2L);
        var result = service.create(request);

        assertEquals(99L, result.getId()); assertEquals(InternalTaskStatus.ASSIGNED, result.getStatus());
        assertEquals(2L, result.getAssigneeUserId());
    }

    @Test
    void create_shouldRejectUnauthorizedMemberBeforeTaskOrAssigneeAccess() {
        User member = user(2L, "员工");
        StoreContextHolder.setContext(new StoreContext(2L, 10L, "member"));
        when(users.findById(2L)).thenReturn(Optional.of(member));
        when(memberships.findByStoreIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(membership(member, "member")));
        when(permissions.hasPermission(10L, 2L, PermissionModule.ACCOMMODATION,
                PermissionAction.CREATE_INTERNAL_TASK)).thenReturn(false);

        InternalTaskCreateRequest request = new InternalTaskCreateRequest();
        request.setTitle("不应创建");
        request.setAssigneeUserId(99L);

        assertThrows(server.demo.exception.PermissionDeniedException.class,
                () -> service.create(request));
        Mockito.verify(tasks, Mockito.never()).save(any());
        Mockito.verify(cleaners, Mockito.never()).findByUserIdAndStoreId(anyLong(), anyLong());
        Mockito.verify(memberships, Mockito.never()).findByStoreIdAndUserId(10L, 99L);
    }

    @Test
    void create_shouldAllowExplicitlyAuthorizedMemberWithoutManagerCapability() {
        User member = user(2L, "员工");
        StoreContextHolder.setContext(new StoreContext(2L, 10L, "member"));
        when(users.findById(2L)).thenReturn(Optional.of(member));
        when(memberships.findByStoreIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(membership(member, "member")));
        when(permissions.hasPermission(10L, 2L, PermissionModule.ACCOMMODATION,
                PermissionAction.CREATE_INTERNAL_TASK)).thenReturn(true);
        when(tasks.save(any(InternalTask.class))).thenAnswer(invocation -> {
            InternalTask task = invocation.getArgument(0);
            task.setId(100L);
            task.setVersion(0L);
            return task;
        });

        InternalTaskCreateRequest request = new InternalTaskCreateRequest();
        request.setTitle("顺手任务");
        var result = service.create(request);

        assertEquals(100L, result.getId());
        assertFalse(result.isCanManage());
        assertThrows(server.demo.exception.PermissionDeniedException.class,
                () -> service.getManaged(null, 0, 20));
    }

    @Test
    void getAssignees_shouldRequireCreatePermissionBeforeReadingCandidates() {
        User cleaner = user(3L, "保洁员");
        StoreContextHolder.setContext(new StoreContext(3L, 10L, "member"));
        when(users.findById(3L)).thenReturn(Optional.of(cleaner));
        when(memberships.findByStoreIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(membership(cleaner, "member")));
        when(permissions.hasPermission(10L, 3L, PermissionModule.ACCOMMODATION,
                PermissionAction.CREATE_INTERNAL_TASK)).thenReturn(false);

        assertThrows(server.demo.exception.PermissionDeniedException.class, service::getAssignees);
        Mockito.verify(cleaners, Mockito.never()).findByStoreIdAndIsActiveTrue(anyLong());
        Mockito.verify(memberships, Mockito.never()).findActiveUsersByStoreId(anyLong());
    }

    @Test
    void create_shouldRejectCrossStoreAssigneeWithoutGlobalUserLookupOrTaskWrite() {
        User owner = user(1L, "店长");
        StoreContextHolder.setContext(new StoreContext(1L, 10L, "owner"));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(memberships.findByStoreIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(membership(owner, "owner")));
        when(memberships.findByStoreIdAndUserId(10L, 99L)).thenReturn(Optional.empty());
        when(permissions.hasPermission(10L, 1L, PermissionModule.ACCOMMODATION,
                PermissionAction.CREATE_INTERNAL_TASK)).thenReturn(true);

        InternalTaskCreateRequest request = new InternalTaskCreateRequest();
        request.setTitle("跨店执行人");
        request.setAssigneeUserId(99L);

        assertThrows(server.demo.exception.InternalTaskConflictException.class,
                () -> service.create(request));
        Mockito.verify(users, Mockito.never()).findById(99L);
        Mockito.verify(tasks, Mockito.never()).save(any());
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
    void creatorOnly_shouldSeeMineHomeCountAndDetailWithoutCompleteOrManage() {
        User creator = user(2L, "创建人");
        StoreContextHolder.setContext(new StoreContext(2L, 10L, "member"));
        when(users.findById(2L)).thenReturn(Optional.of(creator));
        when(memberships.findByStoreIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(membership(creator, "member")));
        InternalTask creatorOnly = task(101L, 10L, 3L, InternalTaskStatus.ASSIGNED);
        creatorOnly.setCreatedByUserId(2L);
        when(tasks.findVisibleToUser(eq(10L), eq(2L), eq(InternalTaskStatus.ASSIGNED), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(creatorOnly)));
        when(tasks.countVisibleToUser(10L, 2L, InternalTaskStatus.ASSIGNED)).thenReturn(1L);
        when(tasks.countVisibleToUser(10L, 2L, InternalTaskStatus.COMPLETED)).thenReturn(0L);
        when(tasks.findHomeSlice(eq(10L), eq(2L), eq(false), eq(InternalTaskStatus.ASSIGNED), any(),
                eq(false), eq(0), any(), eq(0L), any()))
                .thenReturn(java.util.List.of(creatorOnly));
        when(tasks.countHome(10L, 2L, false, InternalTaskStatus.ASSIGNED, null)).thenReturn(1L);
        when(tasks.findByIdAndStoreId(101L, 10L)).thenReturn(Optional.of(creatorOnly));

        var mine = service.getMine(InternalTaskStatus.ASSIGNED, 0, 20);
        var home = service.listHomeSlice(InternalTaskStatus.ASSIGNED,
                LocalDateTime.of(2026, 7, 5, 0, 0), null, null, null, 50);
        long count = service.countHome(InternalTaskStatus.ASSIGNED,
                LocalDateTime.of(2026, 7, 5, 0, 0));
        var detail = service.getById(101L);

        assertEquals(1, mine.getItems().size());
        assertFalse(mine.getItems().get(0).isCanComplete());
        assertFalse(mine.getItems().get(0).isCanManage());
        assertEquals(1, home.size());
        assertEquals(1L, count);
        assertFalse(detail.isCanComplete());
        assertFalse(detail.isCanManage());

        when(tasks.completeAssigned(eq(101L), eq(10L), eq(2L), anyString(), any(),
                eq(InternalTaskStatus.ASSIGNED), eq(InternalTaskStatus.COMPLETED))).thenReturn(0);
        assertThrows(InternalTaskNotFoundException.class, () -> service.complete(101L));
    }

    @Test
    void unrelatedMember_shouldNotReadCreatorOrAssigneeTaskDetail() {
        User third = user(4L, "第三人");
        StoreContextHolder.setContext(new StoreContext(4L, 10L, "member"));
        when(users.findById(4L)).thenReturn(Optional.of(third));
        when(memberships.findByStoreIdAndUserId(10L, 4L))
                .thenReturn(Optional.of(membership(third, "member")));
        InternalTask task = task(102L, 10L, 3L, InternalTaskStatus.ASSIGNED);
        task.setCreatedByUserId(2L);
        when(tasks.findByIdAndStoreId(102L, 10L)).thenReturn(Optional.of(task));

        assertThrows(InternalTaskNotFoundException.class, () -> service.getById(102L));
    }

    @Test
    void assigneeOnly_shouldSeeMineAndKeepCompleteCapability() {
        User assignee = user(3L, "执行人");
        StoreContextHolder.setContext(new StoreContext(3L, 10L, "member"));
        when(users.findById(3L)).thenReturn(Optional.of(assignee));
        when(memberships.findByStoreIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(membership(assignee, "member")));
        InternalTask task = task(103L, 10L, 3L, InternalTaskStatus.ASSIGNED);
        task.setCreatedByUserId(2L);
        when(tasks.findVisibleToUser(eq(10L), eq(3L), eq(InternalTaskStatus.ASSIGNED), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(task)));
        when(tasks.countVisibleToUser(10L, 3L, InternalTaskStatus.ASSIGNED)).thenReturn(1L);
        when(tasks.countVisibleToUser(10L, 3L, InternalTaskStatus.COMPLETED)).thenReturn(0L);

        var mine = service.getMine(InternalTaskStatus.ASSIGNED, 0, 20);

        assertEquals(1, mine.getItems().size());
        assertTrue(mine.getItems().get(0).isCanComplete());
        assertFalse(mine.getItems().get(0).isCanManage());
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
