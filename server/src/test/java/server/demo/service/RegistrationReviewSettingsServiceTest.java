package server.demo.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.dto.registration.RegistrationReviewSettingsRequest;
import server.demo.dto.registration.RegistrationReviewSettingsResponse;
import server.demo.entity.RegistrationReviewSettings;
import server.demo.exception.BusinessException;
import server.demo.i18n.TestApiMessages;
import server.demo.repository.RegistrationReviewSettingsRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationReviewSettingsServiceTest {

    @Mock
    private RegistrationReviewSettingsRepository settingsRepository;

    @BeforeAll
    static void installMessages() {
        TestApiMessages.install();
    }

    private RegistrationReviewSettingsService createService() {
        return new RegistrationReviewSettingsService(settingsRepository);
    }

    @Test
    void getSettings_shouldReturnDefaultsWhenNotSaved() {
        RegistrationReviewSettingsService service = createService();
        when(settingsRepository.findByStoreId(26L)).thenReturn(Optional.empty());

        RegistrationReviewSettingsResponse response = service.getSettings(26L);

        assertTrue(response.isAutoFinalizeEnabled());
        assertEquals(RegistrationReviewSettings.DEFAULT_LEAD_DAYS, response.getLeadDays());
        assertNotNull(response.getDefaultFinalMessage());
        assertFalse(response.getDefaultFinalMessage().isBlank());
    }

    @Test
    void saveSettings_shouldRejectInvalidLeadDays() {
        RegistrationReviewSettingsService service = createService();
        when(settingsRepository.findByStoreId(26L)).thenReturn(Optional.empty());
        RegistrationReviewSettingsRequest req = new RegistrationReviewSettingsRequest();
        req.setLeadDays(0);

        assertThrows(BusinessException.class, () -> service.saveSettings(26L, req));
    }

    @Test
    void saveSettings_shouldRejectTooLongFinalMessage() {
        RegistrationReviewSettingsService service = createService();
        when(settingsRepository.findByStoreId(26L)).thenReturn(Optional.empty());
        RegistrationReviewSettingsRequest req = new RegistrationReviewSettingsRequest();
        req.setFinalMessage("x".repeat(RegistrationReviewSettings.MAX_FINAL_MESSAGE_LENGTH + 1));

        assertThrows(BusinessException.class, () -> service.saveSettings(26L, req));
    }

    @Test
    void saveSettings_shouldUpsertAndTrimValues() {
        RegistrationReviewSettingsService service = createService();
        when(settingsRepository.findByStoreId(26L)).thenReturn(Optional.empty());
        when(settingsRepository.save(any(RegistrationReviewSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RegistrationReviewSettingsRequest req = new RegistrationReviewSettingsRequest();
        req.setAutoFinalizeEnabled(false);
        req.setLeadDays(6);
        req.setFinalMessage("  终审已通过，入住指南已开放  ");

        RegistrationReviewSettingsResponse response = service.saveSettings(26L, req);

        assertFalse(response.isAutoFinalizeEnabled());
        assertEquals(6, response.getLeadDays());
        assertEquals("终审已通过，入住指南已开放", response.getFinalMessage());
    }

    @Test
    void resolveFinalMessage_shouldFallbackToDefaultWhenBlank() {
        RegistrationReviewSettingsService service = createService();
        RegistrationReviewSettings settings = RegistrationReviewSettings.defaultsFor(26L);

        String resolved = service.resolveFinalMessage(settings);

        assertNotNull(resolved);
        assertFalse(resolved.isBlank());

        settings.setFinalMessage("  自定义终审消息  ");
        assertEquals("自定义终审消息", service.resolveFinalMessage(settings));
    }
}
