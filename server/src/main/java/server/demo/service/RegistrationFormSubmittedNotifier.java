package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.Notification;
import server.demo.entity.RegistrationForm;
import server.demo.entity.Reservation;
import server.demo.entity.StoreUser;
import server.demo.enums.NotificationType;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.i18n.ApiMessages;
import server.demo.repository.NotificationRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.service.push.PushDispatchService;
import server.demo.util.StoreTimeZoneUtil;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 住宿者表格提交后的站内 TASK 通知 + 手机推送。
 *
 * <p>接收人：拥有表格审核入口（STATISTICS/VIEW_STATS，与工作台审核列表一致）的在职用户；
 * 若无人具备该权限则回退给全部在职用户，保证提交不被漏看。
 */
@Service
public class RegistrationFormSubmittedNotifier {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationFormSubmittedNotifier.class);

    private static final String TITLE_KEY = "api.t.3e12ce3ea24a";
    private static final String CONTENT_KEY = "api.t.cef9db967247";
    private static final String GUEST_FALLBACK_KEY = "api.t.2d505c05d710";

    private final NotificationRepository notificationRepository;
    private final StoreUserRepository storeUserRepository;
    private final PermissionService permissionService;
    private final PushDispatchService pushDispatchService;
    private final Clock clock;

    public RegistrationFormSubmittedNotifier(NotificationRepository notificationRepository,
                                             StoreUserRepository storeUserRepository,
                                             PermissionService permissionService,
                                             PushDispatchService pushDispatchService,
                                             Clock clock) {
        this.notificationRepository = notificationRepository;
        this.storeUserRepository = storeUserRepository;
        this.permissionService = permissionService;
        this.pushDispatchService = pushDispatchService;
        this.clock = clock;
    }

    public void notifySubmitted(Long storeId, RegistrationForm form, Reservation reservation) {
        if (storeId == null || form == null || reservation == null) {
            return;
        }

        Set<Long> receiverIds = resolveReceiverUserIds(storeId);
        if (receiverIds.isEmpty()) {
            return;
        }

        String guestName = safeText(reservation.getGuestName(), ApiMessages.get(GUEST_FALLBACK_KEY));
        String orderNumber = safeText(reservation.getOrderNumber(), "-");
        String title = ApiMessages.get(TITLE_KEY);
        String content = String.format(ApiMessages.get(CONTENT_KEY), guestName, orderNumber);

        LocalDateTime nowUtc = StoreTimeZoneUtil.nowUtc(clock);
        List<Notification> notifications = new ArrayList<>(receiverIds.size());
        for (Long userId : receiverIds) {
            Notification notification = new Notification(userId, NotificationType.TASK.name(), title, content);
            notification.setRelatedId(form.getId());
            notification.setCreatedAt(nowUtc);
            notifications.add(notification);
        }

        try {
            notificationRepository.saveAll(notifications);
        } catch (Exception e) {
            logger.error("Save registration-submitted notifications failed. storeId={}, formId={}, err={}",
                    storeId, form.getId(), e.getMessage(), e);
        }

        // 推送在事务提交后触发，避免事务回滚却发出推送；文案按设备语言渲染
        Runnable pushTask = () -> {
            try {
                pushDispatchService.dispatchToUsers(
                        receiverIds,
                        PushDispatchService.PushCategory.TASK,
                        PushDispatchService.PushText.keyed(TITLE_KEY, CONTENT_KEY, guestName, orderNumber),
                        Map.of(
                                "type", "task",
                                "formId", String.valueOf(form.getId()),
                                "orderNumber", orderNumber
                        )
                );
            } catch (Exception e) {
                logger.warn("Dispatch registration-submitted push failed. storeId={}, formId={}, err={}",
                        storeId, form.getId(), e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pushTask.run();
                }
            });
        } else {
            pushTask.run();
        }
    }

    private Set<Long> resolveReceiverUserIds(Long storeId) {
        List<StoreUser> members = storeUserRepository.findActiveUsersByStoreId(storeId);
        Set<Long> allIds = new LinkedHashSet<>();
        for (StoreUser member : members) {
            if (member != null && member.getUser() != null && member.getUser().getId() != null) {
                allIds.add(member.getUser().getId());
            }
        }

        Set<Long> reviewerIds = new LinkedHashSet<>();
        for (Long userId : allIds) {
            try {
                if (permissionService.hasPermission(storeId, userId, PermissionModule.STATISTICS, PermissionAction.VIEW_STATS)) {
                    reviewerIds.add(userId);
                }
            } catch (Exception e) {
                logger.warn("Resolve registration reviewer permission failed. storeId={}, userId={}, err={}",
                        storeId, userId, e.getMessage());
            }
        }
        return reviewerIds.isEmpty() ? allIds : reviewerIds;
    }

    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
