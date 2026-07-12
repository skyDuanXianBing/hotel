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
        return ApiResponse.success("获取保洁员列表成功", cleaners);
    }

    @Deprecated
    @GetMapping("/user/{userId}/store/{storeId}")
    public ApiResponse<List<Cleaner>> getCleanersByUserIdAndStoreId(
            @PathVariable Long userId,
            @PathVariable Long storeId) {
        List<Cleaner> cleaners = cleanerService.getCleanersByStoreId(storeId);
        return ApiResponse.success("获取保洁员列表成功", cleaners);
    }

    @Deprecated
    @GetMapping("/user/{userId}")
    public ApiResponse<List<Cleaner>> getCleanersByUserId(@PathVariable Long userId) {
        Long storeId = getCurrentStoreId();
        List<Cleaner> cleaners = cleanerService.getCleanersByStoreId(storeId);
        return ApiResponse.success("获取保洁员列表成功", cleaners);
    }

    @Deprecated
    @GetMapping("/store/{storeId}")
    public ApiResponse<List<Cleaner>> getCleanersByStoreId(@PathVariable Long storeId) {
        List<Cleaner> cleaners = cleanerService.getCleanersByStoreId(getCurrentStoreId());
        return ApiResponse.success("获取保洁员列表成功", cleaners);
    }

    @GetMapping("/{id}")
    public ApiResponse<Cleaner> getCleanerById(@PathVariable Long id) {
        return cleanerService.getCleanerById(getCurrentStoreId(), id)
                .map(cleaner -> ApiResponse.success("获取保洁员详情成功", cleaner))
                .orElse(ApiResponse.error("保洁员不存在"));
    }

    @PostMapping
    public ApiResponse<Cleaner> createCleaner(@RequestBody Cleaner cleaner) {
        Cleaner createdCleaner = cleanerService.createCleaner(cleaner);
        return ApiResponse.success("创建保洁员成功", createdCleaner);
    }

    @GetMapping("/identity-audit")
    public ApiResponse<List<String>> auditIdentities() {
        requireManager();
        List<String> issues = cleanerIdentityService.auditActiveIdentities(getCurrentStoreId());
        return ApiResponse.success(issues.isEmpty() ? "保洁员身份检查通过" : "发现保洁员身份问题", issues);
    }

    @PostMapping("/{id}/reconcile-identity")
    public ApiResponse<Cleaner> reconcileIdentity(@PathVariable Long id) {
        requireManager();
        return ApiResponse.success("保洁员身份修复完成", cleanerIdentityService.reconcileIdentity(getCurrentStoreId(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Cleaner> updateCleaner(
            @PathVariable Long id,
            @RequestBody Cleaner cleaner) {
        try {
            Cleaner updatedCleaner = cleanerService.updateCleaner(getCurrentStoreId(), id, cleaner);
            return ApiResponse.success("更新保洁员成功", updatedCleaner);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCleaner(@PathVariable Long id) {
        try {
            cleanerService.deleteCleaner(getCurrentStoreId(), id);
            return ApiResponse.success("删除保洁员成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private Long getCurrentStoreId() {
        StoreContext storeContext = StoreContextHolder.getContext();
        if (storeContext == null || storeContext.getStoreId() == null) {
            throw new RuntimeException("无法获取当前门店信息");
        }
        return storeContext.getStoreId();
    }

    private void requireManager() {
        StoreContext context = StoreContextHolder.getContext();
        String role = context == null ? null : context.getRole();
        if (!"owner".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            throw new PermissionDeniedException("仅门店所有者或管理员可执行身份治理");
        }
    }
}
