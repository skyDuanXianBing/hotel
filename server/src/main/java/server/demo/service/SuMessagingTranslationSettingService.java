package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.SuMessagingTranslationSettingDTO;
import server.demo.entity.SuMessagingUserSetting;
import server.demo.repository.SuMessagingUserSettingRepository;

import java.util.Set;

import server.demo.i18n.ApiMessages;
@Service
public class SuMessagingTranslationSettingService {

    private static final Set<String> SUPPORTED_TARGET_LANGUAGES = Set.of("zh-CN", "zh-TW", "en", "ja");

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
            throw new IllegalArgumentException(ApiMessages.get("api.t.d82109249329"));
        }
    }

    private static void validateRequest(SuMessagingTranslationSettingDTO request) {
        if (request == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.5e5be3eb3c21"));
        }
        if (request.getEnabled() == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.21c6d360e510"));
        }
        if (request.getTargetLanguage() == null || request.getTargetLanguage().isBlank()) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.460ce9217fb8"));
        }
        if (!SUPPORTED_TARGET_LANGUAGES.contains(request.getTargetLanguage())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.703096170de5"));
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
                sanitizeTargetLanguage(setting.getTranslationTargetLanguage()),
                true
        );
    }

    // 历史数据可能存过不再支持的语种（如 ko），读取时回退默认，避免把非法值下发给客户端
    private static String sanitizeTargetLanguage(String targetLanguage) {
        if (targetLanguage == null || !SUPPORTED_TARGET_LANGUAGES.contains(targetLanguage)) {
            return SuMessagingUserSetting.DEFAULT_TRANSLATION_TARGET_LANGUAGE;
        }
        return targetLanguage;
    }
}
