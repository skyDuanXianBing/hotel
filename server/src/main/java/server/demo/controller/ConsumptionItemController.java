package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.ConsumptionItemDTO;
import server.demo.entity.ConsumptionItem;
import server.demo.service.ConsumptionItemService;

import java.util.List;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/consumption-items")
@StoreScoped
public class ConsumptionItemController {

    @Autowired
    private ConsumptionItemService consumptionItemService;

    /**
     * 获取当前门店的所有消费项（门店级架构）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsumptionItem>>> getAll() {
        try {
            List<ConsumptionItem> items = consumptionItemService.getAll();
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.4462eedf1dee"), items));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.10ff05664bd7") + e.getMessage()));
        }
    }

    /**
     * 根据ID获取消费项
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsumptionItem>> getById(@PathVariable Long id) {
        try {
            ConsumptionItem item = consumptionItemService.getById(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.4afe021e8d76"), item));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.5ce0a8efdd29") + e.getMessage()));
        }
    }

    /**
     * 根据分类获取消费项
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ConsumptionItem>>> getByCategory(@PathVariable String category) {
        try {
            List<ConsumptionItem> items = consumptionItemService.getByCategory(category);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.4462eedf1dee"), items));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.10ff05664bd7") + e.getMessage()));
        }
    }

    /**
     * 根据启用状态获取消费项
     */
    @GetMapping("/enabled/{enabled}")
    public ResponseEntity<ApiResponse<List<ConsumptionItem>>> getByEnabled(@PathVariable Boolean enabled) {
        try {
            List<ConsumptionItem> items = consumptionItemService.getByEnabled(enabled);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.4462eedf1dee"), items));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.10ff05664bd7") + e.getMessage()));
        }
    }

    /**
     * 创建消费项（storeId由StoreScopedEntityListener自动注入）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConsumptionItem>> create(@Valid @RequestBody ConsumptionItemDTO dto) {
        try {
            ConsumptionItem item = new ConsumptionItem();
            item.setCategory(dto.getCategory());
            item.setName(dto.getName());
            item.setPrice(dto.getPrice());
            item.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
            item.setDescription(dto.getDescription());
            ConsumptionItem created = consumptionItemService.create(item);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.ff904a2217b7"), created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.af9dc661881f") + e.getMessage()));
        }
    }

    /**
     * 更新消费项
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsumptionItem>> update(
            @PathVariable Long id,
            @Valid @RequestBody ConsumptionItemDTO dto) {
        try {
            ConsumptionItem updates = new ConsumptionItem();
            updates.setCategory(dto.getCategory());
            updates.setName(dto.getName());
            updates.setPrice(dto.getPrice());
            updates.setEnabled(dto.getEnabled());
            updates.setDescription(dto.getDescription());
            ConsumptionItem updated = consumptionItemService.update(id, updates);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.d813ef3f5904"), updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.0c299ead4a39") + e.getMessage()));
        }
    }

    /**
     * 更新消费项启用状态
     */
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<ApiResponse<ConsumptionItem>> updateEnabled(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {
        try {
            ConsumptionItem updated = consumptionItemService.updateEnabled(id, enabled);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.438e3e68d993"), updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.841c933da597") + e.getMessage()));
        }
    }

    /**
     * 删除消费项
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            consumptionItemService.delete(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.d199e49e1d90"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.ef3ac66cff6d") + e.getMessage()));
        }
    }
}
