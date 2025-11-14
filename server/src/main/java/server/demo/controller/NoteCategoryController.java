package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.entity.NoteCategory;
import server.demo.service.NoteCategoryService;

import java.util.List;

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
            return ApiResponse.success("获取分类列表成功", categories);
        } catch (Exception e) {
            return ApiResponse.error("获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据类型获取分类
     */
    @GetMapping("/type/{type}")
    public ApiResponse<List<NoteCategory>> getCategoriesByType(@PathVariable String type) {
        try {
            List<NoteCategory> categories = noteCategoryService.getCategoriesByType(type);
            return ApiResponse.success("获取分类列表成功", categories);
        } catch (Exception e) {
            return ApiResponse.error("获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建分类
     */
    @PostMapping
    public ApiResponse<NoteCategory> createCategory(@RequestBody NoteCategory category) {
        try {
            NoteCategory created = noteCategoryService.createCategory(category);
            return ApiResponse.success("创建分类成功", created);
        } catch (Exception e) {
            return ApiResponse.error("创建分类失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建分类
     */
    @PostMapping("/batch")
    public ApiResponse<List<NoteCategory>> createCategories(@RequestBody List<NoteCategory> categories) {
        try {
            List<NoteCategory> created = noteCategoryService.createCategories(categories);
            return ApiResponse.success("批量创建分类成功", created);
        } catch (Exception e) {
            return ApiResponse.error("批量创建分类失败: " + e.getMessage());
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
            return ApiResponse.success("更新分类成功", updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("更新分类失败: " + e.getMessage());
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        try {
            noteCategoryService.deleteCategory(id);
            return ApiResponse.success("删除分类成功", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("删除分类失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新分类排序
     */
    @PutMapping("/order")
    public ApiResponse<List<NoteCategory>> updateCategoriesOrder(@RequestBody List<NoteCategory> categories) {
        try {
            List<NoteCategory> updated = noteCategoryService.updateCategoriesOrder(categories);
            return ApiResponse.success("更新排序成功", updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("更新排序失败: " + e.getMessage());
        }
    }
}
