package server.demo.service.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import server.demo.entity.NotificationSetting;
import server.demo.entity.PushDeviceToken;
import server.demo.entity.StoreUser;
import server.demo.enums.PushPlatform;
import server.demo.repository.NotificationSettingRepository;
import server.demo.repository.PushDeviceTokenRepository;
import server.demo.repository.StoreUserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推送分发：把业务事件（聊天消息、订单变更、住宿者表格提交）发给门店用户的移动设备。
 *
 * <p>iOS 走 APNs；Android 设备在此预留 FCM 发送位（FCM 未配置前仅记录日志）。
 * 接收人遵循 App 内通知设置：聊天类看 chatPopup，订单/任务类看 orderPopup。
 */
@Service
public class PushDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(PushDispatchService.class);

    private static final Set<String> INVALID_TOKEN_REASONS = Set.of(
            "BadDeviceToken", "Unregistered", "DeviceTokenNotForTopic"
    );

    private final StoreUserRepository storeUserRepository;
    private final PushDeviceTokenRepository pushDeviceTokenRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final ApnsPushService apnsPushService;

    public PushDispatchService(StoreUserRepository storeUserRepository,
                               PushDeviceTokenRepository pushDeviceTokenRepository,
                               NotificationSettingRepository notificationSettingRepository,
                               ApnsPushService apnsPushService) {
        this.storeUserRepository = storeUserRepository;
        this.pushDeviceTokenRepository = pushDeviceTokenRepository;
        this.notificationSettingRepository = notificationSettingRepository;
        this.apnsPushService = apnsPushService;
    }

    /**
     * 推送给门店全部在职用户（按通知设置过滤）。
     */
    @Async
    public void dispatchToStoreUsers(Long storeId, PushCategory category, String title, String body,
                                     Map<String, String> customData) {
        if (storeId == null) {
            return;
        }
        List<StoreUser> members = storeUserRepository.findActiveUsersByStoreId(storeId);
        Set<Long> receiverIds = new LinkedHashSet<>();
        for (StoreUser member : members) {
            if (member != null && member.getUser() != null && member.getUser().getId() != null) {
                receiverIds.add(member.getUser().getId());
            }
        }
        dispatchToUsers(receiverIds, category, title, body, customData);
    }

    /**
     * 推送给指定用户集合（按通知设置过滤）。
     */
    @Async
    public void dispatchToUsers(Collection<Long> userIds, PushCategory category, String title, String body,
                                Map<String, String> customData) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        List<Long> candidateIds = new ArrayList<>(new LinkedHashSet<>(userIds));
        List<Long> allowedIds = filterByNotificationSettings(candidateIds, category);
        if (allowedIds.isEmpty()) {
            return;
        }

        List<PushDeviceToken> tokens = pushDeviceTokenRepository.findByUserIdInAndEnabledTrue(allowedIds);
        if (tokens.isEmpty()) {
            return;
        }

        for (PushDeviceToken token : tokens) {
            if (token.getPlatform() == PushPlatform.IOS) {
                sendViaApns(token, title, body, customData);
            } else if (token.getPlatform() == PushPlatform.ANDROID) {
                // FCM 发送位：配置 Firebase 服务账号后在此接入 FCM HTTP v1 发送
                logger.debug("Skip Android push (FCM not configured yet). tokenId={}", token.getId());
            }
        }
    }

    private void sendViaApns(PushDeviceToken token, String title, String body, Map<String, String> customData) {
        apnsPushService.send(token.getDeviceToken(), title, body, customData)
                .thenAccept(result -> {
                    if (result.status() == ApnsPushService.ApnsSendResult.Status.REJECTED
                            && result.rejectionReason() != null
                            && INVALID_TOKEN_REASONS.contains(result.rejectionReason())) {
                        disableToken(token.getId(), result.rejectionReason());
                    }
                });
    }

    private void disableToken(Long tokenId, String reason) {
        try {
            pushDeviceTokenRepository.findById(tokenId).ifPresent(token -> {
                token.setEnabled(false);
                pushDeviceTokenRepository.save(token);
                logger.info("Disabled push device token id={} due to APNs rejection {}", tokenId, reason);
            });
        } catch (Exception e) {
            logger.warn("Failed to disable push device token id={}: {}", tokenId, e.getMessage());
        }
    }

    /**
     * 按用户通知设置过滤：聊天类看 chatPopup，订单/任务类看 orderPopup；无设置记录时默认允许。
     */
    private List<Long> filterByNotificationSettings(List<Long> userIds, PushCategory category) {
        List<Long> allowed = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            NotificationSetting setting = notificationSettingRepository.findByUserId(userId).orElse(null);
            if (setting == null) {
                allowed.add(userId);
                continue;
            }
            boolean enabled = switch (category) {
                case CHAT -> !Boolean.FALSE.equals(setting.getChatPopup());
                case ORDER, TASK -> !Boolean.FALSE.equals(setting.getOrderPopup());
            };
            if (enabled) {
                allowed.add(userId);
            }
        }
        return allowed;
    }

    public enum PushCategory {
        CHAT,
        ORDER,
        TASK
    }
}
