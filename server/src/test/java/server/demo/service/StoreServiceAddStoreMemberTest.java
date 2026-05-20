package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.AddStoreMemberRequest;
import server.demo.dto.PermissionDTO;
import server.demo.dto.StoreUserDTO;
import server.demo.entity.Role;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.entity.User;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.RoleRepository;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceAddStoreMemberTest {

    private StoreService service;
    private StoreUserRepository storeUserRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private StoreUserPermissionRepository storeUserPermissionRepository;

    @BeforeEach
    void setUp() {
        service = new StoreService();
        storeUserRepository = Mockito.mock(StoreUserRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        roleRepository = Mockito.mock(RoleRepository.class);
        storeUserPermissionRepository = Mockito.mock(StoreUserPermissionRepository.class);

        ReflectionTestUtils.setField(service, "storeUserRepository", storeUserRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(service, "storeUserPermissionRepository", storeUserPermissionRepository);
    }

    @Test
    void addStoreMember_shouldRestoreInactiveMemberAndOverwritePermissions() {
        Store store = createStore(26L);
        User operatorUser = createUser(6L, "owner@example.com");
        User invitedUser = createUser(5L, "284033031@qq.com");

        StoreUser operator = new StoreUser();
        operator.setStore(store);
        operator.setUser(operatorUser);
        operator.setRole("owner");
        operator.setIsActive(true);

        Role oldRole = createRole(99L, 26L, "old-role");
        Role newRole = createRole(101L, 26L, "frontdesk");

        StoreUser inactiveMember = new StoreUser();
        inactiveMember.setId(44L);
        inactiveMember.setStore(store);
        inactiveMember.setUser(invitedUser);
        inactiveMember.setRole("admin");
        inactiveMember.setIsActive(false);
        inactiveMember.setInvitedBy(2L);
        inactiveMember.setRoles(Set.of(oldRole));

        AddStoreMemberRequest request = new AddStoreMemberRequest();
        request.setEmail("284033031@qq.com");
        request.setRole("member");
        request.setRoleIds(List.of(101L));

        PermissionDTO permissionDTO = new PermissionDTO();
        permissionDTO.setModule(PermissionModule.ORDER);
        permissionDTO.setAction(PermissionAction.VIEW_ORDERS);
        request.setExtraPermissions(List.of(permissionDTO));

        StoreUserPermission persistedPermission = new StoreUserPermission();
        persistedPermission.setStoreUser(inactiveMember);
        persistedPermission.setModule(PermissionModule.ORDER);
        persistedPermission.setAction(PermissionAction.VIEW_ORDERS);
        persistedPermission.setRoomTypeId(0L);
        persistedPermission.setAllRoomTypes(false);

        when(storeUserRepository.findByStoreIdAndUserId(26L, 6L)).thenReturn(Optional.of(operator));
        when(userRepository.findByEmail("284033031@qq.com")).thenReturn(Optional.of(invitedUser));
        when(storeUserRepository.findByStoreIdAndUserId(26L, 5L)).thenReturn(Optional.of(inactiveMember));
        when(roleRepository.findById(101L)).thenReturn(Optional.of(newRole));
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserPermissionRepository.findByStoreUser_Id(44L)).thenReturn(List.of(persistedPermission));

        StoreUserDTO result = service.addStoreMember(26L, 6L, request);

        assertEquals(44L, result.getId());
        assertEquals("member", result.getRole());
        assertTrue(Boolean.TRUE.equals(result.getIsActive()));
        assertEquals(6L, result.getInvitedBy());
        assertEquals(1, result.getRoles().size());
        assertEquals(101L, result.getRoles().get(0).getId());
        assertEquals(1, result.getExtraPermissions().size());
        assertEquals(PermissionModule.ORDER, result.getExtraPermissions().get(0).getModule());
        assertEquals(PermissionAction.VIEW_ORDERS, result.getExtraPermissions().get(0).getAction());

        ArgumentCaptor<StoreUser> storeUserCaptor = ArgumentCaptor.forClass(StoreUser.class);
        verify(storeUserRepository).save(storeUserCaptor.capture());
        StoreUser restored = storeUserCaptor.getValue();
        assertTrue(Boolean.TRUE.equals(restored.getIsActive()));
        assertEquals("member", restored.getRole());
        assertEquals(1, restored.getRoles().size());
        assertTrue(restored.getRoles().stream().anyMatch(role -> Long.valueOf(101L).equals(role.getId())));

        verify(storeUserPermissionRepository).deleteByStoreUser_Id(44L);
        verify(storeUserPermissionRepository).saveAll(any());
        verify(storeUserRepository, never()).existsByStoreIdAndUserId(26L, 5L);
    }

    @Test
    void addStoreMember_shouldRejectActiveExistingMember() {
        Store store = createStore(26L);
        User operatorUser = createUser(6L, "owner@example.com");
        User invitedUser = createUser(5L, "member@example.com");

        StoreUser operator = new StoreUser();
        operator.setStore(store);
        operator.setUser(operatorUser);
        operator.setRole("owner");
        operator.setIsActive(true);

        StoreUser activeMember = new StoreUser();
        activeMember.setId(45L);
        activeMember.setStore(store);
        activeMember.setUser(invitedUser);
        activeMember.setRole("member");
        activeMember.setIsActive(true);

        AddStoreMemberRequest request = new AddStoreMemberRequest();
        request.setEmail("member@example.com");
        request.setRole("member");

        when(storeUserRepository.findByStoreIdAndUserId(26L, 6L)).thenReturn(Optional.of(operator));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(invitedUser));
        when(storeUserRepository.findByStoreIdAndUserId(26L, 5L)).thenReturn(Optional.of(activeMember));
        when(storeUserRepository.existsByStoreIdAndUserId(26L, 5L)).thenReturn(true);

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.addStoreMember(26L, 6L, request)
        );

        assertEquals("该用户已是门店成员", error.getMessage());
        verify(storeUserRepository, never()).save(any(StoreUser.class));
        verify(storeUserPermissionRepository, never()).deleteByStoreUser_Id(any());
    }

    private static Store createStore(Long id) {
        Store store = new Store();
        store.setId(id);
        return store;
    }

    private static User createUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(email);
        user.setNickname(email);
        user.setIsActive(true);
        return user;
    }

    private static Role createRole(Long id, Long storeId, String name) {
        Role role = new Role();
        role.setId(id);
        role.setStoreId(storeId);
        role.setName(name);
        role.setIsSystem(false);
        return role;
    }
}
