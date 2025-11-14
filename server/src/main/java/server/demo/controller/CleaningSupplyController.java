package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.entity.CleaningSupply;
import server.demo.service.CleaningSupplyService;

import java.util.List;

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
        return ApiResponse.success("获取易耗品列表成功", supplies);
    }

    /**
     * 根据用户ID获取易耗品列表(已废弃,使用getAllSupplies)
     */
    @Deprecated
    @GetMapping("/user/{userId}")
    public ApiResponse<List<CleaningSupply>> getSuppliesByUserId(@PathVariable Long userId) {
        List<CleaningSupply> supplies = cleaningSupplyService.getSuppliesByUserId(userId);
        return ApiResponse.success("获取易耗品列表成功", supplies);
    }

    /**
     * 根据ID获取易耗品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<CleaningSupply> getSupplyById(@PathVariable Long id) {
        return cleaningSupplyService.getSupplyById(id)
                .map(supply -> ApiResponse.success("获取易耗品详情成功", supply))
                .orElse(ApiResponse.error("易耗品不存在"));
    }

    /**
     * 创建易耗品
     */
    @PostMapping
    public ApiResponse<CleaningSupply> createSupply(@RequestBody CleaningSupply supply) {
        CleaningSupply createdSupply = cleaningSupplyService.createSupply(supply);
        return ApiResponse.success("创建易耗品成功", createdSupply);
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
            return ApiResponse.success("更新易耗品成功", updatedSupply);
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
        return ApiResponse.success("删除易耗品成功", null);
    }

    /**
     * 清空易耗品内容
     */
    @PutMapping("/{id}/clear")
    public ApiResponse<CleaningSupply> clearSupply(@PathVariable Long id) {
        try {
            CleaningSupply supply = cleaningSupplyService.clearSupply(id);
            return ApiResponse.success("清空易耗品成功", supply);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
