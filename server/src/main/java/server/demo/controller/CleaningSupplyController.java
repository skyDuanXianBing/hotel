package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.entity.CleaningSupply;
import server.demo.service.CleaningSupplyService;

import java.util.List;

import server.demo.i18n.ApiMessages;
/**
 * 保洁易耗品控制器
 */
@RestController
@RequestMapping("/api/v1/cleaning-supplies")
@StoreScoped
public class CleaningSupplyController {

    @Autowired
    private CleaningSupplyService cleaningSupplyService;

    /**
     * 获取易耗品列表(门店级)
     */
    @GetMapping
    public ApiResponse<List<CleaningSupply>> getAllSupplies() {
        List<CleaningSupply> supplies = cleaningSupplyService.getAllSupplies();
        return ApiResponse.success(ApiMessages.get("api.t.16712ee892ca"), supplies);
    }

    /**
     * 根据用户ID获取易耗品列表(已废弃,使用getAllSupplies)
     */
    @Deprecated
    @GetMapping("/user/{userId}")
    public ApiResponse<List<CleaningSupply>> getSuppliesByUserId(@PathVariable Long userId) {
        List<CleaningSupply> supplies = cleaningSupplyService.getSuppliesByUserId(userId);
        return ApiResponse.success(ApiMessages.get("api.t.16712ee892ca"), supplies);
    }

    /**
     * 根据ID获取易耗品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<CleaningSupply> getSupplyById(@PathVariable Long id) {
        return cleaningSupplyService.getSupplyById(id)
                .map(supply -> ApiResponse.success(ApiMessages.get("api.t.d5d5f2de6afe"), supply))
                .orElse(ApiResponse.error(ApiMessages.get("api.t.b9b31a4ffc83")));
    }

    /**
     * 创建易耗品
     */
    @PostMapping
    public ApiResponse<CleaningSupply> createSupply(@RequestBody CleaningSupply supply) {
        CleaningSupply createdSupply = cleaningSupplyService.createSupply(supply);
        return ApiResponse.success(ApiMessages.get("api.t.d532fdad1e48"), createdSupply);
    }

    /**
     * 更新易耗品
     */
    @PutMapping("/{id}")
    public ApiResponse<CleaningSupply> updateSupply(
            @PathVariable Long id,
            @RequestBody CleaningSupply supply) {
        try {
            CleaningSupply updatedSupply = cleaningSupplyService.updateSupply(id, supply);
            return ApiResponse.success(ApiMessages.get("api.t.772d7b567310"), updatedSupply);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除易耗品
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSupply(@PathVariable Long id) {
        cleaningSupplyService.deleteSupply(id);
        return ApiResponse.success(ApiMessages.get("api.t.a2f7c21167df"), null);
    }

    /**
     * 清空易耗品内容
     */
    @PutMapping("/{id}/clear")
    public ApiResponse<CleaningSupply> clearSupply(@PathVariable Long id) {
        try {
            CleaningSupply supply = cleaningSupplyService.clearSupply(id);
            return ApiResponse.success(ApiMessages.get("api.t.6a1e58055e63"), supply);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
