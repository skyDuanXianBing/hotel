package server.demo.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.AnnouncementDTO;
import server.demo.entity.Announcement;
import server.demo.repository.AnnouncementRepository;
import server.demo.util.StoreContextUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AnnouncementService {
    private static final String DEFAULT_LOCALE = "zh-CN";
    private static final String DEFAULT_TYPE = "GENERAL";
    private static final String DEFAULT_SEVERITY = "INFO";
    private static final int MAX_LOCALE_LENGTH = 20;
    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_TYPE_LENGTH = 30;
    private static final int MAX_SEVERITY_LENGTH = 20;
    private static final int DEFAULT_HOME_LIMIT = 3;
    private static final int MAX_HOME_LIMIT = 20;
    private static final int DEFAULT_SORT_ORDER = 0;

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDTO> listHomeAnnouncements(String locale, Integer limit) {
        Long storeId = StoreContextUtils.requireStoreId();
        String normalizedLocale = normalizeLocale(locale);
        int normalizedLimit = normalizeLimit(limit);

        return announcementRepository.findHomeAnnouncements(
                        storeId,
                        normalizedLocale,
                        LocalDateTime.now(),
                        Announcement.SCOPE_GLOBAL,
                        Announcement.SCOPE_STORE,
                        PageRequest.of(0, normalizedLimit)
                ).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDTO> listStoreAnnouncements() {
        Long storeId = StoreContextUtils.requireStoreId();

        return announcementRepository.findStoreManagementAnnouncements(
                        storeId,
                        Announcement.SCOPE_STORE
                ).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public AnnouncementDTO createStoreAnnouncement(AnnouncementDTO request) {
        Long storeId = StoreContextUtils.requireStoreId();
        Announcement announcement = new Announcement();
        announcement.setScope(Announcement.SCOPE_STORE);
        announcement.setStoreId(storeId);
        applyStoreManagedFields(announcement, request, true);

        return toDTO(announcementRepository.save(announcement));
    }

    @Transactional
    public AnnouncementDTO updateStoreAnnouncement(Long id, AnnouncementDTO request) {
        Long storeId = StoreContextUtils.requireStoreId();
        Announcement announcement = findOwnedStoreAnnouncement(id, storeId);
        announcement.setScope(Announcement.SCOPE_STORE);
        announcement.setStoreId(storeId);
        applyStoreManagedFields(announcement, request, false);

        return toDTO(announcementRepository.save(announcement));
    }

    @Transactional
    public AnnouncementDTO disableStoreAnnouncement(Long id) {
        Long storeId = StoreContextUtils.requireStoreId();
        Announcement announcement = findOwnedStoreAnnouncement(id, storeId);
        announcement.setActive(false);

        return toDTO(announcementRepository.save(announcement));
    }

    private AnnouncementDTO toDTO(Announcement announcement) {
        AnnouncementDTO dto = new AnnouncementDTO();
        dto.setId(announcement.getId());
        dto.setScope(announcement.getScope());
        dto.setStoreId(announcement.getStoreId());
        dto.setLocale(announcement.getLocale());
        dto.setTitle(announcement.getTitle());
        dto.setContent(announcement.getContent());
        dto.setType(announcement.getType());
        dto.setSeverity(announcement.getSeverity());
        dto.setActive(announcement.getActive());
        dto.setSortOrder(announcement.getSortOrder());
        dto.setStartsAt(announcement.getStartsAt());
        dto.setEndsAt(announcement.getEndsAt());
        dto.setCreatedAt(announcement.getCreatedAt());
        dto.setUpdatedAt(announcement.getUpdatedAt());
        return dto;
    }

    private Announcement findOwnedStoreAnnouncement(Long id, Long storeId) {
        if (id == null) {
            throw new IllegalArgumentException("公告ID不能为空");
        }

        return announcementRepository
                .findByIdAndScopeAndStoreId(id, Announcement.SCOPE_STORE, storeId)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在或不属于当前门店"));
    }

    private void applyStoreManagedFields(
            Announcement announcement,
            AnnouncementDTO request,
            boolean isCreate
    ) {
        if (request == null) {
            throw new IllegalArgumentException("公告内容不能为空");
        }

        announcement.setTitle(normalizeRequiredText(request.getTitle(), "公告标题", MAX_TITLE_LENGTH));
        announcement.setContent(normalizeRequiredText(request.getContent(), "公告内容", null));
        announcement.setLocale(normalizeStoredLocale(request.getLocale()));
        announcement.setType(normalizeCode(request.getType(), DEFAULT_TYPE, MAX_TYPE_LENGTH, "公告类型"));
        announcement.setSeverity(normalizeCode(
                request.getSeverity(),
                DEFAULT_SEVERITY,
                MAX_SEVERITY_LENGTH,
                "公告级别"
        ));
        announcement.setSortOrder(request.getSortOrder() == null ? DEFAULT_SORT_ORDER : request.getSortOrder());
        if (isCreate) {
            announcement.setActive(request.getActive() == null || request.getActive());
        } else if (request.getActive() != null) {
            announcement.setActive(request.getActive());
        }
        announcement.setStartsAt(request.getStartsAt());
        announcement.setEndsAt(request.getEndsAt());
        validateSchedule(announcement.getStartsAt(), announcement.getEndsAt());
    }

    private static String normalizeRequiredText(String value, String fieldLabel, Integer maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + "不能为空");
        }

        String normalized = value.trim();
        if (maxLength != null && normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private static String normalizeStoredLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return null;
        }

        String normalized = locale.trim();
        if (normalized.length() > MAX_LOCALE_LENGTH) {
            throw new IllegalArgumentException("语言标识不能超过" + MAX_LOCALE_LENGTH + "个字符");
        }
        return normalized;
    }

    private static String normalizeCode(
            String value,
            String defaultValue,
            int maxLength,
            String fieldLabel
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private static void validateSchedule(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && startsAt.isAfter(endsAt)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String normalized = locale.trim();
        if (normalized.length() > 20) {
            return normalized.substring(0, 20);
        }
        return normalized;
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_HOME_LIMIT;
        }
        return Math.min(limit, MAX_HOME_LIMIT);
    }
}
