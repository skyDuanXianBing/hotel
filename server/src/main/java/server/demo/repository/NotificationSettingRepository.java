package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.NotificationSetting;

import java.util.Optional;

/**
 * 通知设置 Repository
 */
@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    /**
     * 根据用户ID查找通知设置
     */
    Optional<NotificationSetting> findByUserId(Long userId);
}
