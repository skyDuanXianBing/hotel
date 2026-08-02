package server.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationReviewSettings;
import server.demo.entity.Store;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.StoreRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationAutoFinalizeSchedulerTest {

    @Mock
    private RegistrationFormRepository registrationFormRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private RegistrationReviewSettingsService registrationReviewSettingsService;

    @Mock
    private RegistrationAdminService registrationAdminService;

    private RegistrationAutoFinalizeScheduler createScheduler() {
        return new RegistrationAutoFinalizeScheduler(
                registrationFormRepository,
                storeRepository,
                registrationReviewSettingsService,
                registrationAdminService,
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneId.of("UTC"))
        );
    }

    @Test
    void tick_shouldFinalizeDueFormsForEnabledStore() {
        RegistrationAutoFinalizeScheduler scheduler = createScheduler();
        Store store = new Store();
        store.setId(26L);
        store.setTimezone("Asia/Tokyo");
        when(storeRepository.findAll()).thenReturn(List.of(store));
        RegistrationReviewSettings settings = RegistrationReviewSettings.defaultsFor(26L);
        when(registrationReviewSettingsService.getEffective(26L)).thenReturn(settings);
        RegistrationForm form = new RegistrationForm();
        form.setId(8L);
        // 门店本地今天 = 2026-10-01，leadDays = 7 → threshold = 2026-10-08
        when(registrationFormRepository.findDueReviewedForFinalize(
                eq(26L),
                eq(LocalDate.of(2026, 10, 8)),
                eq(PageRequest.of(0, 100))
        )).thenReturn(List.of(form));

        scheduler.tick();

        verify(registrationAdminService).autoFinalizeForm(26L, 8L, settings);
    }

    @Test
    void tick_shouldSkipStoreWhenAutoFinalizeDisabled() {
        RegistrationAutoFinalizeScheduler scheduler = createScheduler();
        Store store = new Store();
        store.setId(26L);
        when(storeRepository.findAll()).thenReturn(List.of(store));
        RegistrationReviewSettings settings = RegistrationReviewSettings.defaultsFor(26L);
        settings.setAutoFinalizeEnabled(false);
        when(registrationReviewSettingsService.getEffective(26L)).thenReturn(settings);

        scheduler.tick();

        verify(registrationFormRepository, never()).findDueReviewedForFinalize(
                anyLong(), any(LocalDate.class), any());
        verify(registrationAdminService, never()).autoFinalizeForm(anyLong(), anyLong(), any());
    }

    @Test
    void tick_shouldContinueWhenOneStoreFails() {
        RegistrationAutoFinalizeScheduler scheduler = createScheduler();
        Store failingStore = new Store();
        failingStore.setId(1L);
        Store healthyStore = new Store();
        healthyStore.setId(26L);
        healthyStore.setTimezone("Asia/Tokyo");
        when(storeRepository.findAll()).thenReturn(List.of(failingStore, healthyStore));
        when(registrationReviewSettingsService.getEffective(1L))
                .thenThrow(new RuntimeException("db error"));
        RegistrationReviewSettings settings = RegistrationReviewSettings.defaultsFor(26L);
        when(registrationReviewSettingsService.getEffective(26L)).thenReturn(settings);
        RegistrationForm form = new RegistrationForm();
        form.setId(8L);
        when(registrationFormRepository.findDueReviewedForFinalize(
                eq(26L),
                eq(LocalDate.of(2026, 10, 8)),
                eq(PageRequest.of(0, 100))
        )).thenReturn(List.of(form));

        scheduler.tick();

        verify(registrationAdminService).autoFinalizeForm(26L, 8L, settings);
    }
}
