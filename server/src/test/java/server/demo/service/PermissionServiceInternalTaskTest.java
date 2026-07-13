package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class PermissionServiceInternalTaskTest {
    private PermissionService service;
    private StoreUserRepository memberships;
    private StoreUserPermissionRepository extraPermissions;

    @BeforeEach
    void setUp() {
        service = new PermissionService();
        memberships = Mockito.mock(StoreUserRepository.class);
        extraPermissions = Mockito.mock(StoreUserPermissionRepository.class);
        ReflectionTestUtils.setField(service, "storeUserRepository", memberships);
        ReflectionTestUtils.setField(service, "storeUserPermissionRepository", extraPermissions);
    }

    @Test
    void ownerHasImplicitPermissionButAdminNeedsExplicitMemberPermission() {
        StoreUser owner = membership(1L, "owner", true);
        when(memberships.findByStoreIdAndUserId(10L, 1L)).thenReturn(Optional.of(owner));
        assertTrue(hasCreatePermission(1L));

        StoreUser admin = membership(2L, "admin", true);
        when(memberships.findByStoreIdAndUserId(10L, 2L)).thenReturn(Optional.of(admin));
        when(extraPermissions.findByStoreUser_Id(2L)).thenReturn(List.of());
        assertFalse(hasCreatePermission(2L));

        StoreUserPermission explicit = new StoreUserPermission(
                admin, PermissionModule.ACCOMMODATION, PermissionAction.CREATE_INTERNAL_TASK);
        when(extraPermissions.findByStoreUser_Id(2L)).thenReturn(List.of(explicit));
        assertTrue(hasCreatePermission(2L));
    }

    @Test
    void roleSourceAndInactiveMembershipNeverGrantProtectedPermission() {
        StoreUser member = membership(3L, "member", false);
        StoreUserPermission explicit = new StoreUserPermission(
                member, PermissionModule.ACCOMMODATION, PermissionAction.CREATE_INTERNAL_TASK);
        when(memberships.findByStoreIdAndUserId(10L, 3L)).thenReturn(Optional.of(member));
        when(extraPermissions.findByStoreUser_Id(3L)).thenReturn(List.of(explicit));
        assertFalse(hasCreatePermission(3L));
    }

    @Test
    void otherAdminPermissionsKeepExistingManagerShortcut() {
        StoreUser admin = membership(2L, "admin", true);
        when(memberships.findByStoreIdAndUserId(10L, 2L)).thenReturn(Optional.of(admin));
        assertTrue(service.hasPermission(10L, 2L, PermissionModule.ORDER, PermissionAction.VIEW_ORDERS));
    }

    private boolean hasCreatePermission(Long userId) {
        return service.hasPermission(10L, userId, PermissionModule.ACCOMMODATION,
                PermissionAction.CREATE_INTERNAL_TASK);
    }

    private static StoreUser membership(Long id, String role, boolean active) {
        StoreUser storeUser = new StoreUser();
        storeUser.setId(id);
        storeUser.setRole(role);
        storeUser.setIsActive(active);
        return storeUser;
    }
}
