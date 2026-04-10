package server.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Notification;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 分页查询用户的所有通知
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 分页查询用户指定类型的通知
     */
    Page<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
            Long userId, String notificationType, Pageable pageable);

    /**
     * 分页查询用户指定类型通知（支持已读状态/关键词筛选）
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.userId = :userId
              AND (:notificationType IS NULL OR n.notificationType = :notificationType)
              AND (:isRead IS NULL OR n.isRead = :isRead)
              AND (
                    :keyword IS NULL
                    OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> searchByUserIdAndTypeAndReadStatusAndKeyword(
            @Param("userId") Long userId,
            @Param("notificationType") String notificationType,
            @Param("isRead") Boolean isRead,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 查询用户未读通知数量
     */
    Long countByUserIdAndIsRead(Long userId, Boolean isRead);

    /**
     * 查询用户指定类型的未读通知数量
     */
    Long countByUserIdAndNotificationTypeAndIsRead(Long userId, String notificationType, Boolean isRead);

    /**
     * 标记所有通知为已读
     */
    @Modifying
        @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
        int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    /**
     * 标记指定类型的通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.notificationType = :type AND n.isRead = false")
    int markAllAsReadByType(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("readAt") LocalDateTime readAt
    );

    /**
     * 删除指定天数之前的已读通知
     */
    @Modifying
        @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.isRead = true AND n.readAt < :threshold")
        int deleteOldReadNotifications(@Param("userId") Long userId, @Param("threshold") LocalDateTime threshold);
}
