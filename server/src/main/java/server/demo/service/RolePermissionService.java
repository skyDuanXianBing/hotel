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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        // 创建新权限（归一化+去重，避免重复插入导致唯一键冲突）
        List<PermissionDTO> normalizedDtos = normalizeAndDedupe(permissionDTOs);
        List<RolePermission> permissions = new ArrayList<>();
        for (PermissionDTO dto : normalizedDtos) {
            RolePermission permission = new RolePermission();
            permission.setRole(role);
            permission.setModule(dto.getModule());
            permission.setAction(dto.getAction());
            permission.setRoomTypeId(normalizeRoomTypeId(dto.getRoomTypeId(), dto.getAllRoomTypes()));
            permission.setAllRoomTypes(Boolean.TRUE.equals(dto.getAllRoomTypes()));
            permissions.add(permission);
        }

        rolePermissionRepository.saveAll(permissions);
    }

    private static Long normalizeRoomTypeId(Long roomTypeId, Boolean allRoomTypes) {
        if (Boolean.TRUE.equals(allRoomTypes)) {
            return 0L;
        }
        return roomTypeId != null ? roomTypeId : 0L;
    }

    private static List<PermissionDTO> normalizeAndDedupe(List<PermissionDTO> permissionDTOs) {
        if (permissionDTOs == null || permissionDTOs.isEmpty()) {
            return List.of();
        }

        // 规则：
        // 1) 同一 (module, action) 若存在 allRoomTypes=true，则仅保留这一条（roomTypeId 归一化为 0），忽略其它房型条目。
        // 2) 否则按 (module, action, roomTypeIdNormalized) 去重，roomTypeId 为空归一化为 0。
        Map<String, PermissionDTO> deduped = new LinkedHashMap<>();
        Map<String, Boolean> hasAllRoomTypesByAction = new LinkedHashMap<>();

        for (PermissionDTO dto : permissionDTOs) {
            if (dto == null || dto.getModule() == null || dto.getAction() == null) {
                continue;
            }

            String moduleActionKey = dto.getModule().name() + "::" + dto.getAction().name();
            boolean allRoomTypes = Boolean.TRUE.equals(dto.getAllRoomTypes());
            if (allRoomTypes) {
                hasAllRoomTypesByAction.put(moduleActionKey, true);
            } else if (!hasAllRoomTypesByAction.containsKey(moduleActionKey)) {
                hasAllRoomTypesByAction.put(moduleActionKey, false);
            }
        }

        for (PermissionDTO dto : permissionDTOs) {
            if (dto == null || dto.getModule() == null || dto.getAction() == null) {
                continue;
            }

            String moduleActionKey = dto.getModule().name() + "::" + dto.getAction().name();
            boolean hasAllRoomTypes = Boolean.TRUE.equals(hasAllRoomTypesByAction.get(moduleActionKey));
            boolean allRoomTypes = Boolean.TRUE.equals(dto.getAllRoomTypes());

            if (hasAllRoomTypes) {
                if (!allRoomTypes) {
                    continue;
                }

                PermissionDTO normalized = new PermissionDTO();
                normalized.setModule(dto.getModule());
                normalized.setAction(dto.getAction());
                normalized.setAllRoomTypes(true);
                normalized.setRoomTypeId(0L);

                deduped.put(moduleActionKey + "::ALL", normalized);
                continue;
            }

            Long roomTypeId = normalizeRoomTypeId(dto.getRoomTypeId(), dto.getAllRoomTypes());
            PermissionDTO normalized = new PermissionDTO();
            normalized.setModule(dto.getModule());
            normalized.setAction(dto.getAction());
            normalized.setAllRoomTypes(false);
            normalized.setRoomTypeId(roomTypeId);

            deduped.put(moduleActionKey + "::" + roomTypeId, normalized);
        }

        return new ArrayList<>(deduped.values());
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
        if (permission.getRoomTypeId() != null && permission.getRoomTypeId() > 0) {
            roomTypeRepository.findById(permission.getRoomTypeId())
                    .ifPresent(roomType -> dto.setRoomTypeName(roomType.getName()));
        }

        return dto;
    }
}
