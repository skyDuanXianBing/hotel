package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.CreateRoleRequest;
import server.demo.dto.PermissionDTO;
import server.demo.dto.RoleDTO;
import server.demo.dto.UpdateRoleRequest;
import server.demo.entity.Role;
import server.demo.repository.RoleRepository;

import java.util.List;
import java.util.stream.Collectors;

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
     * 获取所有角色
     */
    public List<RoleDTO> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 搜索角色
     */
    public List<RoleDTO> searchRoles(String keyword) {
        List<Role> roles = roleRepository.searchRoles(keyword);
        return roles.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 根据ID获取角色
     */
    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        return convertToDTOWithPermissions(role);
    }

    /**
     * 创建角色
     */
    @Transactional
    public RoleDTO createRole(CreateRoleRequest request) {
        // 检查角色名是否已存在
        if (roleRepository.existsByName(request.getName())) {
            throw new RuntimeException("角色名已存在");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setIsSystem(false); // 用户创建的角色不是系统角色

        Role savedRole = roleRepository.save(role);

        return convertToDTO(savedRole);
    }

    /**
     * 更新角色
     */
    @Transactional
    public RoleDTO updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        // 如果是系统角色,不允许修改名称
        if (role.getIsSystem() && request.getName() != null && !request.getName().equals(role.getName())) {
            throw new RuntimeException("系统角色不允许修改名称");
        }

        // 检查新名称是否已被其他角色使用
        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByName(request.getName())) {
                throw new RuntimeException("角色名已存在");
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
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        // 检查是否为系统角色
        if (role.getIsSystem()) {
            throw new RuntimeException("系统角色不允许删除");
        }

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
            throw new RuntimeException("角色不存在");
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
