package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.PermissionDTO;
import server.demo.entity.Role;
import server.demo.entity.RolePermission;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreServiceCurrentUserPermissionsTest {

    private StoreService service;
    private StoreUserRepository storeUserRepository;
    private StoreUserPermissionRepository storeUserPermissionRepository;

    @BeforeEach
    void setUp() {
        service = new StoreService();
        storeUserRepository = Mockito.mock(StoreUserRepository.class);
        storeUserPermissionRepository = Mockito.mock(StoreUserPermissionRepository.class);

        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "storeUserPermissionRepository", storeUserPermissionRepository);
    }

    @Test
    void getCurrentUserEffectivePermissions_memberMergesRoleAndExtraPermissions() {
        StoreUser storeUser = new StoreUser();
        storeUser.setId(8L);
        storeUser.setRole("member");
        storeUser.setRoles(Set.of(createRoleWithPermissions()));

        StoreUserPermission extraOrderModify = new StoreUserPermission();
        extraOrderModify.setModule(PermissionModule.ORDER);
        extraOrderModify.setAction(PermissionAction.MODIFY_ORDER);
        extraOrderModify.setRoomTypeId(0L);

        StoreUserPermission extraRoomStatusAll = new StoreUserPermission();
        extraRoomStatusAll.setModule(PermissionModule.ACCOMMODATION);
        extraRoomStatusAll.setAction(PermissionAction.VIEW_ROOM_STATUS);
        extraRoomStatusAll.setRoomTypeId(0L);
        extraRoomStatusAll.setAllRoomTypes(true);

        Mockito.when(storeUserRepository.findByStoreIdAndUserId(11L, 21L)).thenReturn(Optional.of(storeUser));
        Mockito.when(storeUserPermissionRepository.findByStoreUser_Id(8L))
                .thenReturn(List.of(extraOrderModify, extraRoomStatusAll));

        List<PermissionDTO> result = service.getCurrentUserEffectivePermissions(11L, 21L);

        assertTrue(result.stream().anyMatch(permission ->
                permission.getModule() == PermissionModule.ORDER
                        && permission.getAction() == PermissionAction.VIEW_ORDERS
        ));
        assertTrue(result.stream().anyMatch(permission ->
                permission.getModule() == PermissionModule.ORDER
                        && permission.getAction() == PermissionAction.MODIFY_ORDER
        ));

        List<PermissionDTO> roomStatusPermissions = result.stream()
                .filter(permission -> permission.getModule() == PermissionModule.ACCOMMODATION
                        && permission.getAction() == PermissionAction.VIEW_ROOM_STATUS)
                .toList();
        assertEquals(1, roomStatusPermissions.size());
        assertTrue(Boolean.TRUE.equals(roomStatusPermissions.get(0).getAllRoomTypes()));
        assertEquals(0L, roomStatusPermissions.get(0).getRoomTypeId());
    }

    @Test
    void getCurrentUserEffectivePermissions_ownerReturnsManagerPermissions() {
        StoreUser storeUser = new StoreUser();
        storeUser.setRole("owner");

        Mockito.when(storeUserRepository.findByStoreIdAndUserId(11L, 21L)).thenReturn(Optional.of(storeUser));

        List<PermissionDTO> result = service.getCurrentUserEffectivePermissions(11L, 21L);

        assertTrue(result.stream().anyMatch(permission ->
                permission.getModule() == PermissionModule.ORDER
                        && permission.getAction() == PermissionAction.VIEW_ORDERS
        ));
        assertTrue(result.stream().anyMatch(permission ->
                permission.getModule() == PermissionModule.STATISTICS
                        && permission.getAction() == PermissionAction.VIEW_STATS
        ));
        assertTrue(result.stream().anyMatch(permission ->
                permission.getModule() == PermissionModule.SETTINGS
                        && permission.getAction() == PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS
        ));
        assertTrue(result.stream().anyMatch(permission ->
                permission.getModule() == PermissionModule.SENSITIVE
                        && permission.getAction() == PermissionAction.VIEW_FINANCIAL_DATA
        ));
        assertTrue(result.stream().anyMatch(permission ->
                permission.getModule() == PermissionModule.ACCOMMODATION
                        && permission.getAction() == PermissionAction.VIEW_ROOM_STATUS
                        && Boolean.TRUE.equals(permission.getAllRoomTypes())
        ));
    }

    private static Role createRoleWithPermissions() {
        Role role = new Role();
        Set<RolePermission> permissions = new HashSet<>();

        RolePermission viewOrders = new RolePermission();
        viewOrders.setRole(role);
        viewOrders.setModule(PermissionModule.ORDER);
        viewOrders.setAction(PermissionAction.VIEW_ORDERS);
        viewOrders.setRoomTypeId(0L);
        permissions.add(viewOrders);

        RolePermission viewRoomStatusForSpecificType = new RolePermission();
        viewRoomStatusForSpecificType.setRole(role);
        viewRoomStatusForSpecificType.setModule(PermissionModule.ACCOMMODATION);
        viewRoomStatusForSpecificType.setAction(PermissionAction.VIEW_ROOM_STATUS);
        viewRoomStatusForSpecificType.setRoomTypeId(15L);
        viewRoomStatusForSpecificType.setAllRoomTypes(false);
        permissions.add(viewRoomStatusForSpecificType);

        role.setRolePermissions(permissions);
        return role;
    }
}
