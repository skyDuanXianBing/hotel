package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.SortConfigDTO;
import server.demo.entity.SortConfig;
import server.demo.service.SortConfigService;

import java.util.List;
import java.util.Map;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/sort-configs")
public class SortConfigController {

    @Autowired
    private SortConfigService sortConfigService;

    /**
     * 获取指定类型的排序配置列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SortConfig>>> getSortConfigs(
            @RequestParam Long userId,
            @RequestParam String sortType) {
        try {
            List<SortConfig> configs = sortConfigService.getSortConfigs(userId, sortType);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.08d7080b0753"), configs));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.414e74074e02") + e.getMessage()));
        }
    }

    /**
     * 获取排序映射(entityId -> sortOrder)
     */
    @GetMapping("/map")
    public ResponseEntity<ApiResponse<Map<Long, Integer>>> getSortOrderMap(
            @RequestParam Long userId,
            @RequestParam String sortType) {
        try {
            Map<Long, Integer> sortMap = sortConfigService.getSortOrderMap(userId, sortType);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.d60bc2b1f93f"), sortMap));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.599ce40686d5") + e.getMessage()));
        }
    }

    /**
     * 批量更新排序配置
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> updateSortOrders(
            @RequestParam Long userId,
            @Valid @RequestBody SortConfigDTO dto) {
        try {
            sortConfigService.updateSortOrders(userId, dto.getSortType(), dto.getEntityIds());
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.a4c955a69e64"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.8863b677cc39") + e.getMessage()));
        }
    }

    /**
     * 删除指定类型的所有排序配置
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteSortConfigs(
            @RequestParam Long userId,
            @RequestParam String sortType) {
        try {
            sortConfigService.deleteSortConfigs(userId, sortType);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.c46b2b1019ca"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.84d6459bfd46") + e.getMessage()));
        }
    }
}
