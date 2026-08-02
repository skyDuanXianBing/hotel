package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.registration.RegistrationReviewSettingsRequest;
import server.demo.dto.registration.RegistrationReviewSettingsResponse;
import server.demo.entity.RegistrationReviewSettings;
import server.demo.exception.BusinessException;
import server.demo.repository.RegistrationReviewSettingsRepository;

import server.demo.i18n.ApiMessages;

/**
 * 登记表两段式审查设置：读取/保存门店级自动终审配置。
 * 未保存过的门店使用默认值（启用、提前 7 天、i18n 默认终审消息）。
 */
@Service
public class RegistrationReviewSettingsService {

    public static final String DEFAULT_FINAL_MESSAGE_KEY = "api.t.6f3a9c1e2b48";
    private static final String LEAD_DAYS_RANGE_MESSAGE_KEY = "api.t.9d2c5e7a1f36";
    private static final String FINAL_MESSAGE_LENGTH_MESSAGE_KEY = "api.t.4b8d1e6c3a59";

    private final RegistrationReviewSettingsRepository settingsRepository;

    public RegistrationReviewSettingsService(RegistrationReviewSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Transactional(readOnly = true)
    public RegistrationReviewSettings getEffective(Long storeId) {
        return settingsRepository.findByStoreId(storeId)
                .orElseGet(() -> RegistrationReviewSettings.defaultsFor(storeId));
    }

    @Transactional(readOnly = true)
    public RegistrationReviewSettingsResponse getSettings(Long storeId) {
        RegistrationReviewSettings settings = getEffective(storeId);
        return toResponse(settings);
    }

    @Transactional
    public RegistrationReviewSettingsResponse saveSettings(Long storeId, RegistrationReviewSettingsRequest req) {
        RegistrationReviewSettings settings = settingsRepository.findByStoreId(storeId)
                .orElseGet(() -> RegistrationReviewSettings.defaultsFor(storeId));

        if (req.getAutoFinalizeEnabled() != null) {
            settings.setAutoFinalizeEnabled(req.getAutoFinalizeEnabled());
        }

        if (req.getLeadDays() != null) {
            int leadDays = req.getLeadDays();
            if (leadDays < RegistrationReviewSettings.MIN_LEAD_DAYS
                    || leadDays > RegistrationReviewSettings.MAX_LEAD_DAYS) {
                throw BusinessException.of(LEAD_DAYS_RANGE_MESSAGE_KEY);
            }
            settings.setLeadDays(leadDays);
        }

        if (req.getFinalMessage() != null) {
            String finalMessage = req.getFinalMessage().trim();
            if (finalMessage.isEmpty()
                    || finalMessage.length() > RegistrationReviewSettings.MAX_FINAL_MESSAGE_LENGTH) {
                throw BusinessException.of(FINAL_MESSAGE_LENGTH_MESSAGE_KEY);
            }
            settings.setFinalMessage(finalMessage);
        }

        settings = settingsRepository.save(settings);
        return toResponse(settings);
    }

    public String resolveFinalMessage(RegistrationReviewSettings settings) {
        String finalMessage = settings != null ? settings.getFinalMessage() : null;
        if (finalMessage == null || finalMessage.isBlank()) {
            return ApiMessages.get(DEFAULT_FINAL_MESSAGE_KEY);
        }
        return finalMessage.trim();
    }

    private RegistrationReviewSettingsResponse toResponse(RegistrationReviewSettings settings) {
        RegistrationReviewSettingsResponse response = new RegistrationReviewSettingsResponse();
        response.setAutoFinalizeEnabled(settings.isAutoFinalizeEnabled());
        response.setLeadDays(settings.effectiveLeadDays());
        response.setFinalMessage(settings.getFinalMessage());
        response.setDefaultFinalMessage(ApiMessages.get(DEFAULT_FINAL_MESSAGE_KEY));
        return response;
    }
}
