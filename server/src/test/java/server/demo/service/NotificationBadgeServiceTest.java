package server.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.demo.dto.NotificationBadgeSummaryDTO;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.SuMessageRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationBadgeServiceTest {

    @Mock
    private SuMessageRepository suMessageRepository;

    @Mock
    private RegistrationFormRepository registrationFormRepository;

    @Mock
    private PermissionService permissionService;

    private NotificationBadgeService badgeService;

    @BeforeEach
    void setUp() {
        badgeService = new NotificationBadgeService(suMessageRepository, registrationFormRepository, permissionService);
    }

    @Test
    void summaryFor_shouldIncludePendingReviewsWhenUserCanReview() {
        when(suMessageRepository.countUnreadMessagesByStoreId(7L, SuMessagingSenderType.GUEST)).thenReturn(4L);
        when(permissionService.hasPermission(7L, 101L, PermissionModule.STATISTICS, PermissionAction.VIEW_STATS))
                .thenReturn(true);
        when(registrationFormRepository.countHomeByStatus(7L, RegistrationFormStatus.SUBMITTED, null)).thenReturn(3L);

        NotificationBadgeSummaryDTO summary = badgeService.summaryFor(7L, 101L);

        assertEquals(4L, summary.unreadMessages());
        assertEquals(3L, summary.pendingReviews());
        assertEquals(7L, summary.total());
    }

    @Test
    void summaryFor_shouldExcludePendingReviewsWhenUserCannotReview() {
        when(suMessageRepository.countUnreadMessagesByStoreId(7L, SuMessagingSenderType.GUEST)).thenReturn(4L);
        when(permissionService.hasPermission(7L, 102L, PermissionModule.STATISTICS, PermissionAction.VIEW_STATS))
                .thenReturn(false);

        NotificationBadgeSummaryDTO summary = badgeService.summaryFor(7L, 102L);

        assertEquals(4L, summary.unreadMessages());
        assertEquals(0L, summary.pendingReviews());
        assertEquals(4L, summary.total());
    }

    @Test
    void summaryFor_shouldExcludePendingReviewsWhenPermissionCheckFails() {
        when(suMessageRepository.countUnreadMessagesByStoreId(7L, SuMessagingSenderType.GUEST)).thenReturn(2L);
        when(permissionService.hasPermission(7L, 103L, PermissionModule.STATISTICS, PermissionAction.VIEW_STATS))
                .thenThrow(new RuntimeException("db error"));

        NotificationBadgeSummaryDTO summary = badgeService.summaryFor(7L, 103L);

        assertEquals(2L, summary.unreadMessages());
        assertEquals(0L, summary.pendingReviews());
        assertEquals(2L, summary.total());
    }
}
