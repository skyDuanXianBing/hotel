package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.AssignRoomTypePricePlanRequest;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomTypePricePlan;
import server.demo.service.PricePlanService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/price-plans")
public class PricePlanController {

    @Autowired
    private PricePlanService pricePlanService;

    /**
     * 获取所有价格计划
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PricePlan>>> getAllPricePlans(
            @RequestParam Long userId) {
        List<PricePlan> pricePlans = pricePlanService.getAllPricePlans(userId);
        return ResponseEntity.ok(ApiResponse.success("获取价格计划列表成功", pricePlans));
    }

    /**
     * 根据ID获取价格计划
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PricePlan>> getPricePlanById(
            @PathVariable Long id,
            @RequestParam Long userId) {
        return pricePlanService.getPricePlanById(userId, id)
                .map(plan -> ResponseEntity.ok(ApiResponse.success("获取价格计划成功", plan)))
                .orElse(ResponseEntity.ok(ApiResponse.error("价格计划不存在")));
    }

    /**
     * 创建价格计划
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PricePlan>> createPricePlan(
            @RequestParam Long userId,
            @Valid @RequestBody PricePlan pricePlan) {
        try {
            PricePlan created = pricePlanService.createPricePlan(userId, pricePlan);
            return ResponseEntity.ok(ApiResponse.success("创建价格计划成功", created));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 更新价格计划
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PricePlan>> updatePricePlan(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody PricePlan pricePlan) {
        try {
            PricePlan updated = pricePlanService.updatePricePlan(userId, id, pricePlan);
            return ResponseEntity.ok(ApiResponse.success("更新价格计划成功", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除价格计划
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePricePlan(
            @PathVariable Long id,
            @RequestParam Long userId) {
        try {
            pricePlanService.deletePricePlan(userId, id);
            return ResponseEntity.ok(ApiResponse.success("删除价格计划成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取价格计划关联的所有房型
     */
    @GetMapping("/{id}/room-types")
    public ResponseEntity<ApiResponse<List<RoomTypePricePlan>>> getRoomTypesByPricePlan(
            @PathVariable Long id) {
        List<RoomTypePricePlan> roomTypes = pricePlanService.getRoomTypesByPricePlan(id);
        return ResponseEntity.ok(ApiResponse.success("获取房型列表成功", roomTypes));
    }

    /**
     * 获取房型的所有价格计划
     */
    @GetMapping("/room-types/{roomTypeId}")
    public ResponseEntity<ApiResponse<List<RoomTypePricePlan>>> getPricePlansByRoomType(
            @PathVariable Long roomTypeId) {
        List<RoomTypePricePlan> pricePlans = pricePlanService.getPricePlansByRoomType(roomTypeId);
        return ResponseEntity.ok(ApiResponse.success("获取价格计划列表成功", pricePlans));
    }

    /**
     * 为房型分配价格计划
     */
    @PostMapping("/room-types/{roomTypeId}/assign/{pricePlanId}")
    public ResponseEntity<ApiResponse<RoomTypePricePlan>> assignPricePlanToRoomType(
            @PathVariable Long roomTypeId,
            @PathVariable Long pricePlanId,
            @RequestParam Long userId,
            @Valid @RequestBody AssignRoomTypePricePlanRequest request) {
        try {
            RoomTypePricePlan assigned = pricePlanService.assignPricePlanToRoomType(
                    userId, roomTypeId, pricePlanId, request);
            return ResponseEntity.ok(ApiResponse.success("分配价格计划成功", assigned));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 更新房型价格计划
     */
    @PutMapping("/room-type-price-plans/{id}")
    public ResponseEntity<ApiResponse<RoomTypePricePlan>> updateRoomTypePricePlan(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody RoomTypePricePlan roomTypePricePlan) {
        try {
            RoomTypePricePlan updated = pricePlanService.updateRoomTypePricePlan(userId, id, roomTypePricePlan);
            return ResponseEntity.ok(ApiResponse.success("更新房型价格计划成功", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 删除房型价格计划关联
     */
    @DeleteMapping("/room-type-price-plans/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoomTypePricePlan(
            @PathVariable Long id,
            @RequestParam Long userId) {
        try {
            pricePlanService.deleteRoomTypePricePlan(userId, id);
            return ResponseEntity.ok(ApiResponse.success("删除房型价格计划关联成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 统计价格计划关联的房型数量
     */
    @GetMapping("/{id}/room-types/count")
    public ResponseEntity<ApiResponse<Long>> countRoomTypesByPricePlan(
            @PathVariable Long id) {
        long count = pricePlanService.countRoomTypesByPricePlan(id);
        return ResponseEntity.ok(ApiResponse.success("获取房型数量成功", count));
    }
}
