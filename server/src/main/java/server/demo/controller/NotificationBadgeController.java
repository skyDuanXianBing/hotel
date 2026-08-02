package server.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.NotificationBadgeSummaryDTO;
import server.demo.i18n.ApiMessages;
import server.demo.service.NotificationBadgeService;
import server.demo.util.StoreContextUtils;

/**
 * App 图标角标汇总（未读聊天 + 待审查表格），供通知中心轮询与推送 badge 对齐。
 */
@RestController
@RequestMapping("/api/v1/notifications")
@StoreScoped
public class NotificationBadgeController {

    private final NotificationBadgeService notificationBadgeService;

    public NotificationBadgeController(NotificationBadgeService notificationBadgeService) {
        this.notificationBadgeService = notificationBadgeService;
    }

    @GetMapping("/badge-summary")
    public ApiResponse<NotificationBadgeSummaryDTO> badgeSummary() {
        NotificationBadgeSummaryDTO summary = notificationBadgeService.summaryFor(
                StoreContextUtils.requireStoreId(),
                StoreContextUtils.requireUserId()
        );
        return ApiResponse.success(ApiMessages.get("api.common.success"), summary);
    }
}
