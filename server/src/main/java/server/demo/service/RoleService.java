package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.CreateRoleRequest;
import server.demo.dto.PermissionDTO;
import server.demo.dto.RoleDTO;
import server.demo.dto.UpdateRoleRequest;
import server.demo.entity.Role;
import server.demo.repository.RoleRepository;

import java.util.List;
import java.util.stream.Collectors;

import server.demo.i18n.ApiMessages;
/**
 * 角色管理服务
 */
@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePermissionService rolePermissionService;

    /**
     * 获取当前门店ID
     */
    private Long getCurrentStoreId() {
        StoreContext context = StoreContextHolder.getContext();
        if (context == null || context.getStoreId() == null) {
            throw new RuntimeException(ApiMessages.get("api.t.642b7e97c7d4"));
        }
        return context.getStoreId();
    }

    /**
     * 获取所有角色(门店级)
     */
    public List<RoleDTO> getAllRoles() {
        Long storeId = getCurrentStoreId();
        List<Role> roles = roleRepository.findByStoreId(storeId);
        return roles.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 搜索角色(门店级)
     */
    public List<RoleDTO> searchRoles(String keyword) {
        Long storeId = getCurrentStoreId();
        List<Role> roles = roleRepository.searchRolesByStoreId(storeId, keyword);
        return roles.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 根据ID获取角色
     */
    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.3b434ed58cbf")));
        return convertToDTOWithPermissions(role);
    }

    /**
     * 创建角色(门店级)
     */
    @Transactional
    public RoleDTO createRole(CreateRoleRequest request) {
        Long storeId = getCurrentStoreId();

        // 检查角色名在当前门店是否已存在
        if (roleRepository.existsByStoreIdAndName(storeId, request.getName())) {
            throw new RuntimeException(ApiMessages.get("api.t.aa16d9175f3b"));
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setStoreId(storeId);
        role.setIsSystem(false); // 用户创建的角色不是系统角色

        Role savedRole = roleRepository.save(role);

        return convertToDTO(savedRole);
    }

    /**
     * 更新角色(门店级)
     */
    @Transactional
    public RoleDTO updateRole(Long id, UpdateRoleRequest request) {
        Long storeId = getCurrentStoreId();

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.3b434ed58cbf")));

        // 验证角色属于当前门店
        if (!storeId.equals(role.getStoreId())) {
            throw new RuntimeException(ApiMessages.get("api.t.be3eb08c7f4e"));
        }

        // 如果是系统角色,不允许修改名称
        if (role.getIsSystem() && request.getName() != null && !request.getName().equals(role.getName())) {
            throw new RuntimeException(ApiMessages.get("api.t.f61eb46bb0f0"));
        }

        // 检查新名称在当前门店是否已被其他角色使用
        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByStoreIdAndName(storeId, request.getName())) {
                throw new RuntimeException(ApiMessages.get("api.t.aa16d9175f3b"));
            }
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        Role updatedRole = roleRepository.save(role);

        return convertToDTO(updatedRole);
    }

    /**
     * 删除角色(门店级)
     */
    @Transactional
    public void deleteRole(Long id) {
        Long storeId = getCurrentStoreId();

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.3b434ed58cbf")));

        // 验证角色属于当前门店
        if (!storeId.equals(role.getStoreId())) {
            throw new RuntimeException(ApiMessages.get("api.t.27156dd540b1"));
        }

        // 检查是否为系统角色
        if (role.getIsSystem()) {
            throw new RuntimeException(ApiMessages.get("api.t.47dbf13fca59"));
        }

        roleRepository.deleteStoreUserRoleLinks(id);

        // 删除角色的所有权限
        rolePermissionService.deleteRolePermissions(id);

        // 删除角色
        roleRepository.deleteById(id);
    }

    /**
     * 获取角色的权限
     */
    public List<PermissionDTO> getRolePermissions(Long roleId) {
        return rolePermissionService.getRolePermissions(roleId);
    }

    /**
     * 更新角色的权限
     */
    @Transactional
    public void updateRolePermissions(Long roleId, List<PermissionDTO> permissions) {
        // 检查角色是否存在
        if (!roleRepository.existsById(roleId)) {
            throw new RuntimeException(ApiMessages.get("api.t.3b434ed58cbf"));
        }

        rolePermissionService.updateRolePermissions(roleId, permissions);
    }

    /**
     * 转换为DTO(不包含权限)
     */
    private RoleDTO convertToDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setIsSystem(role.getIsSystem());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }

    /**
     * 转换为DTO(包含权限)
     */
    private RoleDTO convertToDTOWithPermissions(Role role) {
        RoleDTO dto = convertToDTO(role);
        List<PermissionDTO> permissions = rolePermissionService.getRolePermissions(role.getId());
        dto.setPermissions(permissions);
        return dto;
    }
}
