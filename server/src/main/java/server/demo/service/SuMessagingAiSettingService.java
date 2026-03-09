package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.SuMessagingAiSettingDTO;
import server.demo.entity.SuMessagingAiSetting;
import server.demo.repository.SuMessagingAiSettingRepository;

@Service
public class SuMessagingAiSettingService {

    private static final boolean DEFAULT_AUTO_REPLY_ENABLED = true;

    private final SuMessagingAiSettingRepository repository;

    public SuMessagingAiSettingService(SuMessagingAiSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SuMessagingAiSettingDTO getOrCreate(Long storeId) {
        return repository.findByStoreId(storeId)
                .map(SuMessagingAiSettingService::toDto)
                .orElseGet(() -> new SuMessagingAiSettingDTO(DEFAULT_AUTO_REPLY_ENABLED));
    }

    @Transactional(readOnly = true)
    public boolean isAutoReplyEnabled(Long storeId) {
        return repository.findByStoreId(storeId)
                .map(SuMessagingAiSetting::getAutoReplyEnabled)
                .orElse(DEFAULT_AUTO_REPLY_ENABLED);
    }

    @Transactional
    public SuMessagingAiSettingDTO update(Long storeId, SuMessagingAiSettingDTO request) {
        SuMessagingAiSetting setting = repository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultSetting(storeId));
        setting.setAutoReplyEnabled(Boolean.TRUE.equals(request.getAutoReplyEnabled()));
        return toDto(repository.save(setting));
    }

    private SuMessagingAiSetting createDefaultSetting(Long storeId) {
        SuMessagingAiSetting setting = new SuMessagingAiSetting();
        setting.setStoreId(storeId);
        setting.setAutoReplyEnabled(DEFAULT_AUTO_REPLY_ENABLED);
        return setting;
    }

    private static SuMessagingAiSettingDTO toDto(SuMessagingAiSetting setting) {
        return new SuMessagingAiSettingDTO(setting.getAutoReplyEnabled());
    }
}
