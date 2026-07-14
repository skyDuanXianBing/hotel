package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.SuMessagingTranslationSettingDTO;
import server.demo.entity.SuMessagingUserSetting;
import server.demo.repository.SuMessagingUserSettingRepository;

import java.util.Set;

@Service
public class SuMessagingTranslationSettingService {

    private static final Set<String> SUPPORTED_TARGET_LANGUAGES = Set.of("zh-CN", "en", "ja", "ko");

    private final SuMessagingUserSettingRepository repository;

    public SuMessagingTranslationSettingService(SuMessagingUserSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SuMessagingTranslationSettingDTO get(Long userId) {
        requireUserId(userId);
        return repository.findByUserId(userId)
                .map(SuMessagingTranslationSettingService::toDto)
                .orElseGet(SuMessagingTranslationSettingService::defaultSetting);
    }

    @Transactional
    public SuMessagingTranslationSettingDTO update(Long userId, SuMessagingTranslationSettingDTO request) {
        requireUserId(userId);
        validateRequest(request);

        SuMessagingUserSetting setting = repository.findByUserId(userId)
                .orElseGet(() -> createDefaultSetting(userId));
        setting.setTranslationEnabled(request.getEnabled());
        setting.setTranslationTargetLanguage(request.getTargetLanguage());
        return toDto(repository.save(setting));
    }

    private static void requireUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户上下文不能为空");
        }
    }

    private static void validateRequest(SuMessagingTranslationSettingDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("翻译设置不能为空");
        }
        if (request.getEnabled() == null) {
            throw new IllegalArgumentException("翻译开关不能为空");
        }
        if (request.getTargetLanguage() == null || request.getTargetLanguage().isBlank()) {
            throw new IllegalArgumentException("目标语言不能为空");
        }
        if (!SUPPORTED_TARGET_LANGUAGES.contains(request.getTargetLanguage())) {
            throw new IllegalArgumentException("目标语言仅支持 zh-CN、en、ja、ko");
        }
    }

    private static SuMessagingUserSetting createDefaultSetting(Long userId) {
        SuMessagingUserSetting setting = new SuMessagingUserSetting();
        setting.setUserId(userId);
        setting.setTranslationEnabled(SuMessagingUserSetting.DEFAULT_TRANSLATION_ENABLED);
        setting.setTranslationTargetLanguage(SuMessagingUserSetting.DEFAULT_TRANSLATION_TARGET_LANGUAGE);
        return setting;
    }

    private static SuMessagingTranslationSettingDTO defaultSetting() {
        return new SuMessagingTranslationSettingDTO(
                SuMessagingUserSetting.DEFAULT_TRANSLATION_ENABLED,
                SuMessagingUserSetting.DEFAULT_TRANSLATION_TARGET_LANGUAGE,
                false
        );
    }

    private static SuMessagingTranslationSettingDTO toDto(SuMessagingUserSetting setting) {
        return new SuMessagingTranslationSettingDTO(
                Boolean.TRUE.equals(setting.getTranslationEnabled()),
                setting.getTranslationTargetLanguage(),
                true
        );
    }
}
