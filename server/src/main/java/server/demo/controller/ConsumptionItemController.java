package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.ConsumptionItemDTO;
import server.demo.entity.ConsumptionItem;
import server.demo.service.ConsumptionItemService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consumption-items")
public class ConsumptionItemController {

    @Autowired
    private ConsumptionItemService consumptionItemService;

    /**
     * 获取用户的所有消费项
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsumptionItem>>> getAll(@RequestParam Long userId) {
        try {
            List<ConsumptionItem> items = consumptionItemService.getAllByUserId(userId);
            return ResponseEntity.ok(ApiResponse.success("获取消费项列表成功", items));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取消费项列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取消费项
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsumptionItem>> getById(@PathVariable Long id) {
        try {
            ConsumptionItem item = consumptionItemService.getById(id);
            return ResponseEntity.ok(ApiResponse.success("获取消费项成功", item));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取消费项失败: " + e.getMessage()));
        }
    }

    /**
     * 根据分类获取消费项
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ConsumptionItem>>> getByCategory(
            @PathVariable String category,
            @RequestParam Long userId) {
        try {
            List<ConsumptionItem> items = consumptionItemService.getByCategory(userId, category);
            return ResponseEntity.ok(ApiResponse.success("获取消费项列表成功", items));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取消费项列表失败: " + e.getMessage()));
        }
    }

    /**
     * 根据启用状态获取消费项
     */
    @GetMapping("/enabled/{enabled}")
    public ResponseEntity<ApiResponse<List<ConsumptionItem>>> getByEnabled(
            @PathVariable Boolean enabled,
            @RequestParam Long userId) {
        try {
            List<ConsumptionItem> items = consumptionItemService.getByEnabled(userId, enabled);
            return ResponseEntity.ok(ApiResponse.success("获取消费项列表成功", items));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取消费项列表失败: " + e.getMessage()));
        }
    }

    /**
     * 创建消费项
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConsumptionItem>> create(
            @Valid @RequestBody ConsumptionItemDTO dto,
            @RequestParam Long userId) {
        try {
            ConsumptionItem item = new ConsumptionItem(userId, dto.getCategory(), dto.getName(), dto.getPrice());
            item.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
            item.setDescription(dto.getDescription());
            ConsumptionItem created = consumptionItemService.create(item);
            return ResponseEntity.ok(ApiResponse.success("创建消费项成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建消费项失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("更新消费项成功", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新消费项失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("更新状态成功", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新状态失败: " + e.getMessage()));
        }
    }

    /**
     * 删除消费项
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            consumptionItemService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("删除消费项成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("删除消费项失败: " + e.getMessage()));
        }
    }
}
