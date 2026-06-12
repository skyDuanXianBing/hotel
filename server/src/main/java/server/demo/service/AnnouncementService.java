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

@Service
public class AnnouncementService {
    private static final String DEFAULT_LOCALE = "zh-CN";
    private static final int DEFAULT_HOME_LIMIT = 3;
    private static final int MAX_HOME_LIMIT = 20;

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
        dto.setSortOrder(announcement.getSortOrder());
        dto.setStartsAt(announcement.getStartsAt());
        dto.setEndsAt(announcement.getEndsAt());
        return dto;
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
