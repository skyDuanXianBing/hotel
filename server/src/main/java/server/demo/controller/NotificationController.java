package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.NotificationDTO;
import server.demo.entity.Notification;
import server.demo.service.NotificationService;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 分页获取用户的所有通知
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Notification>>> getNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        try {
            Page<Notification> notifications = notificationService.getNotifications(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.8968506af714"), notifications));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.ef888e1ab229") + e.getMessage()));
        }
    }

    /**
     * 分页获取用户指定类型的通知
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<Page<Notification>>> getNotificationsByType(
            @PathVariable String type,
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        try {
            Page<Notification> notifications = notificationService.getNotificationsByTypeWithFilters(
                    userId,
                    type,
                    isRead,
                    keyword,
                    page,
                    size
            );
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.8968506af714"), notifications));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.ef888e1ab229") + e.getMessage()));
        }
    }

    /**
     * 分页获取用户系统消息组通知
     */
    @GetMapping("/groups/system")
    public ResponseEntity<ApiResponse<Page<Notification>>> getSystemGroupNotifications(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        try {
            Page<Notification> notifications = notificationService.getSystemGroupNotifications(
                    userId,
                    isRead,
                    keyword,
                    page,
                    size
            );
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.696c90ae6612"), notifications));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.42a76b232ab9") + e.getMessage()));
        }
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@RequestParam Long userId) {
        try {
            Long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.3d1e1a863976"), count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.4ff1d556167c") + e.getMessage()));
        }
    }

    /**
     * 获取指定类型的未读通知数量
     */
    @GetMapping("/unread-count/{type}")
    public ResponseEntity<ApiResponse<Long>> getUnreadCountByType(
            @PathVariable String type,
            @RequestParam Long userId) {
        try {
            Long count = notificationService.getUnreadCountByType(userId, type);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.3d1e1a863976"), count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.4ff1d556167c") + e.getMessage()));
        }
    }

    /**
     * 获取系统消息组未读通知数量
     */
    @GetMapping("/groups/system/unread-count")
    public ResponseEntity<ApiResponse<Long>> getSystemGroupUnreadCount(@RequestParam Long userId) {
        try {
            Long count = notificationService.getSystemGroupUnreadCount(userId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.009adf275c7b"), count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.59d39492a8bb") + e.getMessage()));
        }
    }

    /**
     * 创建通知
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Notification>> createNotification(
            @RequestParam Long userId,
            @Valid @RequestBody NotificationDTO dto) {
        try {
            Notification notification = new Notification(userId, dto.getNotificationType(), dto.getTitle(), dto.getContent());
            notification.setRelatedId(dto.getRelatedId());
            Notification created = notificationService.createNotification(notification);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.baca6ac438df"), created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.5f2d29100c82") + e.getMessage()));
        }
    }

    /**
     * 标记通知为已读
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Notification>> markAsRead(@PathVariable Long id) {
        try {
            Notification notification = notificationService.markAsRead(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.81645692675d"), notification));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.f02982f77e49") + e.getMessage()));
        }
    }

    /**
     * 标记所有通知为已读
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(@RequestParam Long userId) {
        try {
            int count = notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.791cfbd98033"), count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.5782b962ea84") + e.getMessage()));
        }
    }

    /**
     * 标记指定类型的所有通知为已读
     */
    @PatchMapping("/read-all/{type}")
    public ResponseEntity<ApiResponse<Integer>> markAllAsReadByType(
            @PathVariable String type,
            @RequestParam Long userId) {
        try {
            int count = notificationService.markAllAsReadByType(userId, type);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.791cfbd98033"), count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.5782b962ea84") + e.getMessage()));
        }
    }

    /**
     * 标记系统消息组通知为已读
     */
    @PatchMapping("/groups/system/read-all")
    public ResponseEntity<ApiResponse<Integer>> markSystemGroupAsRead(@RequestParam Long userId) {
        try {
            int count = notificationService.markSystemGroupAsRead(userId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.26a499fc8f65"), count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.06af62218d86") + e.getMessage()));
        }
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.b1578ebfe9e7"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.4d319b470365") + e.getMessage()));
        }
    }
}
