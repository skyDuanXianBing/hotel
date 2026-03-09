package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.dto.SuMessagingAiSettingDTO;
import server.demo.entity.SuMessagingAiSetting;
import server.demo.repository.SuMessagingAiSettingRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuMessagingAiSettingServiceTest {

    @Test
    void getOrCreate_shouldDefaultToEnabled() {
        SuMessagingAiSettingRepository repository = Mockito.mock(SuMessagingAiSettingRepository.class);
        when(repository.findByStoreId(10L)).thenReturn(Optional.empty());

        SuMessagingAiSettingService service = new SuMessagingAiSettingService(repository);
        SuMessagingAiSettingDTO setting = service.getOrCreate(10L);

        assertTrue(setting.getAutoReplyEnabled());
        verify(repository, never()).save(any(SuMessagingAiSetting.class));
    }

    @Test
    void update_shouldPersistNewValue() {
        SuMessagingAiSettingRepository repository = Mockito.mock(SuMessagingAiSettingRepository.class);
        SuMessagingAiSetting existing = new SuMessagingAiSetting();
        existing.setStoreId(10L);
        existing.setAutoReplyEnabled(true);

        when(repository.findByStoreId(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(SuMessagingAiSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        SuMessagingAiSettingService service = new SuMessagingAiSettingService(repository);
        SuMessagingAiSettingDTO updated = service.update(10L, new SuMessagingAiSettingDTO(false));

        assertFalse(updated.getAutoReplyEnabled());
    }

    @Test
    void update_shouldCreateSettingWhenMissing() {
        SuMessagingAiSettingRepository repository = Mockito.mock(SuMessagingAiSettingRepository.class);
        when(repository.findByStoreId(10L)).thenReturn(Optional.empty());
        when(repository.save(any(SuMessagingAiSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        SuMessagingAiSettingService service = new SuMessagingAiSettingService(repository);
        SuMessagingAiSettingDTO updated = service.update(10L, new SuMessagingAiSettingDTO(false));

        assertFalse(updated.getAutoReplyEnabled());
        verify(repository).save(any(SuMessagingAiSetting.class));
    }
}
