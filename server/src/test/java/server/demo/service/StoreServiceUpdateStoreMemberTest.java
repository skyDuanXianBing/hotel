package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import server.demo.dto.PermissionDTO;
import server.demo.dto.StoreUserDTO;
import server.demo.dto.UpdateStoreMemberPermissionRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceUpdateStoreMemberTest {

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
    void updateStoreMemberPermission_shouldUpdateUserNameAndReturnIt() {
        Store store = createStore(26L);
        StoreUser operator = createStoreUser(60L, store, createUser(6L, "owner@example.com"), "owner");
        User targetUser = createUser(5L, "member@example.com");
        targetUser.setName("Old Name");
        StoreUser target = createStoreUser(44L, store, targetUser, "member");

        UpdateStoreMemberPermissionRequest request = new UpdateStoreMemberPermissionRequest();
        request.setName("  Jane Doe  ");

        mockMemberLookup(operator, target);
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserPermissionRepository.findByStoreUser_Id(44L)).thenReturn(List.of());

        StoreUserDTO result = service.updateStoreMemberPermission(26L, 6L, 5L, request);

        assertEquals("Jane Doe", targetUser.getName());
        assertEquals("Jane Doe", result.getUser().getName());
        assertEquals("member@example.com", result.getUser().getEmail());
        assertEquals("member@example.com", result.getUser().getUsername());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("Jane Doe", userCaptor.getValue().getName());
        assertEquals("member@example.com", userCaptor.getValue().getEmail());
        assertEquals("member@example.com", userCaptor.getValue().getUsername());
        verify(storeUserPermissionRepository, never()).deleteByStoreUser_Id(44L);
        verify(storeUserPermissionRepository, never()).deleteAll(Mockito.<Iterable<StoreUserPermission>>any());
        verify(storeUserPermissionRepository, never()).saveAll(any());
    }

    @Test
    void updateStoreMemberPermission_shouldUpdateOnlyExtraPermissionDiff() {
        Store store = createStore(26L);
        StoreUser operator = createStoreUser(60L, store, createUser(6L, "owner@example.com"), "owner");
        StoreUser target = createStoreUser(44L, store, createUser(5L, "member@example.com"), "member");
        StoreUserPermission existing = createStoreUserPermission(
                target,
                PermissionModule.ACCOMMODATION,
                PermissionAction.VIEW_ROOM_STATUS,
                0L,
                true
        );
        StoreUserPermission added = createStoreUserPermission(
                target,
                PermissionModule.ORDER,
                PermissionAction.VIEW_ORDERS,
                0L,
                false
        );

        UpdateStoreMemberPermissionRequest request = new UpdateStoreMemberPermissionRequest();
        request.setExtraPermissions(List.of(
                createPermissionDTO(
                        PermissionModule.ACCOMMODATION,
                        PermissionAction.VIEW_ROOM_STATUS,
                        null,
                        true
                ),
                createPermissionDTO(PermissionModule.ORDER, PermissionAction.VIEW_ORDERS, null, null)
        ));

        mockMemberLookup(operator, target);
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserPermissionRepository.findByStoreUser_Id(44L))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(existing, added));
        when(storeUserPermissionRepository.saveAll(any())).thenAnswer(invocation -> toPermissionList(invocation.getArgument(0)));

        StoreUserDTO result = service.updateStoreMemberPermission(26L, 6L, 5L, request);

        verify(storeUserPermissionRepository, never()).deleteByStoreUser_Id(44L);
        verify(storeUserPermissionRepository, never()).deleteAll(Mockito.<Iterable<StoreUserPermission>>any());

        ArgumentCaptor<Iterable<StoreUserPermission>> saveCaptor = createPermissionIterableCaptor();
        verify(storeUserPermissionRepository).saveAll(saveCaptor.capture());
        List<StoreUserPermission> saved = toPermissionList(saveCaptor.getValue());
        assertEquals(1, saved.size());
        assertStoreUserPermission(saved.get(0), PermissionModule.ORDER, PermissionAction.VIEW_ORDERS, 0L, false);
        assertPermissionDTO(result.getExtraPermissions(), PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_STATUS, 0L, true);
        assertPermissionDTO(result.getExtraPermissions(), PermissionModule.ORDER, PermissionAction.VIEW_ORDERS, 0L, false);
    }

    @Test
    void updateStoreMemberPermission_shouldUpdateNameAndExtraPermissionsTogether() {
        Store store = createStore(26L);
        StoreUser operator = createStoreUser(60L, store, createUser(6L, "owner@example.com"), "owner");
        User targetUser = createUser(5L, "member@example.com");
        targetUser.setName("Old Name");
        StoreUser target = createStoreUser(44L, store, targetUser, "member");
        StoreUserPermission existing = createStoreUserPermission(
                target,
                PermissionModule.ORDER,
                PermissionAction.VIEW_ORDERS,
                0L,
                false
        );
        StoreUserPermission added = createStoreUserPermission(
                target,
                PermissionModule.SETTINGS,
                PermissionAction.VIEW_SETTINGS,
                0L,
                false
        );

        UpdateStoreMemberPermissionRequest request = new UpdateStoreMemberPermissionRequest();
        request.setName("  New Name  ");
        request.setExtraPermissions(List.of(
                createPermissionDTO(PermissionModule.SETTINGS, PermissionAction.VIEW_SETTINGS, null, null)
        ));

        mockMemberLookup(operator, target);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserPermissionRepository.findByStoreUser_Id(44L))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(added));
        when(storeUserPermissionRepository.saveAll(any())).thenAnswer(invocation -> toPermissionList(invocation.getArgument(0)));

        StoreUserDTO result = service.updateStoreMemberPermission(26L, 6L, 5L, request);

        assertEquals("New Name", targetUser.getName());
        assertEquals("New Name", result.getUser().getName());

        ArgumentCaptor<Iterable<StoreUserPermission>> deleteCaptor = createPermissionIterableCaptor();
        verify(storeUserPermissionRepository).deleteAll(deleteCaptor.capture());
        List<StoreUserPermission> deleted = toPermissionList(deleteCaptor.getValue());
        assertEquals(1, deleted.size());
        assertStoreUserPermission(deleted.get(0), PermissionModule.ORDER, PermissionAction.VIEW_ORDERS, 0L, false);

        ArgumentCaptor<Iterable<StoreUserPermission>> saveCaptor = createPermissionIterableCaptor();
        verify(storeUserPermissionRepository).saveAll(saveCaptor.capture());
        List<StoreUserPermission> saved = toPermissionList(saveCaptor.getValue());
        assertEquals(1, saved.size());
        assertStoreUserPermission(saved.get(0), PermissionModule.SETTINGS, PermissionAction.VIEW_SETTINGS, 0L, false);
        verify(storeUserPermissionRepository, never()).deleteByStoreUser_Id(44L);
        assertPermissionDTO(result.getExtraPermissions(), PermissionModule.SETTINGS, PermissionAction.VIEW_SETTINGS, 0L, false);
    }

    @Test
    void updateStoreMemberPermission_shouldKeepSameExtraPermissionSetWithoutDeleteOrInsert() {
        Store store = createStore(26L);
        StoreUser operator = createStoreUser(60L, store, createUser(6L, "owner@example.com"), "owner");
        StoreUser target = createStoreUser(44L, store, createUser(5L, "member@example.com"), "member");
        StoreUserPermission existing = createStoreUserPermission(
                target,
                PermissionModule.ACCOMMODATION,
                PermissionAction.VIEW_ROOM_STATUS,
                0L,
                true
        );

        UpdateStoreMemberPermissionRequest request = new UpdateStoreMemberPermissionRequest();
        request.setExtraPermissions(List.of(
                createPermissionDTO(
                        PermissionModule.ACCOMMODATION,
                        PermissionAction.VIEW_ROOM_STATUS,
                        0L,
                        true
                )
        ));

        mockMemberLookup(operator, target);
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserPermissionRepository.findByStoreUser_Id(44L))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(existing));

        StoreUserDTO result = service.updateStoreMemberPermission(26L, 6L, 5L, request);

        verify(storeUserPermissionRepository, never()).deleteByStoreUser_Id(44L);
        verify(storeUserPermissionRepository, never()).deleteAll(Mockito.<Iterable<StoreUserPermission>>any());
        verify(storeUserPermissionRepository, never()).saveAll(any());
        assertPermissionDTO(result.getExtraPermissions(), PermissionModule.ACCOMMODATION, PermissionAction.VIEW_ROOM_STATUS, 0L, true);
    }

    @Test
    void getStoreMembersDTO_shouldReturnUserName() {
        Store store = createStore(26L);
        User memberUser = createUser(5L, "member@example.com");
        memberUser.setName("Member Name");
        StoreUser member = createStoreUser(44L, store, memberUser, "member");

        when(storeUserRepository.findAllUsersByStoreId(26L)).thenReturn(List.of(member));
        when(storeUserPermissionRepository.findByStoreUser_IdIn(List.of(44L))).thenReturn(List.of());

        List<StoreUserDTO> result = service.getStoreMembersDTO(26L);

        assertEquals(1, result.size());
        assertEquals("Member Name", result.get(0).getUser().getName());
        assertEquals("member@example.com", result.get(0).getUser().getEmail());
        verify(storeUserRepository, never()).findActiveUsersByStoreId(26L);
    }

    @Test
    void getStoreMembersDTO_shouldIncludeInactiveMembers() {
        Store store = createStore(26L);
        User activeUser = createUser(5L, "active@example.com");
        activeUser.setName("Active Member");
        StoreUser activeMember = createStoreUser(44L, store, activeUser, "member");
        activeMember.setIsActive(true);

        User inactiveUser = createUser(7L, "inactive@example.com");
        inactiveUser.setName("Inactive Member");
        StoreUser inactiveMember = createStoreUser(45L, store, inactiveUser, "member");
        inactiveMember.setIsActive(false);

        when(storeUserRepository.findAllUsersByStoreId(26L)).thenReturn(List.of(activeMember, inactiveMember));
        when(storeUserPermissionRepository.findByStoreUser_IdIn(List.of(44L, 45L))).thenReturn(List.of());

        List<StoreUserDTO> result = service.getStoreMembersDTO(26L);

        assertEquals(2, result.size());
        assertEquals("Active Member", result.get(0).getUser().getName());
        assertTrue(Boolean.TRUE.equals(result.get(0).getIsActive()));
        assertEquals("Inactive Member", result.get(1).getUser().getName());
        assertFalse(Boolean.TRUE.equals(result.get(1).getIsActive()));
        verify(storeUserRepository).findAllUsersByStoreId(26L);
        verify(storeUserRepository, never()).findActiveUsersByStoreId(26L);
    }

    @Test
    void getStoreMembers_shouldKeepUsingActiveOnlyRepositoryMethod() {
        Store store = createStore(26L);
        StoreUser activeMember = createStoreUser(44L, store, createUser(5L, "active@example.com"), "member");

        when(storeUserRepository.findActiveUsersByStoreId(26L)).thenReturn(List.of(activeMember));

        List<StoreUser> result = service.getStoreMembers(26L);

        assertEquals(1, result.size());
        assertEquals(44L, result.get(0).getId());
        verify(storeUserRepository).findActiveUsersByStoreId(26L);
        verify(storeUserRepository, never()).findAllUsersByStoreId(26L);
    }

    @Test
    void updateStoreMemberPermission_shouldKeepUserNameWhenOldPayloadOmitsName() {
        Store store = createStore(26L);
        StoreUser operator = createStoreUser(60L, store, createUser(6L, "admin@example.com"), "admin");
        User targetUser = createUser(5L, "member@example.com");
        targetUser.setName("Existing Name");
        StoreUser target = createStoreUser(44L, store, targetUser, "member");

        UpdateStoreMemberPermissionRequest request = new UpdateStoreMemberPermissionRequest();
        request.setIsActive(false);

        mockMemberLookup(operator, target);
        when(storeUserRepository.save(any(StoreUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeUserPermissionRepository.findByStoreUser_Id(44L)).thenReturn(List.of());

        StoreUserDTO result = service.updateStoreMemberPermission(26L, 6L, 5L, request);

        assertEquals("Existing Name", targetUser.getName());
        assertEquals("Existing Name", result.getUser().getName());
        assertTrue(Boolean.FALSE.equals(result.getIsActive()));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateStoreMemberPermission_shouldRejectNonManagerWhenUpdatingName() {
        Store store = createStore(26L);
        StoreUser operator = createStoreUser(60L, store, createUser(6L, "staff@example.com"), "member");

        UpdateStoreMemberPermissionRequest request = new UpdateStoreMemberPermissionRequest();
        request.setName("New Name");

        when(storeUserRepository.findByStoreIdAndUserId(26L, 6L)).thenReturn(Optional.of(operator));

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.updateStoreMemberPermission(26L, 6L, 5L, request)
        );

        assertEquals("只有管理员可以修改成员权限", error.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(storeUserRepository, never()).save(any(StoreUser.class));
    }

    @Test
    void updateStoreMemberPermission_shouldRejectBlankName() {
        Store store = createStore(26L);
        StoreUser operator = createStoreUser(60L, store, createUser(6L, "owner@example.com"), "owner");
        StoreUser target = createStoreUser(44L, store, createUser(5L, "member@example.com"), "member");

        UpdateStoreMemberPermissionRequest request = new UpdateStoreMemberPermissionRequest();
        request.setName("   ");

        mockMemberLookup(operator, target);

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.updateStoreMemberPermission(26L, 6L, 5L, request)
        );

        assertEquals("员工姓名不能为空", error.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(storeUserRepository, never()).save(any(StoreUser.class));
    }

    private void mockMemberLookup(StoreUser operator, StoreUser target) {
        when(storeUserRepository.findByStoreIdAndUserId(26L, 6L)).thenReturn(Optional.of(operator));
        when(storeUserRepository.findByStoreIdAndUserId(26L, 5L)).thenReturn(Optional.of(target));
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

    private static StoreUser createStoreUser(Long id, Store store, User user, String role) {
        StoreUser storeUser = new StoreUser();
        storeUser.setId(id);
        storeUser.setStore(store);
        storeUser.setUser(user);
        storeUser.setRole(role);
        storeUser.setIsActive(true);
        storeUser.setRoles(Set.of());
        return storeUser;
    }

    private static PermissionDTO createPermissionDTO(
            PermissionModule module,
            PermissionAction action,
            Long roomTypeId,
            Boolean allRoomTypes
    ) {
        PermissionDTO permission = new PermissionDTO();
        permission.setModule(module);
        permission.setAction(action);
        permission.setRoomTypeId(roomTypeId);
        permission.setAllRoomTypes(allRoomTypes);
        return permission;
    }

    private static StoreUserPermission createStoreUserPermission(
            StoreUser storeUser,
            PermissionModule module,
            PermissionAction action,
            Long roomTypeId,
            Boolean allRoomTypes
    ) {
        StoreUserPermission permission = new StoreUserPermission(storeUser, module, action);
        permission.setRoomTypeId(roomTypeId);
        permission.setAllRoomTypes(allRoomTypes);
        return permission;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Iterable<StoreUserPermission>> createPermissionIterableCaptor() {
        return ArgumentCaptor.forClass((Class) Iterable.class);
    }

    private static List<StoreUserPermission> toPermissionList(Iterable<StoreUserPermission> permissions) {
        List<StoreUserPermission> out = new ArrayList<>();
        for (StoreUserPermission permission : permissions) {
            out.add(permission);
        }
        return out;
    }

    private static void assertStoreUserPermission(
            StoreUserPermission permission,
            PermissionModule module,
            PermissionAction action,
            Long roomTypeId,
            Boolean allRoomTypes
    ) {
        assertEquals(module, permission.getModule());
        assertEquals(action, permission.getAction());
        assertEquals(roomTypeId, permission.getRoomTypeId());
        assertEquals(allRoomTypes, permission.getAllRoomTypes());
    }

    private static void assertPermissionDTO(
            List<PermissionDTO> permissions,
            PermissionModule module,
            PermissionAction action,
            Long roomTypeId,
            Boolean allRoomTypes
    ) {
        for (PermissionDTO permission : permissions) {
            if (permission.getModule() != module || permission.getAction() != action) {
                continue;
            }
            assertEquals(roomTypeId, permission.getRoomTypeId());
            assertEquals(allRoomTypes, permission.getAllRoomTypes());
            return;
        }

        assertTrue(false, "Expected permission not found: " + module + " " + action);
    }
}
