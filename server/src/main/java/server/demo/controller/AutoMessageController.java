package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.entity.AutoMessage;
import server.demo.service.AutoMessageService;

import java.util.List;

import server.demo.i18n.ApiMessages;
/**
 * 自动化消息控制器
 */
@RestController
@RequestMapping("/api/v1/auto-messages")
@StoreScoped
public class AutoMessageController {

    @Autowired
    private AutoMessageService autoMessageService;

    /**
     * 获取所有自动化消息
     */
    @GetMapping
    public ApiResponse<List<AutoMessage>> getAllAutoMessages() {
        List<AutoMessage> messages = autoMessageService.getAllAutoMessages();
        return ApiResponse.success(ApiMessages.get("api.t.7294371549c2"), messages);
    }

    /**
     * 根据用户ID获取自动化消息列表
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<AutoMessage>> getAutoMessagesByUserId(@PathVariable Long userId) {
        List<AutoMessage> messages = autoMessageService.getAutoMessagesByUserId(userId);
        return ApiResponse.success(ApiMessages.get("api.t.7294371549c2"), messages);
    }

    /**
     * 根据ID获取自动化消息详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AutoMessage> getAutoMessageById(@PathVariable Long id) {
        return autoMessageService.getAutoMessageById(id)
                .map(message -> ApiResponse.success(ApiMessages.get("api.t.16f672f00711"), message))
                .orElse(ApiResponse.error(ApiMessages.get("api.t.00af031c25a4")));
    }

    /**
     * 创建自动化消息
     */
    @PostMapping
    public ApiResponse<AutoMessage> createAutoMessage(@RequestBody AutoMessage autoMessage) {
        AutoMessage createdMessage = autoMessageService.createAutoMessage(autoMessage);
        return ApiResponse.success(ApiMessages.get("api.t.35855e12a2f9"), createdMessage);
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
            return ApiResponse.success(ApiMessages.get("api.t.83184af74bd9"), updatedMessage);
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
        return ApiResponse.success(ApiMessages.get("api.t.2f023edab351"), null);
    }

    /**
     * 切换自动化消息启用状态
     */
    @PutMapping("/{id}/toggle")
    public ApiResponse<AutoMessage> toggleAutoMessage(@PathVariable Long id) {
        try {
            AutoMessage message = autoMessageService.toggleAutoMessage(id);
            return ApiResponse.success(ApiMessages.get("api.t.d35e917cc1d6"), message);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/replay")
    public ApiResponse<Void> replayAutoMessage(@RequestBody ReplayAutoMessageRequest request) {
        try {
            if (request == null) {
                return ApiResponse.error(ApiMessages.get("api.t.420cd3482ef3"));
            }
            autoMessageService.replayAutoMessage(request.getReservationId(), request.getAutoMessageId());
            return ApiResponse.success(ApiMessages.get("api.t.bcfc54303b58"), null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    public static class ReplayAutoMessageRequest {
        private Long reservationId;
        private Long autoMessageId;

        public Long getReservationId() {
            return reservationId;
        }

        public void setReservationId(Long reservationId) {
            this.reservationId = reservationId;
        }

        public Long getAutoMessageId() {
            return autoMessageId;
        }

        public void setAutoMessageId(Long autoMessageId) {
            this.autoMessageId = autoMessageId;
        }
    }
}
