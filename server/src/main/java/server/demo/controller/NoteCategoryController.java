package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.entity.NoteCategory;
import server.demo.service.NoteCategoryService;

import java.util.List;

import server.demo.i18n.ApiMessages;
/**
 * 记一笔分类 Controller（门店级架构）
 */
@RestController
@RequestMapping("/api/v1/note-categories")
@StoreScoped
public class NoteCategoryController {

    @Autowired
    private NoteCategoryService noteCategoryService;

    /**
     * 获取当前门店的所有分类
     */
    @GetMapping
    public ApiResponse<List<NoteCategory>> getAllCategories() {
        try {
            List<NoteCategory> categories = noteCategoryService.getAllCategories();
            return ApiResponse.success(ApiMessages.get("api.t.6925bad9fe14"), categories);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.534df1aa5570") + e.getMessage());
        }
    }

    /**
     * 根据类型获取分类
     */
    @GetMapping("/type/{type}")
    public ApiResponse<List<NoteCategory>> getCategoriesByType(@PathVariable String type) {
        try {
            List<NoteCategory> categories = noteCategoryService.getCategoriesByType(type);
            return ApiResponse.success(ApiMessages.get("api.t.6925bad9fe14"), categories);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.534df1aa5570") + e.getMessage());
        }
    }

    /**
     * 创建分类
     */
    @PostMapping
    public ApiResponse<NoteCategory> createCategory(@RequestBody NoteCategory category) {
        try {
            NoteCategory created = noteCategoryService.createCategory(category);
            return ApiResponse.success(ApiMessages.get("api.t.a39f922247b9"), created);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.9b15f08efe77") + e.getMessage());
        }
    }

    /**
     * 批量创建分类
     */
    @PostMapping("/batch")
    public ApiResponse<List<NoteCategory>> createCategories(@RequestBody List<NoteCategory> categories) {
        try {
            List<NoteCategory> created = noteCategoryService.createCategories(categories);
            return ApiResponse.success(ApiMessages.get("api.t.a9313b20ffce"), created);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.7a06ba273872") + e.getMessage());
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public ApiResponse<NoteCategory> updateCategory(
            @PathVariable Long id,
            @RequestBody NoteCategory category) {
        try {
            NoteCategory updated = noteCategoryService.updateCategory(id, category);
            return ApiResponse.success(ApiMessages.get("api.t.a07c81656f7f"), updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.441bb1711491") + e.getMessage());
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        try {
            noteCategoryService.deleteCategory(id);
            return ApiResponse.success(ApiMessages.get("api.t.9abaa4511085"), null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.0b1d675dbbf7") + e.getMessage());
        }
    }

    /**
     * 批量更新分类排序
     */
    @PutMapping("/order")
    public ApiResponse<List<NoteCategory>> updateCategoriesOrder(@RequestBody List<NoteCategory> categories) {
        try {
            List<NoteCategory> updated = noteCategoryService.updateCategoriesOrder(categories);
            return ApiResponse.success(ApiMessages.get("api.t.a4c955a69e64"), updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.8863b677cc39") + e.getMessage());
        }
    }
}
