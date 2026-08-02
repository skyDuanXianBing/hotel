package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.entity.Notification;
import server.demo.entity.Reservation;
import server.demo.entity.StoreUser;
import server.demo.enums.ReservationStatus;
import server.demo.repository.NotificationRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.service.push.PushDispatchService;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import server.demo.i18n.ApiMessages;
@Service
public class OrderNotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(OrderNotificationDispatchService.class);
    private static final String ORDER_NOTIFICATION_TYPE = "ORDER";

    private final NotificationRepository notificationRepository;
    private final StoreUserRepository storeUserRepository;
    private final PushDispatchService pushDispatchService;
    private final Clock clock;

    public OrderNotificationDispatchService(
            NotificationRepository notificationRepository,
            StoreUserRepository storeUserRepository,
            PushDispatchService pushDispatchService,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.storeUserRepository = storeUserRepository;
        this.pushDispatchService = pushDispatchService;
        this.clock = clock;
    }

    public void notifyOrderCreated(Long storeId, Reservation reservation, Long fallbackUserId) {
        dispatch(storeId, reservation, fallbackUserId, OrderEventType.CREATED);
    }

    public void notifyOrderUpdated(Long storeId, Reservation reservation, Long fallbackUserId) {
        dispatch(storeId, reservation, fallbackUserId, OrderEventType.UPDATED);
    }

    public void notifyOrderCancelled(Long storeId, Reservation reservation, Long fallbackUserId) {
        dispatch(storeId, reservation, fallbackUserId, OrderEventType.CANCELLED);
    }

    private void dispatch(Long storeId, Reservation reservation, Long fallbackUserId, OrderEventType eventType) {
        if (storeId == null || reservation == null || eventType == null) {
            return;
        }

        Set<Long> receiverIds = resolveReceiverUserIds(storeId, fallbackUserId);
        if (receiverIds.isEmpty()) {
            return;
        }

        String title = eventType.title();
        String content = buildContent(eventType, reservation);
        LocalDateTime nowUtc = StoreTimeZoneUtil.nowUtc(clock);
        List<Notification> notifications = new ArrayList<>(receiverIds.size());
        for (Long userId : receiverIds) {
            Notification notification = new Notification(userId, ORDER_NOTIFICATION_TYPE, title, content);
            notification.setRelatedId(reservation.getId());
            notification.setCreatedAt(nowUtc);
            notifications.add(notification);
        }

        try {
            notificationRepository.saveAll(notifications);
        } catch (Exception e) {
            logger.error(
                    "Dispatch order notifications failed. storeId={}, reservationId={}, eventType={}, receivers={}, err={}",
                    storeId,
                    reservation.getId(),
                    eventType.name(),
                    receiverIds.size(),
                    e.getMessage(),
                    e
            );
        }

        // 手机推送（App 未打开也弹窗），标题/正文与 App 内订单通知一致（按设备语言渲染）
        try {
            pushDispatchService.dispatchToUsers(
                    receiverIds,
                    PushDispatchService.PushCategory.ORDER,
                    PushDispatchService.PushText.keyed(
                            eventType.titleKey(),
                            eventType.contentKey(),
                            contentArgs(reservation)
                    ),
                    java.util.Map.of(
                            "type", "order",
                            "reservationId", String.valueOf(reservation.getId())
                    )
            );
        } catch (Exception e) {
            logger.warn("Dispatch order push failed. storeId={}, reservationId={}, err={}",
                    storeId, reservation.getId(), e.getMessage());
        }
    }

    private Set<Long> resolveReceiverUserIds(Long storeId, Long fallbackUserId) {
        List<StoreUser> members = storeUserRepository.findActiveUsersByStoreId(storeId);
        Set<Long> receiverIds = new LinkedHashSet<>();
        for (StoreUser member : members) {
            if (member == null || member.getUser() == null || member.getUser().getId() == null) {
                continue;
            }
            receiverIds.add(member.getUser().getId());
        }
        if (receiverIds.isEmpty() && fallbackUserId != null) {
            receiverIds.add(fallbackUserId);
        }
        return receiverIds;
    }

    private Object[] contentArgs(Reservation reservation) {
        String channelName = reservation.getChannel() != null
                ? safeText(reservation.getChannel().getName(), ApiMessages.get("api.t.d0f332397a79"))
                : ApiMessages.get("api.t.d0f332397a79");
        String guestName = safeText(reservation.getGuestName(), ApiMessages.get("api.t.d68b39fa381f"));
        String channelOrderNumber = safeText(
                reservation.getChannelOrderNumber(),
                safeText(reservation.getOrderNumber(), "-")
        );
        return new Object[]{channelName, guestName, channelOrderNumber};
    }

    private String buildContent(OrderEventType eventType, Reservation reservation) {
        return String.format(ApiMessages.get(eventType.contentKey()), contentArgs(reservation));
    }

    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    public OrderEventType resolveOtaEventType(
            boolean isNew,
            String oldGuestName,
            String oldGuestPhone,
            String oldChannelOrderNumber,
            String oldSpecialRequests,
            String oldPricePlan,
            LocalDate oldCheckInDate,
            LocalDate oldCheckOutDate,
            ReservationStatus oldStatus,
            Reservation reservation
    ) {
        if (reservation == null) {
            return null;
        }
        if (isNew) {
            return reservation.getStatus() == ReservationStatus.CANCELLED
                    ? OrderEventType.CANCELLED
                    : OrderEventType.CREATED;
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                && oldStatus != ReservationStatus.CANCELLED) {
            return OrderEventType.CANCELLED;
        }

        boolean changed = !Objects.equals(oldGuestName, reservation.getGuestName())
                || !Objects.equals(oldGuestPhone, reservation.getGuestPhone())
                || !Objects.equals(oldChannelOrderNumber, reservation.getChannelOrderNumber())
                || !Objects.equals(oldSpecialRequests, reservation.getSpecialRequests())
                || !Objects.equals(oldPricePlan, reservation.getPricePlan())
                || !Objects.equals(oldCheckInDate, reservation.getCheckInDate())
                || !Objects.equals(oldCheckOutDate, reservation.getCheckOutDate())
                || !Objects.equals(oldStatus, reservation.getStatus());

        return changed ? OrderEventType.UPDATED : null;
    }

    public enum OrderEventType {
        CREATED("api.t.f4b504a9b2bb", "api.t.2c2418e78214"),
        UPDATED("api.t.0b99a0e5a544", "api.t.62907a62c644"),
        CANCELLED("api.t.24d78a40e64d", "api.t.6d0473de4212");

        private final String titleKey;
        private final String contentKey;

        OrderEventType(String titleKey, String contentKey) {
            this.titleKey = titleKey;
            this.contentKey = contentKey;
        }

        public String titleKey() {
            return titleKey;
        }

        public String contentKey() {
            return contentKey;
        }

        /**
         * 使用时解析，避免类加载期冻结语言环境。
         */
        public String title() {
            return ApiMessages.get(titleKey);
        }
    }
}
