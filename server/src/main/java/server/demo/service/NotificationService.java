package server.demo.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import server.demo.entity.Notification;
import server.demo.repository.NotificationRepository;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    private LocalDateTime nowUtc() {
        return StoreTimeZoneUtil.nowUtc(clock);
    }

    /**
     * 创建通知
     */
    public Notification createNotification(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(nowUtc());
        }
        return notificationRepository.save(notification);
    }

    /**
     * 分页获取用户的所有通知
     */
    public Page<Notification> getNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 分页获取用户指定类型的通知
     */
    public Page<Notification> getNotificationsByType(Long userId, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
                userId, type, pageable);
    }

    /**
     * 分页获取用户指定类型通知（支持已读状态/关键词筛选）
     */
    public Page<Notification> getNotificationsByTypeWithFilters(
            Long userId,
            String type,
            Boolean isRead,
            String keyword,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return notificationRepository.searchByUserIdAndTypeAndReadStatusAndKeyword(
                userId,
                type,
                isRead,
                normalizedKeyword,
                pageable
        );
    }

    /**
     * 获取未读通知数量
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * 获取指定类型的未读通知数量
     */
    public Long getUnreadCountByType(Long userId, String type) {
        return notificationRepository.countByUserIdAndNotificationTypeAndIsRead(userId, type, false);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));
        notification.setReadAt(nowUtc());
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    /**
     * 标记所有通知为已读
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, nowUtc());
    }

    /**
     * 标记指定类型的所有通知为已读
     */
    @Transactional
    public int markAllAsReadByType(Long userId, String type) {
        return notificationRepository.markAllAsReadByType(userId, type, nowUtc());
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * 删除旧的已读通知
     */
    @Transactional
    public int deleteOldReadNotifications(Long userId, int days) {
        return notificationRepository.deleteOldReadNotifications(userId, nowUtc().minusDays(days));
    }
}
