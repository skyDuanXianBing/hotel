package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.*;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.RoleService;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/v1/roles")
@StoreScoped
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 获取所有角色
     */
    @GetMapping
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles(
            @RequestParam(required = false) String keyword
    ) {
        try {
            List<RoleDTO> roles;
            if (keyword != null && !keyword.trim().isEmpty()) {
                roles = roleService.searchRoles(keyword);
            } else {
                roles = roleService.getAllRoles();
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "获取角色列表成功", roles));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 根据ID获取角色详情
     */
    @GetMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleById(@PathVariable Long id) {
        try {
            RoleDTO role = roleService.getRoleById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "获取角色详情成功", role));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 创建角色
     */
    @PostMapping
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<RoleDTO>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        try {
            RoleDTO role = roleService.createRole(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "创建角色成功", role));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<RoleDTO>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        try {
            RoleDTO role = roleService.updateRole(id, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "更新角色成功", role));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "删除角色成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 获取角色的权限
     */
    @GetMapping("/{id}/permissions")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getRolePermissions(@PathVariable Long id) {
        try {
            List<PermissionDTO> permissions = roleService.getRolePermissions(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "获取角色权限成功", permissions));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 更新角色的权限
     */
    @PutMapping("/{id}/permissions")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody List<PermissionDTO> permissions
    ) {
        try {
            roleService.updateRolePermissions(id, permissions);
            return ResponseEntity.ok(new ApiResponse<>(true, "更新角色权限成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
