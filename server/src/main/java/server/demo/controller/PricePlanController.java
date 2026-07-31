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
import java.util.Map;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/price-plans")
@StoreScoped
public class PricePlanController extends BaseStoreController {

    @Autowired
    private PricePlanService pricePlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PricePlan>>> getAllPricePlans() {
        List<PricePlan> pricePlans = pricePlanService.getAllPricePlans();
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.70ce0f26eb05"), pricePlans));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PricePlan>> getPricePlanById(@PathVariable Long id) {
        return pricePlanService.getPricePlanById(id)
                .map(plan -> ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.e4afe664dcd0"), plan)))
                .orElse(ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.f6d8111d0db9"))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PricePlan>> createPricePlan(@Valid @RequestBody PricePlan pricePlan) {
        try {
            PricePlan created = pricePlanService.createPricePlan(pricePlan);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.e4f46377fff0"), created));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PricePlan>> updatePricePlan(@PathVariable Long id,
                                                                  @Valid @RequestBody PricePlan pricePlan) {
        try {
            PricePlan updated = pricePlanService.updatePricePlan(id, pricePlan);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.7bfcdda40388"), updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePricePlan(@PathVariable Long id) {
        try {
            pricePlanService.deletePricePlan(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.8e430ac2ae90"), null));
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
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.6232da6ca865"), null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/room-types")
    public ResponseEntity<ApiResponse<List<RoomTypePricePlan>>> getRoomTypesByPricePlan(@PathVariable Long id) {
        List<RoomTypePricePlan> roomTypes = pricePlanService.getRoomTypesByPricePlan(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.b8ec2dcbc5a3"), roomTypes));
    }

    @GetMapping("/room-types/{roomTypeId}")
    public ResponseEntity<ApiResponse<List<RoomTypePricePlan>>> getPricePlansByRoomType(@PathVariable Long roomTypeId) {
        List<RoomTypePricePlan> pricePlans = pricePlanService.getPricePlansByRoomType(roomTypeId);
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.70ce0f26eb05"), pricePlans));
    }

    @PostMapping("/room-types/{roomTypeId}/assign/{pricePlanId}")
    public ResponseEntity<ApiResponse<RoomTypePricePlan>> assignPricePlanToRoomType(
            @PathVariable Long roomTypeId,
            @PathVariable Long pricePlanId,
            @Valid @RequestBody AssignRoomTypePricePlanRequest request) {
        try {
            RoomTypePricePlan assigned = pricePlanService.assignPricePlanToRoomType(roomTypeId, pricePlanId, request);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.3ebe67e749d8"), assigned));
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
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.0af5fdeb2e6d"), updated));
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
                    ? ApiMessages.get("api.t.cd5282f0c11e") + clearedOverrides + ApiMessages.get("api.t.932f2a06b1b5")
                    : ApiMessages.get("api.t.e88e894c9812");
            return ResponseEntity.ok(ApiResponse.success(message, null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/room-type-counts")
    public ResponseEntity<ApiResponse<Map<Long, Long>>> countRoomTypesByPricePlans() {
        return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.22d834c4a325"),
                pricePlanService.countRoomTypesByPricePlanForCurrentStore()
        ));
    }

    @GetMapping("/{id}/room-types/count")
    public ResponseEntity<ApiResponse<Long>> countRoomTypesByPricePlan(@PathVariable Long id) {
        long count = pricePlanService.countRoomTypesByPricePlan(id);
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.22d834c4a325"), count));
    }
}
