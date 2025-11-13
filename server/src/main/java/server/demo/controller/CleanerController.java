package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.entity.Cleaner;
import server.demo.service.CleanerService;

import java.util.List;

/**
 * 保洁员控制器
 */
@RestController
@RequestMapping("/api/v1/cleaners")
public class CleanerController {

    @Autowired
    private CleanerService cleanerService;

    /**
     * 根据用户ID和门店ID获取保洁员列表
     */
    @GetMapping("/user/{userId}/store/{storeId}")
    public ApiResponse<List<Cleaner>> getCleanersByUserIdAndStoreId(
            @PathVariable Long userId,
            @PathVariable Long storeId) {
        List<Cleaner> cleaners = cleanerService.getCleanersByUserIdAndStoreId(userId, storeId);
        return ApiResponse.success("获取保洁员列表成功", cleaners);
    }

    /**
     * 根据用户ID获取保洁员列表
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<Cleaner>> getCleanersByUserId(@PathVariable Long userId) {
        List<Cleaner> cleaners = cleanerService.getCleanersByUserId(userId);
        return ApiResponse.success("获取保洁员列表成功", cleaners);
    }

    /**
     * 根据门店ID获取保洁员列表
     */
    @GetMapping("/store/{storeId}")
    public ApiResponse<List<Cleaner>> getCleanersByStoreId(@PathVariable Long storeId) {
        List<Cleaner> cleaners = cleanerService.getCleanersByStoreId(storeId);
        return ApiResponse.success("获取保洁员列表成功", cleaners);
    }

    /**
     * 根据ID获取保洁员详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Cleaner> getCleanerById(@PathVariable Long id) {
        return cleanerService.getCleanerById(id)
                .map(cleaner -> ApiResponse.success("获取保洁员详情成功", cleaner))
                .orElse(ApiResponse.error("保洁员不存在"));
    }

    /**
     * 创建保洁员
     */
    @PostMapping
    public ApiResponse<Cleaner> createCleaner(@RequestBody Cleaner cleaner) {
        Cleaner createdCleaner = cleanerService.createCleaner(cleaner);
        return ApiResponse.success("创建保洁员成功", createdCleaner);
    }

    /**
     * 更新保洁员
     */
    @PutMapping("/{id}")
    public ApiResponse<Cleaner> updateCleaner(
            @PathVariable Long id,
            @RequestBody Cleaner cleaner) {
        try {
            Cleaner updatedCleaner = cleanerService.updateCleaner(id, cleaner);
            return ApiResponse.success("更新保洁员成功", updatedCleaner);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除保洁员
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCleaner(@PathVariable Long id) {
        cleanerService.deleteCleaner(id);
        return ApiResponse.success("删除保洁员成功", null);
    }
}
