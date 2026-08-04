package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import server.demo.dto.ManagedOperationDtos;
import server.demo.entity.ManagedOperationSettings;
import server.demo.exception.ManagedOperationValidationException;
import server.demo.repository.ManagedOperationMonthlyDataRepository;
import server.demo.repository.ManagedOperationMonthlyFeeRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
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
    void listProperties_shouldReturnRoomCountsAndStampFlag() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationRoomRepository roomRepository = mock(ManagedOperationRoomRepository.class);
        ManagedOperationSettingsService service = service(repository, mock(ManagedOperationPrivateStampStorage.class),
                mock(ManagedOperationSheetStorage.class), roomRepository,
                mock(ManagedOperationMonthlyDataRepository.class));
        ManagedOperationSettings first = persistedSettings("1/stamp.png");
        ManagedOperationSettings second = completeSettings();
        second.setId(10L);
        second.setStoreId(1L);
        when(repository.findByStoreIdOrderByIdAsc(1L)).thenReturn(List.of(first, second));
        when(roomRepository.countByStoreIdGroupBySettings(1L))
                .thenReturn(java.util.Arrays.<Object[]>asList(new Object[]{9L, 3L}));

        List<ManagedOperationDtos.PropertySummary> summaries = service.listProperties(1L);

        assertEquals(2, summaries.size());
        assertEquals(9L, summaries.get(0).id());
        assertEquals(3, summaries.get(0).roomCount());
        assertTrue(summaries.get(0).hasStamp());
        assertEquals(10L, summaries.get(1).id());
        assertEquals(0, summaries.get(1).roomCount());
        assertFalse(summaries.get(1).hasStamp());
    }

    @Test
    void createProperty_shouldRejectDuplicateNameAndBlankName() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationSettingsService service = service(repository, mock(ManagedOperationPrivateStampStorage.class));
        when(repository.existsByStoreIdAndPropertyName(1L, "物业A")).thenReturn(true);

        assertThrows(ManagedOperationValidationException.class,
                () -> service.createProperty(1L, new ManagedOperationDtos.CreatePropertyRequest("物业A")));
        assertThrows(ManagedOperationValidationException.class,
                () -> service.createProperty(1L, new ManagedOperationDtos.CreatePropertyRequest("  ")));
        verify(repository, never()).save(any(ManagedOperationSettings.class));
    }

    @Test
    void createProperty_shouldPersistDefaultsWithNameOnly() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationSettingsService service = service(repository, mock(ManagedOperationPrivateStampStorage.class));
        when(repository.existsByStoreIdAndPropertyName(anyLong(), anyString())).thenReturn(false);
        when(repository.save(any(ManagedOperationSettings.class))).thenAnswer(invocation -> {
            ManagedOperationSettings saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        ManagedOperationDtos.SettingsResponse response =
                service.createProperty(1L, new ManagedOperationDtos.CreatePropertyRequest("新物业"));

        assertTrue(response.persisted());
        assertEquals(11L, response.settings().id());
        assertEquals("新物业", response.settings().propertyName());
        assertEquals(ManagedOperationSettingsService.DEFAULT_INVOICE_ISSUE_DAY,
                response.settings().invoiceIssueDay());
        assertEquals(ManagedOperationSettingsService.DEFAULT_RECEIPT_ISSUE_DAY,
                response.settings().receiptIssueDay());
    }

    @Test
    void getSettings_shouldRequireExistingConfigurationBelongingToStore() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.empty());

        assertThrows(ManagedOperationValidationException.class, () -> service.getSettings(1L, 9L));
        verify(repository, never()).save(any(ManagedOperationSettings.class));

        ManagedOperationSettings persisted = completeSettings();
        persisted.setId(9L);
        persisted.setStoreId(1L);
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(persisted));

        ManagedOperationDtos.SettingsResponse existing = service.getSettings(1L, 9L);

        assertTrue(existing.persisted());
        assertEquals(9L, existing.settings().id());
    }

    @Test
    void saveSettings_shouldReturnPersistedTrue() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        ManagedOperationSettings existing = completeSettings();
        existing.setId(9L);
        existing.setStoreId(1L);
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ManagedOperationSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ManagedOperationDtos.SettingsRequest request = new ManagedOperationDtos.SettingsRequest(
                "物业A", List.of(), new BigDecimal("0.10"), new BigDecimal("0.10"),
                new BigDecimal("8000"), new BigDecimal("2000"), 9, 10, "房东公司", "联系人",
                "100-0001", "房东地址", "运营公司", "100-0002", "运营地址",
                "T123", "03-0000-0000", "issuer@example.test", "测试银行", "本店",
                "普通", "1234567", "ウンエイ");

        ManagedOperationDtos.SettingsResponse response = service.saveSettings(1L, 9L, request);

        assertTrue(response.persisted());
    }

    @Test
    void saveSettings_shouldRejectDuplicateNameFromAnotherConfiguration() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationSettingsService service = service(repository, mock(ManagedOperationPrivateStampStorage.class));
        ManagedOperationSettings existing = completeSettings();
        existing.setId(9L);
        existing.setStoreId(1L);
        existing.setPropertyName("物业B");
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(existing));
        when(repository.existsByStoreIdAndPropertyName(1L, "物业A")).thenReturn(true);
        ManagedOperationDtos.SettingsRequest request = new ManagedOperationDtos.SettingsRequest(
                "物业A", List.of(), new BigDecimal("0.10"), new BigDecimal("0.10"),
                new BigDecimal("8000"), new BigDecimal("2000"), 9, 10, "房东公司", "联系人",
                "100-0001", "房东地址", "运营公司", "100-0002", "运营地址",
                "T123", "03-0000-0000", "issuer@example.test", "测试银行", "本店",
                "普通", "1234567", "ウンエイ");

        assertThrows(ManagedOperationValidationException.class, () -> service.saveSettings(1L, 9L, request));
        verify(repository, never()).save(any(ManagedOperationSettings.class));
    }

    @Test
    void updateIssueDay_shouldValidateRangeAndPersist() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationSettingsService service = service(repository, mock(ManagedOperationPrivateStampStorage.class));
        ManagedOperationSettings existing = completeSettings();
        existing.setId(9L);
        existing.setStoreId(1L);
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ManagedOperationSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ManagedOperationValidationException.class,
                () -> service.updateIssueDay(1L, 9L, new ManagedOperationDtos.IssueDayRequest(0, 10)));
        assertThrows(ManagedOperationValidationException.class,
                () -> service.updateIssueDay(1L, 9L, new ManagedOperationDtos.IssueDayRequest(9, 29)));
        assertThrows(ManagedOperationValidationException.class,
                () -> service.updateIssueDay(1L, 9L, new ManagedOperationDtos.IssueDayRequest(9, null)));

        ManagedOperationDtos.SettingsResponse response =
                service.updateIssueDay(1L, 9L, new ManagedOperationDtos.IssueDayRequest(25, 26));

        assertEquals(25, response.settings().invoiceIssueDay());
        assertEquals(26, response.settings().receiptIssueDay());
    }

    @Test
    void deleteProperty_shouldCleanFilesOnlyAfterCommit() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage stampStorage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSheetStorage sheetStorage = mock(ManagedOperationSheetStorage.class);
        ManagedOperationMonthlyDataRepository monthlyRepository = mock(ManagedOperationMonthlyDataRepository.class);
        ManagedOperationSettingsService service = service(repository, stampStorage, sheetStorage,
                mock(ManagedOperationRoomRepository.class), monthlyRepository);
        ManagedOperationSettings settings = persistedSettings("1/stamp.png");
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(settings));
        when(monthlyRepository.findByStoreIdAndSettingsId(1L, 9L)).thenReturn(List.of());
        TransactionSynchronizationManager.initSynchronization();

        service.deleteProperty(1L, 9L);

        verify(repository, times(1)).delete(settings);
        verify(stampStorage, never()).deleteQuietly(1L, "1/stamp.png");

        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(stampStorage, times(1)).deleteQuietly(1L, "1/stamp.png");
    }

    @Test
    void uploadStamp_shouldRequirePersistedValidConfigurationBeforeWritingFile() {
        ManagedOperationSettingsRepository repository = mock(ManagedOperationSettingsRepository.class);
        ManagedOperationPrivateStampStorage storage = mock(ManagedOperationPrivateStampStorage.class);
        ManagedOperationSettingsService service = service(repository, storage);
        MultipartFile file = mock(MultipartFile.class);

        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.empty());
        assertThrows(ManagedOperationValidationException.class, () -> service.uploadStamp(1L, 9L, file));

        ManagedOperationSettings invalid = new ManagedOperationSettings();
        invalid.setId(9L);
        invalid.setStoreId(1L);
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(invalid));
        assertThrows(ManagedOperationValidationException.class, () -> service.uploadStamp(1L, 9L, file));

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
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("2/foreign.png");

        assertThrows(ManagedOperationValidationException.class, () -> service.uploadStamp(1L, 9L, file));

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
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("1/new.png");
        when(repository.save(settings)).thenReturn(settings);
        TransactionSynchronizationManager.initSynchronization();

        ManagedOperationDtos.StampResponse response = service.uploadStamp(1L, 9L, file);

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
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("1/new.png");
        when(repository.save(settings)).thenReturn(settings);

        service.uploadStamp(1L, 9L, file);

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
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("1/new.png");
        when(repository.save(settings)).thenReturn(settings);
        TransactionSynchronizationManager.initSynchronization();

        service.uploadStamp(1L, 9L, file);
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
        when(repository.findByStoreIdAndId(1L, 9L)).thenReturn(Optional.of(settings));
        when(storage.store(1L, file)).thenReturn("1/new.png");
        when(repository.save(settings)).thenThrow(new IllegalStateException("database failed"));
        TransactionSynchronizationManager.initSynchronization();

        assertThrows(IllegalStateException.class, () -> service.uploadStamp(1L, 9L, file));

        verify(storage, times(1)).deleteQuietly(1L, "1/new.png");
        verify(storage, never()).deleteQuietly(1L, "1/old.png");
    }

    private static ManagedOperationSettingsService service(
            ManagedOperationSettingsRepository repository,
            ManagedOperationPrivateStampStorage storage) {
        return service(repository, storage, mock(ManagedOperationSheetStorage.class),
                mock(ManagedOperationRoomRepository.class), mock(ManagedOperationMonthlyDataRepository.class));
    }

    private static ManagedOperationSettingsService service(
            ManagedOperationSettingsRepository repository,
            ManagedOperationPrivateStampStorage storage,
            ManagedOperationSheetStorage sheetStorage,
            ManagedOperationRoomRepository managedRoomRepository,
            ManagedOperationMonthlyDataRepository monthlyDataRepository) {
        RoomRepository roomRepository = mock(RoomRepository.class);
        when(managedRoomRepository.findByStoreIdAndSettingsIdWithRoom(anyLong(), anyLong()))
                .thenReturn(List.of());
        when(roomRepository.findByStoreIdWithRoomType(anyLong())).thenReturn(List.of());
        return new ManagedOperationSettingsService(
                repository, managedRoomRepository, monthlyDataRepository,
                mock(ManagedOperationMonthlyFeeRepository.class), roomRepository, storage, sheetStorage);
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
