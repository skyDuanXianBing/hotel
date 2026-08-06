package server.demo.service.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import server.demo.entity.NotificationSetting;
import server.demo.entity.PushDeviceToken;
import server.demo.entity.StoreUser;
import server.demo.enums.PushPlatform;
import server.demo.i18n.ApiMessageService;
import server.demo.i18n.AppLocale;
import server.demo.repository.NotificationSettingRepository;
import server.demo.repository.PushDeviceTokenRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.service.NotificationBadgeService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 推送分发：把业务事件（聊天消息、订单变更、住宿者表格提交）发给门店用户的移动设备。
 *
 * <p>iOS 走 APNs；Android 设备在此预留 FCM 发送位（FCM 未配置前仅记录日志）。
 * 接收人遵循 App 内通知设置：聊天类看 chatPopup，订单/任务类看 orderPopup。
 *
 * <p>文案按每个设备令牌注册的 locale 渲染（App 内选择的语言）；推送同时携带
 * App 图标角标数（未读聊天 + 待审查表格，按门店与审核权限计算）。
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
    private final ApiMessageService apiMessageService;
    private final NotificationBadgeService notificationBadgeService;

    public PushDispatchService(StoreUserRepository storeUserRepository,
                               PushDeviceTokenRepository pushDeviceTokenRepository,
                               NotificationSettingRepository notificationSettingRepository,
                               ApnsPushService apnsPushService,
                               ApiMessageService apiMessageService,
                               NotificationBadgeService notificationBadgeService) {
        this.storeUserRepository = storeUserRepository;
        this.pushDeviceTokenRepository = pushDeviceTokenRepository;
        this.notificationSettingRepository = notificationSettingRepository;
        this.apnsPushService = apnsPushService;
        this.apiMessageService = apiMessageService;
        this.notificationBadgeService = notificationBadgeService;
    }

    /**
     * 推送给门店全部在职用户（按通知设置过滤）。
     */
    @Async
    public void dispatchToStoreUsers(Long storeId, PushCategory category, PushText text,
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
        dispatchToUsers(receiverIds, category, text, customData);
    }

    /**
     * 推送给指定用户集合（按通知设置过滤，按设备 locale 渲染文案）。
     */
    @Async
    public void dispatchToUsers(Collection<Long> userIds, PushCategory category, PushText text,
                                Map<String, String> customData) {
        if (userIds == null || userIds.isEmpty() || text == null) {
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

        Map<String, List<PushDeviceToken>> tokensByLocale = new LinkedHashMap<>();
        for (PushDeviceToken token : tokens) {
            if (token != null) {
                tokensByLocale.computeIfAbsent(normalizeLocaleKey(token.getLocale()), k -> new ArrayList<>()).add(token);
            }
        }

        // 角标数按（门店, 用户）计算一次，同用户多设备复用
        Map<String, Integer> badgeCache = new HashMap<>();

        for (Map.Entry<String, List<PushDeviceToken>> entry : tokensByLocale.entrySet()) {
            Locale locale = AppLocale.fromTag(entry.getKey());
            if (locale == null) {
                locale = AppLocale.DEFAULT;
            }
            String title = text.resolveTitle(apiMessageService, locale);
            String body = text.resolveBody(apiMessageService, locale);
            for (PushDeviceToken token : entry.getValue()) {
                if (token.getPlatform() == PushPlatform.IOS) {
                    sendViaApns(token, title, body, customData, badgeFor(token, badgeCache));
                } else if (token.getPlatform() == PushPlatform.ANDROID) {
                    // FCM 发送位：配置 Firebase 服务账号后在此接入 FCM HTTP v1 发送
                    logger.debug("Skip Android push (FCM not configured yet). tokenId={}", token.getId());
                }
            }
        }
    }

    private int badgeFor(PushDeviceToken token, Map<String, Integer> badgeCache) {
        String key = token.getStoreId() + ":" + token.getUserId();
        Integer cached = badgeCache.get(key);
        if (cached != null) {
            return cached;
        }
        int badge = 0;
        try {
            badge = (int) Math.min(Integer.MAX_VALUE,
                    notificationBadgeService.summaryFor(token.getStoreId(), token.getUserId()).total());
        } catch (Exception e) {
            logger.warn("Resolve push badge failed. storeId={}, userId={}, err={}",
                    token.getStoreId(), token.getUserId(), e.getMessage());
        }
        badgeCache.put(key, badge);
        return badge;
    }

    private String normalizeLocaleKey(String locale) {
        if (locale == null || locale.isBlank()) {
            return AppLocale.DEFAULT.toLanguageTag();
        }
        return locale.trim();
    }

    private void sendViaApns(PushDeviceToken token, String title, String body, Map<String, String> customData,
                             int badge) {
        apnsPushService.send(token.getDeviceToken(), title, body, customData, badge)
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

    /**
     * 推送文案描述：key（按设备 locale 渲染）与原文（客人名、消息正文等业务数据）可自由组合；
     * 同一段文案 literal 优先于 key。
     */
    public record PushText(String titleKey, String titleLiteral, String bodyKey, String bodyLiteral,
                           Object[] bodyArgs) {

        /**
         * 标题和正文都来自 i18n key。
         */
        public static PushText keyed(String titleKey, String bodyKey, Object... bodyArgs) {
            return new PushText(titleKey, null, bodyKey, null, bodyArgs);
        }

        String resolveTitle(ApiMessageService messages, Locale locale) {
            if (titleLiteral != null) {
                return titleLiteral;
            }
            return titleKey != null ? messages.resolve(locale, titleKey) : "";
        }

        String resolveBody(ApiMessageService messages, Locale locale) {
            if (bodyLiteral != null) {
                return bodyLiteral;
            }
            if (bodyKey == null) {
                return "";
            }
            // 模板与 App 内通知共用，占位符为 String.format 的 %s 风格（非 MessageFormat 的 {0}），
            // 因此先按设备 locale 取模板，再手动 String.format 替换参数。
            String template = messages.resolve(locale, bodyKey);
            if (bodyArgs == null || bodyArgs.length == 0) {
                return template;
            }
            try {
                return String.format(template, bodyArgs);
            } catch (Exception e) {
                return template;
            }
        }
    }
}
