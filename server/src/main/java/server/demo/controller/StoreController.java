package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.dto.*;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.exception.PermissionDeniedException;
import server.demo.service.StoreService;
import server.demo.service.SuPropertyService;

import java.util.List;

/**
 * Store management controller
 */
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @Autowired
    private SuPropertyService suPropertyService;

    /**
     * Get all stores for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreDTO>>> getUserStores(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<StoreDTO> stores = storeService.getUserStores(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Get user stores success", stores));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get store by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreDTO>> getStoreById(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            StoreDTO store = storeService.getStoreById(id, userId)
                    .orElseThrow(() -> new RuntimeException("Store not found or no permission"));
            return ResponseEntity.ok(new ApiResponse<>(true, "Get store success", store));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Create new store
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StoreDTO>> createStore(
            @Valid @RequestBody CreateStoreRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            StoreDTO store = storeService.createStore(userId, request);

            boolean shouldCreateSuProperty = request.getCreateSuProperty() == null || Boolean.TRUE.equals(request.getCreateSuProperty());
            String message = "门店创建成功";
            if (shouldCreateSuProperty) {
                SuPropertyService.UpsertResult result = suPropertyService.upsertStoreProperty(store.getId());
                if (result.success()) {
                    message = message + "；" + result.message() + "（hotelid=" + result.hotelId() + "）";
                } else {
                    message = message + "；渠道物业创建失败（hotelid=" + result.hotelId() + "）："
                            + (result.message() != null ? result.message() : "未知错误");
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, message, store));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Update store
     */
    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<StoreDTO>> updateStore(
            @PathVariable Long id,
            @Valid @RequestBody CreateStoreRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            StoreDTO store = storeService.updateStore(id, userId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Update store success", store));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Add member to store (支持权限角色分配)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteStore(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            storeService.deleteStoreWithSuRemoveProperty(id, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Delete store success", null));
        } catch (server.demo.exception.SuPropertyDeleteFailedException e) {
            String code = e.getErrorCode();
            String message = e.getMessage() != null ? e.getMessage() : "Delete store failed";
            return ResponseEntity.ok(new ApiResponse<>(false, message, code != null ? java.util.Map.of("code", code) : null));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Delete store failed";
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if ("No permission".equals(message) || "Only owner can delete store".equals(message)) {
                status = HttpStatus.FORBIDDEN;
            }
            return ResponseEntity.status(status).body(new ApiResponse<>(false, message, null));
        }
    }

    @PostMapping("/{id}/members")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<StoreUserDTO>> addStoreMember(
            @PathVariable Long id,
            @Valid @RequestBody AddStoreMemberRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            StoreUserDTO member = storeService.addStoreMember(id, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "添加成员成功", member));
        } catch (PermissionDeniedException e) {
            throw e;
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Remove member from store
     */
    @DeleteMapping("/{id}/members/{memberId}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<Void>> removeStoreMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            HttpServletRequest httpRequest
    ) {
        try {
            Long operatorUserId = (Long) httpRequest.getAttribute("userId");
            storeService.removeStoreMember(id, operatorUserId, memberId);
            return ResponseEntity.ok(new ApiResponse<>(true, "移除成员成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get all store members (返回详细信息)
     */
    @GetMapping("/{id}/members")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<List<StoreUserDTO>>> getStoreMembers(@PathVariable Long id) {
        try {
            List<StoreUserDTO> members = storeService.getStoreMembersDTO(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "获取门店成员列表成功", members));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/{id}/my-permissions")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getMyPermissions(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            List<PermissionDTO> permissions = storeService.getCurrentUserEffectivePermissions(id, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "获取当前成员权限成功", permissions));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get store member detail
     */
    @GetMapping("/{id}/members/{userId}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<StoreUserDTO>> getStoreMemberDetail(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        try {
            StoreUserDTO member = storeService.getStoreMemberDetail(id, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "获取成员详情成功", member));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Update store member permission
     */
    @PutMapping("/{id}/members/{userId}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<StoreUserDTO>> updateStoreMemberPermission(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStoreMemberPermissionRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Long operatorUserId = (Long) httpRequest.getAttribute("userId");
            StoreUserDTO member = storeService.updateStoreMemberPermission(id, operatorUserId, userId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "更新成员权限成功", member));
        } catch (PermissionDeniedException e) {
            throw e;
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Transfer store owner
     */
    @PostMapping("/{id}/owner/transfer")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_EMPLOYEE_ACCOUNTS)
    public ResponseEntity<ApiResponse<Void>> transferStoreOwner(
            @PathVariable Long id,
            @Valid @RequestBody TransferStoreOwnerRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Long operatorUserId = (Long) httpRequest.getAttribute("userId");
            storeService.transferStoreOwner(id, operatorUserId, request.getTargetUserId());
            return ResponseEntity.ok(new ApiResponse<>(true, "更换负责人成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get store policy
     */
    @GetMapping("/{id}/policy")
    public ResponseEntity<ApiResponse<StorePolicyDTO>> getStorePolicy(@PathVariable Long id) {
        try {
            StorePolicyDTO policy = storeService.getStorePolicy(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Get store policy success", policy));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Update store policy
     */
    @PutMapping("/{id}/policy")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MODIFY_STORE_SETTINGS)
    public ResponseEntity<ApiResponse<StorePolicyDTO>> updateStorePolicy(
            @PathVariable Long id,
            @RequestBody StorePolicyDTO policyDTO,
            HttpServletRequest httpRequest
    ) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            StorePolicyDTO policy = storeService.updateStorePolicy(id, userId, policyDTO);
            return ResponseEntity.ok(new ApiResponse<>(true, "Update store policy success", policy));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
