package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.Role;
import server.demo.entity.RolePermission;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 权限校验服务
 */
@Service
public class PermissionService {

    @Autowired
    private StoreUserRepository storeUserRepository;

    @Autowired
    private StoreUserPermissionRepository storeUserPermissionRepository;

    @Value("${permission.enforcement.enabled:false}")
    private boolean enforcementEnabled;

    public boolean isEnforcementEnabled() {
        return enforcementEnabled;
    }

    public static final class RoomTypeScope {
        private final boolean allRoomTypes;
        private final Set<Long> roomTypeIds;

        private RoomTypeScope(boolean allRoomTypes, Set<Long> roomTypeIds) {
            this.allRoomTypes = allRoomTypes;
            this.roomTypeIds = roomTypeIds != null ? roomTypeIds : Collections.emptySet();
        }

        public static RoomTypeScope none() {
            return new RoomTypeScope(false, Collections.emptySet());
        }

        public static RoomTypeScope all() {
            return new RoomTypeScope(true, Collections.emptySet());
        }

        public static RoomTypeScope of(Set<Long> roomTypeIds) {
            return new RoomTypeScope(false, roomTypeIds != null ? Set.copyOf(roomTypeIds) : Collections.emptySet());
        }

        public boolean isAllRoomTypes() {
            return allRoomTypes;
        }

        public Set<Long> getRoomTypeIds() {
            return roomTypeIds;
        }

        public boolean isEmpty() {
            return !allRoomTypes && (roomTypeIds == null || roomTypeIds.isEmpty());
        }
    }

