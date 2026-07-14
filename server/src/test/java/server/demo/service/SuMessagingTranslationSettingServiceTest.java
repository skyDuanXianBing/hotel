package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.dto.SuMessagingTranslationSettingDTO;
import server.demo.entity.SuMessagingUserSetting;
import server.demo.repository.SuMessagingUserSettingRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingTranslationSettingServiceTest {

    @Test
    void get_shouldReturnDefaultsWithoutPersistingWhenUserHasNoSetting() {
        SuMessagingUserSettingRepository repository = Mockito.mock(SuMessagingUserSettingRepository.class);
        when(repository.findByUserId(91L)).thenReturn(Optional.empty());

        SuMessagingTranslationSettingService service = new SuMessagingTranslationSettingService(repository);
        SuMessagingTranslationSettingDTO result = service.get(91L);

        assertFalse(result.getEnabled());
        assertEquals("zh-CN", result.getTargetLanguage());
        assertFalse(result.getConfigured());
        verify(repository, never()).save(any(SuMessagingUserSetting.class));
    }

    @Test
    void get_shouldReturnConfiguredWhenUserHasExplicitlySavedDefaults() {
        SuMessagingUserSettingRepository repository = Mockito.mock(SuMessagingUserSettingRepository.class);
        when(repository.findByUserId(91L)).thenReturn(Optional.of(existingSetting(91L)));

        SuMessagingTranslationSettingService service = new SuMessagingTranslationSettingService(repository);
        SuMessagingTranslationSettingDTO result = service.get(91L);

        assertFalse(result.getEnabled());
        assertEquals("zh-CN", result.getTargetLanguage());
        assertTrue(result.getConfigured());
    }

    @Test
    void update_shouldCreateAccountScopedSettingForTrustedUserId() {
        SuMessagingUserSettingRepository repository = Mockito.mock(SuMessagingUserSettingRepository.class);
        when(repository.findByUserId(91L)).thenReturn(Optional.empty());
        when(repository.save(any(SuMessagingUserSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuMessagingTranslationSettingService service = new SuMessagingTranslationSettingService(repository);
        SuMessagingTranslationSettingDTO result = service.update(
                91L,
                new SuMessagingTranslationSettingDTO(true, "en")
        );

        assertTrue(result.getEnabled());
        assertEquals("en", result.getTargetLanguage());
        assertTrue(result.getConfigured());

        ArgumentCaptor<SuMessagingUserSetting> settingCaptor = ArgumentCaptor.forClass(SuMessagingUserSetting.class);
        verify(repository).save(settingCaptor.capture());
        assertEquals(91L, settingCaptor.getValue().getUserId());
        assertTrue(settingCaptor.getValue().getTranslationEnabled());
        assertEquals("en", settingCaptor.getValue().getTranslationTargetLanguage());
    }

    @Test
    void update_shouldReturnConfiguredWhenExplicitlySavingDefaultValues() {
        SuMessagingUserSettingRepository repository = Mockito.mock(SuMessagingUserSettingRepository.class);
        when(repository.findByUserId(91L)).thenReturn(Optional.empty());
        when(repository.save(any(SuMessagingUserSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuMessagingTranslationSettingService service = new SuMessagingTranslationSettingService(repository);
        SuMessagingTranslationSettingDTO result = service.update(
                91L,
                new SuMessagingTranslationSettingDTO(false, "zh-CN", false)
        );

        assertFalse(result.getEnabled());
        assertEquals("zh-CN", result.getTargetLanguage());
        assertTrue(result.getConfigured());
    }

    @ParameterizedTest
    @ValueSource(strings = {"zh-CN", "en", "ja", "ko"})
    void update_shouldAcceptOnlySupportedCanonicalLanguageCodes(String targetLanguage) {
        SuMessagingUserSettingRepository repository = Mockito.mock(SuMessagingUserSettingRepository.class);
        SuMessagingUserSetting existing = existingSetting(91L);
        when(repository.findByUserId(91L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        SuMessagingTranslationSettingService service = new SuMessagingTranslationSettingService(repository);
        SuMessagingTranslationSettingDTO result = service.update(
                91L,
                new SuMessagingTranslationSettingDTO(true, targetLanguage)
        );

        assertEquals(targetLanguage, result.getTargetLanguage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"zh", "zh-cn", "EN", "en-US", "ja-JP", "ko-KR", " en", ""})
    void update_shouldRejectUnsupportedOrNonCanonicalLanguageCodes(String targetLanguage) {
        SuMessagingUserSettingRepository repository = Mockito.mock(SuMessagingUserSettingRepository.class);
        SuMessagingTranslationSettingService service = new SuMessagingTranslationSettingService(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.update(91L, new SuMessagingTranslationSettingDTO(true, targetLanguage))
        );
        verify(repository, never()).findByUserId(any());
        verify(repository, never()).save(any(SuMessagingUserSetting.class));
    }

    @Test
    void update_shouldRejectMissingEnabledValue() {
        SuMessagingUserSettingRepository repository = Mockito.mock(SuMessagingUserSettingRepository.class);
        SuMessagingTranslationSettingService service = new SuMessagingTranslationSettingService(repository);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.update(91L, new SuMessagingTranslationSettingDTO(null, "en"))
        );

        assertEquals("翻译开关不能为空", error.getMessage());
        verify(repository, never()).save(any(SuMessagingUserSetting.class));
    }

    private static SuMessagingUserSetting existingSetting(Long userId) {
        SuMessagingUserSetting setting = new SuMessagingUserSetting();
        setting.setUserId(userId);
        setting.setTranslationEnabled(false);
        setting.setTranslationTargetLanguage("zh-CN");
        return setting;
    }
}
