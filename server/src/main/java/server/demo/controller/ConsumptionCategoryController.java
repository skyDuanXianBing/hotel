package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.ConsumptionCategoryDTO;
import server.demo.entity.ConsumptionCategory;
import server.demo.service.ConsumptionCategoryService;

import java.util.List;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/consumption-categories")
@StoreScoped
public class ConsumptionCategoryController {

    @Autowired
    private ConsumptionCategoryService categoryService;

    /**
     * 获取当前门店的所有分类（门店级架构）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsumptionCategory>>> getAll() {
        try {
            List<ConsumptionCategory> categories = categoryService.getAll();
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.6925bad9fe14"), categories));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.534df1aa5570") + e.getMessage()));
        }
    }

    /**
     * 根据ID获取分类
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsumptionCategory>> getById(@PathVariable Long id) {
        try {
            ConsumptionCategory category = categoryService.getById(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.c467a2d7f0de"), category));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.59145fedbdd0") + e.getMessage()));
        }
    }

    /**
     * 创建分类（storeId由StoreScopedEntityListener自动注入）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConsumptionCategory>> create(
            @Valid @RequestBody ConsumptionCategoryDTO dto) {
        try {
            ConsumptionCategory category = new ConsumptionCategory();
            category.setName(dto.getName());
            category.setDescription(dto.getDescription());
            ConsumptionCategory created = categoryService.create(category);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.a39f922247b9"), created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.9b15f08efe77") + e.getMessage()));
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsumptionCategory>> update(
            @PathVariable Long id,
            @Valid @RequestBody ConsumptionCategoryDTO dto) {
        try {
            ConsumptionCategory updates = new ConsumptionCategory();
            updates.setName(dto.getName());
            updates.setDescription(dto.getDescription());
            ConsumptionCategory updated = categoryService.update(id, updates);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.a07c81656f7f"), updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.441bb1711491") + e.getMessage()));
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            categoryService.delete(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.9abaa4511085"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.0b1d675dbbf7") + e.getMessage()));
        }
    }
}
