package server.demo.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import server.demo.dto.AnnouncementDTO;
import server.demo.entity.Announcement;
import server.demo.repository.AnnouncementRepository;
import server.demo.util.StoreContextUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnouncementServiceTest {

    private static final Long STORE_ID = 26L;
    private static final Long OTHER_STORE_ID = 99L;

    private final AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
    private final AnnouncementService announcementService = new AnnouncementService(announcementRepository);

    @Test
    void listStoreAnnouncements_shouldUseCurrentStoreAndStoreScope() {
        Announcement announcement = createAnnouncement(8L, STORE_ID);
        when(announcementRepository.findStoreManagementAnnouncements(
                STORE_ID,
                Announcement.SCOPE_STORE
        )).thenReturn(List.of(announcement));

        List<AnnouncementDTO> result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(STORE_ID);

            result = announcementService.listStoreAnnouncements();
        }

        assertEquals(1, result.size());
        assertEquals(8L, result.get(0).getId());
        assertEquals(Announcement.SCOPE_STORE, result.get(0).getScope());
        assertEquals(STORE_ID, result.get(0).getStoreId());
        verify(announcementRepository).findStoreManagementAnnouncements(
                STORE_ID,
                Announcement.SCOPE_STORE
        );
    }

    @Test
    void createStoreAnnouncement_shouldIgnoreRequestScopeAndStoreId() {
        AnnouncementDTO request = new AnnouncementDTO();
        request.setScope(Announcement.SCOPE_GLOBAL);
        request.setStoreId(OTHER_STORE_ID);
        request.setLocale("   ");
        request.setTitle("  Store notice  ");
        request.setContent("  Visible on home  ");
        request.setType("general");
        request.setSeverity("info");
        request.setSortOrder(12);

        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement saved = invocation.getArgument(0);
            saved.setId(8L);
            return saved;
        });

        AnnouncementDTO result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(STORE_ID);

            result = announcementService.createStoreAnnouncement(request);
        }

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        Announcement saved = captor.getValue();
        assertEquals(Announcement.SCOPE_STORE, saved.getScope());
        assertEquals(STORE_ID, saved.getStoreId());
        assertNull(saved.getLocale());
        assertEquals("Store notice", saved.getTitle());
        assertEquals("Visible on home", saved.getContent());
        assertEquals("GENERAL", saved.getType());
        assertEquals("INFO", saved.getSeverity());
        assertTrue(saved.getActive());
        assertEquals(12, saved.getSortOrder());
        assertEquals(8L, result.getId());
    }

    @Test
    void updateStoreAnnouncement_shouldRejectAnnouncementOutsideCurrentStoreScope() {
        AnnouncementDTO request = new AnnouncementDTO();
        request.setTitle("Title");
        request.setContent("Content");

        when(announcementRepository.findByIdAndScopeAndStoreId(
                8L,
                Announcement.SCOPE_STORE,
                STORE_ID
        )).thenReturn(Optional.empty());

        RuntimeException ex;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(STORE_ID);

            ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> announcementService.updateStoreAnnouncement(8L, request)
            );
        }

        assertEquals("公告不存在或不属于当前门店", ex.getMessage());
        verify(announcementRepository, never()).save(any(Announcement.class));
    }

    @Test
    void updateStoreAnnouncement_shouldAllowReEnableAndValidateSchedule() {
        Announcement existing = createAnnouncement(8L, STORE_ID);
        existing.setActive(false);
        AnnouncementDTO request = new AnnouncementDTO();
        request.setTitle("Updated");
        request.setContent("Updated content");
        request.setActive(true);
        request.setStartsAt(LocalDateTime.of(2026, 6, 1, 12, 0));
        request.setEndsAt(LocalDateTime.of(2026, 6, 2, 12, 0));

        when(announcementRepository.findByIdAndScopeAndStoreId(
                8L,
                Announcement.SCOPE_STORE,
                STORE_ID
        )).thenReturn(Optional.of(existing));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnnouncementDTO result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(STORE_ID);

            result = announcementService.updateStoreAnnouncement(8L, request);
        }

        assertTrue(result.getActive());
        assertEquals("Updated", result.getTitle());
        assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), result.getStartsAt());
        assertEquals(LocalDateTime.of(2026, 6, 2, 12, 0), result.getEndsAt());
    }

    @Test
    void updateStoreAnnouncement_shouldRejectEndBeforeStart() {
        Announcement existing = createAnnouncement(8L, STORE_ID);
        AnnouncementDTO request = new AnnouncementDTO();
        request.setTitle("Updated");
        request.setContent("Updated content");
        request.setStartsAt(LocalDateTime.of(2026, 6, 2, 12, 0));
        request.setEndsAt(LocalDateTime.of(2026, 6, 1, 12, 0));

        when(announcementRepository.findByIdAndScopeAndStoreId(
                8L,
                Announcement.SCOPE_STORE,
                STORE_ID
        )).thenReturn(Optional.of(existing));

        RuntimeException ex;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(STORE_ID);

            ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> announcementService.updateStoreAnnouncement(8L, request)
            );
        }

        assertEquals("开始时间不能晚于结束时间", ex.getMessage());
        verify(announcementRepository, never()).save(any(Announcement.class));
    }

    @Test
    void disableStoreAnnouncement_shouldSoftDeleteBySettingActiveFalse() {
        Announcement existing = createAnnouncement(8L, STORE_ID);
        when(announcementRepository.findByIdAndScopeAndStoreId(
                8L,
                Announcement.SCOPE_STORE,
                STORE_ID
        )).thenReturn(Optional.of(existing));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnnouncementDTO result;
        try (MockedStatic<StoreContextUtils> storeContextUtils = mockStatic(StoreContextUtils.class)) {
            storeContextUtils.when(StoreContextUtils::requireStoreId).thenReturn(STORE_ID);

            result = announcementService.disableStoreAnnouncement(8L);
        }

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        verify(announcementRepository, never()).delete(any(Announcement.class));
        assertFalse(captor.getValue().getActive());
        assertFalse(result.getActive());
    }

    private static Announcement createAnnouncement(Long id, Long storeId) {
        Announcement announcement = new Announcement();
        announcement.setId(id);
        announcement.setScope(Announcement.SCOPE_STORE);
        announcement.setStoreId(storeId);
        announcement.setLocale("zh-CN");
        announcement.setTitle("Notice");
        announcement.setContent("Content");
        announcement.setType("GENERAL");
        announcement.setSeverity("INFO");
        announcement.setActive(true);
        announcement.setSortOrder(0);
        return announcement;
    }
}
