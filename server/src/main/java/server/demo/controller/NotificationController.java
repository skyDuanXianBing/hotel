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
            return ResponseEntity.ok(ApiResponse.success("获取通知列表成功", notifications));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取通知列表失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("获取通知列表成功", notifications));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取通知列表失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("获取系统消息列表成功", notifications));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取系统消息列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@RequestParam Long userId) {
        try {
            Long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(ApiResponse.success("获取未读数量成功", count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取未读数量失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("获取未读数量成功", count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取未读数量失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统消息组未读通知数量
     */
    @GetMapping("/groups/system/unread-count")
    public ResponseEntity<ApiResponse<Long>> getSystemGroupUnreadCount(@RequestParam Long userId) {
        try {
            Long count = notificationService.getSystemGroupUnreadCount(userId);
            return ResponseEntity.ok(ApiResponse.success("获取系统消息未读数量成功", count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取系统消息未读数量失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("创建通知成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建通知失败: " + e.getMessage()));
        }
    }

    /**
     * 标记通知为已读
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Notification>> markAsRead(@PathVariable Long id) {
        try {
            Notification notification = notificationService.markAsRead(id);
            return ResponseEntity.ok(ApiResponse.success("标记已读成功", notification));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("标记已读失败: " + e.getMessage()));
        }
    }

    /**
     * 标记所有通知为已读
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(@RequestParam Long userId) {
        try {
            int count = notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(ApiResponse.success("标记全部已读成功", count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("标记全部已读失败: " + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success("标记全部已读成功", count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("标记全部已读失败: " + e.getMessage()));
        }
    }

    /**
     * 标记系统消息组通知为已读
     */
    @PatchMapping("/groups/system/read-all")
    public ResponseEntity<ApiResponse<Integer>> markSystemGroupAsRead(@RequestParam Long userId) {
        try {
            int count = notificationService.markSystemGroupAsRead(userId);
            return ResponseEntity.ok(ApiResponse.success("标记系统消息全部已读成功", count));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("标记系统消息全部已读失败: " + e.getMessage()));
        }
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.ok(ApiResponse.success("删除通知成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("删除通知失败: " + e.getMessage()));
        }
    }
}
