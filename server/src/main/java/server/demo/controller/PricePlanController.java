package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.AssignRoomTypePricePlanRequest;
import server.demo.dto.ForceDeleteRequest;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomTypePricePlan;
import server.demo.service.PricePlanService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/price-plans")
@StoreScoped
public class PricePlanController extends BaseStoreController {

    @Autowired
    private PricePlanService pricePlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PricePlan>>> getAllPricePlans() {
        List<PricePlan> pricePlans = pricePlanService.getAllPricePlans();
        return ResponseEntity.ok(ApiResponse.success("获取价格计划列表成功", pricePlans));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PricePlan>> getPricePlanById(@PathVariable Long id) {
        return pricePlanService.getPricePlanById(id)
                .map(plan -> ResponseEntity.ok(ApiResponse.success("获取价格计划成功", plan)))
                .orElse(ResponseEntity.ok(ApiResponse.error("价格计划不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PricePlan>> createPricePlan(@Valid @RequestBody PricePlan pricePlan) {
        try {
            PricePlan created = pricePlanService.createPricePlan(pricePlan);
            return ResponseEntity.ok(ApiResponse.success("创建价格计划成功", created));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PricePlan>> updatePricePlan(@PathVariable Long id,
                                                                  @Valid @RequestBody PricePlan pricePlan) {
        try {
            PricePlan updated = pricePlanService.updatePricePlan(id, pricePlan);
            return ResponseEntity.ok(ApiResponse.success("更新价格计划成功", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePricePlan(@PathVariable Long id) {
        try {
            pricePlanService.deletePricePlan(id);
            return ResponseEntity.ok(ApiResponse.success("删除价格计划成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/force-delete")
    public ResponseEntity<ApiResponse<Void>> forceDeletePricePlan(
            @PathVariable Long id,
            @Valid @RequestBody ForceDeleteRequest request) {
        try {
            pricePlanService.forceDeletePricePlan(id, Boolean.TRUE.equals(request.getConfirm()));
            return ResponseEntity.ok(ApiResponse.success("彻底删除价格计划成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/room-types")
    public ResponseEntity<ApiResponse<List<RoomTypePricePlan>>> getRoomTypesByPricePlan(@PathVariable Long id) {
        List<RoomTypePricePlan> roomTypes = pricePlanService.getRoomTypesByPricePlan(id);
        return ResponseEntity.ok(ApiResponse.success("获取房型列表成功", roomTypes));
    }

    @GetMapping("/room-types/{roomTypeId}")
    public ResponseEntity<ApiResponse<List<RoomTypePricePlan>>> getPricePlansByRoomType(@PathVariable Long roomTypeId) {
        List<RoomTypePricePlan> pricePlans = pricePlanService.getPricePlansByRoomType(roomTypeId);
        return ResponseEntity.ok(ApiResponse.success("获取价格计划列表成功", pricePlans));
    }

    @PostMapping("/room-types/{roomTypeId}/assign/{pricePlanId}")
    public ResponseEntity<ApiResponse<RoomTypePricePlan>> assignPricePlanToRoomType(
            @PathVariable Long roomTypeId,
            @PathVariable Long pricePlanId,
            @Valid @RequestBody AssignRoomTypePricePlanRequest request) {
        try {
            RoomTypePricePlan assigned = pricePlanService.assignPricePlanToRoomType(roomTypeId, pricePlanId, request);
            return ResponseEntity.ok(ApiResponse.success("分配价格计划成功", assigned));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/room-type-price-plans/{id}")
    public ResponseEntity<ApiResponse<RoomTypePricePlan>> updateRoomTypePricePlan(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoomTypePricePlanRequest request) {
        try {
            RoomTypePricePlan updated = pricePlanService.updateRoomTypePricePlan(id, request);
            return ResponseEntity.ok(ApiResponse.success("更新房型价格计划成功", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/room-type-price-plans/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoomTypePricePlan(
            @PathVariable Long id,
            @RequestParam(name = "clearOverrides", defaultValue = "false") boolean clearOverrides) {
        try {
            long clearedOverrides = pricePlanService.deleteRoomTypePricePlan(id, clearOverrides);
            String message = clearOverrides
                    ? "删除房型价格计划关联成功，已清理 " + clearedOverrides + " 条按日期覆盖价"
                    : "删除房型价格计划关联成功";
            return ResponseEntity.ok(ApiResponse.success(message, null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/room-types/count")
    public ResponseEntity<ApiResponse<Long>> countRoomTypesByPricePlan(@PathVariable Long id) {
        long count = pricePlanService.countRoomTypesByPricePlan(id);
        return ResponseEntity.ok(ApiResponse.success("获取房型数量成功", count));
    }
}
