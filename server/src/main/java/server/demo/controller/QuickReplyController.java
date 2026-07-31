package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.entity.QuickReply;
import server.demo.service.QuickReplyService;

import java.util.List;

import server.demo.i18n.ApiMessages;
/**
 * 快捷回复控制器
 */
@RestController
@RequestMapping("/api/v1/quick-replies")
@StoreScoped
public class QuickReplyController {

    @Autowired
    private QuickReplyService quickReplyService;

    /**
     * 获取所有快捷回复
     */
    @GetMapping
    public ApiResponse<List<QuickReply>> getAllQuickReplies() {
        List<QuickReply> quickReplies = quickReplyService.getAllQuickReplies();
        return ApiResponse.success(ApiMessages.get("api.t.1ad8f2e7a29e"), quickReplies);
    }

    /**
     * 根据ID获取快捷回复详情
     */
    @GetMapping("/{id}")
    public ApiResponse<QuickReply> getQuickReplyById(@PathVariable Long id) {
        return quickReplyService.getQuickReplyById(id)
                .map(quickReply -> ApiResponse.success(ApiMessages.get("api.t.87ca2696a5fe"), quickReply))
                .orElse(ApiResponse.error(ApiMessages.get("api.t.8c2528a4b0b3")));
    }

    /**
     * 创建快捷回复
     */
    @PostMapping
    public ApiResponse<QuickReply> createQuickReply(@RequestBody QuickReply quickReply) {
        QuickReply createdQuickReply = quickReplyService.createQuickReply(quickReply);
        return ApiResponse.success(ApiMessages.get("api.t.0d95279881b9"), createdQuickReply);
    }

    /**
     * 更新快捷回复
     */
    @PutMapping("/{id}")
    public ApiResponse<QuickReply> updateQuickReply(@PathVariable Long id, @RequestBody QuickReply quickReply) {
        try {
            QuickReply updatedQuickReply = quickReplyService.updateQuickReply(id, quickReply);
            return ApiResponse.success(ApiMessages.get("api.t.519a8633e5ad"), updatedQuickReply);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除快捷回复
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteQuickReply(@PathVariable Long id) {
        quickReplyService.deleteQuickReply(id);
        return ApiResponse.success(ApiMessages.get("api.t.09ff7dd1869e"), null);
    }
}
