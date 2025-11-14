package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.entity.Role;
import server.demo.entity.RolePermission;
import server.demo.entity.StoreUser;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.StoreUserRepository;

import java.util.Optional;
import java.util.Set;

/**
 * 权限验证服务
 */
@Service
public class PermissionService {

    @Autowired
    private StoreUserRepository storeUserRepository;

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
