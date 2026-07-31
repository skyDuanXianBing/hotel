package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.entity.NotificationSetting;
import server.demo.service.NotificationSettingService;

import server.demo.i18n.ApiMessages;
/**
 * 通知设置控制器
 */
@RestController
@RequestMapping("/api/v1/notification-settings")
public class NotificationSettingController {

    @Autowired
    private NotificationSettingService notificationSettingService;

    /**
     * 获取用户的通知设置
     * @param userId 用户ID
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<NotificationSetting> getByUserId(@PathVariable Long userId) {
        NotificationSetting setting = notificationSettingService.getOrCreateByUserId(userId);
        return ApiResponse.success(ApiMessages.get("api.t.de24bcb09084"), setting);
    }

    /**
     * 更新用户的通知设置
     * @param userId 用户ID
     * @param settings 通知设置
     */
    @PutMapping("/user/{userId}")
    public ApiResponse<NotificationSetting> updateSettings(
            @PathVariable Long userId,
            @RequestBody NotificationSetting settings) {
        NotificationSetting updatedSetting = notificationSettingService.updateSettings(userId, settings);
        return ApiResponse.success(ApiMessages.get("api.t.102f9e2c1e38"), updatedSetting);
    }
}
