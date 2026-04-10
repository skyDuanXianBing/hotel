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
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class OrderNotificationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(OrderNotificationDispatchService.class);
    private static final String ORDER_NOTIFICATION_TYPE = "ORDER";

    private final NotificationRepository notificationRepository;
    private final StoreUserRepository storeUserRepository;
    private final Clock clock;

    public OrderNotificationDispatchService(
            NotificationRepository notificationRepository,
            StoreUserRepository storeUserRepository,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.storeUserRepository = storeUserRepository;
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

        String title = eventType.title;
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

    private String buildContent(OrderEventType eventType, Reservation reservation) {
        String channelName = reservation.getChannel() != null
                ? safeText(reservation.getChannel().getName(), "未知渠道")
                : "未知渠道";
        String guestName = safeText(reservation.getGuestName(), "未知客人");
        String channelOrderNumber = safeText(
                reservation.getChannelOrderNumber(),
                safeText(reservation.getOrderNumber(), "-")
        );

        return switch (eventType) {
            case CREATED -> String.format(
                    "%s发来了一个新订单，客人姓名：%s，渠道订单号: %s",
                    channelName,
                    guestName,
                    channelOrderNumber
            );
            case UPDATED -> String.format(
                    "%s修改了%s的订单，渠道订单号: %s",
                    channelName,
                    guestName,
                    channelOrderNumber
            );
            case CANCELLED -> String.format(
                    "%s取消了%s的订单，渠道订单号: %s",
                    channelName,
                    guestName,
                    channelOrderNumber
            );
        };
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
        CREATED("订单创建"),
        UPDATED("订单修改"),
        CANCELLED("订单取消");

        private final String title;

        OrderEventType(String title) {
            this.title = title;
        }
    }
}
