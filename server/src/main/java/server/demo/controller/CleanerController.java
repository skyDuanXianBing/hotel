package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.entity.Cleaner;
import server.demo.service.CleanerService;
import server.demo.service.CleanerIdentityService;
import server.demo.exception.PermissionDeniedException;

import java.util.List;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/cleaners")
@StoreScoped
public class CleanerController {

    @Autowired
    private CleanerService cleanerService;

    @Autowired
    private CleanerIdentityService cleanerIdentityService;

    @GetMapping
    public ApiResponse<List<Cleaner>> getCleaners() {
        Long storeId = getCurrentStoreId();
        List<Cleaner> cleaners = cleanerService.getCleanersByStoreId(getCurrentStoreId());
        return ApiResponse.success(ApiMessages.get("api.t.908b60717e6f"), cleaners);
    }

    @Deprecated
    @GetMapping("/user/{userId}/store/{storeId}")
    public ApiResponse<List<Cleaner>> getCleanersByUserIdAndStoreId(
            @PathVariable Long userId,
            @PathVariable Long storeId) {
        List<Cleaner> cleaners = cleanerService.getCleanersByStoreId(storeId);
        return ApiResponse.success(ApiMessages.get("api.t.908b60717e6f"), cleaners);
    }

    @Deprecated
    @GetMapping("/user/{userId}")
    public ApiResponse<List<Cleaner>> getCleanersByUserId(@PathVariable Long userId) {
        Long storeId = getCurrentStoreId();
        List<Cleaner> cleaners = cleanerService.getCleanersByStoreId(storeId);
        return ApiResponse.success(ApiMessages.get("api.t.908b60717e6f"), cleaners);
    }

    @Deprecated
    @GetMapping("/store/{storeId}")
    public ApiResponse<List<Cleaner>> getCleanersByStoreId(@PathVariable Long storeId) {
        List<Cleaner> cleaners = cleanerService.getCleanersByStoreId(getCurrentStoreId());
        return ApiResponse.success(ApiMessages.get("api.t.908b60717e6f"), cleaners);
    }

    @GetMapping("/{id}")
    public ApiResponse<Cleaner> getCleanerById(@PathVariable Long id) {
        return cleanerService.getCleanerById(getCurrentStoreId(), id)
                .map(cleaner -> ApiResponse.success(ApiMessages.get("api.t.14f7418c7719"), cleaner))
                .orElse(ApiResponse.error(ApiMessages.get("api.t.bea2ad1fb3f3")));
    }

    @PostMapping
    public ApiResponse<Cleaner> createCleaner(@RequestBody Cleaner cleaner) {
        Cleaner createdCleaner = cleanerService.createCleaner(cleaner);
        return ApiResponse.success(ApiMessages.get("api.t.9842b593b45a"), createdCleaner);
    }

    @GetMapping("/identity-audit")
    public ApiResponse<List<String>> auditIdentities() {
        requireManager();
        List<String> issues = cleanerIdentityService.auditActiveIdentities(getCurrentStoreId());
        return ApiResponse.success(issues.isEmpty() ? ApiMessages.get("api.t.c626eefe4cb3") : ApiMessages.get("api.t.0371e5eb58e1"), issues);
    }

    @PostMapping("/{id}/reconcile-identity")
    public ApiResponse<Cleaner> reconcileIdentity(@PathVariable Long id) {
        requireManager();
        return ApiResponse.success(ApiMessages.get("api.t.17bb91d4d323"), cleanerIdentityService.reconcileIdentity(getCurrentStoreId(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Cleaner> updateCleaner(
            @PathVariable Long id,
            @RequestBody Cleaner cleaner) {
        try {
            Cleaner updatedCleaner = cleanerService.updateCleaner(getCurrentStoreId(), id, cleaner);
            return ApiResponse.success(ApiMessages.get("api.t.e23cde6ff653"), updatedCleaner);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCleaner(@PathVariable Long id) {
        try {
            cleanerService.deleteCleaner(getCurrentStoreId(), id);
            return ApiResponse.success(ApiMessages.get("api.t.9198a8bbbff5"), null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private Long getCurrentStoreId() {
        StoreContext storeContext = StoreContextHolder.getContext();
        if (storeContext == null || storeContext.getStoreId() == null) {
            throw new RuntimeException(ApiMessages.get("api.t.642b7e97c7d4"));
        }
        return storeContext.getStoreId();
    }

    private void requireManager() {
        StoreContext context = StoreContextHolder.getContext();
        String role = context == null ? null : context.getRole();
        if (!"owner".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new PermissionDeniedException(ApiMessages.get("api.t.801ea6613c98"));
        }
    }
}
