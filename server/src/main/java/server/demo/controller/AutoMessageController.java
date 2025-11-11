package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.entity.AutoMessage;
import server.demo.service.AutoMessageService;

import java.util.List;

/**
 * 自动化消息控制器
 */
@RestController
@RequestMapping("/api/v1/auto-messages")
public class AutoMessageController {

    @Autowired
    private AutoMessageService autoMessageService;

    /**
     * 获取所有自动化消息
     */
    @GetMapping
    public ApiResponse<List<AutoMessage>> getAllAutoMessages() {
        List<AutoMessage> messages = autoMessageService.getAllAutoMessages();
        return ApiResponse.success("获取自动化消息列表成功", messages);
    }

    /**
     * 根据用户ID获取自动化消息列表
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<AutoMessage>> getAutoMessagesByUserId(@PathVariable Long userId) {
        List<AutoMessage> messages = autoMessageService.getAutoMessagesByUserId(userId);
        return ApiResponse.success("获取自动化消息列表成功", messages);
    }

    /**
     * 根据ID获取自动化消息详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AutoMessage> getAutoMessageById(@PathVariable Long id) {
        return autoMessageService.getAutoMessageById(id)
                .map(message -> ApiResponse.success("获取自动化消息详情成功", message))
                .orElse(ApiResponse.error("自动化消息不存在"));
    }

    /**
     * 创建自动化消息
     */
    @PostMapping
    public ApiResponse<AutoMessage> createAutoMessage(@RequestBody AutoMessage autoMessage) {
        AutoMessage createdMessage = autoMessageService.createAutoMessage(autoMessage);
        return ApiResponse.success("创建自动化消息成功", createdMessage);
    }

    /**
     * 更新自动化消息
     */
    @PutMapping("/{id}")
    public ApiResponse<AutoMessage> updateAutoMessage(
            @PathVariable Long id,
            @RequestBody AutoMessage autoMessage) {
        try {
            AutoMessage updatedMessage = autoMessageService.updateAutoMessage(id, autoMessage);
            return ApiResponse.success("更新自动化消息成功", updatedMessage);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除自动化消息
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAutoMessage(@PathVariable Long id) {
        autoMessageService.deleteAutoMessage(id);
        return ApiResponse.success("删除自动化消息成功", null);
    }

    /**
     * 切换自动化消息启用状态
     */
    @PutMapping("/{id}/toggle")
    public ApiResponse<AutoMessage> toggleAutoMessage(@PathVariable Long id) {
        try {
            AutoMessage message = autoMessageService.toggleAutoMessage(id);
            return ApiResponse.success("切换状态成功", message);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
