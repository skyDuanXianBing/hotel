package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.entity.CleaningConfig;
import server.demo.service.CleaningConfigService;

import java.util.List;

/**
 * 保洁配置控制器
 */
@RestController
@RequestMapping("/api/v1/cleaning-configs")
public class CleaningConfigController {

    @Autowired
    private CleaningConfigService cleaningConfigService;

    /**
     * 根据用户ID获取保洁配置列表
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<CleaningConfig>> getConfigsByUserId(@PathVariable Long userId) {
        List<CleaningConfig> configs = cleaningConfigService.getConfigsByUserId(userId);
        return ApiResponse.success("获取保洁配置列表成功", configs);
    }

    /**
     * 根据用户ID和门店ID获取或创建保洁配置
     */
    @GetMapping("/user/{userId}/store/{storeId}")
    public ApiResponse<CleaningConfig> getOrCreateConfig(
            @PathVariable Long userId,
            @PathVariable Long storeId) {
        CleaningConfig config = cleaningConfigService.getOrCreateConfig(userId, storeId);
        return ApiResponse.success("获取保洁配置成功", config);
    }

    /**
     * 根据ID获取保洁配置详情
     */
    @GetMapping("/{id}")
    public ApiResponse<CleaningConfig> getConfigById(@PathVariable Long id) {
        return cleaningConfigService.getConfigById(id)
                .map(config -> ApiResponse.success("获取保洁配置详情成功", config))
                .orElse(ApiResponse.error("保洁配置不存在"));
    }

    /**
     * 更新保洁配置
     */
    @PutMapping("/{id}")
    public ApiResponse<CleaningConfig> updateConfig(
            @PathVariable Long id,
            @RequestBody CleaningConfig config) {
        try {
            CleaningConfig updatedConfig = cleaningConfigService.updateConfig(id, config);
            return ApiResponse.success("更新保洁配置成功", updatedConfig);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除保洁配置
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        cleaningConfigService.deleteConfig(id);
        return ApiResponse.success("删除保洁配置成功", null);
    }
}
