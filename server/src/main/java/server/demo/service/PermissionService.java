package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.demo.entity.Role;
import server.demo.entity.RolePermission;
import server.demo.entity.StoreUser;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.StoreUserRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 权限验证服务
 */
@Service
public class PermissionService {

    @Autowired
    private StoreUserRepository storeUserRepository;

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

    /**
     * 解析某个房型范围型权限（比如 VIEW_ROOM_STATUS）的房型范围。
     *
     * 规则：
     * - owner/admin 直接视为 all。
     * - 若存在 allRoomTypes=true，则 all。
     * - 否则收集 roomTypeId>0 的集合。
     * - 兼容旧数据：若匹配记录 roomTypeId=0 且 allRoomTypes=false，视为 all。
     */
    public RoomTypeScope resolveRoomTypeScope(Long storeId, Long userId, PermissionModule module, PermissionAction action) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);
        if (storeUserOpt.isEmpty()) {
            return RoomTypeScope.none();
        }

        StoreUser storeUser = storeUserOpt.get();
        if ("owner".equals(storeUser.getRole()) || "admin".equals(storeUser.getRole())) {
            return RoomTypeScope.all();
        }

        Set<Role> roles = storeUser.getRoles();
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
                    // 兼容旧数据：存在但未按房型细分
                    return RoomTypeScope.all();
                }
                if (roomTypeId > 0) {
                    roomTypeIds.add(roomTypeId);
                }
            }
        }

        if (roomTypeIds.isEmpty()) {
            return RoomTypeScope.none();
        }
        return RoomTypeScope.of(roomTypeIds);
    }

    /**
     * 检查用户是否拥有指定权限
     * @param storeId 门店ID
     * @param userId 用户ID
     * @param module 权限模块
     * @param action 权限操作
     * @return 是否拥有权限
     */
    public boolean hasPermission(Long storeId, Long userId, PermissionModule module, PermissionAction action) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);

        if (storeUserOpt.isEmpty()) {
            return false;
        }

        StoreUser storeUser = storeUserOpt.get();

        // owner和admin拥有所有权限
        if ("owner".equals(storeUser.getRole()) || "admin".equals(storeUser.getRole())) {
            return true;
        }

        // 检查用户的权限角色
        Set<Role> roles = storeUser.getRoles();
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        // 遍历用户的所有角色，检查是否有匹配的权限
        for (Role role : roles) {
            Set<RolePermission> permissions = role.getRolePermissions();
            if (permissions != null) {
                for (RolePermission permission : permissions) {
                    if (permission.getModule() == module && permission.getAction() == action) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查用户是否拥有指定权限（包含房型）
     * @param storeId 门店ID
     * @param userId 用户ID
     * @param module 权限模块
     * @param action 权限操作
     * @param roomTypeId 房型ID（可选）
     * @return 是否拥有权限
     */
    public boolean hasPermission(Long storeId, Long userId, PermissionModule module,
                                  PermissionAction action, Long roomTypeId) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);

        if (storeUserOpt.isEmpty()) {
            return false;
        }

        StoreUser storeUser = storeUserOpt.get();

        // owner和admin拥有所有权限
        if ("owner".equals(storeUser.getRole()) || "admin".equals(storeUser.getRole())) {
            return true;
        }

        // 检查用户的权限角色
        Set<Role> roles = storeUser.getRoles();
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        // 遍历用户的所有角色，检查是否有匹配的权限
        for (Role role : roles) {
            Set<RolePermission> permissions = role.getRolePermissions();
            if (permissions != null) {
                for (RolePermission permission : permissions) {
                    if (permission.getModule() == module && permission.getAction() == action) {
                        // 检查房型权限
                        if (permission.getAllRoomTypes()) {
                            // 拥有所有房型权限
                            return true;
                        } else if (roomTypeId != null && roomTypeId.equals(permission.getRoomTypeId())) {
                            // 拥有指定房型权限
                            return true;
                        } else if (roomTypeId == null) {
                            // 不需要特定房型权限
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查用户是否是门店管理员（owner或admin）
     */
    public boolean isStoreAdmin(Long storeId, Long userId) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);

        if (storeUserOpt.isEmpty()) {
            return false;
        }

        StoreUser storeUser = storeUserOpt.get();
        return "owner".equals(storeUser.getRole()) || "admin".equals(storeUser.getRole());
    }

    /**
     * 检查用户是否是门店所有者
     */
    public boolean isStoreOwner(Long storeId, Long userId) {
        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);

        if (storeUserOpt.isEmpty()) {
            return false;
        }

        StoreUser storeUser = storeUserOpt.get();
        return "owner".equals(storeUser.getRole());
    }
}
