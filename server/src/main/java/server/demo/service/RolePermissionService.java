package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.PermissionDTO;
import server.demo.entity.Role;
import server.demo.entity.RolePermission;
import server.demo.enums.PermissionModule;
import server.demo.repository.RolePermissionRepository;
import server.demo.repository.RoleRepository;
import server.demo.repository.RoomTypeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色权限服务
 */
@Service
public class RolePermissionService {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    /**
     * 获取角色的所有权限
     */
    public List<PermissionDTO> getRolePermissions(Long roleId) {
        List<RolePermission> permissions = rolePermissionRepository.findByRoleId(roleId);
        return permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 获取角色在指定模块的权限
     */
    public List<PermissionDTO> getRolePermissionsByModule(Long roleId, PermissionModule module) {
        List<RolePermission> permissions = rolePermissionRepository.findByRoleIdAndModule(roleId, module);
        return permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 更新角色权限
     */
    @Transactional
    public void updateRolePermissions(Long roleId, List<PermissionDTO> permissionDTOs) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        // 删除现有权限
        rolePermissionRepository.deleteByRoleId(roleId);

        // 创建新权限
        List<RolePermission> permissions = new ArrayList<>();
        for (PermissionDTO dto : permissionDTOs) {
            RolePermission permission = new RolePermission();
            permission.setRole(role);
            permission.setModule(dto.getModule());
            permission.setAction(dto.getAction());
            permission.setRoomTypeId(dto.getRoomTypeId());
            permission.setAllRoomTypes(dto.getAllRoomTypes() != null ? dto.getAllRoomTypes() : false);
            permissions.add(permission);
        }

        rolePermissionRepository.saveAll(permissions);
    }

    /**
     * 删除角色的所有权限
     */
    @Transactional
    public void deleteRolePermissions(Long roleId) {
        rolePermissionRepository.deleteByRoleId(roleId);
    }

    /**
     * 删除角色在指定模块的权限
     */
    @Transactional
    public void deleteRolePermissionsByModule(Long roleId, PermissionModule module) {
        rolePermissionRepository.deleteByRoleIdAndModule(roleId, module);
    }

    /**
     * 转换为DTO
     */
    private PermissionDTO convertToDTO(RolePermission permission) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setModule(permission.getModule());
        dto.setAction(permission.getAction());
        dto.setRoomTypeId(permission.getRoomTypeId());
        dto.setAllRoomTypes(permission.getAllRoomTypes());

        // 如果有房型ID,获取房型名称
        if (permission.getRoomTypeId() != null) {
            roomTypeRepository.findById(permission.getRoomTypeId())
                    .ifPresent(roomType -> dto.setRoomTypeName(roomType.getName()));
        }

        return dto;
    }
}
