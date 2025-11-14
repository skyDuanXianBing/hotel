package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.entity.QuickReply;
import server.demo.service.QuickReplyService;

import java.util.List;

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
        return ApiResponse.success("获取快捷回复列表成功", quickReplies);
    }

    /**
     * 根据ID获取快捷回复详情
     */
    @GetMapping("/{id}")
    public ApiResponse<QuickReply> getQuickReplyById(@PathVariable Long id) {
        return quickReplyService.getQuickReplyById(id)
                .map(quickReply -> ApiResponse.success("获取快捷回复详情成功", quickReply))
                .orElse(ApiResponse.error("快捷回复不存在"));
    }

    /**
     * 创建快捷回复
     */
    @PostMapping
    public ApiResponse<QuickReply> createQuickReply(@RequestBody QuickReply quickReply) {
        QuickReply createdQuickReply = quickReplyService.createQuickReply(quickReply);
        return ApiResponse.success("创建快捷回复成功", createdQuickReply);
    }

    /**
     * 更新快捷回复
     */
    @PutMapping("/{id}")
    public ApiResponse<QuickReply> updateQuickReply(@PathVariable Long id, @RequestBody QuickReply quickReply) {
        try {
            QuickReply updatedQuickReply = quickReplyService.updateQuickReply(id, quickReply);
            return ApiResponse.success("更新快捷回复成功", updatedQuickReply);
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
        return ApiResponse.success("删除快捷回复成功", null);
    }
}
