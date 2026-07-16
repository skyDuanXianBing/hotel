package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.ManagedOperationSettings;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.repository.ManagedOperationRoomRepository;
import server.demo.repository.ManagedOperationSettingsRepository;
import server.demo.repository.RoomRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedOperationSettingsServiceTest {
    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void snapshotValidation_shouldRejectStampOnlyDefaultConfiguration() {
        ManagedOperationSettings settings = new ManagedOperationSettings();
        settings.setStampStorageKey("1/stamp.png");

        assertThrows(ManagedOperationValidationException.class,
                () -> ManagedOperationSettingsService.validateSnapshotSettings(settings));
    }

    @Test
    void snapshotValidation_shouldAcceptCompleteRequiredParties() {
        ManagedOperationSettings settings = new ManagedOperationSettings();
        settings.setPropertyName("物业A");
        settings.setOwnerCompanyName("房东公司");
        settings.setIssuerCompanyName("运营公司");

        assertDoesNotThrow(() -> ManagedOperationSettingsService.validateSnapshotSettings(settings));
    }

    @Test
    void snapshotValidation_shouldAllowZeroFractionButRejectFractionalYen() {
        ManagedOperationSettings settings = completeSettings();
        settings.setCleaningFeeGross(new BigDecimal("8000.00"));
        settings.setRegistrationFeeNet(new BigDecimal("2000.00"));
        assertDoesNotThrow(() -> ManagedOperationSettingsService.validateSnapshotSettings(settings));

        settings.setCleaningFeeGross(new BigDecimal("8000.50"));
        assertThrows(ManagedOperationValidationException.class,
                () -> ManagedOperationSettingsService.validateSnapshotSettings(settings));
    }

    @Test
    void getSettings_shouldReportWhetherConfigurationIsPersistedWithoutImplicitSave() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        when(repository.findByStoreId(1L)).thenReturn(Optional.empty());

        ManagedOperationDtos.SettingsResponse defaults = service.getSettings(1L);

        assertFalse(defaults.persisted());
        verify(repository, never()).save(any(ManagedOperationSettings.class));

        ManagedOperationSettings persisted = completeSettings();
        persisted.setId(9L);
        persisted.setStoreId(1L);
        when(repository.findByStoreId(1L)).thenReturn(Optional.of(persisted));

        ManagedOperationDtos.SettingsResponse existing = service.getSettings(1L);

        assertTrue(existing.persisted());
    }

    @Test
    void saveSettings_shouldReturnPersistedTrue() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        when(repository.findByStoreId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(ManagedOperationSettings.class))).thenAnswer(invocation -> {
            ManagedOperationSettings saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });
        ManagedOperationDtos.SettingsRequest request = new ManagedOperationDtos.SettingsRequest(
                "物业A", List.of(), new BigDecimal("0.10"), new BigDecimal("0.10"),
                new BigDecimal("8000"), new BigDecimal("2000"), "房东公司", "联系人",
                "100-0001", "房东地址", "运营公司", "100-0002", "运营地址",
                "T123", "03-0000-0000", "issuer@example.test", "测试银行", "本店",
                "普通", "1234567", "ウンエイ");

        ManagedOperationDtos.SettingsResponse response = service.saveSettings(1L, request);

        assertTrue(response.persisted());
    }

    @Test
    void uploadStamp_shouldRequirePersistedValidConfigurationBeforeWritingFile() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        MultipartFile file = mock(MultipartFile.class);

        when(repository.findByStoreId(1L)).thenReturn(Optional.empty());
        assertThrows(ManagedOperationValidationException.class, () -> service.uploadStamp(1L, file));

        ManagedOperationSettings invalid = new ManagedOperationSettings();
        invalid.setId(9L);
        invalid.setStoreId(1L);
        when(repository.findByStoreId(1L)).thenReturn(Optional.of(invalid));
        assertThrows(ManagedOperationValidationException.class, () -> service.uploadStamp(1L, file));

        verify(storage, never()).store(anyLong(), any(MultipartFile.class));
        verify(repository, never()).save(any(ManagedOperationSettings.class));
    }

    @Test
    void uploadStamp_shouldRejectStorageKeyFromAnotherStore() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        ManagedOperationSettings settings = persistedSettings("1/old.png");
        MultipartFile file = mock(MultipartFile.class);
        when(repository.findByStoreId(1L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("2/foreign.png");

        assertThrows(ManagedOperationValidationException.class, () -> service.uploadStamp(1L, file));

        verify(repository, never()).save(any(ManagedOperationSettings.class));
        verify(storage, never()).deleteQuietly(2L, "2/foreign.png");
    }

    @Test
    void uploadStamp_shouldDeleteOldFileOnlyAfterCommit() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        ManagedOperationSettings settings = persistedSettings("1/old.png");
        MultipartFile file = mock(MultipartFile.class);
        when(repository.findByStoreId(1L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("1/new.png");
        when(repository.save(settings)).thenReturn(settings);
        TransactionSynchronizationManager.initSynchronization();

        ManagedOperationDtos.StampResponse response = service.uploadStamp(1L, file);

        assertTrue(response.hasStamp());
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        verify(storage, never()).deleteQuietly(1L, "1/old.png");
        verify(storage, never()).deleteQuietly(1L, "1/new.png");

        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }

        verify(storage, times(1)).deleteQuietly(1L, "1/old.png");
        verify(storage, never()).deleteQuietly(1L, "1/new.png");
    }

    @Test
    void uploadStamp_withoutTransactionCallbackShouldNeverDeleteOldFileEarly() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        ManagedOperationSettings settings = persistedSettings("1/old.png");
        MultipartFile file = mock(MultipartFile.class);
        when(repository.findByStoreId(1L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("1/new.png");
        when(repository.save(settings)).thenReturn(settings);

        service.uploadStamp(1L, file);

        verify(storage, never()).deleteQuietly(1L, "1/old.png");
        verify(storage, never()).deleteQuietly(1L, "1/new.png");
    }

    @Test
    void uploadStamp_shouldDeleteNewFileOnRollbackAndPreserveOldFile() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        ManagedOperationSettings settings = persistedSettings("1/old.png");
        MultipartFile file = mock(MultipartFile.class);
        when(repository.findByStoreId(1L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("1/new.png");
        when(repository.save(settings)).thenReturn(settings);
        TransactionSynchronizationManager.initSynchronization();

        service.uploadStamp(1L, file);
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        verify(storage, times(1)).deleteQuietly(1L, "1/new.png");
        verify(storage, never()).deleteQuietly(1L, "1/old.png");
    }

    @Test
    void uploadStamp_shouldDeleteNewFileImmediatelyWhenPersistenceFails() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        ManagedOperationSettings settings = persistedSettings("1/old.png");
        MultipartFile file = mock(MultipartFile.class);
        when(repository.findByStoreId(1L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("1/new.png");
        when(repository.save(settings)).thenThrow(new IllegalStateException("database failed"));
        TransactionSynchronizationManager.initSynchronization();

        assertThrows(IllegalStateException.class, () -> service.uploadStamp(1L, file));

        verify(storage, times(1)).deleteQuietly(1L, "1/new.png");
        verify(storage, never()).deleteQuietly(1L, "1/old.png");
    }

    private static ManagedOperationSettingsService service(
            ManagedOperationSettingsRepository repository,
            ManagedOperationPrivateStampStorage storage) {
        ManagedOperationRoomRepository managedRoomRepository = mock(ManagedOperationRoomRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        when(managedRoomRepository.findByStoreIdAndSettingsIdWithRoom(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(roomRepository.findByStoreIdWithRoomType(anyLong())).thenReturn(List.of());
        return new ManagedOperationSettingsService(
                repository, managedRoomRepository, roomRepository, storage);
    }

    private static ManagedOperationSettings persistedSettings(String stampKey) {
        ManagedOperationSettings settings = completeSettings();
        settings.setId(9L);
        settings.setStoreId(1L);
        settings.setStampStorageKey(stampKey);
        return settings;
    }

    private static ManagedOperationSettings completeSettings() {
        ManagedOperationSettings settings = new ManagedOperationSettings();
        settings.setPropertyName("物业A");
        settings.setOwnerCompanyName("房东公司");
        settings.setIssuerCompanyName("运营公司");
        return settings;
    }
}
