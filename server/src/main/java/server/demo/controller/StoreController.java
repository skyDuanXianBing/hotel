package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.*;
import server.demo.entity.StoreUser;
import server.demo.service.StoreService;

import java.util.List;

/**
 * Store management controller
 */
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

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
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Create store success", store));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Update store
     */
    @PutMapping("/{id}")
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
     * Invite user to store
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> inviteUser(
            @PathVariable Long id,
            @Valid @RequestBody InviteMemberRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            storeService.inviteUser(id, userId, request.getEmail(), request.getRole());
            return ResponseEntity.ok(new ApiResponse<>(true, "Invite user success", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Remove member from store
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeStoreMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            HttpServletRequest httpRequest
    ) {
        try {
            Long operatorUserId = (Long) httpRequest.getAttribute("userId");
            storeService.removeStoreMember(id, operatorUserId, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Remove member success", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get all store members
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<StoreUser>>> getStoreMembers(@PathVariable Long id) {
        try {
            List<StoreUser> members = storeService.getStoreMembers(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Get store members success", members));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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
