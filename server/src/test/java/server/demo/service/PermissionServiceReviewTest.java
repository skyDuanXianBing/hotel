package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.entity.StoreUser;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class PermissionServiceReviewTest {

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
    void activeMemberCanViewButCannotWriteWithoutExplicitPermission() {
        StoreUser member = member(8L, "member", true);
        when(memberships.findByStoreIdAndUserId(10L, 8L)).thenReturn(Optional.of(member));
        when(extraPermissions.findByStoreUser_Id(8L)).thenReturn(List.of());

        assertTrue(service.hasPermission(10L, 8L, PermissionModule.REVIEW, PermissionAction.VIEW));
        assertFalse(service.hasPermission(10L, 8L, PermissionModule.REVIEW, PermissionAction.REPLY));
        assertFalse(service.hasPermission(10L, 8L, PermissionModule.REVIEW, PermissionAction.REVIEW_GUEST));
        assertFalse(service.hasPermission(10L, 8L, PermissionModule.REVIEW, PermissionAction.SYNC));
    }

    @Test
    void inactiveMemberCannotEvenViewReviews() {
        StoreUser member = member(8L, "member", false);
        when(memberships.findByStoreIdAndUserId(10L, 8L)).thenReturn(Optional.of(member));

        assertFalse(service.hasPermission(10L, 8L, PermissionModule.REVIEW, PermissionAction.VIEW));
    }

    private static StoreUser member(Long id, String role, boolean active) {
        StoreUser user = new StoreUser();
        user.setId(id);
        user.setRole(role);
        user.setIsActive(active);
        return user;
    }
}
