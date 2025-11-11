package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.NotificationSetting;
import server.demo.repository.NotificationSettingRepository;

import java.util.Optional;

/**
 * 通知设置 Service
 */
@Service
public class NotificationSettingService {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    /**
     * 根据用户ID获取通知设置,如果不存在则创建默认设置
     */
    @Transactional
    public NotificationSetting getOrCreateByUserId(Long userId) {
        return notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationSetting setting = new NotificationSetting();
                    setting.setUserId(userId);
                    setting.setOrderPopup(true);
                    setting.setOrderSound(true);
                    setting.setChatPopup(true);
                    setting.setChatSound(true);
                    return notificationSettingRepository.save(setting);
                });
    }

    /**
     * 更新通知设置
     */
    @Transactional
    public NotificationSetting updateSettings(Long userId, NotificationSetting settings) {
        NotificationSetting existingSetting = getOrCreateByUserId(userId);

        existingSetting.setOrderPopup(settings.getOrderPopup());
        existingSetting.setOrderSound(settings.getOrderSound());
        existingSetting.setChatPopup(settings.getChatPopup());
        existingSetting.setChatSound(settings.getChatSound());

        return notificationSettingRepository.save(existingSetting);
    }

    /**
     * 根据ID获取通知设置
     */
    public Optional<NotificationSetting> getById(Long id) {
        return notificationSettingRepository.findById(id);
    }
}
