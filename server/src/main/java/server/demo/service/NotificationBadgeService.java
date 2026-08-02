package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.dto.NotificationBadgeSummaryDTO;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.enums.RegistrationFormStatus;
import server.demo.enums.SuMessagingSenderType;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.SuMessageRepository;

/**
 * App 图标角标统计：未读聊天消息数 + 待审查住宿者表格数。
 *
 * <p>待审查数只对拥有表格审核权限（STATISTICS/VIEW_STATS，与审核列表入口一致）的用户计入，
 * 无权限用户的角标只含未读聊天数。
 */
@Service
public class NotificationBadgeService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationBadgeService.class);

    private final SuMessageRepository suMessageRepository;
    private final RegistrationFormRepository registrationFormRepository;
    private final PermissionService permissionService;

    public NotificationBadgeService(SuMessageRepository suMessageRepository,
                                    RegistrationFormRepository registrationFormRepository,
                                    PermissionService permissionService) {
        this.suMessageRepository = suMessageRepository;
        this.registrationFormRepository = registrationFormRepository;
        this.permissionService = permissionService;
    }

    public long countUnreadMessages(Long storeId) {
        if (storeId == null) {
            return 0;
        }
        return suMessageRepository.countUnreadMessagesByStoreId(storeId, SuMessagingSenderType.GUEST);
    }

    public long countPendingReviews(Long storeId) {
        if (storeId == null) {
            return 0;
        }
        return registrationFormRepository.countHomeByStatus(storeId, RegistrationFormStatus.SUBMITTED, null);
    }

    public boolean canReview(Long storeId, Long userId) {
        if (storeId == null || userId == null) {
            return false;
        }
        try {
            return permissionService.hasPermission(storeId, userId, PermissionModule.STATISTICS, PermissionAction.VIEW_STATS);
        } catch (Exception e) {
            logger.warn("Resolve review permission for badge failed. storeId={}, userId={}, err={}",
                    storeId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * 某用户在某门店下的角标汇总。
     */
    public NotificationBadgeSummaryDTO summaryFor(Long storeId, Long userId) {
        long unreadMessages = countUnreadMessages(storeId);
        long pendingReviews = canReview(storeId, userId) ? countPendingReviews(storeId) : 0;
        return new NotificationBadgeSummaryDTO(unreadMessages, pendingReviews, unreadMessages + pendingReviews);
    }
}