    public RoomTypeScope resolveRoomTypeScope(Long storeId, Long userId, PermissionModule module, PermissionAction action) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);
        if (storeUserOpt.isEmpty()) {
            return RoomTypeScope.none();
        }

        StoreUser storeUser = storeUserOpt.get();
        if (isStoreManager(storeUser)) {
            return RoomTypeScope.all();
        }

        RoomTypeScope roleScope = resolveRoleRoomTypeScope(storeUser.getRoles(), module, action);
        RoomTypeScope extraScope = resolveExtraRoomTypeScope(loadExtraPermissions(storeUser.getId()), module, action);
        return mergeRoomTypeScopes(roleScope, extraScope);
    }

    public boolean hasPermission(Long storeId, Long userId, PermissionModule module, PermissionAction action) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);
        if (storeUserOpt.isEmpty()) {
            return false;
        }

        StoreUser storeUser = storeUserOpt.get();
        if (isStoreManager(storeUser)) {
            return true;
        }

        List<StoreUserPermission> extraPermissions = loadExtraPermissions(storeUser.getId());
        return hasRolePermission(storeUser.getRoles(), module, action)
                || hasExtraPermission(extraPermissions, module, action);
    }

    public boolean hasPermission(Long storeId, Long userId, PermissionModule module,
                                 PermissionAction action, Long roomTypeId) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);
        if (storeUserOpt.isEmpty()) {
            return false;
        }

        StoreUser storeUser = storeUserOpt.get();
        if (isStoreManager(storeUser)) {
            return true;
        }

        if (roomTypeId == null || action != PermissionAction.VIEW_ROOM_STATUS) {
            return hasPermission(storeId, userId, module, action);
        }

        RoomTypeScope scope = resolveRoomTypeScope(storeId, userId, module, action);
        return scope.isAllRoomTypes() || scope.getRoomTypeIds().contains(roomTypeId);
    }

    public boolean isStoreAdmin(Long storeId, Long userId) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);

        if (storeUserOpt.isEmpty()) {
            return false;
        }

        StoreUser storeUser = storeUserOpt.get();
        return "owner".equals(storeUser.getRole()) || "admin".equals(storeUser.getRole());
    }

    public boolean isStoreOwner(Long storeId, Long userId) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);

        if (storeUserOpt.isEmpty()) {
            return false;
        }

        StoreUser storeUser = storeUserOpt.get();
        return "owner".equals(storeUser.getRole());
    }

    private boolean isStoreManager(StoreUser storeUser) {
        return "owner".equals(storeUser.getRole()) || "admin".equals(storeUser.getRole());
    }

    private List<StoreUserPermission> loadExtraPermissions(Long storeUserId) {
        if (storeUserId == null) {
            return Collections.emptyList();
        }
        List<StoreUserPermission> permissions = storeUserPermissionRepository.findByStoreUser_Id(storeUserId);
        return permissions != null ? permissions : Collections.emptyList();
    }

    private boolean hasRolePermission(Set<Role> roles, PermissionModule module, PermissionAction action) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        for (Role role : roles) {
            Set<RolePermission> permissions = role.getRolePermissions();
            if (permissions == null || permissions.isEmpty()) {
                continue;
            }
            for (RolePermission permission : permissions) {
                if (permission == null) {
                    continue;
                }
                if (permission.getModule() == module && permission.getAction() == action) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasExtraPermission(List<StoreUserPermission> extraPermissions,
                                       PermissionModule module,
                                       PermissionAction action) {
        if (extraPermissions == null || extraPermissions.isEmpty()) {
            return false;
        }

        for (StoreUserPermission permission : extraPermissions) {
            if (permission == null) {
                continue;
            }
            if (permission.getModule() == module && permission.getAction() == action) {
                return true;
            }
        }

        return false;
    }

    private RoomTypeScope resolveRoleRoomTypeScope(Set<Role> roles, PermissionModule module, PermissionAction action) {
        if (roles == null || roles.isEmpty()) {
            return RoomTypeScope.none();
        }

        Set<Long> roomTypeIds = new HashSet<>();
        for (Role role : roles) {
            Set<RolePermission> permissions = role.getRolePermissions();
            if (permissions == null || permissions.isEmpty()) {
                continue;
            }
            for (RolePermission permission : permissions) {
                if (permission == null) {
                    continue;
                }
                if (permission.getModule() != module || permission.getAction() != action) {
                    continue;
                }
                if (Boolean.TRUE.equals(permission.getAllRoomTypes())) {
                    return RoomTypeScope.all();
                }

                Long roomTypeId = permission.getRoomTypeId();
                if (roomTypeId == null || roomTypeId == 0L) {
                    return RoomTypeScope.all();
                }
                if (roomTypeId > 0) {
                    roomTypeIds.add(roomTypeId);
                }
            }
        }

        return roomTypeIds.isEmpty() ? RoomTypeScope.none() : RoomTypeScope.of(roomTypeIds);
    }

    private RoomTypeScope resolveExtraRoomTypeScope(List<StoreUserPermission> extraPermissions,
                                                    PermissionModule module,
                                                    PermissionAction action) {
        if (extraPermissions == null || extraPermissions.isEmpty()) {
            return RoomTypeScope.none();
        }

        Set<Long> roomTypeIds = new HashSet<>();
        for (StoreUserPermission permission : extraPermissions) {
            if (permission == null) {
                continue;
            }
            if (permission.getModule() != module || permission.getAction() != action) {
                continue;
            }
            if (Boolean.TRUE.equals(permission.getAllRoomTypes())) {
                return RoomTypeScope.all();
            }

            Long roomTypeId = permission.getRoomTypeId();
            if (roomTypeId == null || roomTypeId == 0L) {
                return RoomTypeScope.all();
            }
            if (roomTypeId > 0) {
                roomTypeIds.add(roomTypeId);
            }
        }

        return roomTypeIds.isEmpty() ? RoomTypeScope.none() : RoomTypeScope.of(roomTypeIds);
    }

    private RoomTypeScope mergeRoomTypeScopes(RoomTypeScope roleScope, RoomTypeScope extraScope) {
        if (roleScope == null || roleScope.isEmpty()) {
            return extraScope != null ? extraScope : RoomTypeScope.none();
        }
        if (extraScope == null || extraScope.isEmpty()) {
            return roleScope;
        }
        if (roleScope.isAllRoomTypes() || extraScope.isAllRoomTypes()) {
            return RoomTypeScope.all();
        }

        Set<Long> roomTypeIds = new HashSet<>(roleScope.getRoomTypeIds());
        roomTypeIds.addAll(extraScope.getRoomTypeIds());
        return roomTypeIds.isEmpty() ? RoomTypeScope.none() : RoomTypeScope.of(roomTypeIds);
    }
}
