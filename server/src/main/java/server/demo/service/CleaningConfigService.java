package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.CleaningConfig;
import server.demo.repository.CleaningConfigRepository;

import java.util.List;
import java.util.Optional;

/**
 * 保洁配置 Service
 */
@Service
public class CleaningConfigService {

    @Autowired
    private CleaningConfigRepository cleaningConfigRepository;

    /**
     * 根据用户ID获取保洁配置列表
     */
    public List<CleaningConfig> getConfigsByUserId(Long userId) {
        return cleaningConfigRepository.findByUserId(userId);
    }

    /**
     * 根据用户ID和门店ID获取或创建保洁配置
     */
    @Transactional
    public CleaningConfig getOrCreateConfig(Long userId, Long storeId) {
        return cleaningConfigRepository.findByUserIdAndStoreId(userId, storeId)
                .orElseGet(() -> {
                    CleaningConfig config = new CleaningConfig();
                    config.setUserId(userId);
                    config.setStoreId(storeId);
                    config.setEnabled(true);
                    config.setStayStartTime("12:00");
                    config.setStayEndTime("15:00");
                    config.setCheckoutStartTime("10:00");
                    config.setCheckoutEndTime("16:00");
                    config.setAutoStayTask(false);
                    config.setAutoCheckoutTask(false);
                    return cleaningConfigRepository.save(config);
                });
    }

    /**
     * 根据ID获取保洁配置
     */
    public Optional<CleaningConfig> getConfigById(Long id) {
        return cleaningConfigRepository.findById(id);
    }

    /**
     * 更新保洁配置
     */
    @Transactional
    public CleaningConfig updateConfig(Long id, CleaningConfig config) {
        Optional<CleaningConfig> existingConfig = cleaningConfigRepository.findById(id);
        if (existingConfig.isEmpty()) {
            throw new RuntimeException("保洁配置不存在");
        }

        CleaningConfig cfg = existingConfig.get();
        cfg.setEnabled(config.getEnabled());
        cfg.setStayStartTime(config.getStayStartTime());
        cfg.setStayEndTime(config.getStayEndTime());
        cfg.setCheckoutStartTime(config.getCheckoutStartTime());
        cfg.setCheckoutEndTime(config.getCheckoutEndTime());
        cfg.setAutoStayTask(config.getAutoStayTask());
        cfg.setAutoCheckoutTask(config.getAutoCheckoutTask());

        return cleaningConfigRepository.save(cfg);
    }

    /**
     * 删除保洁配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        cleaningConfigRepository.deleteById(id);
    }
}
